// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.reactor.socks;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.TimerService;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.ReactorProvider;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.spi.FilterTcpClientListener;

@SuppressWarnings("deprecation")
public class Socks4TcpClientListener extends FilterTcpClientListener {

	public final static String CONF_DEST = "socks.dest.addr";
	public final static String CONF_CMD = "socks.cmd";
	public final static String CONF_CMD_DEF = "01";
	public final static String CONF_USER = "socks.user";

	private final static Logger _log = Logger.getLogger(Socks4TcpClientListener.class);
	private final static byte VER = 0x04;
	private final static byte REQ_GRANTED = 0x5A;
	private final TimerService _timer;
	private boolean _disableReading;
	private ScheduledFuture<?> _future;
	private ByteBuffer _buffered;
	private boolean _connected;

	public Socks4TcpClientListener(TcpClientChannelListener listener, Map<?, Object> options, TimerService wheelTimer) {
		super(listener, options);
		_timer = wheelTimer;
	}

	@Override
	public void connectionEstablished(TcpChannel channel) {
		super.setChannel(channel);
		Long timeout = getParameter(_options, ReactorProvider.TcpClientOption.TIMEOUT, 0L);
		if (timeout != null && timeout > 0L) {
			_future = _timer.schedule(_channel.getInputExecutor(), this::timeout, (long) timeout,
					TimeUnit.MILLISECONDS);
		}
		_channel.send(encodeConnectHeader(), false);
	}

	private void timeout() { // at this stage, current thread = reactor companion thread
		if (!_future.isCancelled()) {
			if (_log.isDebugEnabled())
				_log.debug("socks response timeout");
			_channel.close();
		}
	}

	@Override
	public int messageReceived(TcpChannel cnx, ByteBuffer buf) {
		if (_connected) {
			return super.messageReceived(cnx, buf);
		} else {
			// check if we have enough bytes
			if (buf.remaining() < 8) {
				if (_log.isDebugEnabled())
					_log.debug("received too short message: " + buf.remaining());
				return 8 - buf.remaining();
			}

			// check timeout
			if (_future != null && !_future.cancel(false)) {
				return 0; // timeout
			}

			byte vn = buf.get(); // reply version
			byte rep = buf.get(); // reply code
			int port = get16(buf); // destination port, meaningful if granted in BIND, otherwise ignore
			int destip = get32(buf); // destination IP, as above – the ip:port the client should bind to

			if (_log.isDebugEnabled())
				_log.debug("received socks reply: vn=" + vn + ", rep=" + rep + ", port=" + port + ", destip=" + destip);

			switch (rep) {
			case REQ_GRANTED:
				if (_log.isDebugEnabled())
					_log.debug("socks req granted.");
				_buffered = buf.hasRemaining() ? copy(buf) : null;
				try {
					_listener.connectionEstablished(Socks4TcpClientListener.this);
				} catch (Throwable err) {
					_log.warn("unexpected exception while calling connectionEstablished", err);
					cnx.close();
					return 0;
				}
				schedule(ExecutorPolicy.INLINE, () -> { // note that we are also making a memory barrier.
					if (!_disableReading && ! checkBuffered()) {
						return;
					}
					_connected = true;
				});
				break;
			default:
				if (_log.isDebugEnabled()) {
					_log.debug("connect failed: " + getRequestFailed(rep));
				}
				cnx.close();
				break;
			}
			return 0;
		}
	}

	@Override
	public void disableReading() {
		schedule(ExecutorPolicy.INLINE, () -> {
			_disableReading = true;
			_channel.disableReading();
		});
	}

	@Override
	public void enableReading() {
		schedule(ExecutorPolicy.SCHEDULE, () -> {
			_disableReading = false;
			if (checkBuffered()) {
				_channel.enableReading();
			}
		});
	}

	@Override
	public void connectionClosed(TcpChannel cnx) {
		if (_connected) {
			super.connectionClosed(cnx);
		} else {
			// we are still in reactor companion thread, so the socket is closed before we
			// can get the actual socks ACK message
			ScheduledFuture<?> future = _future;
			if (future != null) {
				future.cancel(false);
			}
			connectionFailed(cnx, new IOException("socks socket closed"));
		}
	}
	
	private boolean checkBuffered() {
		if (_buffered != null) {
			try {
				_listener.messageReceived(Socks4TcpClientListener.this, _buffered);
				_buffered = null;
			} catch (Throwable err) {
				_log.warn("unexpected error while calling messageReceived", err);
				_channel.close();
				return false;
			}
		}
		return true;
	}

	private String getRequestFailed(byte rep) {
		switch (rep) {
		case 0x5B:
			return "Request rejected or failed";
		case 0x5C:
			return "Request failed because client is not running identd (or not reachable from server)";
		case 0x5D:
			return "Request failed because client's identd could not confirm the user ID in the request";
		}
		return "unknown error code: " + Byte.toString(rep);
	}

	private byte[] encodeConnectHeader() {
		// get parameters
		InetSocketAddress addr = getParameter(_options, CONF_DEST, null);
		String cmd = getParameter(_options, CONF_CMD, CONF_CMD_DEF);
		String userId = getParameter(_options, CONF_USER, "");

		int length = 8 + userId.length() + 1;
		byte[] header = new byte[length];

		// encode version
		header[0] = VER;
		// encode command
		header[1] = (byte) Integer.parseInt(cmd, 16);
		// encode dest port in network byte order / big endian
		header[2] = (byte) ((addr.getPort() & 0xff00) >> 8);
		header[3] = (byte) (addr.getPort() & 0xff);
		// encode dest addr in network byte order / big endian
		System.arraycopy(addr.getAddress().getAddress(), 0, header, 4, 4);
		// encode id
		int len = userId.length();
		if (len > 0) {
			try {
				byte[] user = userId.getBytes("US-ASCII");
				System.arraycopy(user, 0, header, 8, user.length);
			} catch (UnsupportedEncodingException e) {
			}
		}
		header[8 + userId.length()] = '\0';
		return header;
	}

	@SuppressWarnings("unchecked")
	private <T> T getParameter(Map<?, Object> options, Object param, Object defval) {
		Object value = options.get(param);
		if (value == null) {
			value = defval;
		}
		if (value == null) {
			throw new IllegalArgumentException("missing parameter \"" + param + "\"");
		}
		return (T) value;
	}

	// return a short encoded in network byte order (big endian)
	private int get16(ByteBuffer buf) {
		int res = buf.get() & 0xFF;
		res <<= 8;
		res |= buf.get() & 0xFF;
		return res;
	}

	// return an int encoded in network byte order (big endian)
	private int get32(ByteBuffer buf) {
		int res = buf.get();
		res <<= 8;
		res |= buf.get() & 0xFF;
		res <<= 8;
		res |= buf.get() & 0xFF;
		res <<= 8;
		res |= buf.get() & 0xFF;
		return res;
	}

	private ByteBuffer copy(ByteBuffer buf) {
		ByteBuffer copy = ByteBuffer.allocate(buf.remaining());
		copy.put(buf);
		copy.flip();
		return copy;
	}

	private void schedule(ExecutorPolicy policy, Runnable task) {
		Executor queue = _channel.getInputExecutor();
		if (queue instanceof PlatformExecutor) {
			((PlatformExecutor) queue).execute(task, policy);
		} else if (queue instanceof Reactor) {
			((Reactor) queue).getPlatformExecutor().execute(task, policy);
		} else {
			throw new IllegalStateException("Channel Executor must be either a Reactor or a PlatformExecutor");
		}
	}

}
