// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLEngine;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;

import alcatel.tess.hometop.gateways.reactor.Reactor;
import alcatel.tess.hometop.gateways.reactor.Security;
import alcatel.tess.hometop.gateways.reactor.TcpChannel;
import alcatel.tess.hometop.gateways.reactor.TcpClientChannelListener;
import alcatel.tess.hometop.gateways.reactor.util.DataBuffer;

public class TcpClientListenerTLSFilter implements TcpChannel, TcpClientChannelListener {
	
	private TcpChannel _wrappedChannel;
	private final TcpClientChannelListener _wrappedChannelListener;
	private volatile boolean _secured;
	private final static Logger _log = Logger.getLogger(TcpClientListenerTLSFilter.class);
	private final TLSEngineImpl _tls;
	private final ReactorProviderImpl _reactorProvider;
	private Executor _queue;

	/**
	 * Secured protocols
	 */
	private final static String[] SECURED_PROTOCOLS = { "TLS", "DTLS", "SSL" };

	
	/**
	 * Constructor
	 */
	public TcpClientListenerTLSFilter(TcpClientChannelListener listener, Map<?, Object> options, ReactorProviderImpl reactorProvider) 
			throws Exception 
	{
		_wrappedChannelListener = listener;
		if (_wrappedChannelListener == null) {
			throw new IllegalArgumentException("Missing \"TCP_CLIENT_LISTENER\" option from TcpClientFilterOption");
		}
		
		Security sec = (Security) options.get("security");
		if (sec == null) {
			throw new IllegalArgumentException("Missing \"SECURITY\" option from TcpClientFilterOption");
		}
		_secured = sec.isDelayed() ? false : true;
		
		String protocol = getSSLContextProtocol(sec);		
	    _tls = new TLSEngineImpl(sec, true /* client */, protocol, null, -1, "tcp" /* transport */);
	    _reactorProvider = reactorProvider;
	}
		
	// --------------------- TcpClientChannelListener -------------------
	
	@Override
	public void connectionEstablished(TcpChannel cnx) {
		_wrappedChannel = cnx;
		_queue = cnx.getInputExecutor();
		_wrappedChannelListener.connectionEstablished(this);
	}

	@Override
	public void connectionFailed(TcpChannel cnx, Throwable error) {
		_wrappedChannel = cnx;
		_wrappedChannelListener.connectionFailed(this, error);
	}
	
	public void close() {
		_wrappedChannel.close();
	}

	public void shutdown() {
		_wrappedChannel.shutdown();
	}

	public InetSocketAddress getRemoteAddress() {
		return _wrappedChannel.getRemoteAddress();
	}

	public <T> T attachment() {
		return _wrappedChannel.attachment();
	}

	public void flush() {
		_wrappedChannel.flush();
	}

	public void attach(Object attached) {
		_wrappedChannel.attach(attached);
	}

	public boolean isSecure() {
		return _wrappedChannel.isSecure();
	}

	@Override
	public List<SNIHostName> getClientRequestedServerNames() {
		return _wrappedChannel.getClientRequestedServerNames();
	}

	public Reactor getReactor() {
		return _wrappedChannel.getReactor();
	}

	public void upgradeToSecure() {
		_secured = true;
		_wrappedChannel.upgradeToSecure();
	}

	public Security getSecurity() {
		return _wrappedChannel.getSecurity();
	}

	public SSLEngine getSSLEngine() {
		return _tls.sslEngine();
	}

	public void setSoLinger(long linger) {
		_wrappedChannel.setSoLinger(linger);
	}

	public int getPriority() {
		return _wrappedChannel.getPriority();
	}

	public void setPriority(int priority) {
		_wrappedChannel.setPriority(priority);
	}

	public Map<String, Object> exportTlsKey(String asciiLabel, byte[] context_value, int length) {
	    //return _reactorProvider.getTlsExport().exportKey(_tls.sslEngine(), asciiLabel, context_value, length);
	    return Collections.emptyMap();	    
	}

	public void setWriteBlockedPolicy(WriteBlockedPolicy writeBlockedPolicy) {
		_wrappedChannel.setWriteBlockedPolicy(writeBlockedPolicy);
	}

	public InetSocketAddress getLocalAddress() {
		return _wrappedChannel.getLocalAddress();
	}

	public void setSoTimeout(long soTimeout) {
		_wrappedChannel.setSoTimeout(soTimeout);
	}

	public void setSoTimeout(long soTimeout, boolean readOnly) {
		_wrappedChannel.setSoTimeout(soTimeout, readOnly);
	}

	public boolean isClosed() {
		return _wrappedChannel.isClosed();
	}

	public void disableReading() {
		_wrappedChannel.disableReading();
	}

	public void enableReading() {
		_wrappedChannel.enableReading();
	}

	public void setInputExecutor(Executor executor) {
		_queue = executor;
		_wrappedChannel.setInputExecutor(executor);
	}

	public Executor getInputExecutor() {
		return _wrappedChannel.getInputExecutor();
	}

	public void send(ByteBuffer msg, boolean copy) {
		if (_secured) {
	      if (Helpers.isCurrentThreadInQueue(_queue)) {
	    	  sendSecuredFromQueue(msg);
	      } else {
	    	  ByteBuffer message = copy ? Helpers.copy(msg) : msg;
	    	  Helpers.schedule(_queue, ExecutorPolicy.SCHEDULE_HIGH, () -> sendSecuredFromQueue(message));
	      }
		} else {
			_wrappedChannel.send(msg, copy);
		}
	}
	
	private void sendSecuredFromQueue(ByteBuffer msg) {
		_tls.fillsEncoder(msg, null);
		runTLSEngine();
	}

	public void send(ByteBuffer[] msg, boolean copy) {
		int size = 0;
		for (int i = 0; i < msg.length; i ++) {
			size += msg[i].remaining();
		}
		ByteBuffer total = ByteBuffer.allocate(size);
		for (int i = 0; i < msg.length; i ++) {
			total.put(msg[i]);
		}
		total.flip();
		send(total, false);
	}

	public void send(DataBuffer msg) {
		send(msg.getInternalBuffer(), true);
	}

	public void send(ByteBuffer msg) {
		send(msg, true);
	}

	public void send(ByteBuffer[] msg) {
		send(msg, true);
	}

	public void send(byte[] msg) {
		send(ByteBuffer.wrap(msg), true);
	}

	public void send(byte[] msg, boolean copy) {
		send(ByteBuffer.wrap(msg), copy);
	}

	public void send(byte[] msg, int off, int len) {
		send(ByteBuffer.wrap(msg, off, len), true);
	}

	public void send(byte[] msg, int off, int len, boolean copy) {
		send(ByteBuffer.wrap(msg, off, len), copy);
	}

	public int getSendBufferSize() {		
		return _wrappedChannel.getSendBufferSize();
	}

	// ---------------------- TcpMessageListener -------------------------------------------------
	
	@Override
	public void receiveTimeout(TcpChannel cnx) {
		_wrappedChannelListener.receiveTimeout(cnx);
	}

	@Override
	public int messageReceived(TcpChannel cnx, ByteBuffer msg) {
		if (_secured) {
			_tls.fillsDecoder(msg, null);
			runTLSEngine();
		} else {
			return _wrappedChannelListener.messageReceived(this,  msg);
		}
		return 0;
	}

	@Override
	public void writeBlocked(TcpChannel cnx) {
		_wrappedChannelListener.writeBlocked(this);
	}

	@Override
	public void writeUnblocked(TcpChannel cnx) {
		_wrappedChannelListener.writeUnblocked(this);
	}

	@Override
	public void connectionClosed(TcpChannel cnx) {
		_wrappedChannelListener.connectionClosed(this);
	}

	// ------------------------ Private methods ------------------------------------------------------
	
	private static String getSSLContextProtocol(Security security) {
		String[] enabledProtocols = security.getEnabledProtocols();
		if (enabledProtocols != null) {
			for (String protocol : SECURED_PROTOCOLS) {
				if (hasProtocol(enabledProtocols, protocol)) {
					return protocol;
				}
			}
		}
		return "TLS";
	}
	
	private static boolean hasProtocol(String[] protocols, String expectedProtocol) {
		return Stream.of(protocols)
				     .filter(p -> p.toUpperCase().startsWith(expectedProtocol.toUpperCase()))
				     .findFirst().isPresent();
	}
	
	private void runTLSEngine() {
		try {
			TLSEngine.Status status;
			while ((status = _tls.run()) != TLSEngine.Status.NEEDS_INPUT) {
				switch (status) {
				case DECODED:
					invokeListener(() -> _wrappedChannelListener.messageReceived(this, _tls.getDecodedBuffer()), 						 								
							"exception while calling listener messageReceived method");
					break;

				case ENCODED:
					_wrappedChannel.send(_tls.getEncodedBuffer(), true);
					break;

				case CLOSED:
					_log.info("TLS closed");
					_wrappedChannel.close();
					return;
					
				default:
					break;
				}
			}
		}

		catch (Throwable t) {
			_log.warn("TLS exception", t);
			_wrappedChannel.shutdown();
		}
	}

	private boolean invokeListener(Runnable task, String errMsg) {
		try {
			task.run();
			return true;
		} catch (Throwable t) {
			_log.warn(errMsg, t);
			return false;
		}
	}

}
