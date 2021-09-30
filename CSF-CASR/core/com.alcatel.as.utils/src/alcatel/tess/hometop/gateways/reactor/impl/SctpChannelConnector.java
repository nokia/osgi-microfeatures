package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alcatel.as.util.sctp.SctpSocketOption;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.*;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.NotificationHandler;
import com.sun.nio.sctp.SctpStandardSocketOptions;

import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.utils.Log;

public class SctpChannelConnector implements SelectHandler {
	private final static Log _logger = Log.getLogger("as.service.reactor.SctpChannelConnector");
	private final ReactorImpl _reactor;
	private final SctpClientChannelListener _listener;
	private final SocketAddress _local;
	private final InetAddress[] _secondaryLocal;
	private final int _maxOutStreams;
	private final int _maxInStreams;
	private final SocketAddress _to;
	private final Executor _executor;
	private final NioSelector _selector;
	private final Object _attachment;
	private final int _priority;
	private final Security _security;
	private final boolean _nodelay;
	private final Boolean _disableFragments;
	private final Boolean _fragmentInterleave;

	private Future<?> _timer; // accessed from selector thread
	private SelectionKey _key; // accessed from selector thread
	private com.sun.nio.sctp.SctpChannel _socket; // accessed from selector thread
	private boolean _connectionFailedCalled; // accessed from selector thread
	private final int _rcvBufSize;
	private final int _sndBufSize;
	private final boolean _directBuffer; // use direct buffers when reading

	private Map<SctpSocketOption, Object> _sockopts;
	private SctpSocketOptionHelper sockoptHelper = new SctpSocketOptionHelper();
	private final long _linger;
	List<Notification> _notifications = new ArrayList<>(3);

	// Timer scheduled in selector thread
	public class ConnectionTimeout implements Runnable {
		public void run() {
			_logger.info("Could not connect to %s timely.", _to);
			connectionFailed(createFailedChannel(), new TimeoutException("Could not connect to " + _to + " timely"));
		}
	}

	public SctpChannelConnector(SocketAddress local, InetAddress[] secondaryLocal, int maxOutStreams, int maxInStreams,
			SocketAddress to, Object attachment, int priority, SctpClientChannelListener listener, ReactorImpl reactor,
			Executor exec, int rcvBufSize, int sndBufSize, boolean directBuffer, boolean nodelay,
			Boolean disableFragments, Boolean fragmentInterleave, Security security, long linger,
			Map<SctpSocketOption, Object> sockopts) {
		_local = local;
		_secondaryLocal = secondaryLocal;
		_maxOutStreams = maxOutStreams;
		_maxInStreams = maxInStreams;
		_to = to;
		_reactor = reactor;
		_listener = listener;
		_selector = _reactor.getSelector(Helpers._sctpSelectorCounter);
		_attachment = attachment;
		_priority = priority;
		_executor = exec;
		_rcvBufSize = rcvBufSize;
		_sndBufSize = sndBufSize;
		_directBuffer = directBuffer;
		_security = security;
		_nodelay = nodelay;
		_disableFragments = disableFragments;
		_fragmentInterleave = fragmentInterleave;
		_sockopts = sockopts;
		_linger = linger;
	}

	public void connect() {
		connect(0 /* no timeout */);
	}

	public void connect(final long timeout) {
		Helpers.barrier(_executor);
		_selector.schedule(new Runnable() {
			public void run() {
				if (_timer != null) {
					_timer.cancel(false);
				}
				_logger.info("Connecting to %s using timer %d", _to, timeout);
				if (timeout > 0) {
					_timer = _selector.schedule(new ConnectionTimeout(), timeout, TimeUnit.MILLISECONDS);
				}
				doConnect();
			}
		});
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public void selected(SelectionKey key) {
		try {
			if (key.isValid()) {
				if (key.isConnectable()) {
					// we are connecting
					com.sun.nio.sctp.SctpChannel sc = (com.sun.nio.sctp.SctpChannel) key.channel();
					if (!sc.finishConnect()) {
						throw new ConnectException("Could not connect to " + _to);
					}
					connected(sc);
				} else if (key.isReadable()) {
					// we are connected but we are waiting for ASSOC-CHANGE/COMM-UP event
					handleReadCommUP(key);
				}
			} else {
				throw new IOException("selection key not valid");
			}
		}

		catch (Throwable t) {
			_logger.info("Could not connect to %s", t, _to);
			connectionFailed(createFailedChannel(), t);
		}
	}

	private SctpChannel createFailedChannel() {
		return new ClosedSctpChannelImpl(_reactor, _priority, _attachment);
	}

	private SctpChannelImpl createChannel(com.sun.nio.sctp.SctpChannel socket) throws IOException {
		if (_security != null)
			return new SctpChannelSecureImpl(socket, socket.getAllLocalAddresses(), socket.getRemoteAddresses(), _key,
					_reactor, _selector, _listener, _priority, _executor, _attachment, _directBuffer, _nodelay,
					_disableFragments, _fragmentInterleave, _security, true, _linger, _notifications);
		else
			return new SctpChannelImpl(socket, socket.getAllLocalAddresses(), socket.getRemoteAddresses(), _key,
					_reactor, _selector, _listener, _priority, _executor, _attachment, _directBuffer, _nodelay,
					_disableFragments, _fragmentInterleave, _linger, _notifications);
	}

	private void abort() {
		if (_timer != null) {
			_timer.cancel(false);
		}

		try {
			if (_key != null) {
				_key.cancel();
			}
		} catch (Throwable ignored) {
		}

		try {
			if (_socket != null) {
				_socket.close();
			}
		} catch (IOException e) {
		}
	}

	private void doConnect() {
		try {
			_socket = com.sun.nio.sctp.SctpChannel.open();
			_socket.setOption(SctpStandardSocketOptions.SCTP_NODELAY, true);

			for (SctpSocketOption opt : _sockopts.keySet()) {
				Object val = _sockopts.get(opt);
				_logger.debug("Setting socket option %s = %s", opt, val);
				sockoptHelper.setOption(_socket, opt, val);
			}

			if (_local != null) {
				_logger.info("Bind sctp client connection to local addr %s", _local);
				_socket.bind(_local);
			}
			if (_secondaryLocal != null) {
				for (InetAddress secondary : _secondaryLocal) {
					_socket.bindAddress(secondary);
				}
			}

			if (_rcvBufSize > 0) {
				_socket.setOption(SctpStandardSocketOptions.SO_RCVBUF, _rcvBufSize);
			}
			if (_sndBufSize > 0) {
				_socket.setOption(SctpStandardSocketOptions.SO_SNDBUF, _sndBufSize);
			}
			_socket.configureBlocking(false);
			if (!_socket.connect(_to, _maxOutStreams, _maxInStreams)) {
				_key = _selector.registerSelectHandler(_socket, SelectionKey.OP_CONNECT, SctpChannelConnector.this);
			} else {
				connected(_socket);
			}
		}

		catch (Throwable t) {
			_logger.info("Failed to connect to %s", t, _to);
			connectionFailed(createFailedChannel(), t);
		}
	}

	private void connected(com.sun.nio.sctp.SctpChannel channel) throws IOException {
		if (_logger.isInfoEnabled())
			_logger.info("Connection established to %s", _to);

		// check if channel association is available, if not, wait for the
		// ASSOC-CHANGE/COMM-UP event
		if (assocAvail(channel)) {
			connectionEstablished(channel);
		}
	}

	private void connectionEstablished(com.sun.nio.sctp.SctpChannel channel)
			throws IOException {
		if (_timer != null) {
			_timer.cancel(false);
		}

		final SctpChannelImpl cnx = createChannel(channel); // read interest disabled
		_connectionFailedCalled = true;
		_executor.execute(new Runnable() {
			public void run() {
				try {
					_selector.getMeters().addSctpChannel(1, _security != null);
					_listener.connectionEstablished(cnx);
					cnx.armSctpEventTimers();
					cnx.enableReadingInternal();
				} catch (Throwable t) {
					_logger.warn("got unexpected exception while invoking connectionEstablished callback", t);
					cnx.shutdown(); // abort
				}
			}
		});
	}

	private void connectionFailed(final SctpChannel channel, final Throwable t) {
		abort();
		if (!_connectionFailedCalled) {
			_connectionFailedCalled = true;
			_executor.execute(new Runnable() {
				public void run() {
					try {
						_listener.connectionFailed(channel, t);
					}

					catch (Throwable t2) {
						_logger.warn("Got exception while invoking connectionFailed callback", t2);
					}
				}
			});
		}
	}

	private boolean assocAvail(com.sun.nio.sctp.SctpChannel channel) throws IOException {
		if (channel.association() != null) {
			return true;
		}

		// prepare to read upcoming ASSOC-CHANGE/COMM-UP event
		if (_key == null) {
			_key = _selector.registerSelectHandler(_socket, SelectionKey.OP_READ, SctpChannelConnector.this);
		} else {
			_key.interestOps(SelectionKey.OP_READ);
		}

		return false;
	}

	private void handleReadCommUP(SelectionKey key) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(0); // we just wait for the
		com.sun.nio.sctp.SctpChannel channel = (com.sun.nio.sctp.SctpChannel) key.channel();
		AtomicBoolean commUP = new AtomicBoolean();

		MessageInfo info = channel.receive(buf, null, new AbstractNotificationHandler<Object>() {
			@Override
			public HandlerResult handleNotification(Notification notification, Object attachment) {
				// backup notif, we'll pass them to the created sctp channel.
				_logger.debug("storing early notification: " + notification);
				_notifications.add(notification);
				return HandlerResult.RETURN;
			}

			public HandlerResult handleNotification(PeerAddressChangeNotification notification, Object attachment) {
				_logger.debug("storing early notification: " + notification);
				_notifications.add(notification);
				return HandlerResult.RETURN;
			}

			public HandlerResult handleNotification(SendFailedNotification notification, Object attachment) {
				_logger.debug("storing early notification: " + notification);
				_notifications.add(notification);
				return HandlerResult.RETURN;
			}

			public HandlerResult handleNotification(ShutdownNotification notification, Object attachment) {
				connectionFailed(createFailedChannel(),
						new IOException("Conection failed, received SHUTDOWN event: " + notification));
				return HandlerResult.RETURN;
			}

			@Override
			public HandlerResult handleNotification(AssociationChangeNotification notification, Object attachment) {
				_logger.debug("got notification: " + notification);
				switch (notification.event()) {
				case COMM_UP:
					commUP.set(true);
					break;

				case RESTART:
					break;

				default:
					connectionFailed(createFailedChannel(),
							new IOException("Can't initialize sctp association (event:" + notification.event() + ")"));
					break;
				}
				return HandlerResult.RETURN;
			}
		});

		if (info != null) {
			if (info.bytes() == -1) {
				throw new IOException("Connection closed");
			} else {
				throw new IOException("Received a message before receiving COMM-UP event");
			}
		}

		if (commUP.get()) {
			key.interestOps(0);
			connectionEstablished(channel);
		}
	}

}
