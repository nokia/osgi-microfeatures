package com.alcatel.as.ioh.engine.mesh;

import com.alcatel.as.ioh.engine.*;

import java.nio.ByteBuffer;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.*;
import org.osgi.service.component.annotations.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.tools.*;
import alcatel.tess.hometop.gateways.reactor.*;

import org.apache.log4j.Logger;

import com.nextenso.mux.mesh.*;
import com.nextenso.mux.*;
import com.nextenso.mux.util.*;
import com.alcatel.as.service.metering2.*;
import org.osgi.framework.*;

import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

public class MuxMeshImpl extends MuxHandler implements TcpServerProcessor, MuxMesh {

    protected Logger _logger;
    
    private final static int[] IOH_ID = new int[] {
	326
    };

    protected Map<String, String> _props;
    protected String _name, _iohName, _toString;
    protected MuxMeshListener _listener;
    protected Object _attachment;
    protected MuxMeshFactoryImpl _factory;
    protected SimpleMonitorable _monitorable;
    protected Meter _openInMeter, _openOutMeter, _closedMeter;
    protected MuxMeshIOHEngine _engine;
    
    public MuxMeshImpl (MuxMeshFactoryImpl factory, String name, MuxMeshListener listener, Map<String, String> properties){
	_factory = factory;
	_name = name;
	_props = properties;
	_iohName = "mux.mesh."+_name;
	_toString = "MuxMesh["+_name;
	if (isServer ()) _toString += "/server";
	if (isClient ()) _toString += "/client";
	_toString += "]";
	_listener = listener;
	_logger = Logger.getLogger("as.ioh.mux.mesh."+name);
	_engine = new MuxMeshIOHEngine (_iohName, _factory._services);
	_monitorable = new SimpleMonitorable (_iohName, _toString);
	Meter open = _monitorable.createIncrementalMeter (_factory._services.getMeteringService (), "agent.open", null);
	_openInMeter = _monitorable.createIncrementalMeter (_factory._services.getMeteringService (), "agent.open.in", open);
	_openOutMeter = _monitorable.createIncrementalMeter (_factory._services.getMeteringService (), "agent.open.out", open);
	_closedMeter = _monitorable.createIncrementalMeter (_factory._services.getMeteringService (), "agent.closed", null);
    }
    private boolean isServer (){ return "true".equalsIgnoreCase (_props.get ("server"));}
    private boolean isClient (){ return "true".equalsIgnoreCase (_props.get ("client"));}

    public String getName (){ return _name;}
    public void attach (Object o){ _attachment = o;}
    public <T> T attachment (){ return (T) _attachment;}
    public Map<String, String> getProperties (){ return _props;}
    public MuxMeshListener getListener (){ return _listener;}
    public Monitorable getMonitorable (){ return _monitorable;}

    @Override
    public String toString (){ return _toString;}
    
    public MuxMesh start (){
	Dictionary props = new Hashtable ();
	if (!isServer ()){ // if we are a server --> no reason to connect
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" : register MuxHandler");
	    props.put ("protocol", _iohName);
	    props.put ("hidden", "true");
	    props.put ("autoreporting", "false");
	    _factory._osgi.registerService (MuxHandler.class.getName (), this, props);
	}
	if (!isClient ()){ // if we are a client --> no listen
	    if (_logger.isDebugEnabled ()) _logger.debug (this+" : register TcpServerProcessor");
	    props = new Hashtable ();
	    props.put ("processor.id", _iohName);
	    props.put ("processor.advertize.id", "326");
	    props.put ("processor.advertize.name", _iohName);
	    props.put ("advertize.mux.factory.remote", "ioh");
	    _factory._osgi.registerService (TcpServerProcessor.class.getName (), this, props);
	}
	_monitorable.start (_factory._osgi);
	return this;
    }

    // ---------------- MuxHandler interface -----------------------------------------------------------
  
    /** Called by the CalloutAgent when it has seen our MuxHandler */
    @SuppressWarnings("unchecked")
    @Override
    public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
	// Don't forget to call the super.init method !
	super.init(appId, appName, appInstance, muxContext);
	
	// Configure our MUX handler
	getMuxConfiguration().put(CONF_STACK_ID, IOH_ID);
	getMuxConfiguration().put(CONF_STACK_NAME, _iohName);
	getMuxConfiguration().put(CONF_USE_NIO, true);
	getMuxConfiguration().put(CONF_THREAD_SAFE, true);
    }

    @Override
    public boolean accept(int stackAppId, java.lang.String stackName, java.lang.String stackHost, java.lang.String stackInstance){
	if (isClient ()) return true; // we know that we ARE a client --> must connect
	return stackInstance.compareTo (getInstanceName ()) > 0;
    }

    @Override
    public void muxOpened (MuxConnection connection) {
	if (_logger.isInfoEnabled ()) _logger.info (this+" : muxOpened - out : "+connection);
	_openOutMeter.inc (1);
	_listener.muxOpened (this, connection);
    }

    @Override
    public void muxClosed(MuxConnection connection) {
	if (_logger.isInfoEnabled ()) _logger.info (this+" : muxClosed - out : "+connection);
	_openOutMeter.inc (-1);
	_closedMeter.inc (1);
	_listener.muxClosed (this, connection);
    }

    @Override
    public void muxData(MuxConnection connection,
			MuxHeader header,
			java.nio.ByteBuffer buffer){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : muxData : "+buffer.remaining ());
	_listener.muxData (this, connection, header, buffer);
    }


    public int getMinorVersion (){ return 0;} // not used
    public int getMajorVersion (){ return 1;} // not used
    public int[] getCounters (){ return new int[0];} // not used

    
    // ---------------- Mux Server part -----------------------------------------------------------
    
    private class MuxMeshIOHEngine extends IOHEngine {
	private MuxMeshIOHEngine (String name, IOHServices services){
	    super (name, services);
	}
	@Override
	public IOHEngine init (TcpServer server){
	    server.getProperties ().put (PROP_TCP, "false");
	    server.getProperties ().put (PROP_UDP, "false");
	    server.getProperties ().put (PROP_SCTP, "false");
	    server.getProperties ().put (PROP_EXT_SERVER_MIN, "0");
	    return super.init (server);
	}
	@Override
	public void startMuxClient (MuxClient agent){
	    super.startMuxClient (agent);
	    MuxConnectionImpl cnx = new MuxConnectionImpl (agent);
	    agent.setContext (cnx);
	    if (MuxMeshImpl.this._logger.isInfoEnabled ()) MuxMeshImpl.this._logger.info (MuxMeshImpl.this+" : muxOpened - in : "+agent);
	    _openInMeter.inc (1);
	    _listener.muxOpened (MuxMeshImpl.this, cnx);
	}
	@Override
	public void resetMuxClient (MuxClient agent){
	    super.resetMuxClient (agent);
	    MuxConnectionImpl cnx = agent.getContext ();
	    if (cnx == null) return; // muxOpened not yet called
	    if (MuxMeshImpl.this._logger.isInfoEnabled ()) MuxMeshImpl.this._logger.info (MuxMeshImpl.this+" : muxClosed - in : "+agent);
	    _openInMeter.inc (-1);
	    _closedMeter.inc (1);
	    _listener.muxClosed (MuxMeshImpl.this, cnx);
	}
	@Override
	public boolean sendMuxData(MuxClient agent, MuxHeader header, boolean copy, ByteBuffer ... buf) {
	    MuxConnectionImpl cnx = agent.getContext ();
	    muxData (cnx, header, ByteBufferUtils.aggregate (false, false, buf));
	    return true;
	}
    }

    public void serverCreated (TcpServer server){
	String target = (String) server.getProperties ().get (_name+".group"); // it is an alias to "processor.advertize.group.target"
	if (target != null) server.getProperties ().put ("advertize.group.target", target);
	_engine.init (server);
	_engine.schedule (new Runnable (){
		public void run (){ _engine.start (_factory._osgi);}
	    });
	IOHLocalMuxFactory localFactory = new IOHLocalMuxFactory (_engine.name (), _engine);
	localFactory.register (_factory._osgi);
	server.getProperties ().put ("advertize.mux.factory.local", _engine.name ());
    }
    
    public void serverOpened (TcpServer server){
	_logger.warn (this+" : serverOpened");
    }
    public void serverFailed (TcpServer server, Object cause){
	_logger.error (this+" : serverFailed : "+cause);
    }
    public void serverUpdated (TcpServer server){}
    public void serverClosed (TcpServer server){
	_logger.warn (this+" : serverClosed");
    }
    public void serverDestroyed (TcpServer server){
    }
    
    // called in Reactor
    public void connectionAccepted(TcpServer server,
				   TcpChannel acceptedChannel,
				   Map<String, Object> props){
	if (_logger.isDebugEnabled ()) _logger.debug (this+" : connectionAccepted : "+acceptedChannel);
	acceptedChannel.attach (_engine.muxClientAccepted (acceptedChannel, props, false));
    }
	
    public TcpChannelListener getChannelListener (TcpChannel cnx){
	return (TcpChannelListener) cnx.attachment ();
    }


    private class MuxConnectionImpl extends AbstractMuxConnection {
	private MuxClient _client;
	private MuxConnectionImpl (MuxClient client){
	    super (client.getLogger ());
	    _client = client;
	}
	public String toString() {return _client.toString ();}
	public boolean sendMuxData(MuxHeader header, boolean copy, ByteBuffer ... bufs) {
	    ByteBuffer buff = null;
	    if (bufs != null && bufs.length != 0)
		buff = ByteBufferUtils.aggregate (copy, true, bufs);
	    _client.getMuxHandler ().muxData (_client, header, buff);
	    return true;
	}
	
	public void shutdown(Enum<?> reason, String info, Throwable err) {shutdown();}
	public void close(Enum<?> reason, String info, Throwable err) { close();}
	public void close (){_client.close ();}
    }
    
}
