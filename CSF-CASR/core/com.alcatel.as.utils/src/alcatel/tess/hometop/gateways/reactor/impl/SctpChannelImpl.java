package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.util.sctp.SctpSocketOption;
import com.alcatel.as.util.sctp.sctp_boolean;
import com.alcatel.as.util.sctp.sctp_paddrinfo;
import com.sun.nio.sctp.AbstractNotificationHandler;
import com.sun.nio.sctp.AssociationChangeNotification;
import com.sun.nio.sctp.HandlerResult;
import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.PeerAddressChangeNotification;
import com.sun.nio.sctp.SctpStandardSocketOptions;
import com.sun.nio.sctp.SendFailedNotification;
import com.sun.nio.sctp.ShutdownNotification;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.SctpAssociation;
import alcatel.tess.hometop.gateways.reactor.SctpChannel;
import alcatel.tess.hometop.gateways.reactor.SctpChannelListener;
import alcatel.tess.hometop.gateways.reactor.SctpChannelListener.AddressEvent;
import alcatel.tess.hometop.gateways.reactor.impl.Meters.ReactorMeters.SctpMeters;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;
import alcatel.tess.hometop.gateways.utils.Log;

@SuppressWarnings("restriction")
public class SctpChannelImpl extends AbstractNotificationHandler<SctpChannel> implements SctpChannel, SelectHandler {
	private final static Log _logger = Log.getLogger("as.service.reactor.SctpChannelImpl");
	protected final ConcurrentLinkedQueue<Message> _queue = new ConcurrentLinkedQueue<SctpChannelImpl.Message>();
	private final ReactorImpl _reactor;
	protected final SelectionKey _key;
	protected final com.sun.nio.sctp.SctpChannel _socket;
	private final int _remotePort;
	private final AtomicBoolean _writeInterestScheduled = new AtomicBoolean(false);
	private final ReadInterestController _readInterestController;
	protected final AtomicInteger _bufferedBytes = new AtomicInteger();
	private final NioSelector _selector;
	protected final SctpChannelListener _listener;

	protected volatile long _lastIOTime;
	private volatile long _soTimeout;
	private volatile boolean _soTimeoutReadOnly = true;
	protected volatile Future<?> _inactivityTimer;
	private volatile WriteBlockedPolicy _writeBlockedPolicy = WriteBlockedPolicy.NOTIFY;
	protected volatile Executor _inputExecutor;
	private volatile int _priority;
	private boolean _writeBlocked;
    private final AtomicBoolean _closed = new AtomicBoolean();
    private volatile Object _attached;
	protected final SctpMeters _sctpMeters;
	protected final boolean _directBuffer;
	private SctpSocketOptionHelper sockoptHelper = new SctpSocketOptionHelper();
	private final Set<SocketAddress> _localAddrs = new CopyOnWriteArraySet<>();
	private final Set<SocketAddress> _remoteAddrs = new CopyOnWriteArraySet<>();
	private ScheduledFuture<?> _closeTimer;
	private boolean _cleaned; 
	private volatile long _linger;
	private final Map<SocketAddress, Future<?>> _eventTimers = new HashMap<>();
	private final List<Notification> _earlyNotifications;
	private final static String REACTOR_EVENT_TIMER = "reactor.sctp.events.timer";
	protected static class Message {
		Message(MessageInfo info, ByteBuffer buf, boolean copy) {
			_info = info;
			_buf = copy ? Helpers.copy(buf) : buf;
		}

		final MessageInfo _info;
		final ByteBuffer _buf;
	}

	/**
	 * Timer used to detect socket inactivity. This timer runs in the reactor
	 * executor
	 */
	protected class InactivityTimer implements Runnable {
		@Override
		public void run() {
			if (!_inactivityTimer.isCancelled()) {
				long now = System.currentTimeMillis();
				long nowPadded = now + 100;
				if (nowPadded - _lastIOTime > _soTimeout) {
					_logger.info("ReceiveTimeout on %s", SctpChannelImpl.this);
					_lastIOTime = now;
					// We are running within our executor thread.
					_listener.receiveTimeout(SctpChannelImpl.this);
				}
			}
		}
	}

	public SctpChannelImpl(com.sun.nio.sctp.SctpChannel socket, Set<SocketAddress> localAddrs, Set<SocketAddress> remoteAddrs, SelectionKey key,
			ReactorImpl reactor, NioSelector selector, SctpChannelListener listener, int priority,
			Executor inputExecutor, Object attachment, boolean directBuffer, boolean nodelay, Boolean disableFragments,
			Boolean fragmentInterleave, long linger, List<Notification> earlyNotifications) throws IOException
	{
		_earlyNotifications = earlyNotifications;
		_localAddrs.addAll(localAddrs);
		_selector = selector;
		_reactor = reactor;
		_priority = priority;
		_listener = listener;
		_inputExecutor = inputExecutor;
		_attached = attachment;

		_socket = socket;
		if (key == null) {
			key = selector.registerSelectHandler(socket, 0, this);
		} else {
			key.interestOps(0);
			key.attach(this);
		}
		_key = key;
		_readInterestController = new ReadInterestController(_key, _logger.getLogger(), this::readInterest);
		_remotePort = getRemotePort(remoteAddrs);
		_sctpMeters = _reactor.getMeters().newSctpMeters(getRemotePrimaryAddress(remoteAddrs));
		_logger.info("Sctp socket established: %s", this);
		_directBuffer = directBuffer;

		setSocketOption(SctpSocketOption.SCTP_NODELAY, sctp_boolean.TRUE);

		if (disableFragments != null)
			setSocketOption(SctpSocketOption.SCTP_DISABLEFRAGMENTS, new sctp_boolean(disableFragments));
		if (fragmentInterleave != null)
			setSocketOption(SctpSocketOption.SCTP_FRAGMENT_INTERLEAVE, new sctp_boolean(fragmentInterleave));
		_linger = linger;
		_remoteAddrs.addAll(setRemoteAddrs(remoteAddrs));
	}

	// ----------------------------- SctpChannel interface

	protected InetSocketAddress getRemotePrimaryAddress(Set<SocketAddress> remoteAddrs) throws IOException {
		for (SocketAddress addr : remoteAddrs) {
			return ((InetSocketAddress) addr);
		}
		throw new IOException("Can't get remote address from sctp channel: " + this);
	}

	@Override
	public void setInputExecutor(Executor executor) {
		_inputExecutor = executor;
		if (_soTimeout > 0L) {
			setSoTimeout(_soTimeout, _soTimeoutReadOnly);
		}
		armSctpEventTimers();
	}

	/**
	 * Returns the executor used to dispatch listener methods.
	 */
	public Executor getInputExecutor() {
		return _inputExecutor;
	}

	public void attach(Object attached) {
		_attached = attached;
	}

	@SuppressWarnings("unchecked")
	public <T> T attachment() {
		return (T) _attached;
	}

	// Graceful sctp shutdown (warning: the reactor "close" method corresponds to sctp "shutdown" method)
	public void close() {
		if (_closed.compareAndSet(false, true)) {
			_selector.schedule(() -> {
				if (_linger > 0L) {
					_closeTimer = _selector.schedule(() -> abort(null, 0), _linger, TimeUnit.MILLISECONDS);
				}
				scheduleWriteInterest(_key);
			});
		}
	}
	
	// Abort the socket (warning: the reactor "shutdown" method corresponds to sctp graceful "close" method).
	public void shutdown() {
		_selector.schedule(() -> abort(null, 0));
	}

	public void disableReading() {
		_readInterestController.disableReading(_selector);
	}

	public void enableReading() {
		_readInterestController.enableReading(_selector);
	}

	public Set<SocketAddress> getLocalAddresses() throws IOException {
	    return _localAddrs;
	}

	public Set<SocketAddress> getRemoteAddresses() throws IOException {
	    return _remoteAddrs;
	}

	public int getRemotePort() {
		return _remotePort;
	}

	public SctpAssociation getAssociation() throws IOException {
		return new SctpAssociationImpl(_socket.association());
	}

	public int getPriority() {
		return _priority;
	}

	public void setPriority(int p) {
		_priority = p;
	}

	public SctpChannel unbindAddress(InetAddress address) throws IOException {
		_socket.unbindAddress(address);
		return this;
	}

	public Reactor getReactor() {
		return _reactor;
	}

	public boolean isClosed() {
		return !_socket.isOpen();
	}

	public SctpChannel send(boolean copy, SocketAddress addr, int streamNumber, ByteBuffer... data) {
		return send(copy, addr, true, 0, streamNumber, 0L, false, data);
	}

	public SctpChannel send(boolean copy, SocketAddress addr, boolean complete, int ploadPID, int streamNumber, long timeToLive, boolean unordered, ByteBuffer... data) {
		MessageInfo info = MessageInfo.createOutgoing(addr, streamNumber);
		info.complete(complete);
		info.payloadProtocolID(ploadPID);
		info.timeToLive(timeToLive);
		info.unordered(unordered);
		ByteBuffer msg = Helpers.compact(data);
		int remaining = msg.remaining();
		_bufferedBytes.addAndGet(remaining);
		_sctpMeters.sctpWriteBuffer(remaining);
		_queue.add(new Message(info, msg, copy));
		scheduleWriteInterest(_key);
		return this;
	}

	public InetSocketAddress getLocalAddress() {
		try {
			Set<SocketAddress> locals = getLocalAddresses();
			return ((InetSocketAddress) locals.iterator().next());
		}

		catch (IOException e) {
		    throw new RuntimeException("Could not get local address of sctp socket: " + e);
		}
	}

	public void setSoTimeout(long soTimeout) {
		setSoTimeout(soTimeout, true);
	}

	public void setSoTimeout(long soTimeout, boolean readOnly) {
		if (_inactivityTimer != null) {
			_inactivityTimer.cancel(false);
		}

		_soTimeout = soTimeout;
		_soTimeoutReadOnly = readOnly;

		if (soTimeout > 0) {
			_lastIOTime = System.currentTimeMillis();
			_logger.info("schedule inactivity timeout on %s (timeout=%d)", this, soTimeout);
			_inactivityTimer = _reactor.getReactorProvider().getApproxTimerService()
					.scheduleWithFixedDelay(_inputExecutor, new InactivityTimer(), 500, 500, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public int getSendBufferSize() {
		return _bufferedBytes.get();
	}

	// These methods are deprecated ...

	@Deprecated
	public void send(DataBuffer msg) {
		try {
			send(msg.getInternalBuffer(), true);
		}

		finally {
			msg.resetCapacity();
		}
	}

	@Deprecated
	public void send(ByteBuffer msg) {
		send(msg, true);
	}

	public void send(ByteBuffer msg, boolean copy) {
		send(copy, null, 0, msg);
	}

	@Deprecated
	public void send(ByteBuffer[] msg) {
		send(true, null, 0, msg);
	}

	public void send(ByteBuffer[] msg, boolean copy) {
		send(copy, null, 0, msg);
	}

	@Deprecated
	public void send(byte[] msg) {
		send(msg, true);
	}

	@Deprecated
	public void send(byte[] msg, boolean copy) {
		send(msg, 0, msg.length, copy);
	}

	@Deprecated
	public void send(byte[] msg, int off, int len) {
		send(msg, off, len, true);
	}

	@Deprecated
	public void send(byte[] msg, int off, int len, boolean copy) {
		send(copy, null, 0, ByteBuffer.wrap(msg, off, len));
	}
	
	@Override
	public void setSoLinger(long linger) {
		_linger = linger;
	}

	/**
	 * Called when read interest is enabled. Current thread = Selector thread
	 */
	private void readInterest(boolean read) {
		if (read && _earlyNotifications.size() > 0) {
			for (Notification notif : _earlyNotifications) {
				_logger.debug("scheduling early notification: " + notif);
				handleNotification(notif, this);
			}
			_earlyNotifications.clear();
		}
	}
	
	// ------------------- ReactorImpl.Listener methods --------------

	public void selected(SelectionKey key) {
		try {
			// Check if the key is still valid (the socket might be closed).
			if (key.isValid()) {
				// First check if some buffered bytes has to be flushed out.
				if (key.isWritable()) {
					outputReady();
				}

				// Next, Handle available input bytes
				if (key.isReadable()) {
					inputReady();
				}
			} else {
				// key probably cancelled.
				abort(new IOException("Invalid Selection Key (socket closed, or key has been cancelled)"), 0);
			}
		}

		catch (Throwable t) {
			abort(t, -1);
		}
	}

	// ----------------------------- AbstractNotificationHandler -------

	@Override
	public HandlerResult handleNotification(Notification notification, SctpChannel channel) {
		if (notification instanceof AssociationChangeNotification) {
			handleNotification((AssociationChangeNotification) notification, channel);
		} else if (notification instanceof PeerAddressChangeNotification) {
			handleNotification((PeerAddressChangeNotification) notification, channel);
		} else if (notification instanceof SendFailedNotification) {
			handleNotification((SendFailedNotification) notification, channel);
		} else if (notification instanceof ShutdownNotification) {
			handleNotification((ShutdownNotification) notification, channel);
		}
		return HandlerResult.CONTINUE;
	}

	@Override
	public HandlerResult handleNotification(AssociationChangeNotification notification, SctpChannel attachment) {
		switch (notification.event()) {
		case CANT_START:
			abort(new IOException("Can't initialize sctp association (CANT_START event)"), -1);
			return HandlerResult.RETURN;

		case SHUTDOWN:
			abort(null, -1);
			return HandlerResult.RETURN;

		default:
			return HandlerResult.CONTINUE;
		}
	}

	@Override
	public HandlerResult handleNotification(final PeerAddressChangeNotification notification, SctpChannel channel) {
		_logger.info("Received SCTP Peer Address Changed event: %s", notification);

		_inputExecutor.execute(() -> {
			try {
				SocketAddress addr = null;
				addr = notification.address();
				AddressEvent event;

				switch (notification.event()) {
				case ADDR_ADDED:
					event = addrAdded(addr);
					break;
				case ADDR_REMOVED:
					event = addrRemoved(addr);
					break;
				case ADDR_AVAILABLE:
					event = addrAvailable(addr);
					break;
				case ADDR_CONFIRMED:
					event = addrConfirmed(addr);
					break;
				case ADDR_UNREACHABLE:
					event = addrUnreachable(addr);
					break;
				case ADDR_MADE_PRIMARY:
					event = addrMadePrimary(addr);
					break;
				default:	
					event = AddressEvent.valueOf(notification.event().name());
					break;
				}

				if (event != null) {
					_listener.peerAddressChanged(SctpChannelImpl.this, notification.address(), event);
				}
			} catch (Throwable t) {
				_logger.warn("unexpected exception while invoking SctpChannelListener.peerAddressChanged method", t);
			}
		});

		return HandlerResult.CONTINUE;
	}

	@Override
	public HandlerResult handleNotification(final SendFailedNotification notification, SctpChannel attachment) {
		_inputExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					_listener.sendFailed(SctpChannelImpl.this, notification.address(), notification.buffer(),
							notification.errorCode(), notification.streamNumber());
				} catch (Throwable t) {
					_logger.warn("unexpected exception while invoking SctpChannelListener.sendFailed method", t);
				}
			}
		});

		return HandlerResult.CONTINUE;
	}

	@Override
	public HandlerResult handleNotification(ShutdownNotification notification, SctpChannel attachment) {
		_logger.info("handleNotification on channel %s : %s", this, notification);
		abort(null, -1);
		return HandlerResult.RETURN;
	}

	@Override
	public String toString() {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("SctpChannel: local=").append(Helpers.toString(getLocalAddresses()));
			sb.append(", remote=").append(Helpers.toString(getRemoteAddresses()));
			return sb.toString();
		} catch (Exception e) {
			return "unknown sctp address: " + e.toString();
		}
	}

	// -------------------- Package methods ----------------------------

	void enableReadingInternal() {
		_readInterestController.enableReadingInternal(_selector);
	}

	// -------------------- Private methods ----------------------------

	private void inputReady() {
		_readInterestController.disableReadingInternal(_selector);
		_inputExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					inputReadyInExecutor();
				} finally {
					_readInterestController.enableReadingInternal(_selector);
				}
			}
		});
	}

	protected void inputReadyInExecutor() {
		// Read messages from this socket (no more bytes than Helpers.getRcvBufSize(),
		// in order to avoid reading for ever the same socket).
		int bytesRead = 0;
		MessageInfo info;

		ByteBuffer rcvBuf = Helpers.getCurrentThreadReceiveBuffer(_directBuffer);
		try {
			while (bytesRead < Helpers.getRcvBufSize() && (info = _socket.receive(rcvBuf, this, this)) != null) {
				if (info.bytes() == -1) {
					// -1 means EOF. The peer probably closed, and our listener
					// has received a SHUTDOWN event (and must explicitly invoke cnx.close
					// for really closing our channel).
					throw new ClosedChannelException();
				}

				_sctpMeters.sctpRead();

				if (info.bytes() == 0) {
					// 0 means we probably got a service event (our listener has received it).
					// in this case, nothing to do.
					rcvBuf.clear();
					continue;
				}

				if (_inactivityTimer != null) {
					_lastIOTime = System.currentTimeMillis();
				}

				bytesRead += info.bytes();
				_sctpMeters.sctpReadBytes(bytesRead);

				rcvBuf.flip();
				if (rcvBuf.hasRemaining() && _logger.isInfoEnabled()) {
					Helpers.logPacketReceived(_logger.getLogger(), rcvBuf, this);
				}

				_listener.messageReceived(this, rcvBuf, info.address(), info.bytes(), info.isComplete(),
						info.isUnordered(), info.payloadProtocolID(), info.streamNumber());
				rcvBuf.clear();
			}
		}

		catch (Throwable t) {
			abort(t, 0);
		}

		finally {
			rcvBuf.clear();
		}
	}

    // we have flushed (some or all of buffered bytes, we can now update the last io
    // time.
	private void updateWriteTime() {
		if (_inactivityTimer != null && !_soTimeoutReadOnly) {
			_lastIOTime = System.currentTimeMillis();
		}
	}
    
	private void outputReady() throws IOException {
		// we are about to write something, update the last write time.
		updateWriteTime();
		// Remove write interest
		_key.interestOps(_key.interestOps() & ~SelectionKey.OP_WRITE);
		// schedule socket write in the thread pool
		Helpers.getDefaultOutputExecutor().execute(this::flush);
	}

    private void flush() { // run from the threadpool		
		try {
			Message msg;
			while ((msg = _queue.peek()) != null) {
				ByteBuffer msgLog = _logger.isInfoEnabled() ? msg._buf.duplicate() : null;
				int sent = _socket.send(msg._buf, msg._info);
				if (sent > 0) {
					_bufferedBytes.addAndGet(-sent);
					_sctpMeters.sctpWriteBuffer(-sent);
					_sctpMeters.sctpWriteBytes(sent);
					_sctpMeters.sctpWrite();
					if (_logger.isInfoEnabled()) {
						Helpers.logPacketSent(_logger.getLogger(), true, msgLog, sent, toString());
					}
				} else if (sent == 0) {
					// Resume write interest (the _writeInterestScheduled flag is left to true).
					_selector.scheduleNow(this::setWriteInterest);
					writeBlocked();
					return;
				}

				// update last io time every 128 writes
				updateWriteTime();
				
				// pop the message from our queue, we have sent it
				_queue.poll();
			}

			// At this point, all buffered message have been flushed, we can our socket idle timer
			updateWriteTime();

			// All sent: reset writeInterestScheduled to false, and re-enable the schedule
			// of write interests.
			_writeInterestScheduled.set(false);

			// Notify listeners about the new unblocked state.
			writeUnblocked();

			// Check if some buffers have been added *after* we flushed all buffers,
			// but *before* the _writeInterestScheduled flag have been reset to false.
			if (!_queue.isEmpty()) {
				scheduleWriteInterest(_key);
				return;
			}

			// All data sent. Now check if we must shutdown.
			if (_closed.get()) {
				// Re check in case someone added data to send. If true, it means a write
				// interest is in progress.
				if (_queue.isEmpty()) {
					// all sent, no more to send, we can close.
					_socket.shutdown();
				}
			}
		}

		catch (Throwable t) {
			abort(t, -1);
		}
	}

	/**
	 * Cleanup channel. Can be called from selector or input executor thread
	 */
	protected void abort(final Throwable t, final int lingerSec) {
		// Cancel inactivity time.
		Future<?> inactivityTimer = _inactivityTimer;
		if (inactivityTimer != null) {
			inactivityTimer.cancel(false);
		}
		_selector.scheduleNow(() -> {
			doAbort(t, lingerSec);
		});
	}

	// called in selector thread
	private void doAbort(final Throwable t, final int lingerSec) {
		if (!_cleaned) {
			_cleaned = true;			
			_logger.info("Sctp channel closed: %s", t, this);		
			
			if (_closeTimer != null) {
				_closeTimer.cancel(false);
			}
			
			try {
				if (_key != null) {
					_key.cancel();
				}
			} catch (Throwable ignored) {
			}

			if (_socket != null) {
				try {
					if (lingerSec != -1) {
						_socket.setOption(SctpStandardSocketOptions.SO_LINGER, new Integer(lingerSec));
					}
				} catch (Throwable ignored) {
				}

				try {
					_socket.close();
				} catch (Throwable ignored) {
				}
			}
			
			_selector.getMeters().addSctpChannel(-1, isSecure());
			_inputExecutor.execute(() -> {
				try {
					_sctpMeters.close();
					_listener.connectionClosed(SctpChannelImpl.this, t);
				} catch (Throwable err) {
					_logger.warn("Got unexpected exception on %s", err, SctpChannelImpl.this);
				}
				for (Future<?> eventTimer : _eventTimers.values()) {
					eventTimer.cancel(false);
				}
				_eventTimers.clear();
			});
		}

		// Reenable write interest. it ensures any further calls to send() will be scheduled and will then eventually be aborted
		_writeInterestScheduled.set(false);

		// now, decrease send buffer size
		long remaining = _bufferedBytes.getAndSet(0);
		_sctpMeters.sctpWriteBuffer(-remaining);
		_queue.clear();
	}

	/**
	 * The socket being currently written becomes blocked (full). It means that some
	 * bytes are buffered until the socket becomes unblocked.
	 * 
	 * Note: the current thread is the selector thread.
	 */
	private void writeBlocked() {
		if (_writeBlocked == false) {
			_writeBlocked = true;
			switch (_writeBlockedPolicy) {
			case NOTIFY:
				_inputExecutor.execute(new Runnable() {
					public void run() {
						_listener.writeBlocked(SctpChannelImpl.this);
					}
				});
				break;

			case DISABLE_READ:
				disableReading();
				break;

			case IGNORE:
				break;
			}
		}
	}

	/**
	 * The socket being currently written becomes unblocked (all buffered bytes have
	 * been flushed). In this case, invoke the writeUnbloked callback if the
	 * listener is currently blocked.
	 * 
	 * Note: the current thread is the selector thread, so no need to synchronize
	 * anything.
	 */
	private void writeUnblocked() {
		if (_writeBlocked) {
			_writeBlocked = false;
			switch (_writeBlockedPolicy) {
			case NOTIFY:
				_inputExecutor.execute(new Runnable() {
					public void run() {
						_listener.writeUnblocked(SctpChannelImpl.this);
					}
				});
				break;

			case DISABLE_READ:
				enableReading();
				break;

			case IGNORE:
				break;
			}
		}
	}

	private int getRemotePort(Set<SocketAddress> remoteAddrs) throws IOException {
		int remotePort = -1;
		for (SocketAddress addr : remoteAddrs) {
			remotePort = ((InetSocketAddress) addr).getPort();
			if (remotePort != -1) {
				return remotePort;
			}
		}
		throw new IOException("Can't find remote port number");
	}

	protected void scheduleWriteInterest(final SelectionKey key) {
		if (_writeInterestScheduled.compareAndSet(false, true)) {
			_selector.scheduleNow(this::setWriteInterest);
		}
	}
    
	@Override
	public void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy) {
		_writeBlockedPolicy = writeBlockedPolicy;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getSocketOption(SctpSocketOption option, Object extra) throws IOException {
		_logger.debug("Getting option %s = %s", option, extra);
		return (T) sockoptHelper.getOption(_socket, option, extra);
	}

	@Override
	public SctpChannel setSocketOption(SctpSocketOption option, Object param) throws IOException {
		_logger.debug("Setting option %s = %s", option, param);
		sockoptHelper.setOption(_socket, option, param);
		return this;
	}
	
	private void setWriteInterest() {
		try {
			_key.interestOps(_key.interestOps() | SelectionKey.OP_WRITE);
		} catch (Throwable t) {
			abort(t, -1);
		}
	}

	private Set<SocketAddress> setRemoteAddrs(Set<SocketAddress> remoteAddrs) throws IOException {
		_logger.debug("setting remote addresses: %s", remoteAddrs);
		Set<SocketAddress> addrs = new CopyOnWriteArraySet<SocketAddress>(); // ensure thread safety and natural iteration ordering
		for(SocketAddress addr : remoteAddrs) {
			try {
				sctp_paddrinfo info = getSocketOption(SctpSocketOption.SCTP_GET_PEER_ADDR_INFO, addr);
				if (info.spinfo_state == sctp_paddrinfo.sctp_spinfo_state.SCTP_ACTIVE) {
					addrs.add(addr);
					_logger.debug("found primary address: %s from %s", addr, remoteAddrs);
					break;
				}
			} catch (IOException e) {
				_logger.warn("Could not get peer addr info from accepted client address %s (this address won't be considered as possible primary address), error: %s", 
						addr, e.toString());
			}
		}
		if (addrs.size() == 0) {
			_logger.warn("could not determine primary address for sctp connection: %s", this);
		} 
		for(SocketAddress addr : remoteAddrs) {
		    addrs.add(addr); // won't override already added active primary address.
		}
		return addrs;
	}

	/**
	 * scheduled in inputExecutor
	 */
	public void armSctpEventTimers() {
		long millis = Long.getLong(REACTOR_EVENT_TIMER, 0L);
		if (millis > 0) {
			_inputExecutor.execute(() -> {
				for (Future<?> eventTimer : _eventTimers.values()) {
					eventTimer.cancel(false);
				}
				_eventTimers.clear();
				Iterator<SocketAddress> it = _remoteAddrs.iterator();

				if (it.hasNext()) {
					it.next(); // skip primary
					TimerService wheelTimer = _reactor.getReactorProvider().getApproxTimerService();
					while (it.hasNext()) {
						SocketAddress addr = it.next();
						_logger.debug("arming sctp events timer for secondary addr %s", addr);
						Future<?> eventTimer = wheelTimer.schedule(
								_inputExecutor, 
								() -> eventTimerFired (addr),
								millis,
								TimeUnit.MILLISECONDS);
						_eventTimers.put(addr, eventTimer);
					}
				}
			});
		}
	}
	
	
	/**
	 * scheduled in inputExecutor
	 */
	private void eventTimerFired(SocketAddress addr) { 
		try {
			AddressEvent event = AddressEvent.ADDR_UNREACHABLE;
			_listener.peerAddressChanged(SctpChannelImpl.this, addr, event);
		} catch (Throwable t) {
			_logger.warn("unexpected exception while invoking SctpChannelListener.peerAddressChanged method", t);
		}
	}
	
	/**
	 * scheduled in inputExecutor
	 */
	private AddressEvent addrMadePrimary(SocketAddress newPrimaryAddr) {
		Set<SocketAddress> addrs = new CopyOnWriteArraySet<SocketAddress>(); // ensure thread safety and natural iteration ordering
		addrs.add(newPrimaryAddr);
		for (SocketAddress addr : _remoteAddrs) {
			addrs.add(addr); // won't override already added active primary address.
		}
		_remoteAddrs.clear();
		_remoteAddrs.addAll(addrs);
		Future<?> eventTimer = _eventTimers.remove(newPrimaryAddr);
		if (eventTimer != null) {
			// possibly the called called ADDR_UNREACHABLE, now we
			eventTimer.cancel(false);
		}
		return AddressEvent.ADDR_MADE_PRIMARY;
	}

	/**
	 * scheduled in inputExecutor
	 */
	private AddressEvent addrAvailable(SocketAddress addr) {
		Future<?> eventTimer = _eventTimers.remove(addr);
		if (eventTimer != null) {
			eventTimer.cancel(false);
		}
		return AddressEvent.ADDR_AVAILABLE;
	}

	/**
	 * scheduled in inputExecutor
	 */
	private AddressEvent addrRemoved(SocketAddress addr) {
		_remoteAddrs.remove(addr);
		Future<?> eventTimer = _eventTimers.remove(addr);
		if (eventTimer != null) {
			eventTimer.cancel(false);
		}
		return AddressEvent.ADDR_REMOVED;
	}

	/**
	 * scheduled in inputExecutor
	 */
	private AddressEvent addrAdded(SocketAddress addr) {
		_remoteAddrs.add(addr);
		Future<?> eventTimer = _eventTimers.remove(addr);
		if (eventTimer != null) {
			eventTimer.cancel(false);
		}
		return AddressEvent.ADDR_ADDED;
	}

	/**
	 * scheduled in inputExecutor
	 */
	private AddressEvent addrUnreachable(SocketAddress addr) {
		Future<?> eventTimer = _eventTimers.remove(addr);
		if (eventTimer != null) {
			if (! eventTimer.cancel(false)) {
				// event timer expired and already called ADDR_UNREACHABLE: ignore this event
				return null;
			}
		}
		return AddressEvent.ADDR_UNREACHABLE;
	}

	/**
	 * scheduled in inputExecutor
	 */
	private AddressEvent addrConfirmed(SocketAddress addr) {
		Future<?> eventTimer = _eventTimers.remove(addr);
		if (eventTimer != null) {
			if (! eventTimer.cancel(false)) {
				// event timer expired and already called ADDR_UNREACHABLE: swap to ADDR_AVAILABLE
				return AddressEvent.ADDR_AVAILABLE;
			}
		}
		return AddressEvent.ADDR_CONFIRMED;
	}

}
