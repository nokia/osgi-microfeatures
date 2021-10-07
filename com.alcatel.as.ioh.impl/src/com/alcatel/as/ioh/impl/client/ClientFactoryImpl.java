package com.alcatel.as.ioh.impl.client;

import java.util.*;
import java.net.*;
import java.nio.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.impl.conf.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;

import org.apache.log4j.Logger;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.service.concurrent.PlatformExecutors;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;
import com.alcatel.as.service.discovery.Advertisement;

import com.alcatel_lucent.as.management.annotation.config.*;

@Component(service={ClientFactory.class}, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ClientFactoryImpl implements ClientFactory{

    public static final Logger LOGGER = Logger.getLogger("as.ioh.client");

    private volatile Map<String, ClusterInstance> _tcpClusters = new ConcurrentHashMap<String, ClusterInstance> ();
    private volatile Map<String, ClusterInstance> _udpClusters = new ConcurrentHashMap<String, ClusterInstance> ();
    private ReactorProvider _reactorProvider;
    private PlatformExecutors _execs;
    private String _agent;
    private BundleContext _osgi;
    
    @FileDataProperty(title="Client",
		      fileData="defTcpClient.txt",
		      required=true,
		      dynamic=true,
		      section="Tcp",
		      help="Indicates the destinations.")
    public final static String CONF_TCP_CLIENT = "conf.client.tcp";
    
    @FileDataProperty(title="Client",
		      fileData="defUdpClient.txt",
		      required=true,
		      dynamic=true,
		      section="Udp",
		      help="Indicates the destinations.")
    public final static String CONF_UDP_CLIENT = "conf.client.udp";
    
    @Reference
    public void setReactorProvider (ReactorProvider provider){
	LOGGER.info ("@Reference setReactorProvider");
	_reactorProvider = provider;
    }
    
    @Reference
    public void setPlatformExecutors (PlatformExecutors execs){
	LOGGER.info ("@Reference setPlatformExecutors");
	_execs = execs;
    }
    public PlatformExecutors getPlatformExecutors (){
	return _execs;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target="(service.pid=system)")
    public void setSystemConf (Dictionary<String, String> conf) throws Exception {
	_agent = conf.get (ConfigConstants.INSTANCE_NAME);
	LOGGER.info ("@Reference setSystemConf : "+_agent);
    }
    public void unsetSystemConf (Dictionary<String, String> conf) throws Exception {
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }

    @Activate
    public void activate (BundleContext ctx, Map<String, String> conf){
	LOGGER.info ("@Activate");
	_osgi = ctx;
	GogoCommands.registerCommands (ctx);
	update (conf);
    }
    public BundleContext getBundleContext (){ return _osgi;}

    @Modified
    public synchronized void update (Map<String, String> conf) {
	LOGGER.info ("@Modified");
	newTcpClientConfig (conf.get (CONF_TCP_CLIENT));
	newUdpClientConfig (conf.get (CONF_UDP_CLIENT));
    }
    
    public synchronized void newTcpClientConfig (String xml){
	ClientsConfiguration clientConf = null;
	try{
	    clientConf = ClientsConfiguration.parse (xml,  _agent);
	}catch(Exception t){
	    LOGGER.error ("Exception while parsing tcp client configuration - configuration ignored : "+xml, t);
	    return;
	}
	for (ClientsConfiguration.Client client : clientConf.getClients ()){
	    ClusterInstance inst = new ClusterInstance (client.getId ()).read (client, clientConf.getProperties ());
	    _tcpClusters.put (inst.getId (), inst);
	}
    }
    public synchronized void newUdpClientConfig (String xml){
	ClientsConfiguration clientConf = null;
	try{
	    clientConf = ClientsConfiguration.parse (xml,  _agent);
	}catch(Exception t){
	    LOGGER.error ("Exception while parsing udp client configuration - configuration ignored : "+xml, t);
	    return;
	}
	for (ClientsConfiguration.Client client : clientConf.getClients ()){
	    ClusterInstance inst = new ClusterInstance (client.getId ()).read (client, clientConf.getProperties ());
	    _udpClusters.put (inst.getId (), inst);
	}
    }

    public TcpClient newTcpClient (String id, Map<String, Object> params){
	if (id == null) id = ""; // play it safe
	TcpClientImpl client = null;
	params = setSystemProperties (cleanTcpProperties (params));
	ClusterInstance cluster = _tcpClusters.get (id);
	if (cluster == null){
	    client = new TcpClientImpl (this, id, params);
	} else {
	    Property.fillDefaultProperties (params, cluster.getProperties ());
	    client = new TcpClientImpl (this, id, params);
	    for (ServerInstance server : cluster.getServers ()){
		int n = Property.getIntProperty ("connect.size", server.getProperties (false), 1, false);
		for (int i=0; i<n; i++){
		    Map<String, Object> props = server.getProperties (true);
		    props.put ("connect.size.index", i);
		    client.addDestination (server.getAddress (), null, props);
		}
	    }
	}
	return client;
    }

    public UdpClient newUdpClient (String id, Map<String, Object> params){
	if (id == null) id = ""; // play it safe
	UdpClientImpl client = null;
	params = setSystemProperties (cleanUdpProperties (params));
	ClusterInstance cluster = _udpClusters.get (id);
	if (cluster == null){
	    client = new UdpClientImpl (this, id, params);
	} else {
	    Property.fillDefaultProperties (params, cluster.getProperties ());
	    client = new UdpClientImpl (this, id, params);
	    for (ServerInstance server : cluster.getServers ()){
		Map<String, Object> props = server.getProperties (true);
		client.addDestination (server.getAddress (), null, props);
	    }
	}
	return client;
    }

    public static class ClusterInstance {
	private String _id, _toString;
	private Map<String, Object> _props;
	private List<ServerInstance> _servers = new ArrayList<ServerInstance> ();
	private ClusterInstance (String id){
	    _id = id;
	    _toString = new StringBuilder ().append ("ClusterInstance[").append (_id).append (']').toString ();
	}
	public String toString (){
	    return _toString;
	}
       	private ClusterInstance read (ClientsConfiguration.Client client, List<Property> defProps){
	    _props = Property.fillProperties (client.getProperties (), null);
	    Property.fillDefaultProperties (_props, defProps);
	    _props.put ("client.version", client.getVersion ());
	    cleanProperties (_props);
	    for (ServersConfiguration.Server server : client.getServers ().getServers ()){
		try{
		    _servers.add (new ServerInstance (server, this));
		}catch(Exception e){
		    LOGGER.error ("Invalid server configuration : "+server.getIP ());
		}
	    }
	    LOGGER.info (this+" : defined : "+_props+" / "+_servers);
	    return this;
	}
	public String getId (){
	    return _id;
	}
	public List<ServerInstance> getServers (){
	    return _servers;
	}
	public Map<String, Object> getProperties (){ return _props;}
    }
    public static class ServerInstance {
	private InetSocketAddress _address;
	private Map<String, Object> _props;
	private ServerInstance (ServersConfiguration.Server server, ClusterInstance cluster) throws Exception {
	    _address = new InetSocketAddress (InetAddress.getByName (server.getIP ()), server.getPort ());
	    _props = Property.fillProperties (server.getProperties (), null);
	    String name = server.getName ();
	    if (name==null) name = _address.getAddress().getHostAddress() + ":" + _address.getPort();
	    _props.put (Server.PROP_SERVER_NAME, name);
	    _props.put (Server.PROP_SERVER_IP, server.getIP ());
	    _props.put (Server.PROP_SERVER_PORT, server.getPort ());
	    cleanProperties (_props);
	}
	public Map<String, Object> getProperties (boolean copy){
	    if (!copy) return _props;
	    return (Map<String, Object>) ((HashMap<String, Object>)_props).clone ();
	}
	public InetSocketAddress getAddress (){ return _address;}
	public String toString (){return _address.toString ();}
    }

    private static Map<String, Object> cleanTcpProperties (Map<String, Object> props){
	if (props == null) return props = new HashMap<String, Object> ();
	Object o = props.get (TcpClient.PROP_CONNECT_FROM);
	if (o != null && o instanceof String){
	    try{
		o = new InetSocketAddress (InetAddress.getByName ((String) o), 0);
		props.put (TcpClient.PROP_CONNECT_FROM, o);
	    }catch(Exception e){
		LOGGER.error ("Failed to parse connect.from", e);
		throw new IllegalArgumentException ("Invalid connect.from property");
	    }
	}
	return props;
    }
    private static Map<String, Object> cleanUdpProperties (Map<String, Object> props){
	if (props == null) props = new HashMap<String, Object> ();
	else if (props.get ("bind.address") != null) return props;
	Object ip = props.get (UdpClient.PROP_BIND_IP);
	Object port = props.get (UdpClient.PROP_BIND_PORT);
	if (ip == null) ip = "0.0.0.0";
	if (port == null) port = "0";
	try{
	    int i = Integer.parseInt (port.toString ());
	    if (i < 0 || i > 0xFFFF) throw new Exception ("Invalid port value : "+i);
	    InetSocketAddress add = new InetSocketAddress (InetAddress.getByName ((String) ip), i);
	    props.put ("bind.address", add);
	}catch(Exception e){
	    LOGGER.error ("Failed to parse bind info", e);
	    throw new IllegalArgumentException ("Invalid bind properties");
	}
	return props;
    }
    
    private static Map<String, Object> cleanProperties (Map<String, Object> props){
	return cleanUdpProperties (cleanTcpProperties (props));
    }
	    
    private Map<String, Object> setSystemProperties (Map<String, Object> props){
	if (props == null) props = new HashMap<String, Object> ();
	props.put ("system.reactor.provider", _reactorProvider);
	return props;
    }

    // called when client is in init()
    public Object addAdvertListener (TcpClientImpl client, String moduleId, String instanceName){
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("ClientFactoryImpl : addAdvertListener : client="+client+" : module.id="+moduleId+"&instance.name="+instanceName);
	return new AdvertTracker (client, moduleId, instanceName).open ();
    }
    public void removeAdvertListener (Object attachment){
	AdvertTracker tracker = (AdvertTracker) attachment;
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("ClientFactoryImpl : removeAdvertListener : client="+tracker._client);
	tracker.close ();
    }

    private class AdvertTracker implements ServiceTrackerCustomizer {
	private TcpClientImpl _client;
	private ServiceTracker _tracker;
	
	private AdvertTracker(TcpClientImpl client, String moduleId, String instanceName) {
	    _client = client;
	    StringBuilder s = new StringBuilder ()
		.append ("(&(objectClass=")
		.append (Advertisement.class.getName())
		.append (")(module.id=")
		.append (moduleId).append (')');
	    if (instanceName != null) s.append ("(instance.name=").append (instanceName).append (')');
	    s.append (')');
	    try {
		Filter f = _osgi.createFilter(s.toString ());
		_tracker = new ServiceTracker(_osgi, f, this);
	    } catch (InvalidSyntaxException e) {
		LOGGER.warn ("Failed to create tracker for client="+client+", filter="+s.toString (), e);
	    } 
	}
   
	public AdvertTracker open() {
	    _tracker.open();
	    return this;
	}
   
	public void close() {
	    _tracker.close();
	}
   
	/**
	 * A new Advert is registering in the OSGi registry: handle it thread-safely
	 */
	@Override
	public Object addingService(ServiceReference ref) {
	    final String instance = (String) ref.getProperty ("instance.name");
	    Advertisement advert = (Advertisement) _osgi.getService(ref);
	    final InetSocketAddress addr = new InetSocketAddress (advert.getIp(), advert.getPort());
	    LOGGER.warn ("ClientFactoryImpl : advertisementReceived : addDestination : "+instance+"@"+addr);
	    Runnable r = new Runnable (){
		    public void run (){
			LOGGER.warn ("ClientFactoryImpl : advertisementReceived : addDestination : "+instance+"@"+addr+" to "+_client);
			List<TcpClient.Destination> servers = _client.getDestinations ();
			for (int i=0; i<servers.size (); i++){
			    TcpClient.Destination server = servers.get (i);
			    boolean match = instance.equals (server.attachment ());
			    if (match){
				if (addr.equals (server.getRemoteAddress ())){
				    if (LOGGER.isInfoEnabled ()) LOGGER.info ("ClientFactoryImpl : "+instance+" already known with same address");
				    return;
				} else {
				    if (LOGGER.isInfoEnabled ()) LOGGER.info ("ClientFactoryImpl : "+instance+" updating with new address");
				    servers.remove (i);
				    if (server.isOpen ()) server.close ();
				    break;
				}
			    }
			}
			_client.addDestination (addr, instance, null);
		    }
		};
	    _client.execute (r);
	    return instance;
	}
   
	/**
	 * An old Advert is unregistering from the OSGi registry: handle it thread-safely
	 */
	@Override
	public void removedService(ServiceReference ref, Object attachment) {
	    final String instance = (String) attachment;
	    Runnable r = new Runnable (){
		    public void run (){
			LOGGER.warn ("ClientFactoryImpl : un-advertisementReceived : "+instance+" for "+_client);
			List<TcpClient.Destination> servers = _client.getDestinations ();
			for (int i=0; i<servers.size (); i++){
			    TcpClient.Destination server = servers.get (i);
			    boolean match = instance.equals (server.attachment ());
			    if (match){
				servers.remove (i);
				if (server.isOpen ()) server.close ();
				return;
			    }
			}
		    }
		};
	    _client.execute (r);
	}
   
	/**
	 * An existing advertisement is modified (not possible for now ...)
	 */
	@Override
	public void modifiedService(ServiceReference ref, Object attachment) {
	}
    }
}
