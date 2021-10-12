// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.sctp_boolean;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.*;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.NotificationHandler;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpStandardSocketOptions;

import alcatel.tess.hometop.gateways.reactor.AsyncChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannel;
import alcatel.tess.hometop.gateways.reactor.SctpServerChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.utils.Log;

@SuppressWarnings("restriction")
public class SctpServerChannelImpl extends AbstractServerChannel implements SctpServerChannel {
	private final SocketAddress _local;
	private final static Log _logger = Log.getLogger("as.service.reactor.SctpServerChannelImpl");
	private final SctpServerChannelListener _listener;
	private final InetAddress[] _secondaryLocals;
	private final int _maxOutStreams;
	private final int _maxInStreams;
	private final int _backlog;
	private volatile Security _security;
	private volatile com.sun.nio.sctp.SctpServerChannel _ssc;
	private final int _rcvBufSize;
	private final int _sndBufSize;
	private final boolean _directBuffer;
	private final Boolean _disableFragments;
	private final Boolean _fragmentInterleave;
	private final Map<SctpSocketOption, Object> _sockopts;
	private final SctpSocketOptionHelper sockoptHelper = new SctpSocketOptionHelper();
	private final long _linger;
	private final List<Notification> _notifications = new ArrayList<>(1);

	public SctpServerChannelImpl(SocketAddress local, InetAddress[] secondaryLocals, int maxOutStreams,
			int maxInStreams, int priority, SctpServerChannelListener listener, ReactorImpl reactor, int backlog,
			int rcvBufSize, int sndBufSize, boolean enableRead, boolean directBuffer, Boolean disableFragments,
			Boolean fragmentInterleave, Security security, Map<SctpSocketOption, Object> sockopts,
			long disableAcceptTimeout, long linger) {
		super(reactor, null, enableRead, disableAcceptTimeout, priority);
		_local = local;
		_secondaryLocals = secondaryLocals;
		_listener = listener;
		_maxOutStreams = maxOutStreams;
		_maxInStreams = maxInStreams;
		_backlog = backlog;
		_rcvBufSize = rcvBufSize;
		_sndBufSize = sndBufSize;
		_directBuffer = directBuffer;
		_security = security;
		_sockopts = sockopts;
		_fragmentInterleave = fragmentInterleave;
		_disableFragments = disableFragments;
		_linger = linger;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("SctpServerChannel: ");
		sb.append("addr=").append(_local);
		return sb.toString();
	}

	@Override
	public Set<SocketAddress> getAllLocalAddresses() throws IOException {
		return _ssc.getAllLocalAddresses();
	}

	@Override
	public boolean isSecure() {
		return (_security != null);
	}

	@Override
	public void updateSecurity(Security security) {
		if (_security == null) {
			throw new IllegalStateException("Can't updated security parameters on unsecured channel.");
		}
		_security = security;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getSocketOption(SctpSocketOption option, Object extra) throws IOException {
		_logger.debug("Getting option %s = %s", option, extra);
		return (T) sockoptHelper.getOption(_ssc, option, extra);
	}

	@Override
	public SctpServerChannel setSocketOption(SctpSocketOption option, Object param) throws IOException {
		_logger.debug("Setting option %s = %s", option, param);
		sockoptHelper.setOption(_ssc, option, param);
		return this;
	}

	// called from any thread
	@Override
	protected AbstractSelectableChannel doListen() throws IOException {
		_ssc = com.sun.nio.sctp.SctpServerChannel.open();
		try {
			_ssc.configureBlocking(false);

			for (SctpSocketOption opt : _sockopts.keySet()) {
				Object val = _sockopts.get(opt);
				setSocketOption(opt, val);
			}

			if (!_sockopts.containsKey(SctpSocketOption.SCTP_SO_REUSEADDR)) {
				try {
					setSocketOption(SctpSocketOption.SCTP_SO_REUSEADDR, sctp_boolean.TRUE);
				} catch (IOException e) {
					_logger.warn("Unable to set socket option SCTP_SO_REUSEADDR");
				}
			}

			// setSocketOption(SctpSocketOption.SCTP_NODELAY, new sctp_boolean(nodelay));
			if (_disableFragments != null)
				setSocketOption(SctpSocketOption.SCTP_DISABLEFRAGMENTS, new sctp_boolean(_disableFragments));
			if (_fragmentInterleave != null)
				setSocketOption(SctpSocketOption.SCTP_FRAGMENT_INTERLEAVE, new sctp_boolean(_fragmentInterleave));
			_ssc.bind(_local, _backlog);

			_ssc.setOption(SctpStandardSocketOptions.SCTP_INIT_MAXSTREAMS,
					SctpStandardSocketOptions.InitMaxStreams.create(_maxInStreams, _maxOutStreams));

			if (_secondaryLocals != null) {
				for (InetAddress secondary : _secondaryLocals) {
					_ssc.bindAddress(secondary);
				}
			}

			return _ssc;
		}

		catch (IOException e) {
			if (_ssc != null) {
				try {
					_ssc.close();
				} catch (IOException ignored) {
				}
			}
			throw e;
		}
	}

	// called from selector
	@Override
	protected void doAccept(SelectionKey serverKey, NioSelector selector) throws Exception {
		SctpChannel channel = null;

		try {
			// Accept new client connection request
			channel = ((com.sun.nio.sctp.SctpServerChannel) serverKey.channel()).accept();
			if (channel == null) {
				// there is no channel to currently accept.
				return;
			}

			// Configure the accepted channel
			channel.configureBlocking(false);
			channel.setOption(SctpStandardSocketOptions.SCTP_NODELAY, true);

			if (_rcvBufSize > 0) {
				channel.setOption(SctpStandardSocketOptions.SO_RCVBUF, _rcvBufSize);
			}
			if (_sndBufSize > 0) {
				channel.setOption(SctpStandardSocketOptions.SO_SNDBUF, _sndBufSize);
			}

			// Obtain sctp remote addresses, if we can't it means the socket is being closed
			Set<SocketAddress> localAddrs = channel.getAllLocalAddresses();
			Set<SocketAddress> remoteAddrs = channel.getRemoteAddresses();
			if (remoteAddrs == null || remoteAddrs.size() == 0) { // normally, remoteAddrs can't be null.
				_logger.info("could not accept sctp socket on addr " + _local
						+ ": no remote cnx addresses found (likely that the socket is closed)");
				close(channel);
				return;
			}

			// check if channel association is available, if not, wait for the
			// ASSOC-CHANGE/COMM-UP event
			if (!assocAvailable(serverKey, channel, localAddrs, selector, remoteAddrs)) {
				return;
			}

			finishAccept(serverKey, channel, null, localAddrs, selector, remoteAddrs);
		}

		catch (Throwable t) {
			close(channel);
			throw t;
		}
	}

	private void finishAccept(SelectionKey serverKey, SctpChannel channel, SelectionKey channelKey,
			Set<SocketAddress> localAddrs, NioSelector selector, Set<SocketAddress> remoteAddrs) throws IOException {
		// Create reactor sctp channel impl (reads are disabled by default)
		SctpChannelImpl cnx;
		if (_security == null) {
			cnx = new SctpChannelImpl(channel, localAddrs, remoteAddrs, channelKey, _reactor, selector, _listener,
					AsyncChannel.MAX_PRIORITY, _reactor, _attached, _directBuffer, true, null, null, _linger, _notifications);
		} else {
			cnx = new SctpChannelSecureImpl(channel, localAddrs, remoteAddrs, channelKey, _reactor, selector, _listener,
					AsyncChannel.MAX_PRIORITY, _reactor, _attached, _directBuffer, true, null, null, _security, false,
					_linger, _notifications);
		}

		// disable ACCEPT to avoid memory leaks.
		serverKey.interestOps(0);

		_reactor.schedule(new Runnable() {
			public void run() {
				try {
					// Increment number of accepted sockets. The counter will be decremented from
					// cnx.abort method.
					selector.getMeters().addSctpChannel(1, isSecure());
					try {
						_listener.connectionAccepted(SctpServerChannelImpl.this, cnx);
						cnx.armSctpEventTimers();
					} catch (Throwable t) {
						_logger.warn("exception from connectionAccepted callback", t);
					}

					// Enable READ_OP for accepted socket, and ACCEPT_OP for server socket.
					// Optimization: schedule one single task in selector thread which will do the
					// two things.
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

	private void close(SctpChannel channel) {
		try {
			if (channel != null) {
				channel.close();
			}
		} catch (Throwable e) {
		}
	}

	// called from reactor
	@Override
	protected void doServerConnectionClosed() {
		_listener.serverConnectionClosed(SctpServerChannelImpl.this, null);
	}

	@Override
	protected void doServerConnectionOpened() {
		// We have no such callback
	}

	@Override
	protected void doServerConnectionFailed(Throwable t) {
		// We have no such callback
	}

	private boolean assocAvailable(SelectionKey serverKey, com.sun.nio.sctp.SctpChannel channel,
			Set<SocketAddress> localAddrs, NioSelector selector, Set<SocketAddress> remoteAddr) throws IOException {
		if (channel.association() != null) {
			return true;
		}

		// wait forupcoming ASSOC-CHANGE/COMM-UP event, at this point we know the
		// association will be available
		CommUpTracker commUpTracker = new CommUpTracker(serverKey, channel, localAddrs, selector, remoteAddr);
		selector.registerSelectHandler(channel, SelectionKey.OP_READ, commUpTracker);
		return false;
	}

	/**
	 * Read a freshly accepted sctp socket until we get it's COMM_UP assoc event.
	 * Then after, finish accepting the socket.
	 */
	private class CommUpTracker extends AbstractNotificationHandler<SelectionKey> implements SelectHandler {
		private boolean _commUp;
		private final com.sun.nio.sctp.SctpChannel _channel;
		private final SelectionKey _serverKey;
		private final Set<SocketAddress> _localAddrs;
		private final NioSelector _selector;
		private final Set<SocketAddress> _remoteAddrs;

		CommUpTracker(SelectionKey serverKey, SctpChannel channel, Set<SocketAddress> localAddrs, NioSelector selector,
				Set<SocketAddress> remoteAddrs) {
			_serverKey = serverKey;
			_channel = channel;
			_localAddrs = localAddrs;
			_selector = selector;
			_remoteAddrs = remoteAddrs;
		}

		@Override
		public void selected(SelectionKey key) {
			try {
				if (key.isValid()) {
					if (key.isReadable()) {
						ByteBuffer buf = ByteBuffer.allocate(0);
						MessageInfo info = _channel.receive(buf, key, this);

						if (info != null) {
							// we have received a message before receive the COMM_UP event: that's not
							// possible !
							if (info.bytes() == -1) {
								throw new IOException("Could not accept sctp socket, socket closed");
							} else {
								throw new IOException("Could not accept sctp socket, received message before COMM-UP");
							}
						}

						if (_commUp) {
							key.interestOps(0);
							finishAccept(_serverKey, _channel, key, _localAddrs, _selector, _remoteAddrs);
						}
					}
				} else {
					throw new IOException("selection key not valid");
				}
			}

			catch (Throwable t) {
				_logger.info("Could not accept sctp socket", t);
				Helpers.runSafe(() -> key.cancel());
				Helpers.runSafe(() -> _channel.close());
			}
		}

		@Override
		public int getPriority() {
			return AsyncChannel.MAX_PRIORITY;
		}

		@Override
		public HandlerResult handleNotification(Notification notification, SelectionKey key) {
			_logger.debug("storing early notification: " + notification);
			_notifications.add(notification);
			return HandlerResult.RETURN;
		}

		public HandlerResult handleNotification(PeerAddressChangeNotification notification, SelectionKey key) {
			_logger.debug("storing early notification: " + notification);
			_notifications.add(notification);
			return HandlerResult.RETURN;
		}

		public HandlerResult handleNotification(SendFailedNotification notification, SelectionKey key) {
			_logger.debug("storing early notification: " + notification);
			_notifications.add(notification);
			return HandlerResult.RETURN;
		}

		public HandlerResult handleNotification(ShutdownNotification notification, SelectionKey key) {
			_logger.debug("could not accept sctp socket, got SHUTDOWN notification: " + notification);
			Helpers.runSafe(() -> key.cancel());
			Helpers.runSafe(() -> _channel.close());
			return HandlerResult.RETURN;
		}

		@Override
		public HandlerResult handleNotification(AssociationChangeNotification notification, SelectionKey key) {
			switch (notification.event()) {
			case COMM_UP:
				_logger.debug("got notification: " + notification);
				_commUp = true;
				break;

			case RESTART:
				_logger.debug("got notification: " + notification);
				break;

			default:
				_logger.debug("could not accept sctp socket, got notification: " + notification);
				Helpers.runSafe(() -> key.cancel());
				Helpers.runSafe(() -> _channel.close());
				break;
			}
			return HandlerResult.RETURN;
		}

	}

}
