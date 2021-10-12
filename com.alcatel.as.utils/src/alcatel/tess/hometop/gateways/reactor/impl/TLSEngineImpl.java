// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.X509ExtendedKeyManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.sun.nio.sctp.MessageInfo;

import alcatel.tess.hometop.gateways.reactor.Security;

/**
 * TLSEngine implementation.
 * This class implements the SSLEngine state machine.
 */
public class TLSEngineImpl implements TLSEngine {
	
	private final static int SSLCONTEXT_CACHE_SIZE = Integer.getInteger("reactor.sslcontext.cachesize", 100);
	
	private enum BufferMode {
		WRITE, READ
	}

	/** Decrypted received data */
	private static final int IN = 0;
	/** Received data */
	private static final int IN_ENCRYPTED = 1;
	/** Data to send */
	private static final int OUT = 2;
	/** Encrypted data to send */
	private static final int OUT_ENCRYPTED = 3;
	private static final int MAX_BUFFERS = 4;

	/** Buffers for data storage */
	private ByteBuffer _buffers[] = new ByteBuffer[MAX_BUFFERS];
	/** Current mode for each buffer */
	private BufferMode _buffersMode[] = new BufferMode[MAX_BUFFERS];

	private SSLEngine _sslEngine;
	private boolean _isClient;
	private String protocol;

	/** Attachment for each buffer **/
	private static final int OUT_COPY = 1;
	/** Only three attachments: IN, OUT and OUT_COPY **/
	private Object attachments[] = new Object[3];

	private enum InternalState {
		Handshaking, Applicative, Closing, Closed
	}

	private InternalState _state = InternalState.Handshaking;

	/**
	 * Result of previous wrap/unwrap operation : used to manage internal state
	 */
	private SSLEngineResult _result = null;

	/**
	 * Flag telling if we needs client auth (only relevant if we are a server !)
	 */
	private boolean _serverNeedsClientAuth;

	/** The remote ip we are connected to */
	private String _remoteIp;

	/** The remote ip port we are connected to */
	private int _remotePort;

	/** Our logger. */
	private final static Logger _logger = Logger.getLogger("as.service.reactor.TLSEngineImpl");
	
	/** In Server mode, we can detect client requested server names (SNI) */
	private List<SNIHostName> _clientRequestedServerNames = Collections.emptyList();
	
	/**
	 * Key Manager that can retrieve Client requested server names. Only used in server mode.
	 */
	public final class SniKeyManager extends X509ExtendedKeyManager {
		private final X509ExtendedKeyManager keyManager;

		public SniKeyManager(X509ExtendedKeyManager keyManager) {
			this.keyManager = keyManager;
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return keyManager.getClientAliases(keyType, issuers);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return keyManager.chooseClientAlias(keyType, issuers, socket);
		}

		@Override
		public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
			return keyManager.chooseEngineClientAlias(keyType, issuers, engine);
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return keyManager.getServerAliases(keyType, issuers);
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return keyManager.chooseServerAlias(keyType, issuers, socket);
		}

		@Override
		public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
			ExtendedSSLSession session = (ExtendedSSLSession) engine.getHandshakeSession();
			_clientRequestedServerNames = session.getRequestedServerNames()
					.stream()
					.filter(sni -> sni instanceof SNIHostName)
					.map(sni -> (SNIHostName) sni)
					.collect(Collectors.toList());
			return keyManager.chooseEngineServerAlias(keyType, issuers, engine);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return keyManager.getCertificateChain(alias);
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			return keyManager.getPrivateKey(alias);
		}
	}

	/**
	 * Add the DTLS provider to the java security classes
	 * If there is any problem, ignore it, the Exception will be thrown when
	 * we call SSLContext.getInstance("DTLS")
	 */
	static {
		try {
		    Class<?> dtlsProvider = Class.forName("com.nokia.as.dtls.provider.DTLSProvider");
		    java.security.Security.addProvider((java.security.Provider) dtlsProvider.newInstance());
		} catch(Throwable t) {
		    _logger.debug("DTLSProvider not found (dtls disabled)", t);
		}
	}
	
	/**
	 * 
	 * @param security reactor security parameters
	 * @param isClient true if in client mode
	 * @param sslProtocol the SSLContext secured protocol used 
	 *                    (see https://docs.oracle.com/en/java/javase/11/docs/specs/security/standard-names.html#sslcontext-algorithms)
	 * @param remoteIp
	 * @param remotePort
	 * @param networkProtocol (either "TCP", "SCTP", or "UDP")
	 * @throws Exception
	 */
	public TLSEngineImpl(Security security, boolean isClient, String sslProtocol, String remoteIp, int remotePort, String networkProtocol)
			throws Exception {

		this.protocol = networkProtocol;
		SSLContext sslc = getSSLContext(security, sslProtocol, isClient);
		String peerHost = security.endpointIdentity() != null ? security.endpointIdentity() : remoteIp;
		_sslEngine = sslc.createSSLEngine(peerHost, remotePort);
		_serverNeedsClientAuth = security.authenticateClients();
		_remoteIp = remoteIp;
		_remotePort = remotePort;

		String[] enabledProtocols = security.getEnabledProtocols();
		if (_logger.isDebugEnabled()) {
			_logger.debug("configured enabled secured protocols:" + Arrays.toString(enabledProtocols));
		}

		if (enabledProtocols.length > 0) {
			_sslEngine.setEnabledProtocols(enabledProtocols);
		} else {
			// no enabled versions specified for ssl protocol used. Try to use default versions:
			// if sslProtocol is TLS, use default version TLSv1.2 (TODO: in java11, why not using TLSv1.3 ?)
			// if sslProtocol is DTLS, use default version DTLSv1.2
			if (sslProtocol.toUpperCase().startsWith("TLS")) {
				if (_logger.isDebugEnabled()) _logger.debug("No protocol version specified for ssl protocol " + sslProtocol + ". Using by default version=TLSv1.2");
				enabledProtocols = new String[] { "TLSv1.2" };
			} else if (sslProtocol.toUpperCase().startsWith("DTLS")) {
				if (_logger.isDebugEnabled()) _logger.debug("No protocol version specified for ssl protocol " + sslProtocol + ". Using by default version=TLSv1.2");
				enabledProtocols = new String[] { "DTLSv1.2" };
			}
			_sslEngine.setEnabledProtocols(enabledProtocols);
		}
		
		if (_logger.isDebugEnabled()) {
			String[] enabled = _sslEngine.getEnabledProtocols();
			if (enabled != null) {
				_logger.debug("enabled secured protocols:" + Arrays.toString(enabled));
			}
		}
		
		String[] enabledCipherSuites = security.getCipherSuites();
		if (enabledCipherSuites.length > 0) {
			String defaultEnabledCiphers[] = _sslEngine.getEnabledCipherSuites();
			Set<String> tmp = new LinkedHashSet<>();
			if (defaultEnabledCiphers != null && defaultEnabledCiphers.length > 0) {
				// see if the specified ciphers list includes one of the ciphers
				// enabled by default.
				for (String defaultEnabledCipher : defaultEnabledCiphers) {
					for (String enabledCipher : enabledCipherSuites) {
						if (enabledCipher.indexOf("*") != -1 || enabledCipher.indexOf("[") != -1) {
							if (defaultEnabledCipher.matches(enabledCipher)) {
								tmp.add(defaultEnabledCipher);
								break;
							}
						}
					}
				}
			}

			// add all enabled ciphers (except if they contain a regex
			// character)
			for (String enabledCipher : enabledCipherSuites) {
				if (enabledCipher.indexOf("*") == -1) {
					tmp.add(enabledCipher);
				}
			}
			_sslEngine.setEnabledCipherSuites(tmp.toArray(new String[tmp.size()]));

			SSLParameters params = _sslEngine.getSSLParameters();
			if (security.useCipherSuitesOrder()) {
				params.setUseCipherSuitesOrder(true);
			}
			
			//Disable retransmissions for SCTP on DTLS: see https://tools.ietf.org/html/rfc6083#section-3.5
			if(networkProtocol.equalsIgnoreCase("sctp")) {
				Method m = SSLParameters.class.getMethod("setEnableRetransmissions", boolean.class);
				m.invoke(params, false);
			}
			
			//set application protocols for ALPN if applicable
			String[] applicationProtocols = security.getApplicationProtocols();
			if(applicationProtocols.length > 0) {
				Method m = SSLParameters.class.getMethod("setApplicationProtocols", String[].class);
				m.invoke(params, new Object[]{applicationProtocols});
			}
			// Set a valid endpointIdentificationAlgorithm for SSL socket to trigger hostname verification
			// (see https://help.semmle.com/wiki/display/JAVA/Unsafe+certificate+trust+and+improper+hostname+verification)
			if (security.endpointIdentificationAlgorithm() != null) {
				params.setEndpointIdentificationAlgorithm(security.endpointIdentificationAlgorithm());  
			}
			
			// Set SNI parameters
			
			if (isClient) {
				List<String> sni = security.getSNI();
				if (sni.size() > 0) {
					if (_logger.isDebugEnabled()) _logger.debug("setting sni list: " + sni);
					List<SNIServerName> sniList = sni.stream()
						.map(server -> new SNIHostName(server)).collect(Collectors.toList());
					params.setServerNames(sniList);
				}
			} else {
				List<String> sniMatchers = security.getSNIMatchers();
				if (sniMatchers.size() > 0) {
					List<SNIMatcher>  matchers = sniMatchers.stream()
						.map(matcher -> SNIHostName.createSNIMatcher(matcher)).collect(Collectors.toList());
					if (_logger.isDebugEnabled()) _logger.debug("setting sni matcher list: " + matchers);
					params.setSNIMatchers(matchers);
				}
			}
			
			_sslEngine.setSSLParameters(params);
		}

		_isClient = isClient;
		_sslEngine.setUseClientMode(isClient);
		if (!isClient) {
			_sslEngine.setNeedClientAuth(_serverNeedsClientAuth);
		}

		for (int i = 0; i < MAX_BUFFERS; i++) {
			_buffers[i] = ByteBuffer.allocate(getPacketSize());
			_buffersMode[i] = BufferMode.WRITE;
		}

	}
		
	private synchronized SSLContext getSSLContext(Security security, String sslProtocol, boolean isClient) throws Exception {
		SSLContext ctx = security.getSSLContext();
		if (ctx == null) {
			ctx = SSLContext.getInstance(sslProtocol);
			
			KeyManager[] kms = security.getKeyManagers();
			if (! isClient) {
				// add our SNI key manager that can retrieve client requested server names
				addSniKeyManager(kms);
			}
			
			ctx.init(kms, security.getTrustManagers(), null);
			ctx.getServerSessionContext().setSessionCacheSize(Integer.getInteger("reactor.ssl.server.session.cache.size", 1024));
			ctx.getServerSessionContext().setSessionTimeout(Integer.getInteger("reactor.ssl.server.session.cache.timeout", 60));
			ctx.getClientSessionContext().setSessionCacheSize(Integer.getInteger("reactor.ssl.client.session.cache.size", 1024));
			ctx.getClientSessionContext().setSessionTimeout(Integer.getInteger("reactor.ssl.client.session.cache.timeout", 60));
			_logger.info("Created SSLContext for protocol " + sslProtocol);
		}
		return ctx;
	}
	
	private void addSniKeyManager(KeyManager[] kms) {
		for (int i = 0; i < kms.length; i ++) {
			if (kms[i] instanceof X509ExtendedKeyManager) {
				if (_logger.isDebugEnabled()) _logger.debug("Adding SNI Key Manager to " +  kms[i]);
				kms[i] = new SniKeyManager((X509ExtendedKeyManager) kms[i]);
				break;
			}
		}
	}

	private int getPacketSize() {
		int appSize = _sslEngine.getSession().getApplicationBufferSize();
		int netSize = _sslEngine.getSession().getPacketBufferSize();
		return appSize > netSize ? appSize : netSize;
	}

	private void checkReadBuffer(int buffer) {
		checkBufferMode(buffer, BufferMode.READ, 0);
	}

	private void checkBufferMode(int buffer, BufferMode op, int spaceNeeded) {
		if (_buffersMode[buffer] != op) {
			if (op == BufferMode.WRITE)
				_buffers[buffer].compact();
			else
				_buffers[buffer].flip();
			_buffersMode[buffer] = op;
		}

		if (op == BufferMode.WRITE) {
			if (spaceNeeded > _buffers[buffer].remaining()) {
				if (_logger.isDebugEnabled()) {
					log(Level.DEBUG, "Reallocate : " + _buffers[buffer].capacity() + "+" + spaceNeeded);
				}
				ByteBuffer tmp = ByteBuffer.allocate(_buffers[buffer].capacity() + spaceNeeded);
				checkReadBuffer(buffer);
				tmp.put(_buffers[buffer]);
				_buffers[buffer] = tmp;
				// tmp was in write configuration
				_buffersMode[buffer] = BufferMode.WRITE;
			}
		}
	}

	public List<SNIHostName> getClientRequestedServerNames() {
		return _clientRequestedServerNames;
	}
	
	public void fillsDecoder(ByteBuffer buf, Object attachment) {
		checkBufferMode(IN_ENCRYPTED, BufferMode.WRITE, buf.remaining());
		_buffers[IN_ENCRYPTED].put(buf);
		attachments[IN] = attachment;

	}

	public void fillsDecoder(ByteBuffer buf) {
		fillsDecoder(buf, null);
	}

	public void fillsEncoder(ByteBuffer buf) {
		fillsEncoder(buf, null);
	}

	public void fillsEncoder(ByteBuffer buf, Object attachment) {
		checkBufferMode(OUT, BufferMode.WRITE, buf.remaining());
		_buffers[OUT].put(buf);
		attachments[OUT] = attachment;
	}

	public ByteBuffer getDecodedBuffer() {
		checkReadBuffer(IN);
		return _buffers[IN];
	}

	public Object getDecodedAttachment() {
		return attachments[IN];
	}

	public ByteBuffer getEncodedBuffer() {
		checkReadBuffer(OUT_ENCRYPTED);
		return _buffers[OUT_ENCRYPTED];
	}

	public Object getEncodedAttachment() {
		return attachments[OUT];
	}

	public Status run() throws IOException {
		Status res;

		switch (_state) {
		case Handshaking:
			res = doHandshake();
			if (res == null) {
				if (_logger.isDebugEnabled()) {
					log(Level.DEBUG, "... Handshaking done");
				}
				attachments[OUT] = copyAttachment(attachments[OUT_COPY]);
				_state = InternalState.Applicative;
				return run(); // To treat a possible stored message
			}
			return res;

		case Closing:
			Status s = doClosing();
			if (s == Status.CLOSED)
				if (_logger.isDebugEnabled()) {
					log(Level.DEBUG, "... Closing done");
				}

			return s;

		case Applicative:
			// Check if some app buf needs to be encoded
			checkReadBuffer(OUT);
			if (_buffers[OUT].hasRemaining()) {
				return encode();
			}
			// check if some net buf needs to be decoded.
			checkReadBuffer(IN_ENCRYPTED);
			if (_buffers[IN_ENCRYPTED].hasRemaining()) {
				res = decode();
				if (res == null) {
					return run();
				}
				return res;
			}
			return (Status.NEEDS_INPUT);

		case Closed:
			return Status.CLOSED;
		}

		throw new IOException("Wrong internal state " + _state);
	}

	public void close() {
		_sslEngine.closeOutbound();
		_state = InternalState.Closing;
		_result = null;
		if (_logger.isDebugEnabled()) {
			log(Level.DEBUG, "Closing TLS ...");
		}
	}

	/**
	 * Ensure buffers are in correct READ/WRITE mode Wrap data from buffer OUT
	 * into buffer OUT_ENCRYPTED
	 */
	private SSLEngineResult doWrap() throws SSLException {
		checkReadBuffer(OUT);
		checkBufferMode(OUT_ENCRYPTED, BufferMode.WRITE, _sslEngine.getSession().getPacketBufferSize());
		return _sslEngine.wrap(_buffers[OUT], _buffers[OUT_ENCRYPTED]);
	}

	/**
	 * Ensure buffers are in correct READ/WRITE mode Unwrap data from buffer
	 * IN_ENCRYPTED into buffer IN
	 */
	private SSLEngineResult doUnwrap() throws SSLException {
		checkReadBuffer(IN_ENCRYPTED);
		checkBufferMode(IN, BufferMode.WRITE, getPacketSize());
		return _sslEngine.unwrap(_buffers[IN_ENCRYPTED], _buffers[IN]);
	}

	/**
	 * @return The status for the next handshaking step, or null if handshake is
	 *         successfully done
	 * @throws IOException
	 *             if handshaking fails
	 */
	private Status doHandshake() throws IOException {
		try {
			if (_result == null) { // First step
				attachments[OUT_COPY] = copyAttachment(attachments[OUT]);
				attachments[OUT] = generateHeader();
				_sslEngine.beginHandshake();
				if (_isClient) { // In client mode, initialize handshake, and
									// wrap data
					_result = doWrap();
					if (_result.bytesProduced() > 0) {
						return Status.ENCODED;
					}
				} else { // In server mode try to unwrap data received from
							// client
					_result = doUnwrap();
				}
			} else {
				// Check state
				if (_result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
					// We previously ask for more data
				} else if (_result.bytesProduced() > 0) {
					// we previously ask to send data
				} else
					throw new IOException("Wrong internal state " + _result);
			}

			// Do SSLEngine operation, until we have some data to send
			// (ENCODED), we need to
			// receive data (NEEDS_INPUT), or Handshaking is done
			SSLEngineResult.HandshakeStatus hsStatus = _result.getHandshakeStatus();
			while (true) {
				switch (hsStatus) {
				case NEED_TASK:
					_sslEngine.getDelegatedTask().run();
					hsStatus = _sslEngine.getHandshakeStatus();
					if (hsStatus == SSLEngineResult.HandshakeStatus.FINISHED)
						return null;
					continue; //we restart the switch statement after the task
				case NEED_WRAP:
					_result = doWrap();
					hsStatus = _result.getHandshakeStatus();
					if (hsStatus == SSLEngineResult.HandshakeStatus.FINISHED) {
						if (_result.bytesProduced() > 0) { // If we produced
															// data we need to
															// send them
							_state = InternalState.Applicative;
							return Status.ENCODED;
						}
						return null;
					}

					if (_result.bytesProduced() > 0) {
						return Status.ENCODED;
					}
					break;

				case FINISHED:
				case NOT_HANDSHAKING:
					break;
					
				case NEED_UNWRAP:
				default: //case NEED_UNWRAP_AGAIN:
					_result = doUnwrap();
					hsStatus = _result.getHandshakeStatus();
					if (hsStatus == SSLEngineResult.HandshakeStatus.FINISHED)
						return null;
					break;
				}

				switch (_result.getStatus()) {
				case BUFFER_UNDERFLOW:
					// Ignore buffer underflow if there are some data to wrap
					// and sent
					if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP)
						break;
					return Status.NEEDS_INPUT;

				case CLOSED:
					throw new IOException("Failed to handshake : status closed " + _result);

				case OK:
					break;

				case BUFFER_OVERFLOW:
					//we should never get to BUFFER_OVERFLOW
					break;
				}
			}
		} catch (IOException e) {
			log(Level.INFO, "Handshaking failed ", e);
			throw e;
		} catch (Throwable t) {
			log(Level.INFO, "Handshaking failed ", t);
			throw new IOException("Handshaking failed : " + t.getMessage());
		}
	}

	/**
	 * @return The status for the next closing step (Status.CLOSED if closing is
	 *         successfully finished)
	 * @throws IOException
	 *             if closing fails
	 */
	private Status doClosing() throws IOException {
		try {
			if (_result == null) { // First step
				if (_isClient) { // In client mode, initialize handshake, and wrap data
					_result = doWrap();
					if (_result.bytesProduced() > 0) {
						return Status.ENCODED;
					}
				} else { // In server mode try to unwrap data received from client
					_result = doUnwrap();
				}
			} else {
				// Check state
				if (_result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
					// We previously ask for more data
				} else if (_result.bytesProduced() > 0) {
					// we previously ask to send data
				} else
					throw new IOException("Wrong internal state " + _result);
			}

			// Do SSLEngine operation, until we have some data to send (ENCODED), we need to receive data (NEEDS_INPUT), or Handshaking is done
			SSLEngineResult.HandshakeStatus hsStatus = _result.getHandshakeStatus();
			while (true) {
				switch (hsStatus) {
				case NEED_TASK:
					_sslEngine.getDelegatedTask().run();
					hsStatus = _sslEngine.getHandshakeStatus();
					if (hsStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
						return Status.CLOSED;
					continue;
				case NEED_WRAP:
					_result = doWrap();
					hsStatus = _result.getHandshakeStatus();
					if (hsStatus == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
						if (_result.bytesProduced() > 0) { // If we produced data we need to send them
							_state = InternalState.Closed;
							return Status.ENCODED;
						}
						return Status.CLOSED;
					}

					if (_result.bytesProduced() > 0)
						return Status.ENCODED;
					break;

				case NOT_HANDSHAKING:
					return Status.CLOSED;

				case FINISHED:
					break;
					
				case NEED_UNWRAP:
				default: // case NEED_UNWRAP_AGAIN:
					_result = doUnwrap();
					hsStatus = _result.getHandshakeStatus();
					if (hsStatus == SSLEngineResult.HandshakeStatus.FINISHED)
						return Status.CLOSED;
					break;
				}

				switch (_result.getStatus()) {
				case BUFFER_UNDERFLOW:
					// Ignore buffer underflow if there are some data to wrap and sent
					hsStatus = _result.getHandshakeStatus();
					if (hsStatus == SSLEngineResult.HandshakeStatus.NEED_WRAP)
						break;
					return Status.NEEDS_INPUT;

				case CLOSED:
				case OK:
				case BUFFER_OVERFLOW:
					//we should never get to BUFFER_OVERFLOW
					break;
				}
			}
		} catch (IOException e) {
			log(Level.INFO, "Closing failed ", e);
			throw e;
		} catch (Throwable t) {
			log(Level.INFO, "Closing failed ", t);
			throw new IOException("Closing failed : " + t.getMessage());
		}
	}

	private Status decode() throws IOException {
		// Here, we must actually decrypt the tls data, and store the result in
		// our "IN" buffer.
		try {
			_result = doUnwrap();

			if (_result.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING && _result.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.FINISHED) {
				// Start a new Handshaking
				if (_result.getStatus() == SSLEngineResult.Status.CLOSED) {
					if (_logger.isDebugEnabled()) {
						log(Level.DEBUG, "Closing TLS requested ...");
					}
					// normally, we should set the state to InternalState.Closing, but since the shutdown procedure is buggy, let's return Status.CLOSED 
					//_state = InternalState.Closing;
					_state = InternalState.Closed;
					return Status.CLOSED;
				} else {
					if (_logger.isDebugEnabled()) {
						log(Level.DEBUG, "Start new handshaking ...");
					}
					_state = InternalState.Handshaking;
				}

				_result = null;
				return null;
			}

			if (_result.getStatus() == SSLEngineResult.Status.OK) {
				// To test re-handshaking : uncomment this
				/*
				 * if (_isClient && _state == InternalState.Applicative) {
				 * _state = InternalState.Handshaking; _result = null; }
				 */
				if (_result.bytesProduced() == 0) {
					if (_logger.isDebugEnabled()) _logger.debug("unwrap did not produce anything: " + _result);
					return null;
				}
				return (Status.DECODED);
			}
			if (_result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW)
				return (Status.NEEDS_INPUT);

		} catch (Throwable e) {
			log(Level.INFO, "Failed to decode data ", e);
			throw new IOException("Failed to decode data " + e.getMessage());
		}
		throw new IOException("Unexpected unwrap return " + _result);
	}

	private Status encode() throws IOException {
		try {
			_result = doWrap();

			if (_result.getStatus() == SSLEngineResult.Status.OK)
				return (Status.ENCODED);
		} catch (Throwable e) {
			log(Level.INFO, "Failed to encode data ", e);
			throw new IOException("Failed to encode data " + e.getMessage());
		}

		throw new IOException("Unexepected wrap return " + _result);
	}

	/**
	 * Log a message with a remoteIp:remotePort as prefix.
	 */
	private void log(Level level, String msg) {
		log(level, msg, null);
	}

	private void log(Level level, String msg, Throwable t) {
		StringBuffer sb = new StringBuffer();
		if (_remoteIp != null) {
			sb.append("[");
			sb.append(_remoteIp).append(":").append(_remotePort).append("] ");
		}
		sb.append(msg);
		if (t == null) {
			_logger.log(level, sb);
		} else {
			_logger.log(level, sb, t);
		}
	}
	
	private Object generateHeader(){
		if("sctp".equalsIgnoreCase(protocol)) return generateSCTPHandshakeHeader();
		if("udp".equalsIgnoreCase(protocol)) {
			if(attachments[IN] != null) return attachments[IN];
			else return attachments[OUT];
		}
		else return null;
	}
	
	//https://tools.ietf.org/html/rfc6083#section-4.4
	private MessageInfo generateSCTPHandshakeHeader() {
		MessageInfo info = MessageInfo.createOutgoing(null, 0);
		info.unordered(false);
		return info;
	}	
	
	private Object copyAttachment(Object attachment) {
		if(attachment instanceof MessageInfo) return copyMessageInfo((MessageInfo) attachment);
		if(attachment instanceof InetSocketAddress) return copyAddress((InetSocketAddress) attachment);
		return attachment;
	}

	private MessageInfo copyMessageInfo(MessageInfo info) {
		if (info == null)
			return null;
		MessageInfo newInfo = MessageInfo.createOutgoing(info.address(), info.streamNumber());
		newInfo.payloadProtocolID(info.payloadProtocolID());
		newInfo.timeToLive(info.timeToLive());
		newInfo.unordered(info.isUnordered());
		newInfo.complete(info.isComplete());

		return newInfo;
	}
	
	private InetSocketAddress copyAddress(InetSocketAddress addr) {
		return new InetSocketAddress(addr.getAddress(), addr.getPort());
	}

	public SSLEngine sslEngine() {
		return this._sslEngine;
	}
	
}
