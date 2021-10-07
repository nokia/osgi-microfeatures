package com.nextenso.http.agent;

import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.MSelectProperty;
import com.alcatel_lucent.as.management.annotation.config.MonconfVisibility;
import com.alcatel_lucent.as.management.annotation.config.SelectProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.alcatel_lucent.as.management.annotation.config.Visibility;

@Config(name = "httpagent", section = "Http Agent parameters", monconfModule="CalloutAgent", monconfAgent="HttpAgent",
    rootSnmpName = "alcatel.srd.a5350.HttpAgent", rootOid = { 637, 71, 6, 1010 })
public interface AgentProperties {
  // prop values
  public static final String STACK_ANY = "*";
  public static final String YES = "yes";
  public static final String NO = "no";
  public static final String AUTO = "auto";
  public static final long SESSION_TIMEOUT_MINIMUM = 1000L;

  public static final String POLICY_NONE = "none";
  public static final String POLICY_CLIENT_IP = "client-ip";
  public static final String POLICY_COOKIE = "cookie";
  public static final String POLICY_HEADER = "header";
  public static final String COOKIE_JSESSIOND = "JSESSIONID";
  public static final String HEADER_SESSION = "X-Nx-Clid";

  
  @IntProperty(min = 0, max = 3600, title = "Session Timeout", oid = 600, snmpName = "SessionTimeout",
      required = true, dynamic = true, defval = 1200,
      help="The session timeout value in seconds.")
  public static String SESSION_TIMEOUT = "httpagent.session.tmout";

  @SelectProperty(range = { "Yes", "No", "Auto" }, title = "Filter Requests", oid = 601,
      snmpName = "FilterRequests", required = true, dynamic = false, defval = "Auto",
      help="Specifies if the httpStack should send the requests to the httpAgent. The special value <b>Auto</b> means that the httpAgent will choose by looking at the deployed proxylets.")
  public static String REQ_FILTERING = "httpagent.req.filtering";

  @SelectProperty(range = { "Yes", "No", "Auto" }, title = "Filter Responses", oid = 602,
      snmpName = "FilterResponses", required = true, dynamic = false, defval = "Auto",
      help="Specifies if the httpStack should send the responses to the httpAgent. The special value <b>Auto</b> means that the httpAgent will choose by looking at the deployed proxylets.")
  public static String RESP_FILTERING = "httpagent.resp.filtering";

  @SelectProperty(range = { "Yes", "No" }, title = "Filter HttpClient requests", oid = 603,
      snmpName = "FilterHttpClientRequests", required = true, dynamic = false, defval = "No")
  public static String CLIENT_FILTER = "httpagent.client.filtering";

  @BooleanProperty(title = "Buffer Requests", oid = 604, snmpName = "BufferRequests", required = true,
      dynamic = true, defval = true,
      help="Specifies if the httpAgent should always buffer incoming requests.")
  public static String REQ_BUFFERING = "httpagent.req.buffering";

  @BooleanProperty(title = "Buffer Responses", oid = 605, snmpName = "BufferResponses", required = true,
      dynamic = true, defval = false,
      help="Specifies if the httpAgent should always buffer incoming responses. This value should be set to <b>false</b> by default.")
  public static String RESP_BUFFERING = "httpagent.resp.buffering";

  @IntProperty(min = 0, max = 10000, title = "Client Sizing", oid = 606, snmpName = "ClientSizing",
      required = true, dynamic = true, defval = 100, visibility=Visibility.HIDDEN, 
      help="Specifies the expected number of simultaneous clients. It is used by the httpAgent to size internal pools and does not have to be accurate.")
  public static String CLIENT_SIZING = "httpagent.client.sizing";

  @BooleanProperty(title = "High Availability activated", oid = 607, snmpName = "HighAvailabilityActivated",
      required = true, dynamic = false, defval = false,
      help="Activates the High Availability mode. This mode may require a registry to be started at the level group.")
  public static String SESSION_HA = "httpagent.ha";

  @FileDataProperty(title = "Http Agent Proxylets", oid = 610, snmpName = "HttpProxyletContexts", required = false,
      dynamic = false, blueprintEditor = "/bpadminpxleteditor/index.html", fileData = "httpagent.proxylets.xml",
      help="<p>Proxylet XML Configuration. This configuration allows to bypass what is configuring from the Proxy Application Deployment GUI, and specify the exact list of proxylets in XML. <p> If this configuration is left empty, then the Proxy Application Deployment GUI is used for configuring the list of all active proxylets.")
  public static String PROXYLET_CONTEXTS = "httpagent.proxylets";

  @IntProperty(min = 0, max = 10000, title = "Socket Demux Timeout",
      required = false, dynamic = false, defval = 0, 
      help="Socket demux timeout in seconds.")
  public static String SOCKET_TIMEOUT = "httpagent.socket.timeout";
  
  @BooleanProperty(title = "Sip Convergence",
      required = false, dynamic = false, defval = false,
      help="Activate sip/http convergence mode.")
  public static String SIP_CONVERGENCE = "httpagent.sip.convergence";
  
  @MSelectProperty(
      section="Http Agent parameters/IOH Session policy",
      title="IOH Policy", oid=611, snmpName="HttpPolicy", required=true, dynamic=false, 
      range = { POLICY_NONE, POLICY_CLIENT_IP, POLICY_COOKIE, POLICY_HEADER },
      defval= POLICY_NONE,
      help="<p>The session policy used when the agent is connected to HttpIOH:</p>"
          + "<ul>"
          + "<li><b>none:</b> There is no way the session can be determined,</li>"
          + "<li><b>client-ip:</b> All the traffic coming from the same host belongs to the same session,</li>"
          + "<li><b>cookie:</b> The session is identified with a cookie,</li>"
          + "<li><b>header:</b> The session is identified thanks to a HTTP header.</li>"
          + "</ul>")
  public final static String SESSION_POLICY = "httpagent.session.policy";

  @StringProperty(
      section="Http Agent parameters/IOH Session policy",
      title = "Http Session Cookie Name", oid=612, snmpName = "HttpSessionCookieName", required = false,
      dynamic = false, defval = COOKIE_JSESSIOND, 
      help = "<p>Name of the session cookie (significant if policy=cookie)</p>")
  public static String SESSION_COOKIE_NAME = "httpagent.session.cookie.name";

  @StringProperty(
      section="Http Agent parameters/IOH Session policy",
      title = "Http Session Header Name", oid=613, snmpName = "HttpSessionHeaderName", required = false,
      dynamic = false, defval = HEADER_SESSION, 
      help = "<p>Name of the session header (significant if policy=header)</p>")
  public static String SESSION_HEADER_NAME = "httpagent.session.header.name";

  @StringProperty(
      section="Http Agent parameters/IOH Proxy",
      title = "Via Pseudonym", oid=614, snmpName = "HttpViaPseudonym", required = false,
      dynamic = false, defval = "", 
      help = "<p>Pseudonym inserted into the Via header instead of the real host</p>")
  public static String VIA_PSEUDONYM = "httpagent.via.pseudonym";  

  @FileDataProperty(
      section="Http Agent parameters/IOH Proxy",
      title = "Next Proxy Selection", oid = 615, snmpName = "HttpNextProxy", required = false,
      dynamic = false,  fileData = "next-proxy.txt",
      help = "<p>Rules to select the next proxy, for requests received on the proxy ports</p>",
      validation="com.nextenso.proxylet.admin.http.valid.NextProxyValidator")
  public static String NEXT_PROXY = "httpagent.next.proxy";

  @FileDataProperty(
      section="Http Agent parameters/IOH Proxy",
      title = "CONNECT Tunneling", oid = 618, snmpName = "HttpConnectTunneling", required = false,
      dynamic = false,  fileData = "connect-tunneling.txt",
      help = "<p>Rules to allow tunneling when using HTTP CONNECT method</p>",
      validation="com.nextenso.proxylet.admin.http.valid.ConnectTunnelingValidator")
  public static String CONNECT_TUNNELING = "httpagent.connect.tunneling";

  @FileDataProperty(
      section="Http Agent parameters/IOH Next Server",
      title = "Next Server Selection", oid = 616, snmpName = "HttpNextServer", required = false,
      dynamic = false,  fileData = "next-server.txt",
      help = "<p>Rules to select the next server, for requests received on the reverse proxy ports</p>",
      validation="com.nextenso.proxylet.admin.http.valid.NextServerValidator")
  public static String NEXT_SERVER = "httpagent.next.server";

  @BooleanProperty(
      section="Http Agent parameters/IOH Internals",
      title = "Enable Self Connection", oid = 617, snmpName = "HttpSelfConnection", required = false,
      dynamic = true, defval = false,
      help="<p>If true, the IOH can connect to itself</p>")
  public static String SELF_CONNECTION = "httpagent.self.connection";

  @IntProperty(min = 1, max = 50, title = "HTTP2 CLient Pool size (direct)",
      required = false, dynamic = false, defval = 1, 
      help="The size of the HTTP2 Client pool for direct requests.")
  public static String H2_CLIENT_POOL_SIZE = "httpagent.h2client.pool.size";
  
  @IntProperty(min = 1, max = 50, title = "HTTP2 CLient Pool size (proxied)",
      required = false, dynamic = false, defval = 1 , 
      help="The size of the HTTP2 Client pool for proxied requests. This pool is per-proxy destination.")
  public static String H2_PROXIED_CLIENT_POOL_SIZE = "httpagent.h2client.proxy.pool.size";

  @BooleanProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title="Send HTTP2 traffic through Mux", required=true, dynamic=false, 
	      defval= true,
	      help="<p>Change how the HTTP2 traffic will be handled by the HTTP2 Client</p>"
	          + "<ul>"
	          + "<li><b>true :</b> traffic will go throught the IOH </li>"
	          + "<li><b>false :</b> HTTP2 Client will open sockets directly from the agent,</li>"
	          + "</ul>")
  public final static String H2_TRAFFIC_MODE = "httpagent.h2.traffic.mux";
  
  @BooleanProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title="Enable tunneling when proxying in HTTP2", required=true, dynamic=false, 
	      defval= false,
	      help="Enable tunneling when proxying in HTTP2 Client")
  public final static String H2_CLIENT_SINGLE_PROXY_TUNNELING = "httpagent.h2client.proxy.tunneling";
  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Path", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore Path (Direct Mode)")
  public final static String H2_CLIENT_KEYSTORE_PATH = "httpagent.h2client.tcp.secure.keystore.file";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Password", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore Password (Direct mode)")
  public final static String H2_CLIENT_KEYSTORE_PWD = "httpagent.h2client.tcp.secure.keystore.pwd";
  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Type", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore Type (Direct Mode)")
  public final static String H2_CLIENT_KEYSTORE_TYPE = "httpagent.h2client.tcp.secure.keystore.type";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Algorithm", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore Algorithm (Direct Mode)")
  public final static String H2_CLIENT_KEYSTORE_ALGO = "httpagent.h2client.tcp.secure.keystore.algo";
  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Endpoint Identification Algorithm", required = false,
	      dynamic = false, defval = "",
	      help = "HTTP2 Client Endpoint Identification Algorithm (Direct Mode)")
  public final static String H2_CLIENT_ENDPOINT_IDENTITY_ALGO = "httpagent.h2client.tcp.secure.endpoint.identity.algo";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Ciphers", required = false,
	      dynamic = false, help = "HTTP2 Client Ciphers (Direct Mode)")
  public final static String H2_CLIENT_CIPHERS = "httpagent.h2client.tcp.secure.cipher";
  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Ping Delay", required = false,
	      dynamic = false, help = "HTTP2 Ping Delay (Direct Mode)")
  public final static String H2_CLIENT_PING_DELAY = "httpagent.h2client.http2.connection.ping.delay";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Idle Timeout", required = false,
	      dynamic = false, help = "HTTP2 Idle Timeout (Direct Mode)", defval = "60000") 
  public final static String H2_CLIENT_IDLE_TIMEOUT= "httpagent.h2client.http2.connection.idle.timeout";

  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Path", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore Path (Proxy Mode)")
  public final static String H2_CLIENT_PROXY_KEYSTORE_PATH = "httpagent.h2client.proxy.tcp.secure.keystore.file";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Password", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore Password (Proxy Mode)")
  public final static String H2_CLIENT_PROXY_KEYSTORE_PWD = "httpagent.h2client.proxy.tcp.secure.keystore.pwd";
  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Type", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore type (Proxy Mode)")
  public final static String H2_CLIENT_PROXY_KEYSTORE_TYPE = "httpagent.h2client.proxy.tcp.secure.keystore.type";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Keystore Algorithm", required = false,
	      dynamic = false, defval = "", 
	      help = "HTTP2 Client Keystore Algorithm (Proxy Mode)")
  public final static String H2_CLIENT_PROXY_KEYSTORE_ALGO = "httpagent.h2client.proxy.tcp.secure.keystore.algo";
  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Endpoint Identification Algorithm", required = false,
	      dynamic = false, defval = "",
	      help = "HTTP2 Client Endpoint Identification Algorithm (Proxy Mode)")
  public final static String H2_CLIENT_PROXY_ENDPOINT_IDENTITY_ALGO = "httpagent.h2client.proxy.tcp.secure.endpoint.identity.algo";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Client Ciphers", required = false,
	      dynamic = false, help = "HTTP2 Client Ciphers (Proxy Mode)")
  public final static String H2_CLIENT_PROXY_CIPHERS = "httpagent.h2client.proxy.tcp.secure.cipher";
  
  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Ping Delay.", required = false,
	      dynamic = false, help = "HTTP2 Ping Delay (Proxy Mode)")
  public final static String H2_CLIENT_PROXY_PING_DELAY = "httpagent.h2client.proxy.http2.connection.ping.delay";

  @StringProperty(
	      section="Http Agent parameters/HTTP2 Configuration",
	      title = "HTTP2 Idle Timeout", required = false,
	      dynamic = false, help = "HTTP2 Idle Timeout (Proxy Mode)", defval = "-1")
  public final static String H2_CLIENT_PROXY_IDLE_TIMEOUT= "httpagent.h2client.proxy.http2.connection.idle.timeout";
  
  @StringProperty(
      section="Http Agent parameters/IOH Internals",
      monconfVisibility=MonconfVisibility.PRIVATE, 
      title = "Http Stack instance", oid = 609, snmpName = "HttpStackInstance", required = true,
      dynamic = true, defval = "*",
      help="<p> The name of the Http Stack instances that the Http Agent will connect to. The special value <b>*</b> means that the Http Agent will connect to all the Http Stacks no matter the instance.</p>")
  public static String STACK_INSTANCE = "httpagent.stack";
  
  @BooleanProperty(title = "HttpURLConnection wrapping", required = true,
	      dynamic = false, defval = true,
	      help="Specifies if any HttpURLConnection must be wrapped over any available http IOH mux connections. By default the property" +
	           " is set to true, meaning all HttpURLConnection http requests will be forwarded to any http IOH mux connection." +
	           " In this case, the request will be sent out of the http IOH, not from the agent jvm. Setting this property to false means the http request" +
	           " will be sent out of the agent jvm directly, without using the http IOH.")
  public static String WRAP_HTTPURLCONNECTION = "httpagent.wrap.httpurlconnection";
  
  @IntProperty(min = 0, max = 3600, title = "Http2 request timeout",
	      required = true, dynamic = true, defval = 10000, 
	      help="Specified the timeout in milliseconds to use for HTTP2 requests.")
  public static String H2_REQUEST_TIMEOUT = "httpagent.h2client.req.timeout";
}
