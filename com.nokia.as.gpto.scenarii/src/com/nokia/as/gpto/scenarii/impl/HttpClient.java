package com.nokia.as.gpto.scenarii.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.alcatel.as.http.parser.HttpMessageImpl;
import com.alcatel.as.http.parser.HttpParser;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.gpto.common.msg.api.Pair;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider.TcpClientOption;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;

public class HttpClient implements TcpClientChannelListener {
	private static Logger LOG = Logger.getLogger(HttpScenario.class);

	private volatile TcpChannel _chan;
	private HttpParser _parser = new HttpParser();
	private ReactorProvider _reactors;
	private Reactor _reactor;
	private PlatformExecutors _execs;
	private PlatformExecutor exec;
	
	private HttpClientConfig config;
	
	private HttpClientMetering meters;
	private final StringBuilder sb;
	
	private AtomicBoolean isClosed = new AtomicBoolean(false);
	private AtomicLong requestCounter = new AtomicLong();
	private volatile CompletableFuture<Void> connectionFuture;
	private volatile CompletableFuture<Void> closeFuture;
	
	public HttpClient(ReactorProvider reactors, Reactor reactor, PlatformExecutors execs, 
			HttpClientConfig config, HttpClientMetering meters ){
		_reactors = reactors;
		_reactor = reactor;
		_execs = execs;
		this.config = config;
		sb = new StringBuilder();
		connectionFuture = new CompletableFuture<Void>();
		closeFuture = new CompletableFuture<Void>();
		this.meters = meters;
	}
	
	public CompletableFuture<Void> connect() {
		if(config.getHttps()) {
			return connectSecured();
		} else {
			return connectUnsecured();
		}
	}
	
	public CompletableFuture<Void> close() {
		if(!isClosed.get()) {
			if(_chan != null) {
				_chan.close();
			}
			isClosed.set(true);
			LOG.debug(this + " should close");
		}
		
		return closeFuture;
	}
	
	public CompletableFuture<Void> connectUnsecured() {
		LOG.debug("attempting unsecured connection");
		
		String _from = config.getFromAddress().getHostAddress();
		String _to = config.getToAddress().getHostAddress();
		int port = config.getPort();
		
		Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
		opts.put(TcpClientOption.FROM_ADDR, new InetSocketAddress(_from, 0));
		exec = _execs.createQueueExecutor(_execs.getProcessingThreadPoolExecutor());
		opts.put(TcpClientOption.INPUT_EXECUTOR, exec);
		
		
		connectionFuture = new CompletableFuture<>();
		_reactors.tcpConnect(_reactor, new InetSocketAddress(_to, port), HttpClient.this, opts);
		return connectionFuture;
	}
	
	public CompletableFuture<Void> connectSecured() {
		LOG.debug("attempting secure connection");

		Security security;
		try {
			security = new Security()
					.addProtocol("TLSv1.1", "TLSv1.2");
			
			
			if(!config.getKsPath().isEmpty()) {
				try {
					FileInputStream fis = new FileInputStream(config.getKsPath());
					security.keyStore(fis).keyStorePassword(config.getKsPassword());
				} catch (Exception e) {
					LOG.warn("failed to read keystore from path "  + config.getKsPath());
				}
			}
			security.build();
			
			
			
			Map<TcpClientOption, Object> opts = new HashMap<TcpClientOption, Object>();
			opts.put(TcpClientOption.SECURITY, security);
			_reactors.tcpConnect(_reactor, new InetSocketAddress(config.getToAddress(), config.getPort()), HttpClient.this, opts);
		} catch (Exception e) {
			LOG.error("failed to init client ", e);
			connectionFuture.completeExceptionally(e);
		}
		return connectionFuture;

	}
	
	@Override
	public void connectionEstablished(TcpChannel chan) {
		_chan = chan;
		connectionFuture.complete(null);
	}

	@Override
	public void connectionFailed(TcpChannel chan, Throwable err) {
		connectionFuture.completeExceptionally(new RuntimeException("connection failed"));
		LOG.debug("connection failed ", err);
	}

	@Override
	public void connectionClosed(TcpChannel chan) {
		LOG.debug(this + " closed");
		closeFuture.complete(null);
	}
	
	CompletableFuture<HttpResponse> execute(HttpRequest req, Map<String, String> template) {
		ByteBuffer buf;
		
		if(template == null) {
			buf = req.toBytes(sb);
		} else {
			buf = req.toBytes(sb, template);
		}
		
		CompletableFuture<HttpResponse> future = new CompletableFuture<>();
		
		if(isClosed.get()) {
			future.completeExceptionally(new IllegalStateException("connection closed"));
			return future;
		}
		
		if(_chan == null) {
			future.completeExceptionally(new IllegalStateException("not connected"));
			return future;
		}
		
		try {
			Long time = System.currentTimeMillis();
			_chan.attach(Pair.of(future, time));
			
			switch (req.getMethod()) {
			case "GET":
				meters.getRequestHttpSendMeterGET().inc(1);
				break;
			case "POST":
				meters.getRequestHttpSendMeterPOST().inc(1);
				break;
			case "PUT":
				meters.getRequestHttpSendMeterPUT().inc(1);
				break;
			case "DELETE":
				meters.getRequestHttpSendMeterDELETE().inc(1);
				break;
			}		
			
			_chan.send(buf, false);
			requestCounter.addAndGet(1);
			sb.setLength(0);

			return future;
		} catch (Throwable e) {
			future.completeExceptionally(e);
			e.printStackTrace();
			return future;
		}

	}

	CompletableFuture<HttpResponse> execute(HttpRequest req) {
		return execute(req, null);
	}
	
	@Override
	public int messageReceived(TcpChannel chan, ByteBuffer data) {
		HttpMessageImpl req;
		Pair<CompletableFuture<HttpResponse>, Long> pair = chan.attachment();
		CompletableFuture<HttpResponse> queryFuture = pair.getLeft();
		Long time = pair.getRight();
		while ((req = _parser.parseMessage(data)) != null) {
			req.setHasMore();
			if (req.isLast()) {
				Long elapsed = System.currentTimeMillis() - time;
				meters.getRequestHttpReceivedMeter().inc(1);
				meters.getResponseHttpCodeMeter(req.getStatus()).inc(1);
				queryFuture.complete(new HttpResponse(req, elapsed));
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
	
	public long getRequestCount() {
		return requestCounter.longValue();
	}
}
