package com.alcatel_lucent.as.service.jetty.common.connector;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.eclipse.jetty.io.AbstractEndPoint;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.FillInterest;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.thread.Invocable.InvocationType;
import org.eclipse.jetty.websocket.server.WebSocketServerConnection;

import com.alcatel.as.service.concurrent.PlatformExecutor;

import alcatel.tess.hometop.gateways.utils.Utils;

public abstract class AbstractBufferEndPoint extends AbstractEndPoint {

	private final static String EP_CLOSED = "EndPoint closed";
	public final static InetSocketAddress NOIP = new InetSocketAddress(0);
	private final static Logger _log = Logger.getLogger(AbstractBufferEndPoint.class);
	protected volatile boolean closed;
	private boolean secure;
	private Object attachment;
	private final ConcurrentLinkedQueue<ByteArray> queue;
	protected final EndPointOutputStream input;
	private final PlatformExecutor _ioTPoolQueue;
	private final PlatformExecutor _webAgentQueue;
	static BufferConnector connector;

	public AbstractBufferEndPoint(InetSocketAddress remote, Object att, boolean secure, PlatformExecutor tpool) {
		super(connector.getScheduler());
		this.attachment = att;
		this.secure = secure;
		this.input = new EndPointOutputStream();
		this.queue = new ConcurrentLinkedQueue<ByteArray>();
		_ioTPoolQueue = tpool.getPlatformExecutors().createQueueExecutor(tpool);
		_webAgentQueue = tpool.getPlatformExecutors().getCurrentThreadContext().getCurrentExecutor();
	}

	public AbstractBufferEndPoint(InetSocketAddress remote, boolean secure, PlatformExecutor tpool) {
		this(remote, null, secure, tpool);
	}

	/*------------- AbstractEndPoint ----------------------*/

	public abstract void prepareUpgrade();

	@Override
	protected void needsFillInterest() throws IOException { 
		_webAgentQueue.execute(this::fillable);
	}

	// executed from web agent queue
	void fillable() {
		// do nothing if endpoint is still opened and if there is no data to read currently.
		if (queue.isEmpty() && ! closed) {
			return;
		}

		FillInterest fillInterest = getFillInterest();
		if (fillInterest.isInterested()) {
			if (fillInterest.getCallbackInvocationType() == InvocationType.NON_BLOCKING) {
				fillInterest.fillable();
			} else {
				_ioTPoolQueue.execute(() -> fillInterest.fillable());
			}
		}
	}

	@Override
	public int fill(ByteBuffer buffer) throws IOException { // curr thread = io tpool, caller = jetty
		if (closed) {
			if (this.getConnection() instanceof WebSocketServerConnection)
				return 0;
			throw new EofException("CLOSED");
		}
		int filled = 0;
		ByteArray array = queue.peek();
		if (array != null) {
			filled = BufferUtil.fill(buffer, array.buf, array.off, array.len);
			if (filled >= array.len) {
				queue.remove();
			} else {
				array.off += filled;
				array.len -= filled;
			}
		}
		if (Log.getRootLogger().isDebugEnabled() && filled > 0) {
			Log.getRootLogger().debug(this + ", added in buffer=" + filled);
		}
		return filled;
	}

	@Override
	public boolean flush(ByteBuffer... buffers) throws IOException {
		if (closed)
			throw new EofException(EP_CLOSED);
		if (Log.getRootLogger().isDebugEnabled()) {
			for (int i = 0; i < buffers.length; i++) {
				Log.getRootLogger().debug("flush ByteBuffer " + i + " hasArray=" + buffers[i].hasArray() + " class="
						+ buffers[i].getClass().getSimpleName() + " remaining=" + buffers[i].remaining());
			}
		}
		try {
			connector.getEndPointManager().messageReceived(attachment, buffers);
		} catch (Exception e) {
			throw new EofException(e);
		}
		return true;
	}

	@Override
	public Object getTransport() {
		return null;
	}

	@Override
	public boolean isInputShutdown() {
		return closed;
	}

	@Override
	public boolean isOutputShutdown() {
		return closed;
	}

	@Override
	public void onClose() {
		close(true);
	}

	@Override
	protected void onIncompleteFlush() {
		// Nothing to do, "flush" always return true
	}

	@Override
	public boolean isOpen() {
		return !closed;
	}

	public String newSession() {
		return connector.getEndPointManager().newSession(attachment);
	}

	public String renewSessionId() {
		return connector.getEndPointManager().changeSessionId(attachment);
	}

	/* ------------------------------------------------------------ */

	public void close(boolean advertise) {
		if (closed)
			return;
		try {
			if (Log.getRootLogger().isDebugEnabled()) {
				Log.getRootLogger().debug(this + " closed by " + ((advertise) ? "server " : "client ") + attachment);
			}
			closed = true;
			getConnection().onClose();
			if (advertise) {
				connector.getEndPointManager().connectionClosed(attachment);
			}
		} finally {
			fillable();
		}
	}

	protected boolean isSecure() {
		return secure;
	}

	@Override
	public String toString() {
		return getClass().getName() + " [closed=" + closed + ", attachment=" + attachment + "]";
	}

	/* ------------------------------------------------------------ */

	public class EndPointOutputStream extends OutputStream {

		public void write(int b) throws IOException {
			if (closed)
				throw new IOException(EP_CLOSED);

			// offer always returns true
			queue.offer(new ByteArray((byte) b));
		}

		public void write(byte b[], int off, int len) throws IOException {
			if (b == null) {
				throw new NullPointerException();
			}
			if (len == 0) {
				return;
			}
			if ((off >= 0) && ((off + len) <= b.length)) {
				if (closed)
					throw new IOException(EP_CLOSED);

				// offer always returns true
				queue.offer(new ByteArray(b, off, len));
			} else {
				throw new IndexOutOfBoundsException();
			}
		}

		// executed from web agent queue
		@Override
		public void flush() throws IOException {
			fillable();
		}
	}

	final static class ByteArray {

		byte[] buf;
		int off;
		int len;

		public ByteArray(byte[] buf, int off, int len) {
			super();
			this.buf = buf;
			this.off = off;
			this.len = len;
			if (Log.getRootLogger().isDebugEnabled()) {
				Log.getRootLogger().debug(Utils.dumpByteArray("input buffer len=" + len + "\n", buf, off, len, 1024));
			}
		}

		public ByteArray(byte b) {
			this(new byte[] { b }, 0, 1);
		}

	}

}
