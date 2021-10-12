// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.reactor;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.CRLException;
import java.security.cert.CertPathParameters;
import java.security.cert.CertStore;
import java.security.cert.CertStoreParameters;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import javax.net.ssl.CertPathTrustManagerParameters;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Helper class used to store ssl/security parameters.
 */
public class Security {
	
	private volatile InputStream _keyStoreIn;
	private volatile String _keyStorePassword = "changeit";
	private volatile String _keyStoreType = "JKS";
	private volatile String _keyStoreAlgorithm = "SunX509";
	private volatile String _endpointIdentificationAlgorithm = null;
	private volatile String _endpointIdentity;
	private volatile KeyManager[] _keyManagers;
	private volatile TrustManager[] _trustManagers;
	private final List<String> _ciphers = new ArrayList<>();
	private final List<String> _enabledProtocols = new ArrayList<>();
	private final List<String> applicationProtocols = new ArrayList<>();
	private final List<String> _sni = new ArrayList<>(0);
	private final List<String> _sniMatchers = new ArrayList<>(0);
	private volatile boolean _authenticateClients;
	private boolean _protocolAdded = false;
	private boolean delayed = false;
	private volatile SSLContext _sslContext;

	private String[] DEFAULT_CIPHERS = {
		"TLS_RSA_WITH_AES_128_CBC_SHA",
		"TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
		"TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
		"TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
		"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
		"TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256",
		"TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
		"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
		"TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
		"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
	};

	/**
	 * During TLS handshaking, the client requests to negotiate a cipher suite from a list of cryptographic options that it supports, starting 
	 * with its first preference. Then, the server selects a single cipher suite from the list of cipher suites requested by the client. 
	 * Normally, the selection honors the client's preference. However, to mitigate the risks of using weak cipher suites, the server may select 
	 * cipher suites based on its own preference rather than the client's preference, by setting this boolean to true.
	 * see https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#cipher_suite_preference
	 */
	private boolean _useCipherSuitesOrder = false;
	
	// By default, and surprisingly, x500 trust manager does not perform any certification validation.
	// This class wraps any existing x509 trust manager in order to make sure that certificate is valid.
	private static class ValidateX500TrustManager implements X509TrustManager {		
		private final X509TrustManager _wrappedTM;
		
		ValidateX500TrustManager(X509TrustManager wrappedTM) {
			_wrappedTM = wrappedTM;
		}
		
		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			for (X509Certificate cert : chain) {
				cert.checkValidity();
			}
			_wrappedTM.checkClientTrusted(chain, authType);
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			for (X509Certificate cert : chain) {
				cert.checkValidity();
			}
			_wrappedTM.checkServerTrusted(chain, authType);
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return _wrappedTM.getAcceptedIssuers();
		}
	}

	/**
	 * Sets a key store where public/private key and trusted keys are stored.
	 * @param ks the keystore
	 * @param password the keystore password
	 * @return this instance
	 */
	public Security keyStore(InputStream ks) {
		_keyStoreIn = ks;
		return this;
	}
	
	/**
	 * Sets a key store where public/private key and trusted keys are stored.
	 * @param ks the keystore
	 * @param password the keystore password
	 * @return this instance
	 */
	public Security keyStorePassword(String password) {
		_keyStorePassword = password;
		return this;
	}
	
	/**
	 * Sets the keystore type ("JKS" by default)
	 * @param type The keystore type ("JKS" by default)
	 * @return this instance
	 */
	public Security keyStoreType(String type) {
		_keyStoreType = type;
		return this;
	}
		
	/**
	 * Sets the key store algorithm ("SunX509" by default).
	 * @param algorithm the key store algorithm ("SunX509" by default).
	 * @return this instance
	 */
	public Security keyStoreAlgorithm(String algorithm) {
		_keyStoreAlgorithm = algorithm;
		return this;
	}
		
	/**
	 * Sets the peer endpoint identity
	 */
	public Security endpointIdentity(String endpointIdentity) {
		_endpointIdentity = endpointIdentity;
		return this;
	}

	/**
	 * Get the peer endpoint identity Host
	 */
	public String endpointIdentity() {
		return _endpointIdentity;
	}

	/**
	 * Sets the endpoint identification Algorithm.
	 * @param algorithm the key store algorithm
	 * @return this instance
	 */
	public Security endpointIdentificationAlgorithm(String algorithm) {
		_endpointIdentificationAlgorithm = algorithm;
		return this;
	}
	
	/**
	 * Sets the endpoint identification Algorithm.
	 * @param algorithm the key store algorithm
	 * @return this instance
	 */
	public String endpointIdentificationAlgorithm() {
		return _endpointIdentificationAlgorithm;
	}

	/**
	 * Sets the ssl context protocol ("TLSv1.1" by default). 
	 * @param protocol the ssl context protocol ("TLSv1.1" by default). 
	 * @return this instance
	 */
	public Security addProtocol(String... protocols) {
		if (! _protocolAdded) {
			_enabledProtocols.clear();
			_protocolAdded = true;			
		}		
		for (String protocol : protocols) {
			_enabledProtocols.add(protocol);
		}
		return this;
	}
	
	/**
	 * Sets the application protocols (for ALPN for example)
	 */
	public Security addApplicationProtocols(String... protocols) {
		for(String protocol : protocols) {
			applicationProtocols.add(protocol);
		}
		return this;
	}
		
	/**
	 * Sets the cipher suites. The list can contain some regex that are used to match some ciphers which are available by default.
	 * @param ciphers the cipher suites.
	 * @return this instance
	 */
	public Security addCipher(String ... ciphers) {
		for (String cipher : ciphers) {
			_ciphers.add(cipher);
		}
		return this;
	}
	
	/**
	 * Sets whether the local cipher suites preference should be honored. false by default.
	 * see https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html#cipher_suite_preference
	 */
	public Security useCipherSuitesOrder(boolean honorOrder) {
		_useCipherSuitesOrder = honorOrder;
		return this;
	}
	
	/**
	 * Returns whether the local cipher suites preference should be honored.
	 */
	public boolean useCipherSuitesOrder() {
		return _useCipherSuitesOrder;
	}
		
	/**
	 * Sets whether or not the server must authenticate clients. Only used for servers, not clients.
	 * @param authenticateClients Sets whether or not the server must authenticate clients
	 * @return this instance
	 */
	public Security authenticateClients(boolean authenticateClients) {
		_authenticateClients = authenticateClients;
		return this;
	}
	
	/**
	 * Sets the key manager (only to used if you don't provide a key store). 
	 * @param keyManagers the key manager (only to used if you don't provide a key store). 
	 * @return this instance
	 */
	public Security keyManager(KeyManager [] keyManagers) {
		_keyManagers = keyManagers;
		return this;
	}

	/**
	 * Sets the trust manager (only to used if you don't provide a key store). 
	 * @param keyManagers the trust manager (only to used if you don't provide a key store). 
	 * @return this instance
	 */
	public Security trustManager(TrustManager [] trustManagers) {
		_trustManagers = trustManagers;
		return this;
	}

	public Security delayed() {
		this.delayed = true;
		return this;
	}
	
	/**
	 * Adds the TLS server name indication extension (SNI). This method is meant to be used in client mode.
	 */
	public Security setSNI(String ... sni) {
		_sni.clear();
		Stream.of(sni).forEach(_sni::add);
		return this;
	}
	
	/**
	 * Adds the TLS server name indication extension (SNI). This method is meant to be used in client mode.
	 */
	public Security setSNIMatcher(String ... sniMatcher) {
		_sniMatchers.clear();
		Stream.of(sniMatcher).forEach(_sniMatchers::add);
		return this;
	}
	
	/**
	 * Gets the configured Server Name Indications names
	 */
	public List<String> getSNI() {
		return _sni;
	}
	
	/**
	 * Gets the configured Server Name Indications matchers
	 */
	public List<String> getSNIMatchers() {
		return _sniMatchers;
	}
	
	/**
	 * Build the final Security object. Call this method after all setter methods.
	 * @return this instance
	 * @throws Exception on any errors
	 */
	public Security build() throws Exception {				
		if (_keyManagers == null && _keyStoreIn != null && _keyStorePassword != null) {
			
			// Load the key store (which is assumed to also contains trust certifacations)
			
	    	char[] passphrase = _keyStorePassword.toCharArray();
	    	KeyStore ks = KeyStore.getInstance(_keyStoreType);
	    	try (InputStream in = _keyStoreIn) {
	    		ks.load(in, passphrase);
	    	}
	    	
	    	// initialize the key managers
	    	
	    	KeyManagerFactory kmf = KeyManagerFactory.getInstance(_keyStoreAlgorithm);
	    	kmf.init(ks, passphrase);
	    	_keyManagers = kmf.getKeyManagers();

	    	// if user does not provide any trust managers, create our own
	    	if (_trustManagers == null) {
	    		TrustManagerFactory tmf = TrustManagerFactory.getInstance(_keyStoreAlgorithm);
	    		
	    		// possibly setup a Certificate Revocation List trust manager. 
	    		String crlPath = System.getProperty("reactor.crl.path");
	    		if (crlPath == null) {
	    			// no CRL to create, use default one
		    		tmf.init(ks);
	    			_trustManagers = tmf.getTrustManagers();
	    		} else {
	    			// create trust managers suporting CRL.
	    			
	    			if (! "PKIX".equalsIgnoreCase(_keyStoreAlgorithm)) {
	    				throw new CRLException("CRLs not supported for algorithm: " + _keyStoreAlgorithm);
	    			}

	    			// first, create a CertPathParameters
	    			PKIXBuilderParameters xparams = new PKIXBuilderParameters(ks, new X509CertSelector());
	    			
	    			// load the CRL from file.
	    			File crlFile = new File(crlPath);
	    			Collection<? extends CRL> crls = null;
	    			CertificateFactory cf = CertificateFactory.getInstance("X.509");
	    			try (InputStream is = new FileInputStream(crlFile)) {
	    				crls = cf.generateCRLs(is);
	    			} 

	    			// And Construct the ManagerFactoryParameters that can check CRL
	    			CertStoreParameters csp = new CollectionCertStoreParameters(crls);
	    			CertStore store = CertStore.getInstance("Collection", csp);
	    			xparams.addCertStore(store);
	    			xparams.setRevocationEnabled(true);
	    			String trustLength = System.getProperty("reactor.crl.maxlen");
	    			if (trustLength != null) {
	    				xparams.setMaxPathLength(Integer.parseInt(trustLength));
	    			}
	    			
	    			// Now we can create our trust manager with CRL support
	    			ManagerFactoryParameters mfp = new CertPathTrustManagerParameters(xparams);
	    			tmf.init(mfp);
	    			_trustManagers = tmf.getTrustManagers();
	    		}

	    		// add additional certification validity checker
	    		for (int i = 0; i < _trustManagers.length; i ++) {
	    			if (_trustManagers[i] instanceof X509TrustManager) { 
	    				_trustManagers[i] = new ValidateX500TrustManager((X509TrustManager) _trustManagers[i]);
	    			}
	    		}
	    	}
		}	    

	    return this;
	}
	
	/**
	 * Returns the cipher suites configured in this class.
	 * @return the cipher suites configured in this class
	 */
	public String[] getCipherSuites() {
		if(_ciphers.isEmpty()) return DEFAULT_CIPHERS;
		else return _ciphers.toArray(new String[_ciphers.size()]);
	}
	
	/**
	 * Returns the flag which tells whether or not the client must be authenticated.
	 * @return the flag which tells whether or not the client must be authenticated.
	 */
	public boolean authenticateClients() {
		return _authenticateClients;
	}
		
	/**
	 * Returns the enabled protocols.
	 * @deprecated use getEnabledProtocols
	 */
	public String[] getEnabledPrtocols() {
		return getEnabledProtocols();
	}	
	
	/**
	 * Sets the SSLContext to use. If null, the reactor implementation will create one, which will be 
	 * set in this class, using the setSSLContext(SSLContext ctx) method.
	 */
	public SSLContext getSSLContext() {
		return _sslContext; 
	}
	
	/**
	 * Sets the SSLContext to use. If this method is not called, then the reactor implementation will
	 * create one and will then set it to this Security instance using this method. 
	 * @param ctx the SSLContext to use
	 */
	public Security setSSLContext(SSLContext ctx) {
		_sslContext = ctx;
		return this;
	}
	
	/**
	 * Returns the enabled protocols.
	 */
	public String[] getEnabledProtocols() {
		return _enabledProtocols.toArray(new String[_enabledProtocols.size()]);
	}
	
	/**
	 * Returns the application protocols.
	 */
	public String[] getApplicationProtocols() {
		return applicationProtocols.toArray(new String[applicationProtocols.size()]);
	}
	
	public KeyManager[] getKeyManagers() {
		return _keyManagers;
	}
	
	public TrustManager[] getTrustManagers() {
		return _trustManagers;
	}

	public boolean isDelayed() {
		return this.delayed;
	}
	
	@Deprecated
	public Store getKeyStore() {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public Store getTrustStore() {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public class Store { 
		public final InputStream in;
		public final String pass;
		public final String type;
		public Store(InputStream i, String s1, String s2) {
			throw new UnsupportedOperationException();
		}
	}
	
	@Deprecated
	public Security trustStore(InputStream ts) {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public Security trustStorePassword(String password) {
		throw new UnsupportedOperationException();
	}
	
	@Deprecated
	public Security trustStoreType(String type) { 
		throw new UnsupportedOperationException();
	}
}
