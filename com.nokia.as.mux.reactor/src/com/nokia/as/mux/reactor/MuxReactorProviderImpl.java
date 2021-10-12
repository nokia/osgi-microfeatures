// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.mux.reactor;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.mux.MuxConnection;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;
import alcatel.tess.hometop.gateways.utils.CIString;

public class MuxReactorProviderImpl extends ReactorProvider {

	private final static Logger _logger = Logger.getLogger("as.service.reactor.mux");

	private PlatformExecutors _executors;
	private Map<Long, MuxTcpClientChannel> _pendingTcpClientChannels = new ConcurrentHashMap<>();
	private Map<Integer, MuxTcpClientChannel> _tcpClientChannels = new ConcurrentHashMap<>();
	private Map<CIString, Reactor> _aliases = new ConcurrentHashMap<>();
	private Map<Long, Reactor> _reactors = new ConcurrentHashMap<>();
	private ThreadLocal<Reactor> _reactorThreadLocal = new ThreadLocal<Reactor>();
	private MuxConnection _mux;
	private String _ioh, _toString;
	private ServiceRegistration _reg;

	public MuxReactorProviderImpl(String ioh, MuxConnection mux, PlatformExecutors execs) {
		_ioh = ioh;
		_mux = mux;
		_executors = execs;
		_toString = "MuxReactorProvider[" + _mux.getStackInstance() + "]";
		_logger.debug(this + " : created");
	}

	public String toString() {
		return _toString;
	}

	///////// private / package methods

	MuxConnection muxConnection() {
		return _mux;
	}

	void registerReactor(long id, MuxReactor reactor) {
		_reactors.put(id, reactor);
	}

	void unregisterReactor(long id) {
		_reactors.remove(id);
	}

	MuxReactorProviderImpl start(BundleContext bc) {
		Dictionary<String, Object> props = new Hashtable<String, Object>();
		props.put("type", "mux");
		props.put("ioh", _ioh);
		props.put("instance", _mux.getStackInstance());
		_reg = bc.registerService(ReactorProvider.class.getName(), this, props);
		return this;
	}

	void stop() {
		_reg.unregister();
	}

	void registerTcpConnect(long connectionId, MuxTcpClientChannel channel) {
		_pendingTcpClientChannels.put(connectionId, channel);
	}

	MuxTcpClientChannel unregisterTcpConnect(long connectionId) {
		return _pendingTcpClientChannels.remove(connectionId);
	}

	PlatformExecutors getExecutors() {
		return _executors;
	}

	void closeReactorChannels(MuxReactor reactorImpl, boolean abort) {
	}

	void setReactorThreadLocal(Reactor r) {
		_reactorThreadLocal.set(r);
	}

	private static MuxReactor checkStarted(Reactor r) {
		_logger.debug("Checking Reactor : " + r.toString());
		MuxReactor mr = (MuxReactor) r;
		if (!mr.isStarted()) {
			throw new IllegalStateException("Reactor " + r.getName() + " is not started");
		}
		return mr;
	}

	private static void checkPriority(int priority) {
		switch (priority) {
		case AsyncChannel.MAX_PRIORITY:
		case AsyncChannel.MIN_PRIORITY:
			break;
		default:
			throw new IllegalArgumentException("Invalid priority: " + priority);
		}
	}

	///////////// Mux callbacks

	public void tcpSocketConnected(long connectionId, int sockId, int errno, String remoteIP, int remotePort,
			String localIP, int localPort) {
		MuxTcpClientChannel channel = unregisterTcpConnect(connectionId);
		if (channel == null) {
			if (errno == 0) {
				_logger.warn(this + " : tcpSocketConnected : no pending connection for : " + connectionId
						+ " : closing sockId : " + sockId);
				_mux.sendTcpSocketClose(sockId);
			} else {
				_logger.warn(this + " : tcpSocketConnected : no pending connection for : " + connectionId
						+ " : no need to close : errno : " + errno);
			}
			return;
		}
		if (errno == 0) {
			if (_logger.isInfoEnabled())
				_logger.info(new StringBuilder().append(this).append(" : tcpSocketConnected : Success")
						.append(", connectionId=").append(connectionId).append(", sockId=").append(sockId)
						.append(", remoteIP=").append(remoteIP).append(", remotePort=").append(remotePort)
						.append(", localIP=").append(localIP).append(", localPort=").append(localPort).toString());
			_tcpClientChannels.put(sockId, channel.connected(sockId, remoteIP, remotePort, localIP, localPort));
		} else {
			if (_logger.isInfoEnabled())
				_logger.info(new StringBuilder().append(this).append(" : tcpSocketConnected : Failed : ").append(errno)
						.append(", connectionId=").append(connectionId).append(", remoteIP=").append(remoteIP)
						.append(", remotePort=").append(remotePort).toString());
			channel.failed(new java.io.IOException("Errno=" + errno));
		}
	}

	public void tcpSocketData(int sockId, long sessionId, ByteBuffer buf) {
		MuxTcpClientChannel channel = _tcpClientChannels.get(sockId);
		if (channel != null) {
			channel.tcpSocketData(sessionId, buf);
		} else {
			_mux.sendTcpSocketClose(sockId);
		}
	}

	public void tcpSocketClosed(int sockId) {
		MuxTcpClientChannel channel = _tcpClientChannels.get(sockId);
		if (channel != null) {
			channel.closed();
		}
	}
	
	public void tcpSocketAborted(int sockId) {
		MuxTcpClientChannel channel = _tcpClientChannels.get(sockId);
		if(channel != null) {
			channel.closed();
		}
	}
	

	///////////////// ReactorProvider

	public synchronized Reactor create(String name) {
		try {
			Reactor reactor = null;
			if (name != null) {
				reactor = getReactor(name);
				if (reactor == null) {
					Logger logger = Logger.getLogger("as.service.reactor.mux." + name);
					reactor = new MuxReactor(name, logger, this);
					aliasReactor(name, reactor);
				} else {
					throw new IllegalArgumentException("Reactor " + name + " is already existing");
				}
			} else {
				reactor = new MuxReactor(null, null, this);
			}
			return reactor;
		} catch (IOException e) {
			throw new RuntimeException("Could not create reactor " + name, e);
		}
	}

	public Reactor getReactor(String alias) {
		return _aliases.get(new CIString(alias));
	}

	public void aliasReactor(String alias, Reactor reactor) {
		if (reactor == null) {
			_aliases.remove(new CIString(alias));
		} else {
			_aliases.put(new CIString(alias), reactor);
		}
	}

	public Reactor getCurrentThreadReactor() {
		return _reactorThreadLocal.get();
	}

	public void tcpConnect(Reactor reactor, InetSocketAddress to, TcpClientChannelListener listener,
			Map<?, Object> options) {
		_logger.debug(this+ ": preparing TCP connect to " + to + " opts: " + options);
		_logger.debug("Reactor type " + reactor.getClass());

		InetSocketAddress from = null;
		long timeout = 10000L;
		Object attachment = null;
		int priority = AsyncChannel.MAX_PRIORITY;
		Executor inputExec = reactor;
		boolean tcpNoDelay = true;
		boolean useDirectBuffer = false;
		boolean useIpTransparent = false;
		int autoFlushSize = 0;
		int rcvbuf = 0;
		int sndbuf = 0;
		boolean secure = false;
		boolean delayedSecureUpgrade = false;

		if (options != null) {
			from = (InetSocketAddress) options.get(TcpClientOption.FROM_ADDR);
			Long t = (Long) options.get(TcpClientOption.TIMEOUT);
			if (t != null) {
				timeout = t;
			}
			attachment = options.get(TcpClientOption.ATTACHMENT);
			Integer p = (Integer) options.get(TcpClientOption.PRIORITY);
			if (p != null) {
				priority = p;
				checkPriority(priority);
			}
			Security security = (Security) options.get(TcpClientOption.SECURITY);
			if (security == null) {
				Boolean s = (Boolean) options.get(TcpClientOption.SECURE);
				secure = (s != null && s);
			} else {
				secure = true;
				delayedSecureUpgrade = security.isDelayed();
			}
			Executor e = (Executor) options.get(TcpClientOption.INPUT_EXECUTOR);
			if (e != null) {
				inputExec = e;
			}
			Boolean noDelay = (Boolean) options.get(TcpClientOption.TCP_NO_DELAY);
			if (noDelay != null) {
				tcpNoDelay = noDelay;
			}
			Boolean direct = (Boolean) options.get(TcpClientOption.USE_DIRECT_BUFFER);
			if (direct != null) {
				useDirectBuffer = direct;
			}
			Integer i = (Integer) options.get(TcpClientOption.AUTO_FLUSH_SIZE);
			if (i != null) {
				autoFlushSize = i;
			}
			if (options.get(TcpClientOption.SO_SNDBUF) != null) {
				sndbuf = (Integer) options.get(TcpClientOption.SO_SNDBUF);
			}
			if (options.get(TcpClientOption.SO_RCVBUF) != null) {
				rcvbuf = (Integer) options.get(TcpClientOption.SO_RCVBUF);
			}

			Boolean ipTransparent = (Boolean) options.get(TcpClientOption.IP_TRANSPARENT);
			if (ipTransparent != null) {
				useIpTransparent = ipTransparent;
			}
		}
		
		MuxTcpClientChannel channel = new MuxTcpClientChannel(checkStarted(reactor))
				.from(from)
				.to(to)
				.listener(listener)
				.doattach(attachment)
				.timeout(timeout)
				.priority(priority)
				.logger(reactor.getLogger())
				.secure(secure)
				.delayedSecureUpgrade(delayedSecureUpgrade)
				.connect();
	}

	public TcpServerChannel tcpAccept(Reactor reactor, InetSocketAddress listenAddr, TcpServerChannelListener listener,
			Map<TcpServerOption, Object> opts) throws IOException {
		throw new RuntimeException("Method not implemented");
	}

	public UdpChannel udpBind(Reactor reactor, InetSocketAddress local, UdpChannelListener listener,
			Map<UdpOption, Object> opts) throws IOException {
		throw new RuntimeException("Method not implemented");
	}

	public void sctpConnect(Reactor reactor, SocketAddress to, SctpClientChannelListener listener,
			Map<SctpClientOption, Object> options) {
		// TODO
	}

	public SctpServerChannel sctpAccept(Reactor reactor, SocketAddress local, SctpServerChannelListener listener,
			Map<SctpServerOption, Object> options) throws IOException {
		throw new RuntimeException("Method not implemented");
	}

	/////////////////// ReactorProviderCompatibility

	public Reactor getDefaultReactor() throws IOException {
		CIString main = new CIString("Main");
		Reactor defReactor;
		synchronized (this) {
			Reactor old = _aliases.get(main);
			if (old != null) {
				return old;
			}
			defReactor = create("Main");
			_aliases.put(main, defReactor);
		}
		defReactor.start();
		_logger.debug("default reactor " + defReactor);
		return defReactor;
	}

	public Reactor newReactor(String name, boolean start, Logger logger) throws IOException {
		Reactor r = create(name);
		if (start) {
			r.start();
		}
		return r;
	}

	public Reactor newReactor(Logger logger) throws IOException {
		return create(null);
	}

	public Reactor newReactor(Logger logger, String threadName) throws IOException {
		return newReactor(threadName, true, logger);
	}

	public void newTcpClientChannel(InetSocketAddress to, TcpClientChannelListener listener, Reactor reactor,
			Object attachment, long timeout, Logger tr) {
		newTcpClientChannel(to, listener, reactor, attachment, timeout, AsyncChannel.MAX_PRIORITY, tr);
	}

	public void newTcpClientChannel(InetSocketAddress from, InetSocketAddress to, TcpClientChannelListener listener,
			Reactor reactor, Object attachment, long timeout, int priority, Logger tr, boolean secure) {
		MuxTcpClientChannel channel = new MuxTcpClientChannel(checkStarted(reactor))
				.from(from)
				.to(to)
				.listener(listener)
				.doattach(attachment)
				.timeout(timeout)
				.priority(priority)
				.logger(tr)
				.secure(secure)
				.connect();
	}

	public void newTcpServerChannel(InetSocketAddress listenedAddr, TcpServerChannelListener listener, Reactor reactor,
			Object attachment, Logger tr, boolean secure) {
		throw new RuntimeException("Method not implemented");
	}

	public TcpServerChannel newTcpServerChannel(Reactor reactor, TcpServerChannelListener listener,
			InetSocketAddress listenedAddr, Object attachment, boolean secure, Logger tr) throws IOException {
		throw new RuntimeException("Method not implemented");
	}

	public void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor, Object attachment,
			Logger tr) {
		throw new RuntimeException("Method not implemented");
	}

	public void newUdpChannel(InetSocketAddress local, UdpChannelListener listener, Reactor reactor, int priority,
			Object attachment, Logger tr) {
		throw new RuntimeException("Method not implemented");
	}

	public void newSctpClientChannel(SocketAddress local, InetAddress[] secondaryLocals, int maxOutStreams,
			int maxInStreams, SocketAddress to, Object attachment, long timeout, int priority, Logger logger,
			SctpClientChannelListener listener, Reactor reactor) {
		// TODO
	}

	public SctpServerChannel newSctpServerChannel(SocketAddress local, InetAddress[] secondaryLocals, int maxOutStreams,
			int maxInStreams, int priority, Logger logger, SctpServerChannelListener listener, Object attachment,
			Reactor reactor) throws IOException {
		throw new RuntimeException("Method not implemented");
	}

}
