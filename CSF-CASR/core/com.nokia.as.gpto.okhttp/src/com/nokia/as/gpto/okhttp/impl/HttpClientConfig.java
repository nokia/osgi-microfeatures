package com.nokia.as.gpto.okhttp.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class HttpClientConfig {
	private static Logger LOG = Logger.getLogger(HttpClientConfig.class);

	public static final String METHOD_TYPE_KEY = "method";
	public static final String BODY_KEY = "body";
	public static final String URL_KEY = "url";
	public static final String CONTENT_TYPE_KEY = "contentType";
	public static final String KS_PATH_KEY = "clientKeyStorePath";
	public static final String KS_PASSWORD_KEY = "clientKeyStorePassword";
	public static final String USE_HTTP2_KEY = "http2PriorKnowledge";
	public static final String USE_HTTP2_FORCE_KEY = "forceHttp2";

	public static final String FROM_ADDRESS_KEY = "from";
	public static final String BODY_PATH_KEY = "bodyPath";
	public static final String PORT_KEY = "port";
	public static final String HEADERS_KEY = "headers";
	public static final String USE_PROXY_KEY = "useProxy";
	public static final String PROXY_HOST_KEY = "proxyHost";
	public static final String PROXY_PORT_KEY = "proxyPort";
	public static final String MAX_QUERY_KEY = "maxRequestBeforeStop";

	private String methodType;
	private String body;
	private URI uri;
	private boolean useHttp2PriorKnowledge;
	private boolean forceHttp2;
	private InetAddress to;
	private InetAddress from;
	private String contentType;
	private String ksPath;
	private String ksPassword;
	private Integer port;
	private Boolean connectionClose;
	private Boolean proxy;
	private Boolean https;
	private Map<String, String> headers;
	private Path bodyPath;
	private int maxQueryBeforeReset;
	private String proxyHost;
	private Integer  proxyPort;
	public HttpClientConfig(JSONObject props) throws IllegalArgumentException {
		methodType = props.optString(METHOD_TYPE_KEY, "GET");
		body = props.optString(BODY_KEY);

		String url = props.optString(URL_KEY);
		if (url == null) {
			throw new IllegalArgumentException("No URL provided");
		}

		try {
			uri = new URI(url);
		} catch (URISyntaxException e1) {
			throw new IllegalArgumentException(e1);
		}

		contentType = props.optString(CONTENT_TYPE_KEY, "");

		ksPath = props.optString(KS_PATH_KEY);
		ksPassword = props.optString(KS_PASSWORD_KEY);

		try {
			to = InetAddress.getByName(uri.getHost());
		} catch (UnknownHostException e1) {
			throw new IllegalArgumentException(e1);
		}

		String from0 = props.optString(FROM_ADDRESS_KEY, "0.0.0.0");

		try {
			from = InetAddress.getByName(from0);
		} catch (UnknownHostException e1) {
		}

		port = uri.getPort();
		if (port < 0) {
			port = 80;
		}

		JSONObject jsonHeaders = props.optJSONObject(HEADERS_KEY);
		this.headers = new HashMap<>();
		if (jsonHeaders != null) {
			for (String name : JSONObject.getNames(jsonHeaders)) {
				try {
					Object value = jsonHeaders.get(name);
					this.headers.put(name, value.toString());
				} catch (JSONException e) {
					continue;
				}
			}
		}
		
		String pathStr = props.optString(BODY_PATH_KEY);
		if (!pathStr.isEmpty() && body.isEmpty()) {
			
			bodyPath = Paths.get(pathStr);
			if (!Files.isReadable(bodyPath)) {
				throw new IllegalArgumentException("body file cannot be read");
			}
			try {
				body = String.join("", Files.readAllLines(bodyPath));
			} catch (IOException e) {
				throw new IllegalArgumentException("body file cannot be read");
			}
		}
		proxy = props.optBoolean(USE_PROXY_KEY, false);
		maxQueryBeforeReset = props.optInt(MAX_QUERY_KEY, -1);
		https = "https".equals(uri.getScheme());
		
		useHttp2PriorKnowledge = props.optBoolean(USE_HTTP2_KEY, false);
		forceHttp2 = props.optBoolean(USE_HTTP2_FORCE_KEY, false);
		proxyHost = props.optString(PROXY_HOST_KEY, null);
		proxyPort = props.optInt(PROXY_PORT_KEY);
	}

	public String getMethodType() {
		return methodType;
	}

	public String getBody() {
		return body;
	}

	public URI getURL() {
		return uri;
	}

	public String getContentType() {
		return contentType;
	}

	public String getKsPath() {
		return ksPath;
	}

	public InetAddress getToAddress() {
		return to;
	}

	public InetAddress getFromAddress() {
		return from;
	}

	public Integer getPort() {
		return port;
	}

	public Boolean getConnectionClose() {
		return connectionClose;
	}

	public Boolean getProxy() {
		return proxy;
	}

	public Boolean getHttps() {
		return https;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public int getMaxQueryBeforeReset() {
		return maxQueryBeforeReset;
	}
	
	public String getKsPassword() {
		return ksPassword;
	}
	
	public boolean isHttp2PriorKnowledge() {
		return useHttp2PriorKnowledge;
	}

	public String getProxyHost() {
		return proxyHost;
	}

	public Integer getProxyPort() {
		return proxyPort;
	}
	
	public boolean getForceHttp2() {
		return forceHttp2;
	}
	
	KeyStore readKeyStore() throws Exception {
	    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());

	    // get user password and file input stream
	    char[] password = getKsPassword().toCharArray();

	    java.io.FileInputStream fis = null;
	    try {
	        fis = new java.io.FileInputStream(getKsPath());
	        ks.load(fis, password);
	    } finally {
	        if (fis != null) {
	            fis.close();
	        }
	    }
	    return ks;
	}
}
