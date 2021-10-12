// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent;

import static com.nextenso.http.agent.config.HeaderProperties.HEADER_APN;
import static com.nextenso.http.agent.config.HeaderProperties.HEADER_CLID;
import static com.nextenso.http.agent.config.HeaderProperties.HEADER_CLIP;
import static com.nextenso.http.agent.config.HeaderProperties.HEADER_CLIP_PORT;
import static com.nextenso.http.agent.config.HeaderProperties.HEADER_SSL;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpClient.Builder;
import com.alcatel.as.http2.client.api.HttpClient.Version;
import com.alcatel.as.http2.client.api.HttpClientFactory;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpRequest.BodyPublisher;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.http.agent.client.HttpConnection;
import com.nextenso.http.agent.engine.HttpProxyletContainer;
import com.nextenso.http.agent.engine.HttpProxyletEngine;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.proxylet.http.HttpCookie;

import alcatel.tess.hometop.gateways.utils.QuotedStringTokenizer;

public class Utils {
  /**
   * Flags used when encoding/decoding mux messages.
   */
  public static final int CLIENT_FLAGS = 0x00;          /* 0 000 0000 */
  public static final int PRE_FILTER_FLAGS = 0x20;      /* 0 010 0000 */
  public static final int POST_FILTER_FLAGS = 0x40;     /* 0 100 0000 */
  public static final int REDIRECT_FILTER_FLAGS = 0x60; /* 0 110 0000 */
  
  public static final int ACK_MASK = 0x80;              /* 1 000 0000 */
  public static final int FILTER_MASK = 0x70;           /* 0 111 0000 */
  public static final int NO_FILTER_MASK = 0x0F;        /* 0 000 1111 */
  
  /**
   * These flags will describe the kind of data contained
   * in the mux socket.
   */
  public static final int NONE = 0;                /* 0000 0000 */
  public static final int DATA = 1;                /* 0000 0001 */
  public static final int CLOSED = 2;              /* 0000 0010 */
  public static final int CONFIGURE = 3;           /* 0000 0011 */
  public static final int CONFIGURE_CONNECTION = 4;/* 0000 0100 */
  public static final int WENT_THROUGH = 5;        /* 0000 0101 */
  public static final int SESSION = 8;             /* 0000 1000 */
  public static final int WEBSOCKET = 10;          /* 0000 1010 */
  public static final int TUNNEL = 12;             /* 0000 1100 */
  
  /**
   * The keywords used to notify the http stack of the next hop for the coming request.
   */
  public static final String SET_NEXT_HOP = "set_next_hop:";
  public static final String NEXT_HOP_DIRECT = "direct";
  public static final String SET_NEXT_HOP_DIRECT = "set_next_hop:" + NEXT_HOP_DIRECT;
  
  /**
   * Possible messages when configuring the callout
   */
  public static final String req_filtering = "set_req_filtering:";
  public static final String resp_filtering = "set_resp_filtering:";
  public static final String session_timeout = "set_session_timeout:";
  public static final String set_filtering = "set_filtering:";
  public static final byte[] soft_kill = "soft_kill".getBytes();
  public static final MuxHeaderV0 headerConfigure = new MuxHeaderV0();  
  public static final Logger logger = Logger.getLogger("agent.http");
  public static final byte[] EMPTY = new byte[0];
  
  static {
    headerConfigure.set(0L, 0, CONFIGURE);
  }
  
  public final static String HTTP_DOMAIN = "as.agent.http";

  private final static Logger _logger = Logger.getLogger("agent.http");
  private String clidHeaderName, clipHeaderName, apnHeaderName, sslHeaderName, clipPortHeaderName;
  private final MuxConnectionManager connectionManager = new MuxConnectionManager();  
  private Hashtable<Long, Client> clients = new Hashtable<Long, Client>();
  private static volatile PlatformExecutors pfExecutors;
  private static volatile Executor tpoolExecutor;
  private PlatformExecutor httpExecutor;
  private volatile Agent _agent;
  private volatile HttpProxyletContainer _container;
  private volatile TimerService _timerService;
  private HttpProxyletEngine _engine;
  private volatile String _containerIndex;
  private volatile static Utils _defaultUtilsInstance;
  private static volatile HttpMeters monitorable;
  
  private volatile HttpClientFactory _http2ClientFactory;
  private HttpClient _http2Client = null;
  private int _h2Timeout = 0;
  private String _h2PxKeystorePath = null;
  private String _h2PxKeystorePwd  = null;
  private String _h2PxKeystoreType = null;  
  private String _h2PxKeystoreAlgo = null;
  private String _h2PxEndPtIdAlgo = null;
  private List<String> _h2PxCiphers = null;
  private boolean _h2PxTunneling = false;
  private String _h2PxPingDelay = null;
  private String _h2PxIdleTimeout = null;
  
  private String _h2KeystorePath = null;
  private String _h2KeystorePwd  = null;
  private String _h2KeystoreType = null;
  private String _h2KeystoreAlgo = null;
  private String _h2EndPtIdAlgo = null;
  private List<String> _h2Ciphers = null;
  private String _h2PingDelay = null;
  private String _h2IdleTimeout = null;
  
  public void setHTTP2ClientConfig(Dictionary conf) {
      this._h2Timeout = ConfigHelper.getInt(conf, AgentProperties.H2_REQUEST_TIMEOUT, 0);
      if(this._h2Timeout == 0) {
        this._h2Timeout = ConfigHelper.getInt(conf, AgentProperties.SOCKET_TIMEOUT, 10) * 1000;
      }
      if(this._h2Timeout == 0) {
    	  this._h2Timeout = 10_000;
      }
    this._h2PxKeystorePath = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_KEYSTORE_PATH, null, true);
	  this._h2PxKeystorePwd = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_KEYSTORE_PWD, null, true);
	  this._h2PxKeystoreType = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_KEYSTORE_TYPE, null, true);
	  this._h2PxKeystoreAlgo = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_KEYSTORE_ALGO, null, true);
	  this._h2PxEndPtIdAlgo = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_ENDPOINT_IDENTITY_ALGO, null, true);
	  this._h2PxIdleTimeout = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_IDLE_TIMEOUT, null, true);
	  this._h2PxPingDelay = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_PING_DELAY, null, true);
	  this._h2PxTunneling = ConfigHelper.getBoolean(conf, AgentProperties.H2_CLIENT_SINGLE_PROXY_TUNNELING, true);
	  
	  String ciphers = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_CIPHERS, null, true);
	  if(ciphers != null) {
		  String[] cipherArrays = ciphers.split(",");
		  _h2PxCiphers = Arrays.stream(cipherArrays).map(String::trim).collect(Collectors.toList());
	  }
	  
	  this._h2KeystorePath = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_KEYSTORE_PATH, null, true);
	  this._h2KeystorePwd = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_KEYSTORE_PWD, null, true);
	  this._h2KeystoreType = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_KEYSTORE_TYPE, null, true);
	  this._h2PxKeystoreAlgo = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_KEYSTORE_ALGO, null, true);
	  this._h2PxEndPtIdAlgo = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PROXY_ENDPOINT_IDENTITY_ALGO, null, true);
	  this._h2IdleTimeout = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_IDLE_TIMEOUT, null, true);
	  this._h2PingDelay = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_PING_DELAY, null, true);
	  ciphers = ConfigHelper.getString(conf, AgentProperties.H2_CLIENT_CIPHERS, null, true);
	  if(ciphers != null) {
		  String[] cipherArrays = ciphers.split(",");
		  _h2Ciphers = Arrays.stream(cipherArrays).map(String::trim).collect(Collectors.toList());
	  }
  }

  public void setContainer(HttpProxyletContainer container) {
    _container = container;
  }
  
  public HttpProxyletContainer getContainer() {
    return _container;
  }
  
  public String getContainerIndex() {
    return _containerIndex;
  }
  
  public void setContainerIndex(String containerIndex) {
    _containerIndex = containerIndex;  
    synchronized (Utils.class) {
      // store the Utils instance of the first agent being initialized.
      if (_defaultUtilsInstance == null) {
        _defaultUtilsInstance = this;
      }
    }
    synchronized (Utils.class) {
      // store the Utils instance of the first agent being initialized.
      if (_defaultUtilsInstance == null) {
        _defaultUtilsInstance = this;
      }
    }
  }
  
  public static Utils getDefaultUtils() {
    return _defaultUtilsInstance;
  }
  
  public Agent getAgent() {
    return _agent;
  }
  
  public void setAgent(Agent agent) {
    _agent = agent;    
  }

  public static void setPlatformExecutors(PlatformExecutors execs) {
    pfExecutors = execs;
    tpoolExecutor = execs.getIOThreadPoolExecutor();
  }
  
  public void setHttpExecutor(PlatformExecutor httpExecutor) {
    this.httpExecutor = httpExecutor;
  }
   
  public static HttpMeters getMonitorable() {
    return monitorable;
  }

  public static void setMonitorable(HttpMeters monitorable) {
    Utils.monitorable = monitorable;
  }

  public void loadHeaderConfig(Dictionary<?, ?> systemConf) {
    if (systemConf.get(HEADER_CLID) != null) {
      String name = ConfigHelper.getString(systemConf, HEADER_CLID);
      _logger.info("CLID Header: " + name);
      this.clidHeaderName = name;
    }

    if (systemConf.get(HEADER_CLIP) != null) {
      String name = ConfigHelper.getString(systemConf, HEADER_CLIP);
      _logger.info("CLIP Header: " + name);
      this.clipHeaderName = name;
    }
    
    if (systemConf.get(HEADER_CLIP_PORT) != null) {
      String name = ConfigHelper.getString(systemConf, HEADER_CLIP_PORT);
      _logger.info("CLIP-Port Header: " + name);
      this.clipPortHeaderName = name;
    }
    
    if (systemConf.get(HEADER_APN) != null) {
      String name = ConfigHelper.getString(systemConf, HEADER_APN);
      _logger.info("APN Header: " + name);
      this.apnHeaderName = name;
    }
    
    if (systemConf.get(HEADER_SSL) != null) {
      String name = ConfigHelper.getString(systemConf, HEADER_SSL);
      _logger.info("SSL Header: " + name);
      this.sslHeaderName = name;
    }
  }
  
  public HttpProxyletEngine getEngine() {
    return _engine;
  }
  
  public void setEngine(HttpProxyletEngine engine) {
    _engine = engine;
  }
  
  public static PlatformExecutors getPlatformExecutors() {
    return pfExecutors;
  }
  
  public static Executor getThreadPool() {
    return tpoolExecutor;
  }
  
  public MuxConnectionManager getConnectionManager() {
    return connectionManager;
  }
  
  public String getApnHeaderName() {
    return apnHeaderName;
  }
  
  public String getClidHeaderName() {
    return clidHeaderName;
  }
  
  public String getClipHeaderName() {
    return clipHeaderName;
  }
  
  public String getClipPortHeaderName() {
    return clipPortHeaderName;
  }
  
  public String getSslHeaderName() {
    return sslHeaderName;
  }
  
  public PlatformExecutor getHttpExecutor() {
    return httpExecutor;
  }
  
  public Hashtable<Long, Client> getClients() {
    return clients;
  }
  
  public HttpConnection getHttpConnection(MuxConnection connection) {
    return connection.attachment();
  }
  
  public HttpConnection getHttpConnection() {
    if (connectionManager == null) {
      return null;
    }
    MuxConnection connection = connectionManager.getRandomMuxConnection();
    if (connection == null) {
      return null;
    }
    return getHttpConnection(connection);
  }
  
  /**
   * Retrieve a header parameter, including its name and its value.
   */
  public static String getHeaderParam(String headerValue, String paramName) {
    if (headerValue == null || headerValue.indexOf(';') == -1) {
      return (null);
    }
    
    QuotedStringTokenizer tok = new QuotedStringTokenizer(headerValue, ";");
    
    for (int i = 0; tok.hasMoreTokens(); i++) {
      if (i == 0) {
        tok.nextToken();
        continue;
      }
      
      String param = tok.nextToken().trim();
      
      if (headerParamMatches(paramName, param)) {
        return (param);
      }
    }
    
    return (null);
  }
  
  /**
   * Retrieve a header parameter value, not including its name.
   */
  public static String getHeaderParamValue(String headerValue, String paramName) {
    String param = getHeaderParam(headerValue, paramName);
    
    if (param == null)
      return (null);
    
    int equal = param.indexOf('=');
    
    if (equal == -1)
      return null;
    
    return (param.substring(equal + 1));
  }
  
  /**
   * Set a given parameter in one http header.
   *
   * @param headerValue the header value.
   * @param paramName The name of the parameter to set in the the header.
   * @param paramValue The param value to add in the header.
   * @return the header value with the new parameter
   */
  public static String setHeaderParamValue(String headerValue, String paramName, String paramValue) {
    StringBuffer buf;
    
    if (headerValue.indexOf(';') == -1) {
      buf = new StringBuffer(headerValue);
      buf.append(';');
      buf.append(paramName);
      buf.append('=');
      buf.append(paramValue);
      return (buf.toString());
    }
    
    QuotedStringTokenizer tok = new QuotedStringTokenizer(headerValue, ";");
    buf = new StringBuffer();
    
    boolean found = false;
    
    for (int i = 0; tok.hasMoreTokens(); i++) {
      if (i == 0) {
        // this is the header value (without parameters)
        buf.append(tok.nextToken().trim());
        continue;
      }
      
      String param = tok.nextToken().trim();
      
      if (headerParamMatches(paramName, param)) {
        found = true;
        buf.append(';');
        buf.append(paramName);
        buf.append('=');
        buf.append(paramValue);
      } else {
        buf.append(';');
        buf.append(param);
      }
    }
    
    if (!found) {
      buf.append(';');
      buf.append(paramName);
      buf.append('=');
      buf.append(paramValue);
    }
    
    return (buf.toString());
  }
  
  /**
   * Test if a header param matches another header param.
   */
  private static boolean headerParamMatches(String headerParam, String otherHeaderParam) {
    int equal = otherHeaderParam.indexOf('=');
    if (equal == -1 && otherHeaderParam.equalsIgnoreCase(headerParam)) {
      return (true);
    } else if (otherHeaderParam.substring(0, equal).equalsIgnoreCase(headerParam)) {
      return (true);
    }
    
    return (false);
  }
  
  // Inner class used to parse an http response.
  
//  private static class ResponseHandler implements HttpResponseHandler {
//    ResponseHandler(Utils utils) {
//      req = HttpMessageManager.makeRequest(new HttpSessionFacade(null, false, utils), -1);
//    }
//    
//    public void setHttpProtocol(String p) {
//      req.getResponse().getProlog().setProtocol(p);
//    }
//    
//    public void setHttpResponseStatus(int s) {
//      req.getResponse().getProlog().setStatus(s);
//    }
//    
//    public void setHttpResponseReason(String r) {
//      req.getResponse().getProlog().setReason(r);
//    }
//    
//    public void addHttpHeader(String name, String val) {
//      req.getResponse().getHeaders().addHeader(name, val);
//    }
//    
//    public void addHttpHeader(HttpHeaderDescriptor desc, String val) {
//      req.getResponse().getHeaders().addHeader(desc.name(), val);
//    }
//    
//    public void addHttpCookie(HttpCookie cookie) {
//      req.getResponse().getHeaders().addCookie(cookie);
//    }
//    
//    public void addHttpBody(InputStream in, int size) throws IOException {
//      req.getResponse().getBody().appendContent(in, size);
//    }
//    
//    public HttpResponse getResponse() {
//      return (req.getResponse());
//    }
//    
//    private HttpRequest req;
//  }
  
  public static void copyTo(OutputStream out, ByteBuffer ... bufs) throws IOException {
    for (ByteBuffer buf : bufs) {
      int len = buf.remaining();
      if (buf.hasArray()) {
        out.write(buf.array(), buf.arrayOffset() + buf.position(), len);
        buf.position(buf.position()+len);
      } else {
        byte[] tmp = new byte[len];
        buf.get(tmp, buf.position(), len);
        out.write(tmp);
      }
    }
  }
  
  public static byte[] getBytes(String str, String encoding) {
    try {
      return str.getBytes(encoding);
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Can't convert java string \"" + str + "\"", e);
    }
  }

  public void setTimerService(TimerService timerService) {
    _timerService = timerService;
  }
  
  public TimerService getTimerService() {
    return _timerService;
  }
  
  private boolean isH2MuxMode;

	public void setHttp2ClientFactory(HttpClientFactory clientFactory, boolean muxMode) {
		if (clientFactory != null) {
		    if (this._http2ClientFactory == null) {
			this._http2ClientFactory = clientFactory;
			isH2MuxMode = muxMode;
			_logger.warn("Creating new HTTP2 Client - mux traffic:" + muxMode);
			Builder b = clientFactory.newHttpClientBuilder()
			    .setProperty("client.logger", Logger.getLogger("h2client"))
			    .setProperty("reactor.mux", muxMode)
			    .setNoDelay();

			if (_h2KeystorePath != null) {
			    b.secureKeystoreFile(_h2KeystorePath);
			}

			if (_h2KeystorePwd != null) {
			    b.secureKeystorePwd(_h2KeystorePwd);
			}

			if (_h2KeystoreType != null) {
			    b.secureKeystoreType(_h2KeystoreType);
			}

			if (_h2Ciphers != null) {
			    b.secureCipher(_h2Ciphers);
			}
			
			if(_h2KeystoreAlgo != null) {
			    b.secureKeystoreAlgo(_h2KeystoreAlgo);
			}
			
			if(_h2EndPtIdAlgo != null) {
			    b.secureEndpointIdentificationAlgo(_h2EndPtIdAlgo);
			}
			
			if(_h2IdleTimeout != null) {
			    b.setProperty("http2.connection.idle.timeout", _h2IdleTimeout);
			}
		  
			if(_h2PingDelay != null) {
			    b.setProperty("http2.connection.ping.delay", _h2PingDelay);
			}
			  

			_http2Client = b.build();
		    } 
		} else {
			this._http2ClientFactory = null;
			_http2Client = null;
		}
	}
  
  public HttpClientFactory getHttp2ClientFactory() {
    return _http2ClientFactory;
  }
  
  public HttpClient getHttp2Client() {
	  return _http2Client;
  }
  
  public HttpClient newProxiedHttp2Client(String proxyAddress, int proxyPort) {
	  Builder b =  _http2ClientFactory.newHttpClientBuilder()
      .setProperty("client.logger", Logger.getLogger("h2client"))
      .setProperty("reactor.mux", isH2MuxMode)
      .proxy(proxyAddress, proxyPort)
      .setNoDelay();
	  if(_h2PxTunneling) {
		  if(_h2PxKeystorePath != null) {
			  b.secureKeystoreFile(_h2PxKeystorePath);
		  }
		  
		  if(_h2PxKeystorePwd != null) {
			  b.secureKeystorePwd(_h2PxKeystorePwd);
		  }
		  
		  if(_h2PxKeystoreType != null) {
			  b.secureKeystoreType(_h2PxKeystoreType);
		  }
		  
		  if(_h2PxCiphers != null) {
			  b.secureCipher(_h2PxCiphers);
		  }
		  
		  if(_h2PxIdleTimeout != null) {
			  b.setProperty("http2.connection.idle.timeout", _h2PxIdleTimeout);
		  }
		  
		  if(_h2PxPingDelay != null) {
			  b.setProperty("http2.connection.ping.delay", _h2PxPingDelay);
		  }
		  
		  if (_h2PxKeystoreAlgo != null) {
        b.secureKeystoreAlgo(_h2PxKeystoreAlgo);
      }
      
      if (_h2PxEndPtIdAlgo != null) {
        b.secureEndpointIdentificationAlgo(_h2PxEndPtIdAlgo);
      }
			  
	  } else {
		  b.setSingleProxySocket();
			if (_h2PxKeystorePath != null) {
				b.secureProxyKeystoreFile(_h2PxKeystorePath);
			}
	
			if (_h2PxKeystorePwd != null) {
				b.secureProxyKeystorePwd(_h2PxKeystorePwd);
			}
	
			if (_h2PxKeystoreType != null) {
				b.secureProxyKeystoreType(_h2PxKeystoreType);
			}

			if (_h2PxKeystoreAlgo != null) {
				b.secureProxyKeystoreAlgo(_h2PxKeystoreAlgo);
			}
			
			if (_h2PxEndPtIdAlgo != null) {
				b.secureProxyEndpointIdentificationAlgo(_h2PxEndPtIdAlgo);
			}
	
			if (_h2PxCiphers != null) {
				b.secureProxyCipher(_h2PxCiphers);
			}
			
		  if(_h2PxIdleTimeout != null) {
			  b.setProperty("http2.connection.idle.timeout", _h2PxIdleTimeout);
		  }
		  
		  if(_h2PxPingDelay != null) {
			  b.setProperty("http2.connection.ping.delay", _h2PxPingDelay);
		  }
	  }
      return b.build();
  }
  
  public HttpRequest.Builder toHttp2Request(HttpRequestFacade req, BodyPublisher pub) {
    HttpRequest.Builder newReq = _http2ClientFactory.newHttpRequestBuilder(true);
  
    if(pub != null) {
    	newReq.method(req.getMethod(), pub);
    } else if(req.getBody() != null) {
	    pub = _http2ClientFactory.bodyPublishers().ofByteArray(req.getContent());
	    newReq.method(req.getMethod(), pub);
    } else {
    	newReq.method(req.getMethod(), null);
    }
    
    // add headers
    addHeaders(req, newReq);
    
    // add cookies, if any
    Enumeration<HttpCookie> cookies = req.getCookies();
    if (cookies.hasMoreElements()) {
    	addCookies(cookies, newReq);
    }
    
    Object timeout = req.getAttribute(com.nextenso.proxylet.http.HttpRequest.TIMEOUT_ATTR_KEY);
    long customTimeout = _h2Timeout;
    
    if(timeout != null) {
    	if(timeout instanceof Long) {
    		customTimeout = (Long) timeout;
    	} else if(timeout instanceof Duration) {
    		customTimeout = ((Duration)timeout).toMillis();
    	}
    }
    
    if(customTimeout > 0) {
    	newReq.timeout(Duration.ofMillis(customTimeout));
    }
    newReq.version(Version.HTTP_2);
    
    return newReq;
  }

  private void addHeaders(HttpRequestFacade req, HttpRequest.Builder newReq) {
	newReq.uri(URI.create(req.getURL().toString()));
    Enumeration<?> headers = req.getHeaderNames();
    while(headers.hasMoreElements()) {
      String header = (String) headers.nextElement();
      if(header.equalsIgnoreCase("host")) {
    	  continue;
      }
      
	  if(header.equalsIgnoreCase("content-length")) {
		  _logger.debug("ignoring content-length header");
		  continue;
	  }
	  
      try {
        newReq.header(header, req.getHeader(header));
      } catch(IllegalArgumentException e) {
        _logger.debug("illegal header " + header);
        continue;
      }
    }
  }
  
  private void addCookies(Enumeration<HttpCookie> cookies, HttpRequest.Builder newReq) {
	  StringBuilder buf = new StringBuilder();
	  boolean separator = false;
	  while (cookies.hasMoreElements()) {
		  HttpCookie cookie = (HttpCookie) cookies.nextElement();
		  if (separator)
			  buf.append("; ");
		  separator = true;
		  buf.append(cookie.getName()).append('=').append(cookie.getValue());
	  }
	  newReq.header("Cookie", buf.toString());
  }

public HttpRequest.Builder toHttp2Request(HttpRequestFacade req) {
    return toHttp2Request(req, null);
  }
}
