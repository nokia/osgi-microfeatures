package com.nokia.as.reactor.socks;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
public class Socks5TcpClientListener extends FilterTcpClientListener {

	public final static String CONF_DEST = Socks4TcpClientListener.CONF_DEST;
	public final static String CONF_CMD = Socks4TcpClientListener.CONF_CMD;
	public final static String CONF_CMD_DEF = "01";
	public final static String CONF_USER = Socks4TcpClientListener.CONF_USER;
	public final static String CONF_PASSWORD = "socks.password";
	
	private final static Logger _log = Logger.getLogger(Socks5TcpClientListener.class);
	private final static byte SOCKS_VERSION = 0x05;
	private final static int LENGTH_OF_IPV4 = 4;
	private final static int LENGTH_OF_IPV6 = 16;
	private final static byte ATYPE_IPV4 = 0x01;
	private final static byte ATYPE_DOMAIN_NAME = 0x03;
	private final static byte ATYPE_IPV6 = 0x04;
	private final static int RESERVED = 0x00;
	private static final byte AUTHENTICATION_SUCCEEDED = 0x00;

	private final TimerService _timer;
	private boolean _disableReading;
	private ScheduledFuture<?> _future;
	private ByteBuffer _buffered;
	private State _state;
	private AuthMethod _method;

	private interface State {
		int messageReceived(Socks5TcpClientListener context, TcpChannel cnx, ByteBuffer buf);
		void connectionClosed(Socks5TcpClientListener context, TcpChannel cnx);
	}

	private interface AuthMethod {
		byte getCode();
		boolean sendRequest(Socks5TcpClientListener context, TcpChannel cnx);
		int messageReceived(Socks5TcpClientListener context, TcpChannel cnx, ByteBuffer buf);
	};

	public Socks5TcpClientListener(TcpClientChannelListener listener, Map<?, Object> options, TimerService wheelTimer) {
		super(listener, options);
		_timer = wheelTimer;
		_state = _initialState;
	}

	@Override
	public void connectionEstablished(TcpChannel channel) {
		super.setChannel(channel);
		Long timeout = getParameter(_options, ReactorProvider.TcpClientOption.TIMEOUT, 0L);
		if (timeout != null && timeout > 0L) {
			_future = _timer.schedule(_channel.getInputExecutor(), this::timeout, (long) timeout,
					TimeUnit.MILLISECONDS);
		}
		_channel.send(encodeInitialHeader(), false);
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
		return _state.messageReceived(this, cnx, buf);
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
		_state.connectionClosed(this, cnx);
	}

	private byte[] encodeInitialHeader() {
		int length = 2 + _methods.size();
		byte[] header = new byte[length];

		header[0] = SOCKS_VERSION;
		header[1] = (byte) _methods.size();
		Iterator<Byte> it = _methods.keySet().iterator();
		IntStream.range(0, _methods.size()).map(idx -> header[idx + 2] = it.next());
		return header;
	}

	private void sendConnectRequest(TcpChannel cnx) {
		InetSocketAddress addr = getParameter(_options, CONF_DEST, null);
		String cmd = getParameter(_options, CONF_CMD, CONF_CMD_DEF);

		final byte[] bytesOfAddress = addr.getAddress().getAddress();
		final int ADDRESS_LENGTH = bytesOfAddress.length;
		final int port = addr.getPort();

		byte addressType = -1;
		byte[] header = null;

		if (ADDRESS_LENGTH == LENGTH_OF_IPV4) {
			addressType = ATYPE_IPV4;
			header = new byte[6 + LENGTH_OF_IPV4];
		} else if (ADDRESS_LENGTH == LENGTH_OF_IPV6) {
			addressType = ATYPE_IPV6;
			header = new byte[6 + LENGTH_OF_IPV6];
		} else {
			throw new IllegalStateException("Address error");
		}

		header[0] = (byte) SOCKS_VERSION;
		header[1] = (byte) Integer.parseInt(cmd, 16);
		header[2] = RESERVED;
		header[3] = addressType;
		System.arraycopy(bytesOfAddress, 0, header, 4, ADDRESS_LENGTH);// copy address bytes
		header[4 + ADDRESS_LENGTH] = (byte) ((port & 0xff00) >> 8);
		header[5 + ADDRESS_LENGTH] = (byte) (port & 0xff);
		cnx.send(header, false);
	}

	private boolean checkBuffered() {
		if (_buffered != null) {
			try {
				_listener.messageReceived(Socks5TcpClientListener.this, _buffered);
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
		case 0x01:
			return "Connection failed";
		case 0x02:
			return "connection not allowed by ruleset";
		case 0x03:
			return "network unreachable";
		case 0x04:
			return "host unreachable";
		case 0x05:
			return "connection refused by destination host";
		case 0x06:
			return "TTL expired";
		case 0x07:
			return "command not supported / protocol error";
		case 0x08:
			return "address type not supported";
		}
		return "unknown error code: " + Byte.toString(rep);
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

	public String getIPv4AsString(int ip) {
		if (ip == 0) {
			return "";
		}
		StringBuilder buff = new StringBuilder(15);
		buff.append((ip >>> 24) & 0xFF);
		buff.append('.');
		buff.append(((ip >> 16) & 0xFF));
		buff.append('.');
		buff.append(((ip >> 8) & 0xFF));
		buff.append('.');
		buff.append(ip & 0xFF);
		return buff.toString();
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

	// ------------- Authent methods ---------------------

	private final static AuthMethod _noAuthMethod = new AuthMethod() {
		@Override
		public byte getCode() {
			return 0x00;
		}

		@Override
		public boolean sendRequest(Socks5TcpClientListener context, TcpChannel cnx) {
			return false;
		}

		@Override
		public int messageReceived(Socks5TcpClientListener context, TcpChannel cnx, ByteBuffer buf) {
			return 0;
		}
	};

	private final static AuthMethod _userPasswordAuthMethod = new AuthMethod() {
		@Override
		public byte getCode() {
			return 0x02;
		}

		@Override
		public boolean sendRequest(Socks5TcpClientListener context, TcpChannel cnx) {
		    /*
		     * RFC 1929
		     * 
		     * +----+------+----------+------+----------+
		     * |VER | ULEN | UNAME | PLEN | PASSWD | |
		     * +----+------+----------+------+----------+ | 1 | 1 | 1 to 255 | 1 | 1 to 255 |
		     * +----+------+----------+------+----------+ The VER field contains the current version of the
		     * subnegotiation, which is X’01’.
		     */

			String user = context.getParameter(context._options, CONF_USER, "");
			String password = context.getParameter(context._options, CONF_PASSWORD, "");
			byte[] userBytes = user.getBytes();
			byte[] passwordBytes = password.getBytes();			
			byte[] authRequest = new byte[3 + userBytes.length + passwordBytes.length];
		    authRequest[0] = 0x01; // VERSION
		    authRequest[1] = (byte) userBytes.length; // ULEN
		    System.arraycopy(userBytes, 0, authRequest, 2, userBytes.length);
		    authRequest[2 + userBytes.length] = (byte) passwordBytes.length; 
		    System.arraycopy(passwordBytes, 0, authRequest, 3 + userBytes.length, passwordBytes.length);

		    cnx.send(authRequest, false);
			return true;
		}

		@Override
		public int messageReceived(Socks5TcpClientListener context, TcpChannel cnx, ByteBuffer buf) {
			/**
			 * RFC 1929. resp size=2. byte[0] = version (1). bytes[1] = status (0=success, else failure)
			 */
			if (buf.remaining() < 2) {
				return 2 - buf.remaining();
			}
			buf.get(); // skip version
			byte status = buf.get();
			return status == AUTHENTICATION_SUCCEEDED ? 0 : -1;
		}
	};

	private final static Map<Byte, AuthMethod> _methods = Stream.of(_noAuthMethod, _userPasswordAuthMethod)
			.collect(Collectors.toMap(AuthMethod::getCode, Function.identity()));

	// --------------- States -----------------------------

	/**
	 * Our initial state: we have sent the initial header and we are expecting from
	 * server its preferred authent method.
	 */
	private final static State _initialState = new State() {
		@Override
		public int messageReceived(Socks5TcpClientListener context, TcpChannel cnx, ByteBuffer buf) {
			// check timeout
			if (context._future != null && !context._future.cancel(false)) {
				return 0; // timeout
			}

			if (context._method == null) {
				if (buf.remaining() < 2) {
					if (_log.isDebugEnabled())
						_log.debug("received too short message: " + buf.remaining());
					return 2 - buf.remaining();
				}

				byte sockVersion = buf.get();
				if (sockVersion != SOCKS_VERSION) {
					if (_log.isDebugEnabled()) {
						_log.debug("connect failed: server responded with bad sockVersion: " + sockVersion);
					}
					cnx.close();
					return 0;
				}

				byte authMethod = buf.get();
				context._method = _methods.get(authMethod);
				if (context._method == null) {
					if (_log.isDebugEnabled()) {
						_log.debug("connect failed: server refused proposed methods");
					}
					cnx.close();
					return 0;
				}
				
				if (_log.isDebugEnabled()) {
					_log.debug("server chose authenticate method: " + authMethod);
				}
				
				boolean sent = context._method.sendRequest(context, cnx);
				
				if (! sent) {
					// no auth request sent, send connect request
					context.sendConnectRequest(cnx);
					context._state = _waitForConnectResponseState;
				}
			} else {
				// handle auth method reply

				int status = context._method.messageReceived(context, cnx, buf);
				switch (status) {
				case -1:
					// authentication method failed
					if (_log.isDebugEnabled()) {
						_log.debug("connect failed: authentication method rejected connection");
					}
					cnx.close();
					break;
				case 0:
					// authentication succeeded, send connect request
					context.sendConnectRequest(cnx);
					context._state = _waitForConnectResponseState;
					break;

				default:
					// not enough data available for the authent method
					return status;
				}
			}

			return 0;
		}

		@Override
		public void connectionClosed(Socks5TcpClientListener context, TcpChannel cnx) {
			// we are still in reactor companion thread, so the socket is closed before we
			// can get the actual socks ACK message
			ScheduledFuture<?> future = context._future;
			if (future != null) {
				future.cancel(false);
			}
			context._listener.connectionFailed(cnx, new IOException("socks socket closed"));
		}
	};

	/**
	 * We have sent a connect request to the server, and we are waiting for its
	 * response.
	 */
	private final static State _waitForConnectResponseState = new State() {
		@Override
		public int messageReceived(Socks5TcpClientListener context, TcpChannel cnx, ByteBuffer buf) {
			if (buf.remaining() < 4) {
				return 4 - buf.remaining();
			}

			buf.mark();

			byte version = buf.get();
			byte status = buf.get();
			byte reserved = buf.get();
			byte addrType = buf.get();
			byte[] addr = null;

			if (_log.isDebugEnabled()) {
				_log.debug("Server connect response: version=" + version + ", status=" + status + ", reserved="
						+ reserved + ", addrType=" + addrType);
			}

			switch (addrType) {
			case ATYPE_IPV4:
				if (buf.remaining() < 4) {
					int missing = 4 - buf.remaining();
					buf.reset();
					return missing;
				}
				int destip = context.get32(buf);
				if (_log.isDebugEnabled()) {
					_log.debug("Server replied with ipv4 addr: " + context.getIPv4AsString(destip));
				}
				break;
			case ATYPE_DOMAIN_NAME:
				int size = buf.get() & 0xff;
				if (buf.remaining() < size) {
					int missing = size - buf.remaining();
					buf.reset();
					return missing;
				}
				addr = new byte[size];
				buf.get(addr, 0, size);
				if (_log.isDebugEnabled()) {
					_log.debug("Server replied with ipv4 addr with domain: " + new String(addr));
				}
				break;
			case ATYPE_IPV6:
				if (buf.remaining() < 16) {
					int missing = 16 - buf.remaining();
					buf.reset();
					return missing;
				}
				addr = new byte[16];
				buf.get(addr, 0, addr.length);
				if (_log.isDebugEnabled()) {
					_log.debug("Server replied with ipv6 addr: " + new String(addr));
				}
				break;
			}

			if (buf.remaining() < 2) {
				int missing = 2 - buf.remaining();
				buf.reset();
				return missing;
			}
			int port = context.get16(buf);
			if (_log.isDebugEnabled()) {
				_log.debug("Server replied with port: " + port);
			}

			switch (status) {
			case 0x00:
				// connection accepted
				if (_log.isDebugEnabled())
					_log.debug("socks req granted.");
				context._buffered = buf.hasRemaining() ? context.copy(buf) : null;
				try {
					context._listener.connectionEstablished(context);
				} catch (Throwable err) {
					_log.warn("unexpected exception while calling connectionEstablished", err);
					cnx.close();
					return 0;
				}
				context.schedule(ExecutorPolicy.INLINE, () -> { // note that we are also making a memory barrier.
					if (!context._disableReading && ! context.checkBuffered()) {
						return;
					}
					context._state = _connectedState;
				});
				break;

			default:
				// connection rejected
				if (_log.isDebugEnabled()) {
					_log.debug("connect failed: " + context.getRequestFailed(status));
				}
				cnx.close();
				break;
			}

			return 0;
		}

		@Override
		public void connectionClosed(Socks5TcpClientListener context, TcpChannel cnx) {
			// we are still in reactor companion thread, so the socket is closed before we
			// can get the actual socks ACK message
			ScheduledFuture<?> future = context._future;
			if (future != null) {
				future.cancel(false);
			}
			context._listener.connectionFailed(cnx, new IOException("socks socket closed"));
		}
	};

	private final static State _connectedState = new State() {
		@Override
		public int messageReceived(Socks5TcpClientListener context, TcpChannel cnx, ByteBuffer buf) {
			return context._listener.messageReceived(cnx, buf);
		}

		@Override
		public void connectionClosed(Socks5TcpClientListener context, TcpChannel cnx) {
			context._listener.connectionClosed(cnx);
		}
	};
}
