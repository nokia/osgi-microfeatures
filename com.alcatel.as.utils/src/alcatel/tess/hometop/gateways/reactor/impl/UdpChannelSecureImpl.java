// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.TimerService;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;

/**
 * Secured/DTLS udp channel implementation.
 */
public class UdpChannelSecureImpl extends UdpChannelImpl {
	private final static Logger _logger = Logger.getLogger("as.service.reactor.UdpChannelSecureImpl");
	private final String addr;
	private final int port;
	private final Map<InetSocketAddress, Session> _sessions = new HashMap<>();
	private volatile Security _security;
	private final boolean _isClient;
	private final long _sessionTimeout;

	/**
	 * remote udp secured client.
	 */
	class Session {
		// TLS Engine used to wrap/unwrap client messages.
		private final TLSEngine _tls;
		
		// Inactivity timer used to cleanup user session
		private Future<?> _timer;
		
		private boolean _closed;

		Session() throws Exception {
			_tls = new TLSEngineImpl(_security, _isClient, "DTLS", addr, port, "UDP");
			_reactor.getMeters().udpSession(1);
		}

		TLSEngine getTLSEngine() {
			return _tls;
		}
		
		Future<?> getTimer() {
			return _timer;			
		}
		
		void setTimer(Future<?> timer) {
			_timer = timer;
		}

		// must be called once, when sessio is closed
		public void close() {
			Future<?> timer = _timer;
			if (timer != null) {
				timer.cancel(false);
			}
			if (! _closed) {
				_closed = true;
				_reactor.getMeters().udpSession(-1);
			}
		}
	}

	UdpChannelSecureImpl(ReactorImpl reactor, UdpChannelListener listener, InetSocketAddress local, int priority,
			Object attachment, Executor inputExec, Executor outputExec, boolean enableRead, int sndbuf, int rcvbuf,
			boolean directBuffer, boolean ipTransparent, boolean isClient, Security security, long sessionTimeout) 
		throws IOException
	{
		super(reactor, listener, local, priority, attachment, inputExec, outputExec, enableRead, sndbuf, rcvbuf, directBuffer, ipTransparent);
		addr = super.getLocalAddress().getAddress().getHostAddress();
		port = super.getLocalAddress().getPort();
		_security = security;
		_isClient = isClient;
		_sessionTimeout = sessionTimeout;
	}
	
	/**
	 * Called in queue executor, when an encrypted message is received
	 */
	@Override
	protected void messageReceived(ByteBuffer wrapped, InetSocketAddress from) throws Exception {
		Session session = getSession(from);
		TLSEngine tlsEngine = session.getTLSEngine();
		tlsEngine.fillsDecoder(wrapped, from);
		runTLSEngine(tlsEngine);
	}

	@Override
	public String toString() {
		return "UdpChannelSecure [local=" + getLocalAddress() + ",remote=" + addr + ":" + port + "]";
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public void updateSecurity(Security security) {
		if (_security == null) {
			throw new IllegalStateException("Can't updated security parameters on unsecured channel.");
		}
		_security = security;
	}

	/**
	 * send a message. Can be called from any thread.
	 */
	@Override
	public void send(final InetSocketAddress to, ByteBuffer buf, boolean copy) {
		checkBound();
		if (Helpers.isCurrentThreadInQueue(_queue)) {
			// caller thread is running in current queue: no need to copy even if copy == true 
			// because the tls engine will copy the message
			// 
			doSend(to, buf);
		} else {
			// current thread different from current queue, if copy==true, then copy the buffer
			// because we'll schedule the doSend method.
			schedule(() -> doSend(to, copy ? Helpers.copy(buf) : buf), ExecutorPolicy.SCHEDULE_HIGH);
		}
	}
		
	/**
	 * Called from queue
	 */
	private void doSend(final InetSocketAddress to, ByteBuffer buf) {
		try {
			Session session = getSession(to);
			TLSEngine tls = session.getTLSEngine();
			tls.fillsEncoder(buf, to);
			runTLSEngine(tls);
		} catch (Throwable t) {
			_logger.warn("Unexpected exception while sending udp packet to " + to, t);
		}
	}


	@Override
	public void close() {
		// serialize send method with close method in "inputExecutor"
		schedule(UdpChannelSecureImpl.super::close, ExecutorPolicy.INLINE);
	}
	
	/**
	 * Channel aborted. Called from queue.
	 */
	@Override
	protected void connectionClosed() {
		try {
			super.connectionClosed();
		} finally {
			for (Session s : _sessions.values()) {
				s.close();
			}
			_sessions.clear();
		}
	}

	/**
	 * Runs the tls engine. We'll encode some messages to be sent, or decode some
	 * received encrypted messages. called from queue
	 * 
	 * @param tlsEngine
	 * @throws IOException 
	 */
	@SuppressWarnings("incomplete-switch")
	private void runTLSEngine(TLSEngine tlsEngine) throws IOException {
		TLSEngine.Status status;

		loop: while ((status = tlsEngine.run()) != TLSEngine.Status.NEEDS_INPUT) {
			switch (status) {
			case DECODED:
				ByteBuffer unwrapped = tlsEngine.getDecodedBuffer();
				InetSocketAddress from = (InetSocketAddress) tlsEngine.getDecodedAttachment();
				if (!_closing.get()) {
					if (unwrapped.hasRemaining() && _logger.isInfoEnabled()) {
						Helpers.logPacketReceived(_logger, unwrapped, this);
					}
					_listener.messageReceived(this, unwrapped, from);
				} else {
					if (_logger.isDebugEnabled()) {
						_logger.debug("ignoring received messages, bytes=" + unwrapped.remaining() + " (closing)");
					}
					while (unwrapped.hasRemaining()) {
						unwrapped.get();
					}
				}
				break;

			case ENCODED:
				// This message is either a handshake message, or an application encrypted message: send it
				ByteBuffer wrapped = Helpers.copy(tlsEngine.getEncodedBuffer());
				InetSocketAddress to = (InetSocketAddress) tlsEngine.getEncodedAttachment();
				int bytesToSend = wrapped.remaining();
				_sendQueue.add(new Message(wrapped, to));
				buffered(bytesToSend);
				scheduleWriteInterest();
				break;

			case CLOSED:
				if (_logger.isDebugEnabled()) {
					_logger.debug("tls engine returned CLOSED status");
				}
				super.shutdown();
				break loop;
			}
		}
	}
	
	private Session getSession(InetSocketAddress from) throws Exception {
		Session s = _sessions.get(from);
		if (s == null) {
			s = new Session();
			if (_logger.isDebugEnabled()) _logger.debug("created session for " + from);
			_sessions.put(from, s);
		} else {
			s.getTimer().cancel(false);
		}
		TimerService wheelTimer = _reactor.getReactorProvider().getApproxTimerService();
		s.setTimer(wheelTimer.schedule(_queue, () -> sessionTimeout(from), _sessionTimeout, TimeUnit.MILLISECONDS));
		return s;
	}
	
	private void schedule(Runnable task, ExecutorPolicy policy) {
		if (_queue instanceof PlatformExecutor) {
			((PlatformExecutor) _queue).execute(task, policy);
		} else if (_queue instanceof Reactor) {
			((Reactor) _queue).getPlatformExecutor().execute(task, policy);
		} else {
			_queue.execute(task);
		}
	}

	// called from queue
	private void sessionTimeout(InetSocketAddress from) {
		if (_logger.isDebugEnabled()) _logger.debug("session timeout for " + from);
		Session s = _sessions.remove(from);
		if (s != null) {	
			s.close();
		}
	}

}
