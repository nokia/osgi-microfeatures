// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Channel;
import alcatel.tess.hometop.gateways.reactor.Reactor;

/**
 * Base class for TcpServerChannel and SctpServerChannel implementations.
 */
public abstract class AbstractServerChannel implements Channel {
	// Reactor managing the server channel
	protected final ReactorImpl _reactor;
	
	// context attached to this channel
	protected volatile Object _attached;
	
	// enable server socket by default (false, means you have to call enableReading method)
	protected final boolean _enableRead;
	
	// Our logger
	private final static Logger _log = Logger.getLogger("as.service.reactor.AbstractServerChannel");
	
	// The nio Server channel
	private volatile AbstractSelectableChannel _ssc;
	
	// Timeout used to temporarily disable accept_op when some TooManyOpenFile exceptions are taking place
	private final long _disableAcceptTimeout;
	
	// Map of Acceptors. There is one Acceptor per Selector instance. Key = Server Selection Key. Value=Acceptor
	private final Map<SelectionKey, AcceptorHandler> _acceptors = Collections.synchronizedMap(new HashMap<>());

	// State flag to tell if serverConnectionOpened callback has been invoked
	private final static int ST_OPEN_CALLED = 1;
	
	// State flag to tell if serverConnectionClosed callback has been invoked
	private final static int ST_CLOSE_CALLED = 2;
	
	// State flag to tell if serverConnectionFailed callback has been invoked
	private final static int ST_FAILED_CALLED = 4;
	
	// State flag to tell if we are listening
	private final static int ST_LISTENING = 8;
	
	// State flag to tell if we are closing the server socket
	private final static int ST_CLOSING = 16;
	
	// State flag that is set with some of the above flags.
	private volatile int _state;

	// Selector priority
	private final int _priority;

	// Start listening 
	protected abstract AbstractSelectableChannel doListen() throws IOException;
	
	// Invoke listener on serverConnectionOpened callback
	protected abstract void doServerConnectionOpened();
	
	// Invoke listener on serverConnectionClosed callback
	protected abstract void doServerConnectionClosed();
	
	// Invoke listener on serverConnectionFailed callback
	protected abstract void doServerConnectionFailed(Throwable t);
	
	// Accept a client connection request
	protected abstract void doAccept(SelectionKey serverKey, NioSelector selector) throws Exception;

	/**
	 * Constructor.
	 * @param reactor
	 * @param attachment
	 * @param enableRead
	 * @param disableAcceptTimeout
	 */
	public AbstractServerChannel(ReactorImpl reactor, Object attachment, boolean enableRead, long disableAcceptTimeout, int priority) {
		_reactor = reactor;
		_attached = attachment;
		_enableRead = enableRead;
		_disableAcceptTimeout = disableAcceptTimeout == 0 ? 1000L : disableAcceptTimeout;
		_priority = priority;
	}
	
	/**
	 * Start listening asynchronously and invoke the listener on 
	 * serverConnectionOpened or serverConnectionFailed callbacks.
	 */
	public void listen() {
		try {
			synchronized (this) {
				if (!isFlagSet(ST_LISTENING)) {
					_state |= ST_LISTENING;
				} else {
					return;
				}
			}

			_ssc = doListen();

			// register the socket in all selectors, but do not enable ACCEPT_OP for now.
			NioSelector[] selectors = _reactor.getReactorProvider().getSelectors();
			for (NioSelector selector : selectors) {
				AcceptorHandler acceptor = new AcceptorHandler(selector);
				SelectionKey serverKey = selector.registerSelectHandler(_ssc, 0, acceptor);
				_acceptors.put(serverKey, acceptor);
			}

			if (_log.isInfoEnabled()) {
				_log.info(this.toString() + ": listening ...");
			}

			// notify server socket listener.
			_reactor.schedule(this::serverConnectionOpened);
			
			// Now enable ACCEPT_OP, if needed.
			if (_enableRead) {
				for (Map.Entry<SelectionKey, AcceptorHandler> e : _acceptors.entrySet()) {
					NioSelector selector = e.getValue().getSelector();
					SelectionKey serverKey = e.getKey();
					selector.schedule(() -> {
						serverKey.interestOps(SelectionKey.OP_ACCEPT);
					});
				}
			}
		} catch (Throwable t) {
			_reactor.schedule(() -> serverConnectionFailed(t));
		}
	}

	// Bind to the server socket addr synchronously (called from any threads).
	public void listenSync() throws IOException {
		synchronized (this) {
			if (!isFlagSet(ST_LISTENING)) {
				// in sync mode, the open callback is not actually called, so let's set the
				// state as if we would have called it.
				_state |= (ST_LISTENING | ST_OPEN_CALLED);
			} else {
				return;
			}
		}

		_ssc = doListen();
		
		// register the socket in all selectors, and possibly enable ACCEPT_OP.
		NioSelector[] selectors = _reactor.getReactorProvider().getSelectors();
		for (NioSelector selector : selectors) {
			int interestop = _enableRead ? SelectionKey.OP_ACCEPT : 0;
			AcceptorHandler acceptor = new AcceptorHandler(selector);
			SelectionKey serverKey = selector.registerSelectHandler(_ssc, interestop, acceptor);
			_acceptors.put(serverKey, acceptor);
		}

		if (_log.isInfoEnabled()) {
			_log.info(this.toString() + ": listening ...");
		}
	}

	// called from any threads
	public void shutdown() {
		close();
	}

	// called from any threads.
	public void close() {
		synchronized (this) {
			if (isFlagSet(ST_LISTENING) && !isFlagSet(ST_CLOSING)) {
				_state |= ST_CLOSING;
			} else {
				return;
			}
		}

		// Cancel all server selection keys from all selectors. To wait for all keys to
		// be cancelled, we'll use a Phaser in order to not block the current thread.
		final Phaser phaser = new Phaser() {
			protected boolean onAdvance(int phase, int registeredParties) {
				// called from one selector, when server keys have been cancelled from all
				// selector. At this point, schedule connection closed callback.
				_reactor.schedule(AbstractServerChannel.this::serverConnectionClosed);
				return true;
			}
		};
		phaser.register();

		Iterator<Map.Entry<SelectionKey, AcceptorHandler>> it = _acceptors.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<SelectionKey, AcceptorHandler> e = it.next();
			phaser.register();
			SelectionKey serverKey = e.getKey();
			AcceptorHandler acceptor = e.getValue();
			acceptor.getSelector().scheduleNow(() -> {
				try {
					acceptor.cancel(serverKey);
				} finally {
					phaser.arrive();
				}
			});
		}
		phaser.arrive();
	}

	/**
	 * Return the attachment of this session. The purpose of this method is
	 * identical to {@link SelectionKey#attachment()}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T attachment() {
		return (T) _attached;
	}

	/**
	 * Set the attachment of this session. The purpose of this method is identical
	 * to {@link SelectionKey#attach(java.lang.Object)}.
	 */
	@Override
	public void attach(Object attached) {
		_attached = attached;
	}

	@Override
	public Reactor getReactor() {
		return _reactor;
	}

	// called from any threads
	public void enableReading() {
		for (Map.Entry<SelectionKey, AcceptorHandler> e : _acceptors.entrySet()) {
			e.getValue().getSelector().scheduleNow(() -> {
				try {
					e.getKey().interestOps(SelectionKey.OP_ACCEPT);
				} catch (CancelledKeyException ex) {
				} catch (Exception ex) {
					_log.info("Could not enable server socket ACCEPT mode", ex);
				}
			});
		}
	}

	// called from reactor thread
	private void serverConnectionOpened() {
		try {
			// only invoke serverConnectionOpen one time and don't invoke it if closed or
			// failed callbacks have been called.
			if (!isFlagSet(ST_OPEN_CALLED)) {
				_state |= ST_OPEN_CALLED;
				doServerConnectionOpened();
				if (_enableRead) {
					enableReading();
				}
			}
		} catch (Throwable t) {
			_log.warn("Exception caught while calling connectionOpened callback", t);
			return;
		}
	}

	// called from reactor thread
	private void serverConnectionClosed() {
		// Only invoke closed callback once, and if open callback has been called
		if (isFlagSet(ST_OPEN_CALLED) && !isFlagSet(ST_CLOSE_CALLED)) {
			_state |= ST_CLOSE_CALLED;
			try {
				// _listener.serverConnectionClosed(AbstractServerConnection.this);
				doServerConnectionClosed();
			} catch (Throwable t) {
				_log.warn("got exception while invoking ServerConnectionClosed", t);
			} finally {
				cleanup();
			}
		}
	}

	// called from reactor thread
	private void serverConnectionFailed(Throwable t) {
		try {
			// Only invoke failed callback once, and if open or closed callbacks have not
			// been called
			if (!isFlagSet(ST_FAILED_CALLED)) {
				_state |= ST_FAILED_CALLED;
				if (_log.isInfoEnabled()) {
					_log.info("Unexpected exception caught while listening to " + toString(), t);
				}
				doServerConnectionFailed(t);
			}
		} catch (Throwable t2) {
			_log.warn("Exception caught while calling server connection failed callback", t2);
		} finally {
			cleanup();
		}
	}

	// called from reactor thread
	private void cleanup() {
		try {
			_ssc.close();
		} catch (Throwable ignored) {
		}
	}

	public boolean isFlagSet(int flags) {
		return (_state & flags) == flags;
	}
	
	private String getInfo() {
		return toString();
	}

	/**
	 * Represents a server acceptor. Runs on a given selection, and all methods from
	 * this class must be called from within the associated selector thread.
	 */
	private class AcceptorHandler implements SelectHandler {
		private final NioSelector _selector;
		private ScheduledFuture<?> _acceptTimer;

		AcceptorHandler(NioSelector selector) {
			_selector = selector;
		}

		@Override
		public void selected(SelectionKey serverKey) {
			try {
				doAccept(serverKey, _selector);
			}

			catch (Throwable t) {
				_log.warn("Failed to accept new connection on " + getInfo(), t);
				if (t instanceof IOException && t.getMessage().equalsIgnoreCase("Too many open files")) {
					// We could not accept the socket because currently, we have too many open files. Schedule ACCEPT operation later.
					acceptLater(serverKey);
				} 
			}
		}

		@Override
		public int getPriority() {
			return _priority;
		}

		NioSelector getSelector() {
			return _selector;
		}

		void acceptLater(SelectionKey serverKey) {
			try {
				if (_acceptTimer != null) {
					_acceptTimer.cancel(false);
				}
				_log.warn("Tcp server listen disabled on addr " + getInfo() + " (too many open files, can't accept connections)");				
				serverKey.interestOps(0);
				_acceptTimer = _selector.schedule(() -> enableAccept(serverKey), _disableAcceptTimeout, TimeUnit.MILLISECONDS);
			} catch (Throwable t) {
				_log.error("Could not schedule tcp server listen on " + getInfo(), t);
				try {
					serverKey.interestOps(SelectionKey.OP_ACCEPT);
				} 
				catch (CancelledKeyException err) {					
				}
				catch (Throwable err) {
					_log.error("Could not schedule tcp server listen on " + getInfo(), err);
				}
			}
		}

		void enableAccept(SelectionKey serverKey) {
			_log.warn(getInfo() + ": Accepting connections.");
			try {
				serverKey.interestOps(SelectionKey.OP_ACCEPT);
			} catch (CancelledKeyException e) {
				// server socket closed
			} catch (Throwable t) {
				// the key is probably cancelled, or the socket has been closed
				_log.warn("Could not enable tcp server ACCEPT operation", t);
			}
		}

		void cancel(SelectionKey serverKey) {
			if (_acceptTimer != null) {
				_acceptTimer.cancel(false);
			}
			serverKey.cancel();
		}
	}
	
}
