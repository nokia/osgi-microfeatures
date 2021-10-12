// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.util.httploader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

import com.alcatel.as.http.parser.HttpMessageImpl;
import com.alcatel.as.http.parser.HttpParser;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.metering2.MeteringService;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;

/**
 * Lightweight http loader.
 */
@Component
public class HttpLoader {
	/**
	 * Pattern used to replace a variable present in the POST data: ${couter:name}
	 */
	private final static Pattern VAR_PATTERN = Pattern.compile("\\$\\{counter:(?<key>[^}]*)\\}");

	/**
	 * Key used to find the variable name.
	 */
	private final static String VAR_KEY = "key";

	/**
	 * Our logger.
	 */
	final static Logger _log = Logger.getLogger(HttpLoader.class);

	/**
	 * We need our bundle context when using the metering service
	 */
	@Inject
	BundleContext _bc;

	/**
	 * the executor service
	 */
	@ServiceDependency
	PlatformExecutors _execs;

	/**
	 * The reactor service
	 */
	@ServiceDependency
	ReactorProvider _reactors;

	/**
	 * The metering service
	 */
	@ServiceDependency
	private volatile MeteringService _metering;

	/**
	 * Random used to randomly choose local addresses for sockets
	 */
	Random _rnd = new Random();

	/**
	 * Reactor used to create sockets
	 */
	Reactor _reactor;

	/**
	 * Metrics
	 */
	Meters _meters;

	/**
	 * Succesful messages
	 */
	final AtomicLong _ok = new AtomicLong();

	/**
	 * Failed messages
	 */
	final AtomicLong _ko = new AtomicLong();

	/**
	 * Number of opened sockets
	 */
	final AtomicInteger _cnxs = new AtomicInteger();

	/**
	 * Our Configuration
	 */
	private Config _cnf;
	
	/**
	 * Custo http request headers
	 */
	private Map<String, String> _requestHeaders;
	
	/**
	 * Test start time in millis.
	 */
	private long _startTime;

	Security _security;

	/**
	 * Handle configuration change.
	 */
	@ConfigurationDependency
	void updated(Config cnf) {
		_cnf = cnf;
		if (cnf != null) {
			Map<String, String> headers = cnf.getHeaders();
			if (headers.size() > 0) {
				_requestHeaders = headers;
			}
		}
	}

	/**
	 * Our component is starting.
	 */
	@Start
	void start() {
		_log.warn("Starting http loader");
		_log.warn("max.client.requests=" + _cnf.getMaxClientRequests() + "Methode Type=" + _cnf.getMethodeType()
				+ ", url=" + _cnf.getUrl());

		_meters = new Meters("as.util.httpLoader", "Metrics from Http Loader: ", _metering);
		_meters.init();
		_meters.start(_bc);
		_reactor = _reactors.create("http.client");
		_reactor.start();
		_startTime = System.currentTimeMillis();
		
		try {
			_security = new Security().addProtocol("TLSv1.1", "TLSv1.2")
				.keyStore(new FileInputStream(_cnf.getClientKsPath()))
				.keyStorePassword("password")
				.build();
		} catch (Exception e) {
			_log.error("could not initialize security", e);
		}

		new Thread(this::stat).start();

		for (int i = 0; i < _cnf.getClients(); i++) {
			HttpClient c = new HttpClient();
			if (_cnf.https()) {
				c.connectSecured();
			} else {
				c.connect();
			}
		} 
	}

	/**
	 * Periodically log some statistics
	 */
	public void stat() {
		while (true) {
			try {
				Thread.sleep(1000);
				_log.warn("CNX:" + _cnxs + ", OK:" + _ok.getAndSet(0) + ", KO=" + _ko.getAndSet(0));
				
				if (System.currentTimeMillis() - _startTime > _cnf.getTestDuration() * 1000) {
					return;
				}
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * This class represents a client that is sending requests using its own socket.
	 */
	public class HttpClient implements TcpClientChannelListener {
		/**
		 * Http client socket
		 */
		TcpChannel _chan;

		/**
		 * Http Parser associated with this client socket
		 */
		HttpParser _parser = new HttpParser();

		/**
		 * Number of sent messages over this client socket
		 */
		int _sent;

		/**
		 * local socket address
		 */
		String _from;

		/**
		 * For each connection, we maintain a variable map that is used to replace
		 * variables from POST data. For the moment, we only support counters, but we
		 * may add support for other type of variables, like environment variables,
		 * system properties, etc ...
		 */
		private final Map<String, Object> _vars = new HashMap<>();

		/**
		 * A connection may use its own counter that is incremented each time a new
		 * request is sent.
		 */
		private int _counter;

		HttpClient() {
			_counter = _cnf.getLoopStart();
		}

		public void connect() {
			String[] fromList = _cnf.getFrom();
			_from = fromList[_rnd.nextInt(fromList.length)];
			Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
			opts.put(TcpClientOption.FROM_ADDR, new InetSocketAddress(_from, 0));
			PlatformExecutor exec = _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor());
			opts.put(TcpClientOption.INPUT_EXECUTOR, exec);
			_reactors.tcpConnect(_reactor, new InetSocketAddress(_cnf.getTo(), _cnf.getPort()), HttpClient.this, opts);
		}

		public void connectSecured() {
			try {
				String[] fromList = _cnf.getFrom();
				_from = fromList[_rnd.nextInt(fromList.length)];
				Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
				opts.put(TcpClientOption.SECURITY, _security);
				_reactors.tcpConnect(_reactor, new InetSocketAddress(_cnf.getTo(), _cnf.getPort()), HttpClient.this,
						opts);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void connectionEstablished(TcpChannel chan) {
			_chan = chan;
			_cnxs.incrementAndGet();
			send();
		}

		@Override
		public void connectionFailed(TcpChannel chan, Throwable err) {
			_log.error("connection failed: " + chan + ":" + err.toString());
		}

		@Override
		public void connectionClosed(TcpChannel chan) {
			_cnxs.decrementAndGet();
		}

		void send() {
			try {
				_sent++;

				switch (_cnf.getMethodeType()) {
				case "GET":
					_chan.send(createRequest("GET", false), false);
					_meters.getRequestHttpSendMeterGET().inc(1);
					break;
				case "POST":
					_chan.send(createRequest("POST", true), false);
					_meters.getRequestHttpSendMeterPOST().inc(1);
					break;
				case "PUT":
					_chan.send(createRequest("PUT", true), false);
					_meters.getRequestHttpSendMeterPUT().inc(1);
					break;
				case "DELETE":
					_chan.send(createRequest("DELETE", false), false);
					_meters.getRequestHttpSendMeterDELETE().inc(1);
					break;
				default:
					throw new IllegalArgumentException("Invalid http request method: " + _cnf.getMethodeType());
				}

			} catch (Throwable e) {
				_log.error("http loader error", e);
			}

		}
		/*------- METHODS TO CREATE REQUESTS -------*/

		private byte[] createRequest(String method, boolean includeBody) {
			boolean keepAlive = true;
			if (_cnf.getMaxClientRequests() > 0 && _sent == _cnf.getMaxClientRequests()) {
				keepAlive = false;
			}
			StringBuilder sb = new StringBuilder();
			if (_cnf.proxy()) {
				sb.append(method).append(" http://").append(_cnf.getTo()).append(":").append(_cnf.getPort())
						.append(_cnf.getUrl()).append(" HTTP/1.1\r\n");
				sb.append("Host: ").append(_from).append("\r\n");
				sb.append("Proxy-Connection: keep-alive\r\n");
			} else {
				sb.append(method).append(" ").append(_cnf.getUrl()).append(" HTTP/1.1\r\n");
				sb.append("Host: ").append(_from).append("\r\n");
			}
			if (!keepAlive && _cnf.useConnectionClose()) {
				sb.append("Connection: close\r\n");
			}
			if (_requestHeaders != null) {
				_requestHeaders.entrySet().stream().forEach(hdr -> sb.append(hdr.getKey()).append(": ").append(hdr.getValue()).append("\r\n"));
			}

			if (includeBody) {
				sb.append("Content-Type: ").append(_cnf.getContentType()).append("\r\n");
				String body = getRequestBody();
				sb.append("Content-Length: ").append(body.length()).append("\r\n");
				sb.append("\r\n");
				sb.append(body);
			} else {
				sb.append("\r\n");
			}

			return sb.toString().getBytes();
		}

		private String getRequestBody() {
			String loopVariable = _cnf.getLoopVariable();
			if (loopVariable != null && loopVariable.length() > 0) {
				_counter += _cnf.getLoopIncrement();
				_vars.put(loopVariable, _counter);
				return replaceVars(_cnf.getHtmlBody(), _vars);
			}
			return _cnf.getHtmlBody();
		}

		private String replaceVars(String txt, Map<String, Object> vars) {
			StringBuffer sb = new StringBuffer();
			Matcher m = VAR_PATTERN.matcher(txt);
			while (m.find()) {
				m.appendReplacement(sb, vars.getOrDefault(m.group(VAR_KEY), m.group()).toString());
			}
			m.appendTail(sb);
			return sb.toString();
		}

		/*------------------------------------------*/

		@Override
		public int messageReceived(TcpChannel chan, ByteBuffer data) {
			HttpMessageImpl req;
			while ((req = _parser.parseMessage(data)) != null) {
				req.setHasMore();
				if (req.isLast()) {

					// parsed a full message.
					if (req.getStatus() == 200 || req.getStatus() == 201) {
						_meters.getRequestHttpReceivedOkMeter().inc(1);
						_ok.incrementAndGet();
					} else {
						_meters.getRequestHttpReceivedKoMeter().inc(1);
						_ko.incrementAndGet();
					}

					if (System.currentTimeMillis() - _startTime > (_cnf.getTestDuration() * 1000)) {
						return 0;
					}
					
					if (_cnf.getMaxClientRequests() > 0 && _sent >= _cnf.getMaxClientRequests()) {
						_sent = 0;
						_chan.shutdown();
						if (_cnf.https()) {
							connectSecured();
						} else {
							connect();
						}
					} else {
						send();
					}
				}
			}
			return 0;
		}

		@Override
		public void receiveTimeout(TcpChannel chan) {
		}

		@Override
		public void writeBlocked(TcpChannel chan) {
		}

		@Override
		public void writeUnblocked(TcpChannel chan) {
		}
	}

}
