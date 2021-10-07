package alcatel.tess.hometop.gateways.utils;

/**
 * common usefull constants.
 */
public interface Constants {
  
  public final static int LOG_ERR = 0;
  public final static int LOG_WARN = 1;
  public final static int LOG_INFO = 2;
  public final static int LOG_DEBUG = 3;
  public final static int LOG_CONNECT = 4;
  public final static int LOG_ACCESS = 5;
  public final static int LOG_HTTP = 6;
  public final static int LOG_COOKIES = 7;
  public final static int LOG_INSDU = 8;
  public final static int LOG_OUTSDU = 9;
  public final static int LOG_PXLET = 10;
  
  // jvm misc constants.
  public final static String jvmVersion = (String) java.security.AccessController
      .doPrivileged(new sun.security.action.GetPropertyAction("java.version"));
  
  public static String jvmUserAgent = (String) java.security.AccessController
      .doPrivileged(new sun.security.action.GetPropertyAction("http.agent", "Java" + jvmVersion));
  
  // http constants
  public final static String HTTP = "http://";
  public final static String HTTPS = "https://";
  public final static String TEXT = "text/";
  public final static String HTTP_11 = "HTTP/1.1";
  public final static String HTTP_10 = "HTTP/1.0";
  public final static String ACCEPT_CHARSET = "Accept-Charset";
  public final static String USER_AGENT = "User-Agent";
  public final static String CACHE_CONTROL = "Cache-Control";
  public final static String CONNECTION = "Connection";
  public final static String PROXY_CONNECTION = "Proxy-Connection";
  public final static String DATE = "Date";
  public final static String PRAGMA = "Pragma";
  public final static String TRAILER = "Trailer";
  public final static String TRANSFER_ENCODING = "Transfer-Encoding";
  public final static String UPGRADE = "Upgrade";
  public final static String VIA = "Via";
  public final static String FROM = "From";
  public final static String WARNING = "Warning";
  public final static String ACCEPT = "Accept";
  public final static String HOST = "Host";
  public final static String ACCEPT_ENCODING = "Accept-Encoding";
  public final static String ACCEPT_RANGES = "Accept-Ranges";
  public final static String ACCEPT_LANGUAGE = "Accept-Language";
  public final static String AGE = "Age";
  public final static String ETAG = "Etag";
  public final static String LOCATION = "Location";
  public final static String PROXY_AUTHENTICATE = "Proxy-Authenticate";
  public final static String PROXY_AUTHORIZATION = "Proxy-Authorization";
  public final static String AUTHORIZATION = "Authorization";
  public final static String RETRY_AFTER = "Retry-After";
  public final static String SERVER = "Server";
  public final static String VARY = "Vary";
  public final static String WWW_AUTHENTICATE = "Www-Authenticate";
  public final static String CONTENT_BASE = "Content-Base";
  public final static String ALLOW = "Allow";
  public final static String CONTENT_ENCODING = "Content-Encoding";
  public final static String CONTENT_LANGUAGE = "Content-Language";
  public final static String CONTENT_LENGTH = "Content-Length";
  public final static String CONTENT_LOCATION = "Content-Location";
  public final static String CONTENT_MD5 = "Content-Md5";
  public final static String CONTENT_RANGE = "Content-Range";
  public final static String CONTENT_TYPE = "Content-Type";
  public final static String EXPIRES = "Expires";
  public final static String LAST_MODIFIED = "Last-Modified";
  public final static String IF_MODIFIED_SINCE = "If-Modified-Since";
  public final static String IF_NONE_MATCH = "If-None-Match";
  public final static String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";
  public final static String IF_MATCH = "If-Match";
  public final static String IF_RANGE = "If-Range";
  public final static String REFERER = "Referer";
  public final static String MAX_FORWARDS = "Max-Forwards";
  public final static String COOKIE = "Cookie";
  public final static String KEEP_ALIVE = "Keep-Alive";
  public final static String SET_COOKIE = "Set-Cookie";
  public final static String CR = "\r";
  public final static String LF = "\n";
  public final static String CRLF = "\r\n";
  public final static String LFLF = "\n\n";
  public final static String CRLF_CRLF = "\r\n\r\n";
  public final static String ASCII = "ASCII7";
  public final static String SPACE = " ";
  public final static String SLASH = "/";
  public final static String UA = "User-Agent";
  public final static String DEF_UA = jvmUserAgent;
  public final static byte[] CRLF_B = "\r\n".getBytes();
  public final static byte[] CRLF_CRLF_B = "\r\n\r\n".getBytes();
  public final static byte[] _0_CRLF_CRLF_B = "0\r\n\r\n".getBytes();
  public final static String POST_URL_CTYPE = "application/x-www-form-urlencoded";
  
  // CCPP stuff (UAPROF)
  public final static String CCPP_URI = "\"http://www.w3.org/1999/06/24-CCPPexchange\"";
  public final static String PROFILE = "Profile";
  public final static String PROFILE_DIFF = "Profile-Diff";
  public final static String PROFILE_WARNING = "Profile-Warning";
  public final static String NS = "ns";// Namespace (parameter)
  public final static int NS_VALUE = 19; // ns value for Opt end-to-end
  public final static String OPT_HEADER_VALUE = CCPP_URI + "; " + NS + "=" + NS_VALUE;
  public final static String PROFILE_HEADER = NS_VALUE + "-" + PROFILE;
  public final static String PROFILE_DIFF_HEADER = NS_VALUE + "-" + PROFILE_DIFF; // must append the profileDiff number
  
  // HTTPex stuff (used by CCPP)
  public final static String MAN = "Man"; // Upper 'M'
  public final static String C_MAN = "C-Man";
  public final static String OPT = "Opt"; // Upper 'O'
  public final static String C_OPT = "C-Opt";
  public final static String EXT = "Ext";
  public final static String C_EXT = "C-Ext"; // Upper 'E'
  public final static String METHOD_MGET = "M-GET";
  public final static String METHOD_MPUT = "M-PUT";
  
  // http methods ...
  public final static String METHOD_OPT = "OPTIONS";
  public final static String METHOD_GET = "GET";
  public final static String METHOD_HEAD = "HEAD";
  public final static String METHOD_POST = "POST";
  public final static String METHOD_PUT = "PUT";
  public final static String METHOD_DELETE = "DELETE";
  public final static String METHOD_TRACE = "TRACE";
  public final static String METHOD_CONNECT = "CONNECT";
  
  // misc http parameters ...
  public final static String PARAM_Q = "q";
  public final static String PARAM_STAR = "*";
  public final static String PARAM_CHARSET = "charset";
  public final static String PARAM_LEVEL = "level";
  public final static String PARAM_TYPE = "type";
  public final static String PARAM_NAME = "name";
  public final static String PARAM_FILENAME = "filename";
  public final static String PARAM_DIFFERENCES = "differences";
  public final static String PARAM_PADDING = "padding";
  public final static String PARAM_START = "start";
  public final static String PARAM_START_INFO = "start-info";
  public final static String PARAM_ENC_GZIP = "gzip";
  public final static String PARAM_ENC_COMPRESS = "compress";
  public final static String PARAM_ENC_DEFLATE = "deflate";
  public final static String PARAM_NONE = "none";
  public final static String PARAM_BYTES = "bytes";
  public final static String PARAM_CLOSE = "close";
  public final static String PARAM_MULTIPART_RELATED = "multipart/related";
  public final static String PARAM_CHUNK = "chunk";
  public final static String PARAM_CHUNKED = "chunked";
  public final static String PARAM_COMMENT = "comment";
  public final static String PARAM_DOMAIN = "domain";
  public final static String PARAM_MAX_AGE = "max-age";
  public final static String PARAM_PATH = "path";
  public final static String PARAM_SECURE = "secure";
  public final static String PARAM_SEC = "sec";
  public final static String PARAM_MAC = "mac";
  public final static String PARAM_CREATION_DATE = "creation-date";
  public final static String PARAM_MODIFICATION_DATE = "modification-date";
  public final static String PARAM_READ_DATE = "read-date";
  public final static String PARAM_SIZE = "size";
  
  // cache control parameters ...
  public final static String PARAM_CC_PUBLIC = "public";
  public final static String PARAM_CC_PRIVATE = "private";
  public final static String PARAM_CC_NO_CACHE = "no-cache";
  public final static String PARAM_CC_NO_STORE = "no-store";
  public final static String PARAM_CC_NO_TRANSFORM = "no-transform";
  public final static String PARAM_CC_MUST_REVALIDATE = "must-revalidate";
  public final static String PARAM_CC_PROXY_REVALIDATE = "proxy-revalidate";
  public final static String PARAM_CC_MAX_AGE = "max-age";
  public final static String PARAM_CC_MAX_STALE = "max-stale";
  public final static String PARAM_CC_MIN_FRESH = "min-fresh";
  public final static String PARAM_CC_ONLY_IF_CACHED = "only-if-cached";
  
  // Authorize parameters
  public final static String PARAM_AUTHORIZATION_BASIC = "Basic";
  
  // Accept parameters
  public final static String MIME_VND_WAP_WML = "text/vnd.wap.wml";
  public final static String MIME_VND_WAP_WMLS = "text/vnd.wap.wmlscript";
  public final static String MIME_VND_WAP_WBMP = "image/vnd.wap.wbmp";
  public final static String MIME_BMP = "image/bmp";
  
  // charset parameters
  public final static String CHARSET_ISO8859_1 = "iso-8859-1";
  public final static String CHARSET_UTF_8 = "utf-8";
  public final static String CHARSET_US_ASCII = "us-ascii";
  
  // content types
  public final static String TEXT_PLAIN = "text/plain";
  public final static String TEXT_HTML = "text/html";
  public final static String TEXT_WML = "text/vnd.wap.wml";
  public final static String TEXT_WMLS = "text/vnd.wap.wmlscript";
  public final static String APPLICATION_WMLC = "application/vnd.wap.wmlc";
  public final static String APPLICATION_WMLSC = "application/vnd.wap.wmlscriptc";
  public final static String ALL_ALL = "*/*";
  public final static String APP_VND_SYNCML_XML = "application/vnd.syncml+xml";
  public final static String APP_VND_SYNCML_WBXML = "application/vnd.syncml+wbxml";
  
  // multipart keywords
  public final static String PARAM_BOUNDARY = "boundary";
  public final static String PART_START = "--";
  public final static String PART_SEPARATOR = "\r\n";
  
  // platform line separator
  public final String LINE_SEPARATOR = System.getProperty("line.separator");
  
  // platform line separators
  public final String LINE_SEPARATOR2 = LINE_SEPARATOR + LINE_SEPARATOR;
  
  // empty string constant
  public final static String EMPTY_STRING = "";
  
  public final static String STR_TRUE = "true";
  public final static String STR_FALSE = "false";
  
  // application id of nx sms proxy.
  public static final int APP_SMS_STACK = 262;
  
  // application ids of nx http proxy.
  public static final int APP_HTTP_STACK = 257;
  
  // common system configuration property names
  // GWUtils property names
  public final static String CNF_CLIENT_TIMERPERIOD = "system.timerPeriod";
  public final static String CNF_HTTP_CLIENTFACTORY = "system.httpClientFactory";
  public final static String CNF_CLID_HEADER = "system.clidHeaderName";
  public final static String CNF_APN_HEADER = "system.apnHeaderName";
  public final static String CNF_CLIP_HEADER = "system.clipHeaderName";
  public final static String CNF_CLIP_PORT_HEADER = "system.clipPortHeaderName";
  public final static String CNF_SSL_HEADER = "system.sslHeaderName";
  public final static String CNF_PROTOCOL_HEADER = "system.protocolHeaderName";
  public final static String CNF_PXLETCONTAINER = "pxlet.container";
  public final static String CNF_PXLETCONTEXTS = "pxlet.contexts";
  public final static String CNF_CONF_DIR = "system.confDir";
  public final static String CNF_SOFT_KLV_INTVL = "system.softTcpKeepAliveInterval";
  
  // Property names used by ldap proxylets.
  public final static String CNF_LDAP_URL = "ldap.ldapUrl";
  public final static String CNF_LDAP_INITCTX = "ldap.ldapInitCtx";
  public final static String CNF_LDAP_ROOTDN = "ldap.ldapRootDn";
  public final static String CNF_LDAP_ROOTPSWD = "ldap.ldapRootPass";
  
  // Environment property filename
  public final static String CNF_ENV_FILENAME = "environment.properties";
  
  // System user database id.
  public static final int DB_SYSUSER_ID = 27;
  public static final int DB_SYSUSER_MSISDN_ID = 135;
  
  //
  // Name of the http header indicating that a user has been registered in the
  // proxy platform.
  //
  public static final String SYS_USER_HEADER = "X-Hts_user";
}
