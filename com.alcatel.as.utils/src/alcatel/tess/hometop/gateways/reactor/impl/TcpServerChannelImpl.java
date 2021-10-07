package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannel;
import alcatel.tess.hometop.gateways.reactor.TcpServerChannelListener;

public class TcpServerChannelImpl extends AbstractServerChannel implements TcpServerChannel {
	private final TcpServerChannelListener _listener;
	private final Logger _logger;
	private final int _backlog;
	private final boolean _noDelay;
	private volatile InetSocketAddress _addr;
	private final boolean _useDirectBuffer;
	private final int _autoFlushSize;
	private final int _sndbuf;
	private final int _rcvbuf;
	private volatile Security _security;
	private final long _linger;

	public TcpServerChannelImpl(ReactorImpl reactor, TcpServerChannelListener listener, InetSocketAddress addr,
			Object attachment, int backlog, boolean noDelay, boolean enableRead, boolean useDirectBuffer,
			int autoFlushSize, int sndbuf, int rcvbuf, Security security, long disableAcceptTimeout, long linger) 
	{
		super(reactor, attachment, enableRead, disableAcceptTimeout, 0);
		_listener = listener;
		_addr = addr;
		_logger = Logger.getLogger("as.service.reactor." + reactor.getName() + ".TcpServerChannelImpl");
		_attached = attachment;
		_backlog = backlog;
		_noDelay = noDelay;
		_useDirectBuffer = useDirectBuffer;
		_autoFlushSize = autoFlushSize;
		_sndbuf = sndbuf;
		_rcvbuf = rcvbuf;
		_security = security;
		_linger = linger;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("TcpServerChannel: ");
		sb.append("secure=").append(isSecure());
		sb.append(", addr=").append(_addr);
		return sb.toString();
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return _addr;
	}

	@Override
	public boolean isSecure() {
		return _security != null;
	}

	@Override
	public void updateSecurity(Security security) {
		if (_security == null) {
			throw new IllegalStateException("Can't updated security parameters on unsecured channel.");
		}
		_security = security;
	}

	@Override
	protected AbstractSelectableChannel doListen() throws IOException {
		// open listening socket
		ServerSocketChannel ssc = ServerSocketChannel.open();

		try {
			ServerSocket sock = ssc.socket();
			sock.setReuseAddress(true);
			sock.bind(_addr, _backlog);
			_addr = (InetSocketAddress) sock.getLocalSocketAddress();
			ssc.configureBlocking(false);
			return ssc;
		} catch (IOException e) {
			if (ssc != null) {
				try { ssc.close();} catch (IOException ignored) {}
			}
			throw e;
		}
	}
	
	@Override
	protected void doServerConnectionOpened() {
		_listener.serverConnectionOpened(this);
	}

	@Override
	protected void doServerConnectionClosed() {
		_listener.serverConnectionClosed(this);		
	}

	@Override
	protected void doServerConnectionFailed(Throwable t) {
		_listener.serverConnectionFailed(this, t);		
	}
	
	// called in selector
	@Override
	protected void doAccept(SelectionKey serverKey, NioSelector selector) throws Exception {
		SocketChannel channel = null;
		try {
			// Accept new client connection request
			channel = ((ServerSocketChannel) serverKey.channel()).accept();
			if (channel == null) {
				// another selector thread has handled the event.
				return;
			}
			
			channel.socket().setKeepAlive(true);
			channel.socket().setTcpNoDelay(_noDelay);
			if (_rcvbuf > 0) {
				channel.socket().setReceiveBufferSize(_rcvbuf);
			}
			if (_sndbuf > 0) {
				channel.socket().setSendBufferSize(_sndbuf);
			}
			channel.configureBlocking(false);

			TcpChannelImpl cnx;
			if (isSecure()) {
				cnx = new TcpChannelSecureImpl(channel, null, _reactor, selector, _listener,
						AsyncChannel.MAX_PRIORITY, false, _reactor.getPlatformExecutor(), _useDirectBuffer,
						_autoFlushSize, _security, _security.isDelayed(), _linger);
			} else {
				cnx = new TcpChannelImpl(channel, null, _reactor, selector, _listener, AsyncChannel.MAX_PRIORITY,
						_reactor.getPlatformExecutor(), _useDirectBuffer, _autoFlushSize, _linger);
			}

			cnx.attach(_attached);

			// disable ACCEPT to avoid memory leaks.
			serverKey.interestOps(0);

			if (_logger.isInfoEnabled()) {
				_logger.info("Accepted new connection: " + cnx + " (tcpNoDelay=" + _noDelay + ")");
			}

			_reactor.schedule(new Runnable() {
				public void run() {
					try {
						// Increment number of accepted sockets. The counter will be decremented frm cnx.abort method.
						selector.getMeters().addTcpChannel(1, isSecure()); 
						_listener.connectionAccepted(TcpServerChannelImpl.this, cnx);
						
						// Enable READ_OP for accepted socket, and ACCEPT_OP for server socket.
						// Optimization: schedule one single task in selector thread which will do the two things.
						selector.schedule(() -> {
							cnx.enableReadingInternal();
							try {
								serverKey.interestOps(SelectionKey.OP_ACCEPT);
							} catch (CancelledKeyException e) {									
							}
						});
					} catch (Throwable t) {
						_logger.warn("Exception caught while calling connectionAccepted callback", t);
						cnx.shutdown(); // abort
					}
				}
			});
		}

		catch (Throwable t) {
			if (channel != null) {
				try {
					channel.close();
				} catch (IOException e) {
				}
			}
			throw t;
		}			
	}

}
