// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.mux.reactor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;
import alcatel.tess.hometop.gateways.reactor.Security;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;

public class MuxTcpClientChannel implements TcpChannel {

	private InetSocketAddress _to, _from;
	private boolean _secure;
	private int _priority;
	private volatile Executor _inExec;
	private Object _attachment;
	private MuxReactor _reactor;
	private TcpClientChannelListener _listener;
	private long _timeout;
	private Logger _logger;
	private volatile boolean _closed = true;
	private int _sockId;
	private volatile State _state = STATE_INIT;
	private ScheduledFuture<?> _timeoutFuture;
	private boolean _delayedSecureUpgrade;

	private static final String TCP_PARAMS_UPGRADE_SECURE = "tcp.secure.upgrade";
	
	
	protected MuxTcpClientChannel(MuxReactor reactor) {
		_reactor = reactor;
	}

	////////////// TcpChannel

	public InetSocketAddress getRemoteAddress() {
		return _to;
	}

	public void flush() {
	}

	public boolean isSecure() {
		return _secure;
	}
	
	boolean isDelayedSecureUpgrade() {
		return _delayedSecureUpgrade;
	}

	public void upgradeToSecure() {
		_reactor.muxConnection()
			.sendTcpSocketParams(_sockId, Collections.singletonMap(TCP_PARAMS_UPGRADE_SECURE, "1"));
	}

	public int getPriority() {
		return _priority;
	}

	public void setPriority(int priority) {
		_priority = priority;
	}

	public void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy) {
	}

	public InetSocketAddress getLocalAddress() {
		return _from;
	}

	public void setSoTimeout(long soTimeout) {
	}

	public void setSoTimeout(long soTimeout, boolean readOnly) {
	}

	public boolean isClosed() {
		return _closed;
	}

	public void disableReading() {
	}

	public void enableReading() {
	}

	public void setInputExecutor(Executor executor) {
	}

	public Executor getInputExecutor() {
		return _inExec;
	}

	public void send(ByteBuffer msg, boolean copy) {
		_reactor.muxConnection().sendTcpSocketData(_sockId, copy, msg);
	}

	public void send(ByteBuffer[] msg, boolean copy) {
		_reactor.muxConnection().sendTcpSocketData(_sockId, copy, msg);
	}

	public void send(DataBuffer msg) {
		try {
			send(msg.getInternalBuffer(), true);
		} finally {
			msg.resetCapacity();
		}
	}

	public void send(ByteBuffer msg) {
		send(msg, true);
	}

	public void send(ByteBuffer[] msg) {
		send(msg, true);
	}

	public void send(byte[] msg) {
		send(msg, true);
	}

	public void send(byte[] msg, boolean copy) {
		send(msg, 0, msg.length, copy);
	}

	public void send(byte[] msg, int off, int len) {
		send(msg, 0, msg.length, true);
	}

	public void send(byte[] msg, int off, int len, boolean copy) {
		send(ByteBuffer.wrap(msg, off, len), copy);
	}

	public int getSendBufferSize() {
		return 0;
	}

	public void close() {
		_inExec.execute(() -> {
			_state.close(MuxTcpClientChannel.this, false);
		});
	}

	public void shutdown() {
		_inExec.execute(() -> {
			_state.close(MuxTcpClientChannel.this, true);
		});
	}

	public <T> T attachment() {
		return (T) _attachment;
	}

	public void attach(Object attached) {
		_attachment = attached;
	}

	public Reactor getReactor() {
		return _reactor;
	}

	@Override
	public void setSoLinger(long linger) {
	}
    
    @Override
	public Map<String, Object> exportTlsKey(String asciiLabel, byte[] context_value, int length) {
	  return Collections.emptyMap();
	}
	
	/////////////// Implementation

    public List<SNIHostName> getClientRequestedServerNames() {
    	return Collections.emptyList();
    }

	protected MuxTcpClientChannel from(InetSocketAddress from) {
		_from = from;
		return this;
	}

	protected MuxTcpClientChannel to(InetSocketAddress to) {
		_to = to;
		return this;
	}

	protected MuxTcpClientChannel listener(TcpClientChannelListener listener) {
		_listener = listener;
		return this;
	}

	protected MuxTcpClientChannel doattach(Object attachment) {
		_attachment = attachment;
		return this;
	}

	protected MuxTcpClientChannel timeout(long to) {
		_timeout = to;
		return this;
	}

	protected MuxTcpClientChannel logger(Logger logger) {
		_logger = logger;
		return this;
	}

	protected MuxTcpClientChannel priority(int priority) {
		_priority = priority;
		return this;
	}

	protected MuxTcpClientChannel secure(boolean secure) {
		_secure = secure;
		return this;
	}
	
	protected MuxTcpClientChannel delayedSecureUpgrade(boolean delayed) {
		_delayedSecureUpgrade = delayed;
		return this;
	}
	
	protected long getTimeout() {
		return _timeout;
	}

	protected MuxTcpClientChannel connect() {		
	    _logger.debug("Preparing timeout");
		if (_to == null)
			throw new RuntimeException("Destination Address not set");
		if (_inExec == null)
			_inExec = _reactor;
		_state = STATE_CONNECTING;
		_logger.debug("Scheduling timeout " + _timeout);
		_timeoutFuture = _reactor.schedule(() -> {
			_logger.warn(this + " timeout!");
			_state.connectTimeout(this);
		}, _timeout, TimeUnit.MILLISECONDS);
		_reactor.tcpConnect(this);
		return this;
	}

	protected void sendClose() {
		_reactor.muxConnection().sendTcpSocketClose(_sockId);
	}

	protected void sendShutdown() {
		_reactor.muxConnection().sendTcpSocketAbort(_sockId);
	}

	MuxTcpClientChannel connected(int sockId, String remoteIP, int remotePort, String localIP, int localPort) {
		_inExec.execute(() -> {
			_logger.debug("timeout canceled, connecting");
			if(_timeoutFuture != null) {
				_timeoutFuture.cancel(false);
			}
			_state.connected(MuxTcpClientChannel.this, sockId, remoteIP, remotePort, localIP, localPort);
		});
		return this;
	}

	void tcpSocketData(long sessionId, ByteBuffer buf) {
		ByteBuffer copy = ByteBuffer.allocate(buf.remaining());
		copy.put(buf);
		copy.flip();
		_inExec.execute(() -> {
			_state.data(MuxTcpClientChannel.this, copy);
		});
	}

	void failed(Throwable ex) {
		_inExec.execute(() -> {
			_state.connectFailed(MuxTcpClientChannel.this, ex);
		});
	}
	
	void timeout() {
		_inExec.execute(() -> {
			_state.connectTimeout(MuxTcpClientChannel.this);
		});
	}

	void closed() {
		_inExec.execute(() -> {
			_state.closed(MuxTcpClientChannel.this);
		});
	}

	protected static class State {
		protected void connected(MuxTcpClientChannel channel, int sockId, String remoteIP, int remotePort,
				String localIP, int localPort) {
			throw new IllegalStateException(this + " : connected : not allowed");
		}

		protected void connectFailed(MuxTcpClientChannel channel, Throwable ex) {
			throw new IllegalStateException(this + " : connectFailed : not allowed");
		}

		protected void connectTimeout(MuxTcpClientChannel channel) {
			throw new IllegalStateException(this + " : connectTimeout : not allowed");
		}

		protected void data(MuxTcpClientChannel channel, ByteBuffer data) {
			throw new IllegalStateException(this + " : data : not allowed");
		}

		protected void closed(MuxTcpClientChannel channel) {
			throw new IllegalStateException(this + " : closed : not allowed");
		}

		protected void close(MuxTcpClientChannel channel, boolean shutdown) {
			throw new IllegalStateException(this + " : close : not allowed");
		}
	}

	protected static State STATE_INIT = new State() {
		public String toString() {
			return "STATE_INIT";
		}
	};
	protected static State STATE_CONNECTING = new State() {
		public String toString() {
			return "STATE_CONNECTING";
		}

		@Override
		protected void connected(MuxTcpClientChannel channel, int sockId, String remoteIP, int remotePort,
				String localIP, int localPort) {
			channel._sockId = sockId;
			try {
				channel._from = new InetSocketAddress(java.net.InetAddress.getByName(localIP), localPort);
			} catch (Exception e) {
				channel._logger.warn(channel + " : exception while instantiating From Address", e);
				channel._state = STATE_CONNECTED_CANCELLED;
				channel.sendClose();
				try {
					channel._listener.connectionFailed(channel, e);
				} catch (Throwable t) {
					channel._logger.warn(channel + " : exception while calling connectionFailed", t);
				}
				return;
			}
			channel._state = STATE_CONNECTED;
			channel._closed = false;
			try {
				channel._listener.connectionEstablished(channel);
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectionEstablished - closing", t);
				channel._state = STATE_CLOSING;
				channel.sendClose();
			}
		}

		@Override
		protected void connectFailed(MuxTcpClientChannel channel, Throwable ex) {
			try {
				channel._listener.connectionFailed(channel, ex);
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectionFailed", t);
			} finally {
				channel._state = STATE_CLOSED;
			}
		}

		@Override
		protected void connectTimeout(MuxTcpClientChannel channel) {
			try {
				channel._listener.connectionFailed(channel, new TimeoutException());
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectionFailed", t);
			} finally {
				channel._state = STATE_CONNECTING_CANCELLED;
				channel.sendClose();
			}
		}
	};
	protected static State STATE_CONNECTING_CANCELLED = new State() {
		public String toString() {
			return "STATE_CONNECTING_CANCELLED";
		}

		@Override
		protected void connected(MuxTcpClientChannel channel, int sockId, String remoteIP, int remotePort,
				String localIP, int localPort) {
			channel._logger.warn(channel + " : connected but in cancelled state - closing ");
			channel.sendClose();
		}
		
		@Override
		protected void closed(MuxTcpClientChannel channel) {
			channel._closed = true;
			channel._state = STATE_CLOSED;
			try {
				channel._listener.connectionClosed(channel);
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectionClosed", t);
			}		
		}

		@Override
		protected void connectFailed(MuxTcpClientChannel channel, Throwable ex) {
			channel._logger.debug(channel + " : connectFailed");
			channel._closed = true;
			channel._state = STATE_CLOSED;
		}
	};
	protected static State STATE_CONNECTED_CANCELLED = new State() {
		public String toString() {
			return "STATE_CONNECTED_CANCELLED";
		}

		@Override
		protected void closed(MuxTcpClientChannel channel) {
			channel._closed = true;
			channel._state = STATE_CLOSED;
			try {
				channel._listener.connectionClosed(channel);
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectionClosed", t);
			}
		}
	};
	protected static State STATE_CONNECTED = new State() {
		public String toString() {
			return "STATE_CONNECTED";
		}

		protected void connectTimeout(MuxTcpClientChannel channel) {
			try {
				channel._listener.connectionFailed(channel, new TimeoutException("Timeout"));
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectTimeout - closing", t);
				close(channel,false);
			}
		}

		@Override
		protected void data(MuxTcpClientChannel channel, ByteBuffer data) {
			try {
				channel._listener.messageReceived(channel, data);
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling messageReceived - closing", t);
				close(channel, false);
			}
		}

		@Override
		protected void close(MuxTcpClientChannel channel, boolean shutdown) {
			channel._state = STATE_CLOSING;
			if (shutdown)
				channel.sendShutdown();
			else
				channel.sendClose();
		}

		@Override
		protected void closed(MuxTcpClientChannel channel) {
			channel._closed = true;
			channel._state = STATE_CLOSED;
			try {
				channel._listener.connectionClosed(channel);
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectionClosed", t);
			}
		}
	};
	protected static State STATE_CLOSING = new State() {
		public String toString() {
			return "STATE_CLOSING";
		}

		@Override
		protected void connectTimeout(MuxTcpClientChannel channel) {
			channel._logger.debug(channel + " : connectTimeout");
		}

		@Override
		protected void data(MuxTcpClientChannel channel, ByteBuffer data) {
			channel._logger.debug(channel + " : data");
		}

		@Override
		protected void closed(MuxTcpClientChannel channel) {
			channel._closed = true;
			channel._state = STATE_CLOSED;
			try {
				channel._listener.connectionClosed(channel);
			} catch (Throwable t) {
				channel._logger.warn(channel + " : exception while calling connectionClosed", t);
			}
		}
	};
	protected static State STATE_CLOSED = new State() {
		public String toString() {
			return "STATE_CLOSED";
		}

		@Override
		protected void connectTimeout(MuxTcpClientChannel channel) {
			channel._logger.debug(channel + " : connectTimeout");
		}
	};
	

	@Override
	public String toString() {
		return "MuxTcpClientChannel [_to=" + _to + ", _from=" + _from + ", _secure=" + _secure + ", _priority="
				+ _priority + ", _inExec=" + _inExec + ", _attachment=" + _attachment +
				", _closed=" + _closed + ", _sockId=" + _sockId + ", _state=" + _state + "]";
	}

  /**
   * Get the security object of the channel
   * May return null is the channel is not secure
   */
    public Security getSecurity() { return null; /* ?? */ }

  /**
   * Get the SSLEngine instance of the channel
   * May return null is the channel is not secure
   */
  public SSLEngine getSSLEngine() { return null; /* ?? */ }
    
}
