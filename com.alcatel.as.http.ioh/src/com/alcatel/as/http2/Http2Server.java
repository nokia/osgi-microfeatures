package com.alcatel.as.http2;

import org.osgi.service.component.annotations.*;
import org.apache.log4j.Logger;
import com.alcatel.as.ioh.server.*;
import java.util.concurrent.atomic.*;
import org.osgi.framework.BundleContext;
import alcatel.tess.hometop.gateways.reactor.*;
import java.util.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import java.nio.ByteBuffer;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.metering2.*;
import com.alcatel.as.http.parser.HttpMeters;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Http2Server{
    
    @FileDataProperty(title="Http2 Server",
		      fileData="defHttp2Server.txt",
		      required=true,
		      dynamic=true,
		      section="Server",
		      help="Describes the HTTP2 listening endpoints.")
    public final static String CONF_HTTP2_SERVERS = "http2.tcp.servers";
    
    protected static AtomicInteger ID = new AtomicInteger(1);    

    protected Logger _logger = Logger.getLogger("as.ioh.http2");
    
    protected BundleContext _osgi;
    protected Map<String, Http2Processor> _procs = new HashMap<> ();
    protected ServerFactory _serverF;
    protected MeteringService _meteringS;
    protected ConnectionFactory _connF;
    
    public Http2Server (){
    }

    @Reference
    public void setServerF (ServerFactory sf){ _serverF = sf;}
    public void unsetServerF (ServerFactory sf){}
    @Reference
    public void setMF (MeteringService ms){ _meteringS = ms;}
    public void unsetMF (MeteringService ms){}
    @Reference
    public void setConnectionF (ConnectionFactory cf){
	_connF = cf;
    }
    
    @Activate
    public synchronized void init (BundleContext ctx, Map<String, String> conf){
    	_osgi = ctx;
	for (Http2Processor proc : _procs.values ())
	    proc.register (_osgi);
	update (conf);
    }

    @Modified
    public synchronized void update (Map<String, String> conf){
	_serverF.newTcpServerConfig ("http2", conf.get (CONF_HTTP2_SERVERS));
    }
    
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void setHttp2RequestListener (Http2RequestListener listener, Map<String, String> properties){
	String id = properties.get ("id");
	if (_logger.isDebugEnabled ()) _logger.debug ("@Reference setHttp2ReqListener : "+id+" : "+listener);
	Http2Processor proc = new Http2Processor (this, id, listener, properties);
	_procs.put (id, proc);
	if (_osgi != null){
	    proc.register (_osgi);
	}
    }
    public synchronized void unsetHttp2RequestListener (Http2RequestListener listener, Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }
    
    public String toString () { return "Http2Server";}
    
    protected static class Http2Processor implements TcpServerProcessor, TcpChannelListener {

	protected Http2Server _server;
	protected Logger _logger;
	protected Map<String, String> _props;
	protected String _id, _toString;
	protected Http2RequestListener _reqListener;
	protected BundleContext _osgi;
	
	protected Http2Processor (Http2Server server, String id, Http2RequestListener listener, Map<String, String> props){
	    _server = server;
	    _id = id;
 	    _reqListener = listener;
	    _props = props;
	    _toString = "Http2Processor["+id+"]";
	    _logger = Logger.getLogger ("as.ioh.http2."+id);
	    _logger.info (this+" : created");
	}
	public String toString (){ return _toString;}
	protected Http2Processor register (BundleContext ctx){
	    _logger.info (this+" : register");
	    _osgi = ctx;
	    Dictionary props = new Hashtable ();
	    props.put ("processor.id", "http2."+_id);
	    ctx.registerService (TcpServerProcessor.class.getName (), this, props);
	    return this;
	}
	// called in any thread
	public void serverCreated (TcpServer server){
	    if (server.getProperties ().get (TcpServer.PROP_READ_TIMEOUT) == null)
		server.getProperties ().put (TcpServer.PROP_READ_TIMEOUT, ConnectionConfig.DEF_SERVER_READ_TIMEOUT);
	}
	// called in the Reactor
	public void serverOpened (TcpServer server){
	    Settings settings = new Settings ().load (server.getProperties ());
	    ConnectionConfig conf = new ConnectionConfig (settings, (Logger) server.getProperties().get("server.logger")).load (true, server.getProperties ());
	    HttpMeters meters = new HttpMeters ("http2."+_id+"."+server.getProperties ().get (TcpServer.PROP_SERVER_NAME),
						"HttpMeters for Http2Server "+server.getProperties ().get (TcpServer.PROP_SERVER_NAME),
						_server._meteringS
						).init (null);
	    meters.start(_osgi);
	    conf.meters (meters);
	    server.attach (conf);
	}
	public void serverFailed (TcpServer server, Object cause){}
	public void serverUpdated (TcpServer server){}
	public void serverClosed (TcpServer server){
	    ConnectionConfig conf = (ConnectionConfig) server.attachment ();
	    conf.meters ().stop ();
	}
	public void serverDestroyed (TcpServer server){
	}
	
	// called in Reactor
	public void connectionAccepted(TcpServer server,
				       TcpChannel acceptedChannel,
				       Map<String, Object> props){
	    acceptedChannel.setWriteBlockedPolicy(AsyncChannel.WriteBlockedPolicy.IGNORE);
	    ConnectionConfig conf = (ConnectionConfig) server.attachment ();
	    conf.meters ().getOpenChannelsMeter().inc(1);
	    Connection conn = _server._connF.newServerConnection (conf, acceptedChannel, _reqListener);
	    conn.init ();
	    acceptedChannel.attach (conn);
	    acceptedChannel.enableReading ();
	}
	
	public TcpChannelListener getChannelListener (TcpChannel cnx){
	    return this;
	}

	@Override
	public int messageReceived(TcpChannel channel, ByteBuffer data) {
	    Connection conn = (Connection) channel.attachment ();
	    conn.received (data);
	    return 0;
	}
	@Override
	public void receiveTimeout(TcpChannel channel) {
	    Connection conn = (Connection) channel.attachment ();
	    conn.receiveTimeout ();
	}

	@Override
	public void writeBlocked(TcpChannel channel) {
	}

	@Override
	public void writeUnblocked(TcpChannel channel) {
	}

	@Override
	public void connectionClosed(TcpChannel channel) {
	    Connection conn = (Connection) channel.attachment ();
	    conn.closed ();
	    HttpMeters meters = conn.meters ();
	    meters.getOpenChannelsMeter().inc(-1);
	    meters.getClosedChannelsMeter().inc(1);
	}
    }
}
