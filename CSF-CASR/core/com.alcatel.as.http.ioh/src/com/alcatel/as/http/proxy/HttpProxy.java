package com.alcatel.as.http.proxy;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.util.function.*;

import org.osgi.framework.BundleContext;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.management.*;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.reporter.api.CommandScopes;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.impl.conf.*;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.http2.*;
import com.alcatel.as.http2.client.Http2Client;
import com.alcatel.as.http2.client.api.*;
import com.alcatel_lucent.as.service.dns.*;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HttpProxy {
    
    public static final Logger LOGGER = Logger.getLogger ("as.ioh.http.px");

    public static final String PROP_CLIENT_WRITE_BUFFER_MAX = "px.client.write.buffer.max";
    public static final String PROP_SERVER_WRITE_BUFFER_MAX = "px.server.write.buffer.max";

    public static final String PROP_NEXT_PROXY_HOST = "px.next.host";
    public static final String PROP_NEXT_PROXY_PORT = "px.next.port";
    public static final String PROP_NEXT_PROXY_TUNNEL = "px.next.tunnel";

    public static final String PROP_SERVER_H2_POOL = "px.server.http2.pool";

    public static final String PROP_GATEWAY_H1_H2 = "px.gw.http1.http2";

    public static final String PROP_CLIENT_STREAM_WRITE_BUFFER_MAX = "px.client.stream.write.buffer.max";
    public static final String PROP_SERVER_STREAM_WRITE_BUFFER_MAX = "px.server.stream.write.buffer.max";

    public static final String PROP_CLIENT_HEADER_REMOVE = "px.client.header.remove";
    public static final String PROP_CLIENT_HEADER_ADD = "px.client.header.add";
    public static final String PROP_CLIENT_HEADER_ADD_FIRST = "px.client.header.add.first";
    public static final String PROP_CLIENT_HEADER_REPLACE = "px.client.header.replace";
    public static final String PROP_SERVER_HEADER_REMOVE = "px.server.header.remove";
    public static final String PROP_SERVER_HEADER_ADD = "px.server.header.add";
    public static final String PROP_SERVER_HEADER_ADD_FIRST = "px.server.header.add.first";
    public static final String PROP_SERVER_HEADER_REPLACE = "px.server.header.replace";
    
    public static final String PROP_CLIENT_HEADER_X_FOWARDED = "px.client.header.x-forwarded";
    
    private static final AtomicInteger SEED = new AtomicInteger (0);

    private PlatformExecutors _executors;
    private MeteringService _metering;
    private BundleContext _osgi;
    private Map<String, HttpProxyPluginFactory> _plugins = new HashMap<> ();
    private Map<String, Processor> _procs = new HashMap<> ();
    private ServerFactory _serverFactory;
    private DNSFactory _dnsFactory;
    private ConnectionFactory _h2ConnF;
    private HttpClientFactory _h2ClientF;
    
    @FileDataProperty(title="Http Proxy Endpoints",
		      fileData="defHttpPxServer.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the listening endpoints.")
		      public final static String CONF_TCP_SERVERS = "px.tcp.servers";
    @Reference
    public void setServerFactory (ServerFactory factory) throws Exception {
	LOGGER.info ("@Reference setServerFactory");
	_serverFactory = factory;
    }
    @Reference
    public void setDNSFactory (DNSFactory factory) throws Exception {
	LOGGER.info ("@Reference setDNSFactory");
	_dnsFactory = factory;
    }
    @Reference()
    public void setExecutors(PlatformExecutors executors){
	LOGGER.info ("@Reference setExecutors");
	_executors = executors;
    }
    @Reference()
    public void setMetering(MeteringService metering){
	LOGGER.info ("@Reference setMetering");
	_metering = metering;
    }
    @Reference()
    public void setH2Factory(ConnectionFactory cf){
	LOGGER.info ("@Reference setH2ConnectionFactory");
	_h2ConnF = cf;
    }
    @Reference()
    public void setH2ClientFactory(HttpClientFactory cf){
	LOGGER.info ("@Reference setH2ClientFactory");
	_h2ClientF = cf;
    }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setHttpProxyPluginFactory (HttpProxyPluginFactory pluginF, Map<String, String> properties){
	String id = properties.get ("plugin.id");
	LOGGER.info ("@Reference setHttpProxyPluginFactory : "+id+" : "+pluginF);
	_plugins.put (id, pluginF);
	initProcs ();
    }
    public void unsetHttpProxyPluginFactory (HttpProxyPluginFactory pluginF, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    public PlatformExecutors getPlatformExecutors (){ return _executors;}
    
    
    @Activate
    public synchronized void activate (BundleContext ctx, Map<String, String> conf) throws Exception {
	LOGGER.info ("@Activate");
	updated (conf);
	_osgi = ctx;
	initProcs ();
    }

    @Modified
    public void updated (Map<String, String> conf) throws Exception {
	LOGGER.info ("Configuration : "+conf);
	_serverFactory.newTcpServerConfig ("px", conf.get (CONF_TCP_SERVERS));
    }

    @Deactivate
    public void deactivate (){
	LOGGER.warn ("@Deactivate");
    }

    private void initProcs (){
	if (_osgi == null) return;
	for (String pid: _plugins.keySet ()){
	    String id = "ioh.http.px."+pid;
	    if (_procs.get (id) != null) continue;
	    LOGGER.info ("Registering processor : "+id);
	    Processor proc = new Processor (this, id, _plugins.get (pid));
	    _procs.put (id, proc);
	    proc.register ();
	}
    }
    
    protected static class Processor implements TcpServerProcessor {
	
	protected Logger _logger;

	private HttpProxy _px;
	private HttpProxyPluginFactory _pluginF;
	private String _id, _toString;
	
	public Processor (HttpProxy px, String id, HttpProxyPluginFactory pluginF){
	    _px = px;
	    _id = id;
	    _pluginF = pluginF;
	    _logger = Logger.getLogger ("as."+id);
	    _toString = "HttpProxyProcessor["+_id+"]";
	}

	public String toString (){ return _toString;}
	
	protected void register (){
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", _id);
	    _px._osgi.registerService (TcpServerProcessor.class.getName (), this, props);
	}

	protected PlatformExecutors getPlatformExecutors (){ return _px._executors;}
	protected DNSFactory getDNSFactory (){ return _px._dnsFactory;}

	protected Connection newHttp2Connection (ConnectionConfig conf, TcpChannel channel, Http2RequestListener reqListener){
	    return _px._h2ConnF.newServerConnection (conf, channel, reqListener);
	}
	protected HttpClientFactory h2ClientFactory (){
	    return _px._h2ClientF;
	}
	
	/**********************************************
	 *           TcpServer open/update/close      *
	 **********************************************/
    
	public void serverCreated (TcpServer server){
	    _logger.info ("serverCreated : "+server);
	    server.getProperties ().put (TcpServer.PROP_READ_BUFFER_DIRECT, true);
	    String name = (String) server.getProperties ().get (Server.PROP_SERVER_NAME);
	    Meters meters = new Meters ("as.ioh.http.px."+name, "HttpProxy for server : "+name, _px._metering);
	    server.getProperties ().put (SimpleMonitorable.class.toString (), meters);	    
	    meters.init ();
	    meters.start (_px._osgi);
	}
	public void serverOpened (TcpServer server){
	    _logger.info ("serverStarted : "+server);
	    setConf (server);
	}
	public void serverFailed (TcpServer server, Object cause){
	    _logger.trace ("serverFailed : "+server);
	}
	public void serverUpdated (TcpServer server){
	    _logger.info ("serverUpdated : "+server);
	    setConf (server);
	}
	public void serverClosed (TcpServer server){
	    _logger.info ("serverClosed : "+server);
	    ProxyConf conf = getProxyConf (server.getProperties ());
	    if (conf._h2Clients != null)
		for (HttpClient client: conf._h2Clients) client.close ();
	}
	public void serverDestroyed (TcpServer server){
	    _logger.info ("serverDestroyed : "+server);
	    getMeters (server).stop ();
	}
	public Meters getMeters (TcpServer server){
	    return (Meters) server.getProperties ().get (SimpleMonitorable.class.toString ());
	}
	private void setConf (TcpServer server){
	    Map<String, Object> props = server.getProperties ();
	    ProxyConf conf = new ProxyConf ();
	    conf._serverMaxSendBuffer = Property.getIntProperty (PROP_SERVER_WRITE_BUFFER_MAX, props, 10000000, true); // 10Meg
	    conf._clientMaxSendBuffer = Property.getIntProperty (PROP_CLIENT_WRITE_BUFFER_MAX, props, 10000000, true); // 10Meg
	    conf._connectTimeout = (long) Property.getIntProperty (TcpClient.PROP_CONNECT_TIMEOUT, props, 3000, true);
	    Object o = Property.getProperty (TcpClient.PROP_CONNECT_FROM, props, null, true);
	    if (o != null){
		try{
		    if (o instanceof String){
			conf._connectFrom = InetAddress.getByName (o.toString ());
			_logger.info (TcpClient.PROP_CONNECT_FROM+" : "+conf._connectFrom);
		    } else {
			List<String> list = (List<String>) o;
			conf._connectFroms = list.stream ().map (s -> {
				try{return InetAddress.getByName (s);}
				catch(Exception e){ throw new RuntimeException (e);}
			    }).toArray ();
			if (_logger.isInfoEnabled ())
			    _logger.info (TcpClient.PROP_CONNECT_FROM+" : "+java.util.stream.Stream.of (conf._connectFroms).map(Object::toString).collect (java.util.stream.Collectors.joining (",")));
		    }
		}catch(Exception e){
		    _logger.error (props.get (Server.PROP_SERVER_NAME)+" : invalid connect.from : "+o);
		}
	    }
	    conf._pluginConfig = _pluginF.newPluginConfig (props);
	    conf._plugin = _pluginF.newPlugin (conf._pluginConfig);
	    try{
		Next next = new Next ();
		if (next.load ("http1", props)){
		    conf._proxy1 = next.buildSecurity ();
		    _logger.info (props.get (Server.PROP_SERVER_NAME)+" : use http1 proxy : "+conf._proxy1);
		}
		next = new Next ();
		if (next.load ("http2", props)){
		    conf._proxy2 = next;
		    _logger.info (props.get (Server.PROP_SERVER_NAME)+" : use http2 proxy : "+conf._proxy2);
		}
	    }catch(Exception e){
		_logger.error (server+" : invalid proxy configuration", e);
	    }
	    conf._logger = (Logger) props.get ("server.logger");

	    if (props.get (ConnectionConfig.PROP_CONN_WRITE_BUFFER) == null) // if not explicitly set, then use _clientMaxSendBuffer
		props.put (ConnectionConfig.PROP_CONN_WRITE_BUFFER, String.valueOf (conf._clientMaxSendBuffer));
	    if (props.get ("px.server."+ConnectionConfig.PROP_CONN_WRITE_BUFFER) == null) // if not explicitly set, then use _serverMaxSendBuffer
		props.put ("px.server."+ConnectionConfig.PROP_CONN_WRITE_BUFFER, String.valueOf (conf._serverMaxSendBuffer));
	    
	    // make sure we have a def idle timeout on server side : 1 min
	    if (props.get ("px.server."+ConnectionConfig.PROP_CONN_IDLE_TIMEOUT) == null)
		props.put ("px.server."+ConnectionConfig.PROP_CONN_IDLE_TIMEOUT, "60000");
	    
	    conf._h2Config = new ConnectionConfig (new Settings ().load (props), conf._logger).priorKnowledge (false).load (true, props);
	    conf._h2ClientConfig = new ConnectionConfig (new Settings ().load (props, "px.server."), conf._logger).load (true, props, "px.server.");

	    // if not explicitly set, max buffer per stream is set to connection max buffer
	    conf._serverStreamMaxSendBuffer = Property.getIntProperty (PROP_SERVER_STREAM_WRITE_BUFFER_MAX, props, conf._h2ClientConfig.writeBuffer (), true);
	    conf._clientStreamMaxSendBuffer = Property.getIntProperty (PROP_CLIENT_STREAM_WRITE_BUFFER_MAX, props, conf._h2Config.writeBuffer (), true);

	    conf._secureCiphers = Property.getStringListProperty ("px.server."+TcpServer.PROP_TCP_SECURE_CIPHER, props);
	    conf._secureProtocols = Property.getStringListProperty ("px.server."+TcpServer.PROP_TCP_SECURE_PROTOCOL, props);
	    conf._ksFile = (String) props.get ("px.server."+TcpServer.PROP_TCP_SECURE_KEYSTORE_FILE);
	    conf._ksPwd = (String) props.get ("px.server."+TcpServer.PROP_TCP_SECURE_KEYSTORE_PWD);
	    conf._ksType = (String) props.get ("px.server."+TcpServer.PROP_TCP_SECURE_KEYSTORE_TYPE);
	    conf._ksAlgo = (String) props.get ("px.server."+TcpServer.PROP_TCP_SECURE_KEYSTORE_ALGO);
	    conf._epIdAlgo = (String) props.get ("px.server."+TcpServer.PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO);

	    conf._h1h2Gateway = Property.getBooleanProperty (PROP_GATEWAY_H1_H2, props, false, false);
	    if (conf._h1h2Gateway) _logger.info (props.get (Server.PROP_SERVER_NAME)+" : enabled gateway from http1 to http2");
	    
	    int pool = Property.getIntProperty (PROP_SERVER_H2_POOL, props, 0, false);
	    if (pool > 0){
		conf._h2Clients = new HttpClient[pool];
		for (int k=0; k<pool; k++)
		    conf._h2Clients[k] = conf.newH2Client (_px._h2ClientF);
	    }

	    String xForwarded = (String) props.get (PROP_CLIENT_HEADER_X_FOWARDED);
	    if (xForwarded != null){
		switch (xForwarded.toLowerCase ()){
		case "remove": conf._xForwardedRemove = true; break;
		case "replace":
		    conf._xForwardedAdd = true;
		    conf._xForwardedRemove = true;
		    break;
		default :
		    _logger.error (server+" : invalid x-forwarded policy : "+xForwarded, new IllegalArgumentException (xForwarded));
		}
	    }
	    
	    loadHeaders (true, conf,
			 Property.getStringListProperty (PROP_CLIENT_HEADER_ADD, props),
			 Property.getStringListProperty (PROP_CLIENT_HEADER_ADD_FIRST, props),
			 Property.getStringListProperty (PROP_CLIENT_HEADER_REMOVE, props),
			 Property.getStringListProperty (PROP_CLIENT_HEADER_REPLACE, props));
	    loadHeaders (false, conf,
			 Property.getStringListProperty (PROP_SERVER_HEADER_ADD, props),
			 Property.getStringListProperty (PROP_SERVER_HEADER_ADD_FIRST, props),
			 Property.getStringListProperty (PROP_SERVER_HEADER_REMOVE, props),
			 Property.getStringListProperty (PROP_SERVER_HEADER_REPLACE, props));

	    props.put ("ProxyConf", conf);
	}

	public ProxyConf getProxyConf (Map props){
	    return (ProxyConf) props.get ("ProxyConf");
	}

	protected HttpClient acquireH2Client (ProxyConf conf){
	    HttpClient client = conf.acquireH2Client ();
	    if (client != null) return client;
	    return conf.newH2Client (_px._h2ClientF);
	}
	protected void releaseH2Client (ProxyConf conf, HttpClient client){
	    if (conf._h2Clients != null) return;
	    client.close ();
	}

	/**********************************************
	 *           connection mgmt                  *
	 **********************************************/

	// called in the server Q
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    HttpProxyPlugin plugin = (HttpProxyPlugin) server.getProperties ().get ("HttpProxyPlugin");	    
	    init (acceptedChannel, new ClientContext (this, acceptedChannel, plugin, getMeters (server), props));
	}
	private void init (AsyncChannel channel, ClientContext ctx){
	    channel.setWriteBlockedPolicy (AsyncChannel.WriteBlockedPolicy.IGNORE);
	    channel.attach (ctx);
	    ctx.start ();
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return (TcpChannelListener) cnx.attachment ();
	}

    }

    public static class ProxyConf {
	private static AtomicInteger SEED = new AtomicInteger (0);
	public Next _proxy1, _proxy2;
	public boolean _h1h2Gateway;
	public int _clientMaxSendBuffer, _serverMaxSendBuffer;
	public int _clientStreamMaxSendBuffer, _serverStreamMaxSendBuffer;
	public long _connectTimeout;
	public List<String> _secureCiphers;
	public List<String> _secureProtocols;
	public String _ksFile, _ksPwd, _ksType, _ksAlgo, _epIdAlgo;
	public InetAddress _connectFrom;
	public Object[] _connectFroms;
	public Map<String, HeaderHandler> _clientHeaders, _serverHeaders;
	public ArrayList<HeaderAdder> _clientHeadersAdders, _serverHeadersAdders;
	public Object _pluginConfig;
	public HttpProxyPlugin _plugin;
	public Logger _logger;
	public ConnectionConfig _h2Config;
	public ConnectionConfig _h2ClientConfig;
	public HttpClient[] _h2Clients;
	public boolean _xForwardedAdd, _xForwardedRemove;

	public void setFromAddress (Consumer<InetAddress> f){
	    if (_connectFrom != null){
		f.accept (_connectFrom);
		return;
	    }
	    if (_connectFroms != null) // round robin
		f.accept ((InetAddress) _connectFroms[(SEED.getAndIncrement () & 0xFF) % _connectFroms.length]);
	}
	
	private HttpClient newH2Client (HttpClientFactory cf){
	    HttpClient.Builder builder = cf.newHttpClientBuilder ()
		.setProperty ("client.logger", _logger)
		.setProperty ("http2.config", _h2ClientConfig.copy ()) // need to clone else writeExecutors are mixed up
		.connectTimeout (java.time.Duration.ofMillis (_connectTimeout))
		.initDelay (java.time.Duration.ofMillis (_connectTimeout+500L));
	    setFromAddress (from -> {builder.setProperty (Http2Client.PROP_TCP_CONNECT_SRC, from);});
	    if (_secureCiphers != null) builder.secureCipher (_secureCiphers);
	    if (_secureProtocols != null) builder.secureProtocols (_secureProtocols);
	    if (_ksFile != null) builder.secureKeystoreFile (_ksFile);
	    if (_ksPwd != null) builder.secureKeystorePwd (_ksPwd);
	    if (_ksType != null) builder.secureKeystoreType (_ksType);
	    if (_ksAlgo != null) builder.secureKeystoreAlgo (_ksAlgo);
	    if (_epIdAlgo != null) builder.secureEndpointIdentificationAlgo (_epIdAlgo);
	    if (_proxy2 != null){
		builder.proxy (_proxy2._address.getAddress ().getHostAddress (), _proxy2._address.getPort ());
		if (!_proxy2._proxy2Tunnel) builder.setSingleProxySocket ();
		if (_proxy2._secureCiphers != null) builder.secureProxyCipher (_proxy2._secureCiphers);
		if (_proxy2._secureProtocols != null) builder.secureProxyProtocols (_proxy2._secureProtocols);
		if (_proxy2._ksFile != null) builder.secureProxyKeystoreFile (_proxy2._ksFile);
		if (_proxy2._ksPwd != null) builder.secureProxyKeystorePwd (_proxy2._ksPwd);
		if (_proxy2._ksType != null) builder.secureProxyKeystoreType (_proxy2._ksType);
		if (_proxy2._ksAlgo != null) builder.secureProxyKeystoreAlgo (_proxy2._ksAlgo);
		if (_proxy2._epIdAlgo != null) builder.secureProxyEndpointIdentificationAlgo (_proxy2._epIdAlgo);
	    }
	    return builder.build ();
	}
	protected HttpClient acquireH2Client (){
	    if (_h2Clients == null) return null;
	    int i = SEED.getAndIncrement () & 0x7F_FF_FF_FF;
	    int len = _h2Clients.length;
	    return _h2Clients[i % len];
	}

	protected HeadersContext newClientHeadersContext (){
	    return _clientHeadersAdders != null ?
		new HeadersContext (_clientHeadersAdders) :
		null;
	}
	// name must already be lower-case
	protected boolean handleClientHeader (String name, Supplier<String> value, HeadersContext ctx){
	    if (_clientHeaders != null){
		HeaderHandler handler = _clientHeaders.get (name);
		if (handler != null) return handler.handle (name, value, ctx);
	    }
	    return true;
	}
	protected void endClientHeaders (java.util.function.BiConsumer<String, String> f, HeadersContext ctx){
	    if (ctx == null) return;
	    int n = _clientHeadersAdders.size ();
	    for (int i=0; i<n; i++){
		HeaderAdder adder = _clientHeadersAdders.get (i);
		String add = adder._value;
		String current = ctx._values[i];
		String value = adder._first ?
		    (current == null ? add : add+", "+current) :
		    (current == null ? add : current+", "+add) ;
		f.accept (adder._name, value);
	    }
	}
	
	protected HeadersContext newServerHeadersContext (){
	    return _serverHeadersAdders != null ?
		new HeadersContext (_serverHeadersAdders) :
		null;
	}
	// name must already be lower-case
	protected boolean handleServerHeader (String name, Supplier<String> value, HeadersContext ctx){
	    if (_serverHeaders != null){
		HeaderHandler handler = _serverHeaders.get (name);
		if (handler != null) return handler.handle (name, value, ctx);
	    }
	    return true;
	}
	protected void endServerHeaders (java.util.function.BiConsumer<String, String> f, HeadersContext ctx){
	    if (ctx == null) return;
	    int n = _serverHeadersAdders.size ();
	    for (int i=0; i<n; i++){
		HeaderAdder adder = _serverHeadersAdders.get (i);
		String add = adder._value;
		String current = ctx._values[i];
		String value = adder._first ?
		    (current == null ? add : add+", "+current) :
		    (current == null ? add : current+", "+add) ;
		f.accept (adder._name, value);
	    }
	}
    }

    private static final void loadHeaders (boolean client, ProxyConf conf, List<String> adds, List<String> addsFirst, List<String> removes, List<String> replaces){
	Map<String, HeaderHandler> map = null;
	ArrayList<HeaderAdder> adders = null;
	if (replaces != null && replaces.size () > 0){
	    if (adds == null) adds = new ArrayList<> ();
	    if (removes == null) removes = new ArrayList<> ();
	    for (String s : replaces){
		String[] ss = parseHeaderNameValue (s);
		adds.add (s);
		removes.add (ss[0]);
	    }
	}
	if (adds != null && adds.size () > 0){
	    for (String s : adds){
		String[] ss = parseHeaderNameValue (s);
		if (ss == null) continue;
		if (map == null) map = new HashMap<> ();
		if (adders == null) adders = new ArrayList<> ();	    
		HeaderAdder handler = new HeaderAdder (ss);
		map.put (handler._name, handler);
		adders.add (handler);
	    }
	}
	if (addsFirst != null && addsFirst.size () > 0){
	    for (String s : addsFirst){
		String[] ss = parseHeaderNameValue (s);
		if (ss == null) continue;
		if (map == null) map = new HashMap<> ();
		if (adders == null) adders = new ArrayList<> ();	    
		HeaderAdder handler = new HeaderAdder (ss).first ();
		map.put (handler._name, handler);
		adders.add (handler);
	    }
	}
	if (removes != null && removes.size () > 0){
	    if (map == null) map = new HashMap<> ();
	    for (String s : removes){
		map.put (s.toLowerCase (), HEADER_REMOVER);
	    }
	}
	if (adders != null) adders.trimToSize ();
	if (client){
	    conf._clientHeaders = map;
	    conf._clientHeadersAdders = adders;
	} else {
	    conf._serverHeaders = map;
	    conf._serverHeadersAdders = adders;
	}
    }
    private static String[] parseHeaderNameValue (String s){
	int i = s.indexOf (':');
	if (i == -1 || i == 0 || i == s.length ()-1){
	    LOGGER.error ("Invalid Header property : "+s, new IllegalArgumentException (s));
	    return null;
	}
	String name = s.substring (0, i).trim ().toLowerCase ();
	String value = s.substring (i+1).trim ();
	return new String[]{name, value};
    }
    protected static interface HeaderHandler {
	public boolean handle (String name, Supplier<String> value, HeadersContext ctx);
    }
    protected static HeaderHandler HEADER_REMOVER = new HeaderHandler (){
	    public boolean handle (String name, Supplier<String> value, HeadersContext ctx){ return false;}
	};
    protected static class HeaderAdder implements HeaderHandler {
	String _name, _value;
	boolean _first;
	protected HeaderAdder (String[] ss){ this (ss[0], ss[1]);}
	protected HeaderAdder (String name, String value){ _name = name; _value = value;}
	protected HeaderAdder first (){ _first = true; return this;}
	public boolean handle (String name, Supplier<String> value, HeadersContext ctx){
	    int i = ctx._adders.indexOf (this);
	    String v = ctx._values[i];
	    if (v == null) v = value.get ();
	    else v = v+", "+value.get ();
	    ctx._values[i] = v;
	    return false;
	}
    }
    
    protected static class HeadersContext {
	protected List<HeaderAdder> _adders;
	protected String[] _values;
	protected HeadersContext (ArrayList<HeaderAdder> adders){
	    _adders = adders;
	    _values = new String[adders.size ()];
	}
    }

    protected static class Next {
	public InetSocketAddress _address;
	public boolean _proxy2Tunnel;
	public List<String> _secureCiphers;
	public List<String> _secureProtocols;
	public String _ksFile, _ksPwd, _ksType, _ksAlgo, _epIdAlgo;
	public Security _security;

	public Next (){
	}

	public String toString (){ return new StringBuilder ()
		.append ("Next[").append (_address)
		.append (", tunnel=").append (_proxy2Tunnel)
		.append (", secure=").append (_ksFile != null)
		.append ("]")
		.toString ();
	}

	public boolean load (String version, Map<String, Object> props) throws Exception {
	    version = "."+version;
	    String pxHost = (String) props.get (PROP_NEXT_PROXY_HOST+version);
	    if (pxHost == null) pxHost = (String) props.get (PROP_NEXT_PROXY_HOST);
	    if (pxHost == null) return false;
	    int pxPort = 3128;
	    String pxPortS = (String) props.get (PROP_NEXT_PROXY_PORT+version);
	    if (pxPortS == null) pxPortS = (String) props.get (PROP_NEXT_PROXY_PORT);
	    if (pxPortS != null) pxPort = Integer.parseInt (pxPortS);
	    _address = new InetSocketAddress (InetAddress.getByName (pxHost), pxPort);
	    _secureCiphers = Property.getStringListProperty ("px.next."+TcpServer.PROP_TCP_SECURE_CIPHER+version, props);
	    if (_secureCiphers == null) _secureCiphers = Property.getStringListProperty ("px.next."+TcpServer.PROP_TCP_SECURE_CIPHER, props);
	    _secureProtocols = Property.getStringListProperty ("px.next."+TcpServer.PROP_TCP_SECURE_PROTOCOL+version, props);
	    if (_secureProtocols == null) _secureProtocols = Property.getStringListProperty ("px.next."+TcpServer.PROP_TCP_SECURE_PROTOCOL, props);
	    _ksFile = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_FILE+version);
	    if (_ksFile == null) _ksFile = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_FILE);
	    _ksPwd = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_PWD+version);
	    if (_ksPwd == null) _ksPwd = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_PWD);
	    _ksType = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_TYPE+version);
	    if (_ksType == null) _ksType = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_TYPE);
	    _ksAlgo = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_ALGO+version);
	    if (_ksAlgo == null) _ksAlgo = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_KEYSTORE_ALGO);
	    _epIdAlgo = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO+version);
	    if (_epIdAlgo == null) _epIdAlgo = (String) props.get ("px.next."+TcpServer.PROP_TCP_SECURE_ENDPOINT_IDENTITY_ALGO);
	    _proxy2Tunnel = Property.getBooleanProperty (PROP_NEXT_PROXY_TUNNEL, props, false, false); // tunnel=false by default
	    return true;
	}

	public Next buildSecurity () throws Exception { // TODO : watch for changes
	    if (_ksFile == null) return this;
	    Security security = new Security();
	    if (_secureProtocols != null) security.addProtocol (_secureProtocols.toArray (new String[0]));
	    if (_secureCiphers != null) security.addCipher (_secureCiphers.toArray (new String[0]));
	    if (_ksFile != null) security.keyStore (new java.io.FileInputStream(_ksFile));
	    if (_ksPwd != null) security.keyStorePassword (_ksPwd);
	    if (_ksType != null) security.keyStoreType (_ksType);
	    if (_ksAlgo != null) security.keyStoreAlgorithm (_ksAlgo);
	    if (_epIdAlgo != null) security.endpointIdentificationAlgorithm (_epIdAlgo);
	    security.build (); // may throw an exception
	    _security = security;
	    return this;
	}
    }
}
