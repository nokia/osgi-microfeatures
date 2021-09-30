package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.Notification;
import com.sun.nio.sctp.SctpChannel;

import alcatel.tess.hometop.gateways.reactor.SctpChannelListener;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;
import alcatel.tess.hometop.gateways.utils.Log;
/**
 * Secure sctp socket implementation. Uses DTLS.
 * Mirror of TcpChannelSecureImpl
 */
public class SctpChannelSecureImpl extends SctpChannelImpl {

	private final TLSEngine tlsEngine;
	private final DataBuffer bufferedMessage;

	private final String addr;
	private final int port;
	private final static Log _logger = Log.getLogger("as.service.reactor.SctpChannelSecureImpl");

	public SctpChannelSecureImpl(SctpChannel socket, Set<SocketAddress> localAddrs, Set<SocketAddress> remoteAddrs, SelectionKey key, ReactorImpl reactor, NioSelector selector,
			SctpChannelListener listener, int priority, Executor inputExecutor, Object attachment, boolean directBuffer,
			boolean nodelay, Boolean disableFragments, Boolean fragmentInterleave, Security security, boolean isClient, long linger,
			List<Notification> earlyNotifications)
					throws IOException {
		super(socket, localAddrs, remoteAddrs, key, reactor, selector, listener, priority, inputExecutor, attachment, directBuffer, nodelay, disableFragments, fragmentInterleave, linger, earlyNotifications);
		bufferedMessage = new DataBuffer(Helpers.getRcvBufSize(), false);

		addr = super.getRemotePrimaryAddress(remoteAddrs).getAddress().getHostAddress();
		port = super.getRemotePort();

		try {
			tlsEngine = new TLSEngineImpl(security, isClient, "DTLS", addr, port, "SCTP");
		} catch(Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public String toString() {
		try {
			return "SctpChannelSecure [local=" + getLocalAddress() + ",remote=" + addr + ":" + port + "]";
		} catch(Exception e) {
			return "SctpChannelSecure: closed channel";
		}
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@SuppressWarnings("restriction")
	@Override
	public alcatel.tess.hometop.gateways.reactor.SctpChannel send(boolean copy, SocketAddress addr, boolean complete, int ploadPID, int streamNumber,
			long timeToLive, boolean unordered, ByteBuffer ... data) {
		MessageInfo info = MessageInfo.createOutgoing(addr, streamNumber);
		info.complete(complete);
		info.payloadProtocolID(ploadPID);
		info.timeToLive(timeToLive);
		info.unordered(unordered);
		
		// copy data since we are going to schedule the tls encoder into our channel queue executor
		ByteBuffer copiedData = copy ? copy(data) : null;
				
		// call TLSEngine in queue for thread safety
		_inputExecutor.execute(() -> {
			if (copiedData != null) {
				tlsEngine.fillsEncoder(copiedData, info);
			} else {
				for (ByteBuffer buf : data) {
					tlsEngine.fillsEncoder(buf, info);
				}
			}
			runTLSEngine();
		});
		return this;
	}
	
	/**
	 * Copy the given byte buffers in one single byte buffer
	 * @param data the buffers to copy, which must be in read mode
	 * @return the copied data, in read mode
	 */
	private ByteBuffer copy(ByteBuffer[] data) {
		if (data.length == 1) {
			return Helpers.copy(data[0]); // read mode
		} else {
			ByteBuffer copy = ByteBuffer.allocate(Helpers.length(data));
			for (ByteBuffer buf : data) {
				copy.put(buf);
			}
			copy.flip(); // read mode
			return copy;			
		}
	}

	/**
	 * Handle tls input data (we are running in the channel executor thread).
	 */
	@Override
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

				tlsEngine.fillsDecoder(rcvBuf, info);
				runTLSEngine();
				rcvBuf.clear();
				//_listener.messageReceived(this, rcvBuf, info.address(), info.bytes(), info.isComplete(),
				//		info.isUnordered(), info.payloadProtocolID(), info.streamNumber());
			}
		} catch (Throwable t) {
			abort(t, 0);
		} finally {
			rcvBuf.clear();
		}

	}
	
	protected void abort(boolean scheduleConnectionClosed, Throwable t) {
		bufferedMessage.resetCapacity();
		super.abort(t, -1);
	}

	/**
	 * Runs the tls engine. We'll encode some messages to be sent, or decode some
	 * received encrypted messages.
	 */
	@SuppressWarnings("incomplete-switch")
	private void runTLSEngine() {
		try {
			TLSEngine.Status status;

			loop: while ((status = tlsEngine.run()) != TLSEngine.Status.NEEDS_INPUT) {
				switch (status) {
				case DECODED:
					messageReceived(tlsEngine.getDecodedBuffer(), (MessageInfo) tlsEngine.getDecodedAttachment());
					break;

				case ENCODED:
					_logger.debug("Encoded tls message");
					// This message is either a handshake message, or our encoded message: send it
					doSend(tlsEngine.getEncodedBuffer(), (MessageInfo) tlsEngine.getEncodedAttachment(), true);
					break;

				case CLOSED:
					_logger.debug("tls engine returned CLOSED status");
					abort(false, new IOException("TLS close"));
					break loop;
				}
			}
		}

		catch (Throwable t) {
			abort(false /* don't schedule connectionClosed in channel executor */, t);
		}
	}

	private void doSend(ByteBuffer msg, MessageInfo info, boolean copy) {
		int remaining = msg.remaining();
		_bufferedBytes.addAndGet(remaining);
		_sctpMeters.sctpWriteBuffer(remaining);
		_queue.add(new Message(info, msg, copy));
		scheduleWriteInterest(_key);
	}

	private void messageReceived(ByteBuffer decodedBuf, MessageInfo info) {
		if (decodedBuf.hasRemaining() && _logger.isInfoEnabled()) {
			Helpers.logPacketReceived(_logger.getLogger(), decodedBuf, this);
		}

		// Check if we have buffered a previous response, and append this new one to it.
		if (bufferedMessage.position() > 0) {
			bufferedMessage.put(decodedBuf);
			bufferedMessage.flip();
			_listener.messageReceived(this, bufferedMessage.getInternalBuffer(), info.address(), info.bytes(), info.isComplete(),
							info.isUnordered(), info.payloadProtocolID(), info.streamNumber());
		} else {
			_listener.messageReceived(this, decodedBuf, info.address(), info.bytes(), info.isComplete(),
					info.isUnordered(), info.payloadProtocolID(), info.streamNumber());
		}
	}

}
