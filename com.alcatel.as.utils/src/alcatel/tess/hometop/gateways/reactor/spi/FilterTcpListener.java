package alcatel.tess.hometop.gateways.reactor.spi;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;

/**
 * This class is the superclass of all classes that filter Tcp Listeners. These classes sit on top of an already existing Tcp Client listener 
 * which it uses as its basic sink of data, but possibly transforming the data along the way or providing additional functionality
 */
public class FilterTcpListener implements TcpChannelListener, TcpChannel {

	protected final TcpChannelListener _listener;
    protected final Map<?, Object> _options;
    protected TcpChannel _channel;

	public FilterTcpListener(TcpChannelListener listener, Map<?, Object> options) {
		_listener = listener;
		_options = options;
	}
	
	protected void setChannel(TcpChannel channel) {
		_channel = channel;
	}

    // TcpChannel
    
	public void close() {
		_channel.close();
	}

	public void shutdown() {
		_channel.shutdown();
	}

	public InetSocketAddress getRemoteAddress() {
		return _channel.getRemoteAddress();
	}

	public <T> T attachment() {
		return _channel.attachment();
	}

	public void flush() {
		_channel.flush();
	}

	public void attach(Object attached) {
		_channel.attach(attached);
	}

	public boolean isSecure() {
		return _channel.isSecure();
	}

	public List<SNIHostName> getClientRequestedServerNames() {
		return _channel.getClientRequestedServerNames();
	}
	
	public Reactor getReactor() {
		return _channel.getReactor();
	}

	public void upgradeToSecure() {
		_channel.upgradeToSecure();
	}

	public Security getSecurity() {
		return _channel.getSecurity();
	}

	public SSLEngine getSSLEngine() {
		return _channel.getSSLEngine();
	}

	public void setSoLinger(long linger) {
		_channel.setSoLinger(linger);
	}

	public int getPriority() {
		return _channel.getPriority();
	}

	public void setPriority(int priority) {
		_channel.setPriority(priority);
	}

	public Map<String, Object> exportTlsKey(String asciiLabel, byte[] context_value, int length) {
		return _channel.exportTlsKey(asciiLabel, context_value, length);
	}

	public void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy) {
		_channel.setWriteBlockedPolicy(writeBlockedPolicy);
	}

	public InetSocketAddress getLocalAddress() {
		return _channel.getLocalAddress();
	}

	public void setSoTimeout(long soTimeout) {
		_channel.setSoTimeout(soTimeout);
	}

	public void setSoTimeout(long soTimeout, boolean readOnly) {
		_channel.setSoTimeout(soTimeout, readOnly);
	}

	public boolean isClosed() {
		return _channel.isClosed();
	}

	public void disableReading() {
		_channel.disableReading();
	}

	public void enableReading() {
		_channel.enableReading();
	}

	public void setInputExecutor(Executor executor) {
		_channel.setInputExecutor(executor);
	}

	public Executor getInputExecutor() {
		return _channel.getInputExecutor();
	}

	public final void send(DataBuffer msg) {
		send(msg.getInternalBuffer(), true);
	}
	public final void send(ByteBuffer msg) {
		send(msg, true);
	}
	public final void send(ByteBuffer[] msg) {
		send(msg, true);
	}
	public final void send(byte[] msg) {
		send(msg, true);
	}
	public final void send(byte[] data, boolean copy) {
	    send(data, 0, data.length, copy);
	}
	public final void send(byte[] data, int off, int len) {
	    send(ByteBuffer.wrap(data, off, len), true);
	}
	public final void send(byte[] data, int off, int len, boolean copy) {
	    send(ByteBuffer.wrap(data, off, len), copy);
	}
	public void send(ByteBuffer msg, boolean copy) {
		send(msg, null, copy);
	}
	public void send(ByteBuffer[] msg, boolean copy) {
		send(null, msg, copy);
	}
	public void send(ByteBuffer msg, ByteBuffer[] array, boolean copy) {
		if (msg != null) {
			_channel.send(msg, copy);
		} else if (array != null) {
			_channel.send(array, copy);
		}
	}

	public int getSendBufferSize() {
		return _channel.getSendBufferSize();
	}

	// TcpChannelListener
	
	public void receiveTimeout(TcpChannel cnx) {
		_listener.receiveTimeout(cnx);
	}

	public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
		return _listener.messageReceived(cnx, msg);
	}

	public void writeBlocked(TcpChannel cnx) {
		_listener.writeBlocked(cnx);
	}

	public void writeUnblocked(TcpChannel cnx) {
		_listener.writeUnblocked(cnx);
	}

	public void connectionClosed(TcpChannel cnx) {
		_listener.connectionClosed(cnx);
	}
}
