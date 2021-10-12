// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

// Jdk
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.UdpChannel;
import alcatel.tess.hometop.gateways.reactor.UdpChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;

/**
 * UDP reactor socket.
 */
public class UdpChannelImpl implements UdpChannel, SelectHandler {
	// Private attributes.

	private volatile InetSocketAddress _localAddr;
	protected final UdpChannelListener _listener;
	private SelectionKey _key;
	private volatile Object _attached;
	protected final ReactorImpl _reactor;
	protected volatile DatagramChannel _socket;
	private DataBuffer _sndBufDeprecated;
	private final static Logger _logger = Logger.getLogger("as.service.reactor.UdpChannelImpl");
	private volatile Future<?> _timer;
	private volatile long _soTimeout;
	private volatile boolean _soTimeoutReadOnly = true;
	protected volatile long _lastIOTime;
	private volatile int _priority;
	private boolean _writeBlocked;
	protected final ConcurrentLinkedQueue<Message> _sendQueue = new ConcurrentLinkedQueue<UdpChannelImpl.Message>();
	protected volatile ReadInterestController _readInterestController;
	protected volatile Executor _queue;
	private final AtomicInteger _bufferedBytes = new AtomicInteger();
	private volatile WriteBlockedPolicy _writeBlockedPolicy = WriteBlockedPolicy.NOTIFY;
	private final AtomicBoolean _writeInterestSheduled = new AtomicBoolean();
	protected final NioSelector _selector;
	private final boolean _enableRead;
	protected final AtomicBoolean _closing = new AtomicBoolean(false);
	private final int _sndbuf;
	private final int _rcvbuf;
	private final boolean _ipTransparent;
	private final UdpSocketOptionHelper _socketOptionHelper;
	private volatile boolean _connectionClosedCalled;
	private final AtomicBoolean _binding = new AtomicBoolean();
	private final AtomicBoolean _bound = new AtomicBoolean();
	protected final boolean _directBuffer;

	protected static class Message {
		final InetSocketAddress _addr;
		final ByteBuffer _buf;

		Message(ByteBuffer buf, InetSocketAddress addr) {
			_buf = buf;
			_addr = addr;
		}
	}

	private final Runnable _writeInterestTask = new Runnable() {
		@Override
		public void run() {
			try {
				_key.interestOps(_key.interestOps() | SelectionKey.OP_WRITE);
			} catch (Throwable t) {
				abort(t);
			}
		}
	};

	// read datagrame from inputExecutor queue
	protected Runnable _inputReadyTask = new Runnable() {
		@Override
		public void run() {
		    ByteBuffer rcvBuf = Helpers.getCurrentThreadReceiveBuffer(_directBuffer);

			try {
				// Just keep track of this current read time, just in case a timer has been started.
				_lastIOTime = System.currentTimeMillis();

				// Read the packets.
				InetSocketAddress from;
				int bytesReceived = 0;
				while ((from = (InetSocketAddress) _socket.receive(rcvBuf)) != null) {
					rcvBuf.flip();
					if (_logger.isDebugEnabled()) {
						StringBuilder source = new StringBuilder();
						source.append("[local=").append(", remote=").append(from).append("]");
						Helpers.logPacketReceived(_logger, rcvBuf, source);
					}

					bytesReceived += rcvBuf.remaining();
					_reactor.getMeters().udpReadBytes(rcvBuf.remaining());
					_reactor.getMeters().udpRead();
					if (!_closing.get()) {
						try {
							messageReceived(rcvBuf, from);
						} catch (Throwable t) {
							_logger.warn("Unexpected exception while handing udp packet from " + from, t);
						} finally {
							rcvBuf.clear();
						}
					} else {
						return; // no need to read anymore
					}

					if (bytesReceived > Helpers.getRcvBufSize()) {
						// avoid starvation; we have read 65 kb, release our worker to the thread pool,
						// we will read again later
						break;
					}
				}
				_readInterestController.enableReadingInternal(_selector);
			} catch (Throwable t) {
				abort(t);
		    } finally {
		        rcvBuf.clear();
			}
		}
	};
	
	protected void messageReceived(ByteBuffer buf, InetSocketAddress from) throws Exception {
		_listener.messageReceived(UdpChannelImpl.this, buf, from);
	}

	// write datagrame from a worker thread in the threadpool
	private final Runnable _outputReadyTask = new Runnable() {
		@Override
		public void run() {
			try {
				// Try to flush all pending messages
				Message msg;
				// we'll update last io time every 128 writes
				int counter = 0;
				while ((msg = _sendQueue.peek()) != null) {
					if (!trySend(msg._addr, msg._buf)) {
						// Resume write interest (the _writeInterestScheduled flag is left to true).
						_selector.scheduleNow(_writeInterestTask);
						// Notify user that writing is blocked
						writeBlocked();
						return; // leave write interest flag.
					}
					_sendQueue.poll(); // Remove this one, which we have just sent out.

					if (((counter++) & 127) == 0) {
						updateWriteTime();
					}
				}

				// All sent: reset las iotime, writeInterestScheduled to false, and possibly unblock writer.
				updateWriteTime();

				// All sent: reset writeInterestScheduled to false, and re-enable the schedule of write interests.
				_writeInterestSheduled.set(false);

				// Notify listeners about the new unblocked state.
				writeUnblocked();

				// Check if some buffers have been added *after* we flushed all buffers,
				// but *before* the _writeInterestScheduled flag have been reset to false.
				if (!_sendQueue.isEmpty()) {
					scheduleWriteInterest();
					return;
				}

				// All sent, check if we must close the socket.
				if (_closing.get()) {
					// Re check in case someone added data to send. If true, it means a write
					// interest is in progress.
					if (_sendQueue.isEmpty()) {
						abort(null);
					}
				}
			} catch (Throwable t) {
				abort(t);
			}
		}
	};

	/**
	 * Makes a new Connection.
	 * 
	 * @param inputExec
	 * @param rcvbuf
	 * @param sndbuf
	 * @param directBuffer
	 */
	UdpChannelImpl(ReactorImpl reactor, UdpChannelListener listener, InetSocketAddress local, int priority,
			Object attachment, Executor inputExec, Executor outputExec /* not used */, boolean enableRead, int sndbuf,
			int rcvbuf, boolean directBuffer, boolean ipTransparent) {
		_selector = reactor.getSelector(Helpers._udpSelectorCounter);
		_reactor = reactor;
		_listener = listener;
		_priority = priority;
		_localAddr = local;
		_attached = attachment;
		_queue = inputExec;
		_enableRead = enableRead;
		_sndbuf = sndbuf;
		_rcvbuf = rcvbuf;
		_ipTransparent = ipTransparent;
		_socketOptionHelper = new UdpSocketOptionHelper(_logger);
		_directBuffer = directBuffer;
	}

	// --- public methods.

	@Override
	public String toString() {
		return "UdpChannel[local=" + _localAddr + "]";
	}

	// --- UdpChannel interface -------------------------------------------------

	public void bind() {
		if (_binding.compareAndSet(false, true)) {
			_selector.schedule(() -> {
				try {
					doBind();
					scheduleConnectionOpenedAndEnableRead();
				}

				catch (final Throwable e) {
					closeSocket();
					scheduleConnectionFailed(e);
				}
			});
		}
	}

	public void bindSync() throws IOException {
		Callable<Throwable> task = () -> {
			if (_binding.compareAndSet(false, true)) {
				try {
					doBind();
				} catch (Throwable e) {
					closeSocket();
					return e;
				}
			}
			return null;
		};

		FutureTask<Throwable> ft = new FutureTask<Throwable>(task);
		_selector.schedule(ft);

		try {
			Throwable err = ft.get();
			if (err != null) {
				throw err;
			}
		} catch (Throwable t) {
			throw new IOException("could not bind udp channel: " + toString(), t);
		}
		enableReadMode();
	}

	public int getPriority() {
		return _priority;
	}

	public void setPriority(int p) {
		_priority = p;
	}

	public InetSocketAddress getLocalAddress() {
		return _localAddr;
	}

	public void setSoTimeout(final long soTimeout) {
		setSoTimeout(soTimeout, true);
	}

	public void setSoTimeout(final long soTimeout, boolean readOnly) {
		checkBound();
		// Schedule the task in order to avoid potential deadlock.
		_selector.scheduleNow(() -> {
			if (_timer != null) {
				_timer.cancel(false);
			}

			_soTimeout = soTimeout;
			_soTimeoutReadOnly = readOnly;

			if (_soTimeout > 0) {
				_lastIOTime = System.currentTimeMillis();
				_timer = _reactor.getReactorProvider().getApproxTimerService().scheduleWithFixedDelay(_queue,
						new InactivityTimer(), 500, 500, TimeUnit.MILLISECONDS);
			}
		});
	}

	@SuppressWarnings("unchecked")
	public <T> T attachment() {
		return (T) _attached;
	}

	public void attach(Object attached) {
		_attached = attached;
	}

	public void close() {
		if (_closing.compareAndSet(false, true)) {
			scheduleWriteInterest();
		}
	}

	public void shutdown() {
		_closing.set(true);
		_selector.schedule(() -> abort(null));
	}

	/**
	 * Tells if this connection is closed.
	 */
	public boolean isClosed() {
		return _closing.get();
	}

	/**
	 * Disable this mux connection for read operations.
	 */
	public void disableReading() {
		_readInterestController.disableReading(_selector);
	}

	/**
	 * Enable this connection for read operations.
	 */
	public void enableReading() {
		_readInterestController.enableReading(_selector);
	}

	@Override
	public void send(final InetSocketAddress to, ByteBuffer buf, boolean copy) {
		checkBound();
		int bytesToSend = buf.remaining();
		final ByteBuffer bb = copy ? Helpers.copy(buf) : buf;
		_sendQueue.add(new Message(bb, to));
		buffered(bytesToSend);
		scheduleWriteInterest();
	}

	@Override
	public void send(final InetSocketAddress to, boolean copy, ByteBuffer... bufs) {
		ByteBuffer buf = (bufs.length == 1) ? ((copy) ? Helpers.copy(bufs[0]) : bufs[0]) : Helpers.compact(bufs);
		send(to, buf, false);
	}

	/**
	 * Return the reactor managing this connection.
	 */
	public Reactor getReactor() {
		return _reactor;
	}

	public void setInputExecutor(Executor executor) {
		_queue = executor;
	}

	/**
	 * Returns the executor used to dispatch listener methods.
	 */
	public Executor getInputExecutor() {
		return _queue;
	}

	public int getSendBufferSize() {
		return _bufferedBytes.get();
	}

	@Override
	public boolean isSecure() {
		return false;
	}
	
	@Override
	public void updateSecurity(Security security) {
		throw new IllegalStateException("Can't update security on unsecured channel");
	}

	// --- Reactor.Listener methods ----------------------------------------------

	/**
	 * Method called by the reactor when read/write/open/close operations are ready
	 * to be processed.
	 */
	public void selected(SelectionKey key) {
		try {
			if (_key.isValid()) {
				if (_key.isReadable()) {
					inputReady();
				}

				// Now, check if the channel is ready for writes. Notice that we also check if the
				// key is still valid because the listener.messageReceived() method (see above)
				// may have closed the channel.
				if (_key.isWritable()) {
					outputReady();
				}
			} else {
				// key probably cancelled.
				abort(new IOException("Invalid Selection Key (socket closed, or key has been cancelled)"));
			}
		}

		catch (Throwable t) {
			abort(t);
		}
	}

	// --- Private methods -------------------------------------------------------

	private void doBind() throws IOException {
		if (_logger.isInfoEnabled()) {
			_logger.info("Binding " + UdpChannelImpl.this.toString());
		}

		_socket = DatagramChannel.open();
		if (_ipTransparent) {
			_socketOptionHelper.setIpTransparent(_socket, _localAddr.getAddress() instanceof Inet4Address);
		}
		_socket.socket().bind(_localAddr);
		if (_sndbuf > 0) {
			_socket.socket().setSendBufferSize(_sndbuf);
		}
		if (_rcvbuf > 0) {
			_socket.socket().setReceiveBufferSize(_rcvbuf);
		}
		_localAddr = (InetSocketAddress) _socket.socket().getLocalSocketAddress();
		_socket.configureBlocking(false);
		_key = _selector.registerSelectHandler(_socket, 0, UdpChannelImpl.this);
		_readInterestController = new ReadInterestController(_key, _logger, (op_read) -> {});
		_bound.set(true);
		if (_logger.isInfoEnabled()) {
			_logger.info("Bound " + UdpChannelImpl.this.toString());
		}
	}

	private void enableReadMode() {
		if (!_enableRead) {
			// The user will activate read mode by calling enableReading later. So, we have
			// to disable read mode before enabling reads internally.
			_readInterestController.disableReading(_selector);
		}
		_readInterestController.enableReadingInternal(_selector);
	}

	private void closeSocket() {
		try {
			if (_socket != null) {
				_socket.close();
			}
			if (_timer != null) {
				_timer.cancel(false);
			}
		} catch (Throwable ignored) {
		}
	}

	private boolean trySend(InetSocketAddress to, ByteBuffer buf) {
		try {
			ByteBuffer log = _logger.isInfoEnabled() ? buf.duplicate() : null;
			int sent = _socket.send(buf, to);
			if (sent > 0) {
				// All sent
				buffered(-sent);
				_reactor.getMeters().udpWriteBytes(sent);

				if (log != null) {
					Helpers.logPacketSent(_logger, true, log, sent, getAddress(to));
				}
			} else if (sent == 0) {
				// Nothing was sent
				if (_logger.isInfoEnabled()) {
					_logger.info("Packet not fully sent on " + toString() + " to " + to + ": remaining bytes="
							+ buf.remaining());
				}
				return false;
			} else {
				throw new IOException("UDP socket io exception");
			}
		} catch (IOException e) {
			int remaining = buf.remaining();
			if (remaining > 0) {
				buffered(-remaining);
			}
			if (_logger.isDebugEnabled()) {
				_logger.debug("Could not send udp message from " + _localAddr + " to " + to, e);
			} else {
				_logger.info("Could not send udp message from " + _localAddr + " to " + to + ": " + e.toString());
			}
		}
		return true;
	}

	// we have flushed (some or all of buffered bytes, we can now update the last io time.
	private void updateWriteTime() {
		if (_timer != null && !_soTimeoutReadOnly) {
			_lastIOTime = System.currentTimeMillis();
		}
	}

	private void outputReady() throws IOException {
		// we are about to write something, update the last write time.
		updateWriteTime();

		_key.interestOps(_key.interestOps() & ~SelectionKey.OP_WRITE);
		Helpers.getDefaultOutputExecutor().execute(_outputReadyTask);
	}

	protected void scheduleWriteInterest() {
		if (_writeInterestSheduled.compareAndSet(false, true)) {
			_selector.scheduleNow(_writeInterestTask);
		}
	}

	private void inputReady() {
		_readInterestController.disableReadingInternal(_selector);
		_queue.execute(_inputReadyTask);
	}

	private String getAddress(InetSocketAddress remote) {
		StringBuilder sb = new StringBuilder();
		sb.append("UdpChannel [local=");
		sb.append(_localAddr);
		sb.append(",remote=");
		sb.append(remote);
		sb.append("]");
		return sb.toString();
	}

	protected void checkBound() {
		if (!_bound.get()) {
			throw new IllegalStateException("UdpChannel not opened on local address=" + _localAddr);
		}
	}

	// Called from selector/inputExecutor/outputExecutor thread. Can be invoked multiple times.
	protected void abort(final Throwable t) {
		_selector.scheduleNow(() -> {
			if (_timer != null) {
				_timer.cancel(false);
			}

			if (t != null) {
				Level level = Helpers.isSocketException(t) ? Level.INFO : Level.WARN;
				_logger.log(level, "Got exception on udp channel: " + this, t);
			}

			try {
				_key.cancel();
			} catch (Throwable e) {
			}

			try {
				_socket.close();
			} catch (IOException e) {
			}

			// before resetting sendbuf size and meters, we must re-enable the write interest flag. This ensures that our abort method will be called again
			// in case another thread invokes send() while (or after) the channel is closing.
			_writeInterestSheduled.set(false);
			
			// now we can safely reset deprecated send buffer size
			synchronized (UdpChannelImpl.this) {
				DataBuffer sndBuf = _sndBufDeprecated;
				if (sndBuf != null) {
					sndBuf.resetCapacity(0); 
				}
			}
			int remaining = _bufferedBytes.getAndSet(0);
			_reactor.getMeters().udpWriteBuffer(-remaining);
			_sendQueue.clear();
			
			_logger.info("Udp channel unbound: " + toString());
			scheduleConnectionClosed(); // will invoke the listener only once.
		});
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
				_queue.execute(new Runnable() {
					@Override
					public void run() {
						_listener.writeBlocked(UdpChannelImpl.this);
					}
				});
				break;

			case DISABLE_READ:
				// Disable read on this socket which can't be written.
				disableReading();
				break;

			case IGNORE:
				break;
			}
		}
	}

	private void writeUnblocked() {
		if (_writeBlocked) {
			_writeBlocked = false;
			switch (_writeBlockedPolicy) {
			case NOTIFY:
				_queue.execute(new Runnable() {
					@Override
					public void run() {
						_listener.writeUnblocked(UdpChannelImpl.this);
					}
				});
				break;

			case DISABLE_READ:
				// Re-enable read mode on the socket (we disabled it from our writeBlocked
				// method).
				enableReading();
				break;

			case IGNORE:
				break;
			}
		}
	}

	@Override
	public void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy) {
		_writeBlockedPolicy = writeBlockedPolicy;
	}

	protected void buffered(int bytes) {
		_bufferedBytes.addAndGet(bytes);
		_reactor.getMeters().udpWriteBuffer(bytes);
	}

	private class InactivityTimer implements Runnable {
		@Override
		public void run() {
			if (!_timer.isCancelled()) {
				long now = System.currentTimeMillis();
				long nowPadded = now + 100;
				if (nowPadded - _lastIOTime > _soTimeout) {
					// We are running within the reactor thread.
					_lastIOTime = now;
					_listener.receiveTimeout(UdpChannelImpl.this);
				}
			}
		}
	}

	// ------------------- Deprecated methods -------------------------------

	@Deprecated
	public void send(DataBuffer data) {
		try {
			send(data.getInternalBuffer());
		}

		finally {
			data.resetCapacity();
		}
	}

	@Deprecated
	public void send(byte[] data) {
		send(data, 0, data.length);
	}

	@Deprecated
	public void send(ByteBuffer buf) {
		checkBound();
		synchronized (this) {
			if (_sndBufDeprecated == null) _sndBufDeprecated = new DataBuffer(Helpers.getSndBufSize(), false);
			_sndBufDeprecated.put(buf);
			buffered(buf.remaining());
		}
	}

	@Deprecated
	public void send(ByteBuffer[] bufs) {
		checkBound();
		synchronized (this) {
			if (_sndBufDeprecated == null) _sndBufDeprecated = new DataBuffer(Helpers.getSndBufSize(), false);
			for (ByteBuffer buf : bufs) {
				_sndBufDeprecated.put(buf);
				buffered(buf.remaining());
			}
		}
	}

	@Deprecated
	public void send(byte[] data, int off, int len) {
		checkBound();
		synchronized (this) {
			if (_sndBufDeprecated == null) _sndBufDeprecated = new DataBuffer(Helpers.getSndBufSize(), false);
			_sndBufDeprecated.put(data, off, len);
			buffered(len);
		}
	}

	@Deprecated
	public void flush(InetSocketAddress to) throws IOException {
		checkBound();
		ByteBuffer bb;
		synchronized (this) {
			if (_sndBufDeprecated == null) _sndBufDeprecated = new DataBuffer(Helpers.getSndBufSize(), false);
			_sndBufDeprecated.flip();
			bb = ByteBuffer.allocate(_sndBufDeprecated.remaining());
			bb.order(_sndBufDeprecated.order());
			bb.put(_sndBufDeprecated.getInternalBuffer());
			bb.flip(); // ready to be written
			_sndBufDeprecated.resetCapacity();
		}
		send(to, bb, false);
	}

	@Deprecated
	public void send(ByteBuffer msg, boolean copy) {
		send(msg); // we always copy
	}

	@Deprecated
	public void send(ByteBuffer[] msg, boolean copy) {
		send(msg); // we always copy
	}

	@Deprecated
	public void send(byte[] msg, boolean copy) {
		send(msg); // we always copy
	}

	@Deprecated
	public void send(byte[] msg, int off, int len, boolean copy) {
		send(msg, off, len); // we always copy
	}

	/**
	 * Invoke listener connection opened callback. Method called from selector
	 * thread
	 */
	@SuppressWarnings("deprecation")
	private void scheduleConnectionOpenedAndEnableRead() {
		_queue.execute(() -> {
			try {
				_listener.connectionOpened(UdpChannelImpl.this);
			} catch (Throwable t) {
				_logger.warn("Exception caught while calling connectionOpened callback", t);
			}
			enableReadMode();
		});
	}

	/**
	 * Invoke listener connection closed callback. Only call it if the connection
	 * opened callback has been closed. Method called from selector thread
	 */
	private void scheduleConnectionClosed() {
		if (!_connectionClosedCalled) {
			_connectionClosedCalled = true;
			_queue.execute(this::connectionClosed);
		}
	}
	
	/**
	 * Channel aborted. Called from queue.
	 */
	protected void connectionClosed() {
		try {
			_listener.connectionClosed(UdpChannelImpl.this);
		} catch (Throwable err) {
			_logger.warn("Got unexpected exception on " + UdpChannelImpl.this, err);
		}
	}

	/**
	 * Invoke listener connection failed callback. Method called from selector
	 * thread.
	 */
	@SuppressWarnings("deprecation")
	private void scheduleConnectionFailed(Throwable t) {
		try {
			_listener.connectionFailed(UdpChannelImpl.this, t);
		} catch (Throwable err) {
			_logger.warn("Got unexpected exception on " + UdpChannelImpl.this, err);
		}
	}
}
