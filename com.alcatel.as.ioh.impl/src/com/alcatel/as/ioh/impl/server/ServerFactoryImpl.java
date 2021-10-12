// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.impl.server;

import java.util.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.impl.conf.ServersConfiguration;
import com.alcatel.as.ioh.impl.conf.Property;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.apache.log4j.Logger;
import org.osgi.framework.*;
import org.osgi.service.component.*;

import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.service.discovery.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel.as.service.metering2.*;

@Component(service={ServerFactory.class}, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate=true)
public class ServerFactoryImpl implements ServerFactory {

    public static final String PREFIX = "as.ioh.server";
    public static final String PREFIX_DOT = PREFIX+".";
    public static final String REACTOR_PREFIX = "ioh.server";
    public static final String REACTOR_PREFIX_DOT = REACTOR_PREFIX+".";
    
    public static final Logger LOGGER = Logger.getLogger(PREFIX);

    public static final String DEF_NAMESPACE = "def";
    public static final String API_NAMESPACE = "api";
    
    private final Map<String, List<ServerImpl>> _servers = new HashMap<> ();
    private final Map<String, Wrapper<TcpServerProcessor>> _procsTCP = new HashMap<String, Wrapper<TcpServerProcessor>> ();
    private final Map<String, Wrapper<UdpServerProcessor>> _procsUDP = new HashMap<String, Wrapper<UdpServerProcessor>> ();
    private final Map<String, Wrapper<SctpServerProcessor>> _procsSCTP = new HashMap<String, Wrapper<SctpServerProcessor>> ();

    private String _agent;
    private long _agentId;
    private BundleContext _osgi;
    private ReactorProvider _reactorProvider;
    private Reactor _reactor;
    private PlatformExecutors _executors;
    private MeteringService _metering;
    private InetSocketAddress _fromAddressToServers;//todo ?
    private Map<String, ServersConfiguration> _configTCP = new HashMap<> (), _configUDP = new HashMap<> (), _configSCTP = new HashMap<> ();
    private final SerialExecutor _serialExec = new SerialExecutor (LOGGER);

    @Override
    public String toString (){ return "ServerFactory";}
    
    @FileDataProperty(title="Server",
		      fileData="defTcpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Tcp",
		      help="Describes the listening endpoints.")
    public final static String CONF_TCP_SERVERS = "conf.tcp.servers";
    @FileDataProperty(title="Server",
		      fileData="defUdpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Udp",
		      help="Describes the listening endpoints.")
    public final static String CONF_UDP_SERVERS = "conf.udp.servers";
    @FileDataProperty(title="Server",
		      fileData="defSctpServer.txt",
		      required=true,
		      dynamic=true,
		      section="Sctp",
		      help="Describes the listening endpoints.")
    public final static String CONF_SCTP_SERVERS = "conf.sctp.servers";

    @Reference
    public void setReactorProvider (ReactorProvider provider) throws Exception {
	LOGGER.info ("@Reference setReactorProvider");
	_reactorProvider = provider;
	_reactor = _reactorProvider.create (REACTOR_PREFIX);
	_reactor.start ();
    }

    @Reference
    public void setPlatformExecutors (PlatformExecutors execs) throws Exception {
	LOGGER.info ("@Reference setPlatformExecutors");
	_executors = execs;
    }

    @Reference
    public void setMeteringService (MeteringService metering) throws Exception {
	LOGGER.info ("@Reference setMeteringService");
	_metering = metering;
    }

    @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target="(service.pid=system)")
    public void setSystemConf (Dictionary<String, String> conf) throws Exception {
	_agent = conf.get (ConfigConstants.INSTANCE_NAME);
	_agentId = Long.parseLong (conf.get (ConfigConstants.INSTANCE_ID));
	LOGGER.info ("@Reference setSystemConf : "+_agent);
    }
    public void unsetSystemConf (Dictionary<String, String> conf) throws Exception {
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]	
    }

    @Reference(target = "(component.factory=gogoProcessor)")
    public void bindGogoProcessorFactory (ComponentFactory factory){
	factory.newInstance (new Hashtable (){{put("processor.id", "gogo.command");}});
	factory.newInstance (new Hashtable (){{put("processor.id", "gogo.shell");}});
	factory.newInstance (new Hashtable (){{put("processor.id", "gogo.client");}});
    }

    private List<ServerImpl> getInternalServers (String namespace){
	List<ServerImpl> list = _servers.get (namespace);
	if (list == null){
	    _servers.put (namespace, list = new ArrayList<ServerImpl> ());
	}
	return list;
    }
    
    /******************************
     *           TCP              *
     ******************************/
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind="removedTcpProcessor")
    public void newTcpProcessor (final TcpServerProcessor processor, final Map<String, String> properties){
	Runnable r = new Runnable (){
		public synchronized void run (){
		    String id = properties.get (Server.PROP_PROCESSOR_ID);
		    if (LOGGER.isInfoEnabled ()) LOGGER.info ("@Reference newTcpProcessor : "+processor+" : id="+id);
		    if (id == null) return;
		    Wrapper<TcpServerProcessor> wrapper = new Wrapper<TcpServerProcessor> (processor, properties);
		    _procsTCP.put (id, wrapper);
		    initTcpProcessor (id, wrapper);
		}};
	_serialExec.execute (r);
    }
    public void removedTcpProcessor (final TcpServerProcessor processor, final Map<String, String> properties){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]	
    }
    
    private void initTcpProcessor (String id, Wrapper<TcpServerProcessor> processor){
	for (String namespace : _configTCP.keySet ()){
	    ServersConfiguration config = _configTCP.get (namespace);
	    for (ServersConfiguration.Server tcp : config.getServers ()){
		if (tcp.getProcessor ().equals (id))
		    initTcpServer (processor, namespace, config, tcp);
	    }
	}
    }
    private void initTcpServer (Wrapper<TcpServerProcessor> processor, String namespace, ServersConfiguration config, ServersConfiguration.Server tcp){
	Map<String, Object> props = getProperties (namespace, config, tcp, null);
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("defined new TcpServer: "+props);
	try{
	    getInternalServers (namespace+".tcp").add (new TcpServerImpl (this, processor.get (), processor.getProperties (), props).open ());
	}catch(Exception e){
	    LOGGER.error ("Failed to instanciate TcpServer : "+props, e);
	}
    }
    public TcpServer newTcpServer (TcpServerProcessor processor, Map<String, Object> props){
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("newTcpServer : "+processor+" : "+props);
	String namespace = (String) props.get (Server.PROP_SERVER_NAMESPACE);
	if (namespace == null) namespace = API_NAMESPACE;
	try{
	    ServerImpl server = new TcpServerImpl (this, processor, null, getProperties (namespace, null, null, props)).open ();
	    synchronized (this){
		getInternalServers (namespace+".tcp").add (server);
	    }
	    return (TcpServer) server;
	}catch(Exception e){
	    throw new RuntimeException ("Failed to instanciate TcpServer : "+props, e);
	}
    }

    public void newTcpServerConfig (final String namespace, final String xml){
	if (LOGGER.isInfoEnabled ())
	    LOGGER.info ("newTcpServerConfig : namespace = "+namespace+" :\n"+xml);
	if (xml == null || xml.trim().length() == 0) return;
	if (namespace == null) throw new NullPointerException ("Need namespace");
	_serialExec.execute (new Runnable (){ public void run (){
	    try{tcpServerConfigUpdated (namespace, xml);}
	    catch (Throwable t){
		LOGGER.error ("Exception while updating tcp server configuration : namespace="+namespace+", xml="+xml, t);
	    }
	}});
    }

    /******************************
     *           UDP              *
     ******************************/
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind="removedUdpProcessor")
    public void newUdpProcessor (final UdpServerProcessor processor, final Map<String, String> props){
	Runnable r = new Runnable (){
		public synchronized void run (){
		    String id = props.get (Server.PROP_PROCESSOR_ID);
		    if (LOGGER.isInfoEnabled ()) LOGGER.info ("@Reference newUdpProcessor : "+processor+" : id="+id);
		    if (id == null) return;
		    Wrapper<UdpServerProcessor> wrapper = new Wrapper<UdpServerProcessor> (processor, props);
		    _procsUDP.put (id, wrapper);
		    initUdpProcessor (id, wrapper);
		}};
	_serialExec.execute (r);
    }
    public void removedUdpProcessor (final UdpServerProcessor processor, final Map<String, String> props){
    }

    private void initUdpProcessor (String id, Wrapper<UdpServerProcessor> processor){
	for (String namespace : _configUDP.keySet ()){
	    ServersConfiguration config = _configUDP.get (namespace);
	    for (ServersConfiguration.Server udp : config.getServers ()){
		if (udp.getProcessor ().equals (id))
		    initUdpServer (processor, namespace, config, udp);
	    }
	}
    }
    private void initUdpServer (Wrapper<UdpServerProcessor> processor, String namespace, ServersConfiguration config, ServersConfiguration.Server udp){
	Map<String, Object> props = getProperties (namespace, config, udp, null);
	LOGGER.info ("defined new UdpServer: "+props);
	getInternalServers (namespace+".udp").add (new UdpServerImpl (this, processor.get (), processor.getProperties (), props).open ());
    }
    public UdpServer newUdpServer (UdpServerProcessor processor, Map<String, Object> props){
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("newUdpServer : "+processor+" : "+props);
	String namespace = (String) props.get (Server.PROP_SERVER_NAMESPACE);
	if (namespace == null) namespace = API_NAMESPACE;
	ServerImpl server = new UdpServerImpl (this, processor, null, getProperties (namespace, null, null, props)).open ();
	synchronized (this){
	    getInternalServers (namespace+".udp").add (server);
	}
	return (UdpServer) server;
    }

    public void newUdpServerConfig (final String namespace, final String xml){
	if (LOGGER.isInfoEnabled ())
	    LOGGER.info ("newUdpServerConfig : namespace = "+namespace+" :\n"+xml);
	if (xml == null || xml.trim().length() == 0) return;
	if (namespace == null) throw new NullPointerException ("Need namespace");
	_serialExec.execute (new Runnable (){ public void run (){
	    try{udpServerConfigUpdated (namespace, xml);}
	    catch (Throwable t){
		LOGGER.error ("Exception while updating udp server configuration : namespace="+namespace+", xml="+xml, t);
	    }
	}});
    }

    /******************************
     *           SCTP             *
     ******************************/
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind="removedSctpProcessor")
    public void newSctpProcessor (final SctpServerProcessor processor, final Map<String, String> props){
	Runnable r = new Runnable (){
		public synchronized void run (){
		    String id = props.get (Server.PROP_PROCESSOR_ID);
		    if (LOGGER.isInfoEnabled ()) LOGGER.info ("@Reference newSctpProcessor : "+processor+" : id="+id);
		    if (id == null) return;
		    Wrapper<SctpServerProcessor> wrapper = new Wrapper<SctpServerProcessor> (processor, props);
		    _procsSCTP.put (id, wrapper);
		    initSctpProcessor (id, wrapper);
		}};
	_serialExec.execute (r);
    }
    public void removedSctpProcessor (final SctpServerProcessor processor, final Map<String, String> props){
	// new bnd DS annotations require to have unset method for dynamic dependencies [...]
    }

    private void initSctpProcessor (String id, Wrapper<SctpServerProcessor> processor){
	for (String namespace : _configSCTP.keySet ()){
	    ServersConfiguration config = _configSCTP.get (namespace);
	    for (ServersConfiguration.Server sctp : config.getServers ()){
		if (sctp.getProcessor ().equals (id))
		    initSctpServer (processor, namespace, config, sctp);
	    }
	}
    }
    private void initSctpServer (Wrapper<SctpServerProcessor> processor, String namespace, ServersConfiguration config, ServersConfiguration.Server sctp){
	Map<String, Object> props = getProperties (namespace, config, sctp, null);
	LOGGER.info ("defined new SctpServer: "+props);
	getInternalServers (namespace+".sctp").add (new SctpServerImpl (this, processor.get (), processor.getProperties (), props).open ());
    }
    public SctpServer newSctpServer (SctpServerProcessor processor, Map<String, Object> props){
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("newSctpServer : "+processor+" : "+props);
	String namespace = (String) props.get (Server.PROP_SERVER_NAMESPACE);
	if (namespace == null) namespace = API_NAMESPACE;
	ServerImpl server = new SctpServerImpl (this, processor, null, getProperties (namespace, null, null, props)).open ();
	synchronized (this){
	    getInternalServers (namespace+".sctp").add (server);
	}
	return (SctpServer) server;
    }

    public void newSctpServerConfig (final String namespace, final String xml){
	if (LOGGER.isInfoEnabled ())
	    LOGGER.info ("newSctpServerConfig : namespace = "+namespace+" :\n"+xml);
	if (xml == null || xml.trim().length() == 0) return;
	if (namespace == null) throw new NullPointerException ("Need namespace");
	_serialExec.execute (new Runnable (){ public void run (){
	    try{sctpServerConfigUpdated (namespace, xml);}
	    catch (Throwable t){
		LOGGER.error ("Exception while updating sctp server configuration : namespace="+namespace+", xml="+xml, t);
	    }
	}});
    }

    /******************************
     *           Misc. API            *
     ******************************/
    
    public synchronized List<Server> getServers (String namespace){
	List<Server> servers = new ArrayList<> ();
	if (namespace == null) namespace = API_NAMESPACE;
	if (namespace.equals ("*")){
	    for (String key : _servers.keySet ()){
		servers.addAll (_servers.get (key));
	    }
	} else {
	    List<ServerImpl> list = _servers.get (namespace+".tcp");
	    if (list != null) servers.addAll (list);
	    list = _servers.get (namespace+".udp");
	    if (list != null) servers.addAll (list);
	    list = _servers.get (namespace+".sctp");
	    if (list != null) servers.addAll (list);
	}
	return servers;
    }

    /******************************
     *           Props            *
     ******************************/
    private AtomicInteger KEY_SEED = new AtomicInteger (1);
    
    private Map<String, Object> getProperties (String namespace, ServersConfiguration conf, ServersConfiguration.Server server, Map<String, Object> props){
	if (props == null) props = new HashMap<String, Object>();
	props.put (Server.PROP_SERVER_NAMESPACE, namespace);
	if (server != null){
	    if (server.getIP () != null)
		props.put (Server.PROP_SERVER_IP, server.getIP ());
	    if (server.getInterface () != null)
		props.put (Server.PROP_SERVER_IF, server.getInterface ());
	    props.put (Server.PROP_SERVER_PORT, server.getPort ());
	    if (server.getName () != null)
		props.put (Server.PROP_SERVER_NAME, server.getName ());
	    props.put (ServerImpl.PROP_SERVER_KEY, server.getKey ());
	    props.put (Server.PROP_SERVER_SECURE, server.isSecure ());
	    props.put (Server.PROP_SERVER_STANDBY, server.standby ());
	    Property.fillProperties (server.getProperties (), props);
	} else {
	    props.put (ServerImpl.PROP_SERVER_KEY, String.valueOf (KEY_SEED.getAndIncrement ()));
	}
	setSystemProperties (props);
	if (conf != null) Property.fillDefaultProperties (props, conf.getProperties ());
	return props;
    }
    private void setSystemProperties (Map<String, Object> props){
	setReactor (props);
	props.put ("system.reactor.provider", _reactorProvider);
	props.put ("system.executors", _executors);
	props.put ("system.metering", _metering);
	props.put ("system.osgi", _osgi);
    }
    protected synchronized Reactor setReactor (Map<String, Object> props){ // synchronized because of reactorProvider calls below
	try{
	    Object reactor = props.get (Server.PROP_SERVER_REACTOR);
	    if (reactor == null){
		reactor = props.get (Server.PROP_SERVER_NAME);
		if (reactor == null){
		    props.put (Server.PROP_SERVER_REACTOR, reactor = _reactor);
		    return _reactor;
		}
	    }
	    if (reactor instanceof String){
		String name = (String) reactor;
		if (!name.startsWith (REACTOR_PREFIX_DOT))
		    name = REACTOR_PREFIX_DOT + name;
		Reactor r = _reactorProvider.getReactor (name);
		if (r == null){
		    r = _reactorProvider.create (name);
		    r.start ();
		}
		props.put (Server.PROP_SERVER_REACTOR, r);
		return r;
	    }
	    return (Reactor) reactor;
	}catch(Exception e){
	    LOGGER.error ("Exception in setReactor", e);
	    props.put (Server.PROP_SERVER_REACTOR, _reactor);
	    return _reactor;
	}
    }

    /******************************
     *           Advertizing      *
     ******************************/

    public ServiceRegistration advertize (final String id, final String name, InetSocketAddress serverAddress, Map<String, Object> props) {
	if (id == null) return null;
	Advertisement advert = new Advertisement(serverAddress.getAddress().getHostAddress(),
						 serverAddress.getPort());
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("Advertizing : "+id+"/"+name+"/"+serverAddress);
	Hashtable options = new Hashtable ();
	options.put(ConfigConstants.MODULE_NAME,  name);
	options.put(ConfigConstants.COMPONENT_NAME,  name); // in muxhandler, stack_name is taken from component_name !
	options.put(ConfigConstants.MODULE_ID, id);
	options.put(ConfigConstants.INSTANCE_ID, _agentId);
	options.put("type", "ioh");
	for (String key: props.keySet ())
	    if (key.startsWith ("advertize.")) options.put (key.substring ("advertize.".length ()), props.get (key));
        return _osgi.registerService(Advertisement.class.getName(),
				     advert,
				     options
				     );
    }
    public void unadvertize (ServiceRegistration reg){
	if (reg == null) return;
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("UnAdvertizing : "+reg);
	reg.unregister ();
    }

    /******************************
     *           Start            *
     ******************************/
    
    @Activate
    public void activate (BundleContext ctx, Map<String, String> conf) throws Exception {
	LOGGER.info ("@Activate");
	_osgi = ctx;
	GogoCommands.registerCommands (ctx);
	update (conf);
    }
    
    @Modified
    public void update (final Map<String, String> conf) throws Exception {
	LOGGER.info ("@Modified");
	_serialExec.execute (new Runnable (){ public void run (){
	    try{updated (conf);}
	    catch (Throwable t){
		LOGGER.error ("Exception while updating XML configuration", t);
	    }
	}});
    }
    
    private void updated (Map<String, String> conf) throws Exception {
	tcpServerConfigUpdated (DEF_NAMESPACE, conf.get (CONF_TCP_SERVERS));
	udpServerConfigUpdated (DEF_NAMESPACE, conf.get (CONF_UDP_SERVERS));
	sctpServerConfigUpdated (DEF_NAMESPACE, conf.get (CONF_SCTP_SERVERS));
    }
    private synchronized void tcpServerConfigUpdated (String namespace, String xml){
	ServersConfiguration config;
	try{
	    config = ServersConfiguration.parse (xml, _agent);
	}catch(Throwable t){
	    LOGGER.error ("Exception while parsing tcp server configuration - configuration ignored", t);
	    return;
	}
	_configTCP.put (namespace, config);
	for (ServersConfiguration.Server endpoint : updated (namespace, config, getInternalServers (namespace+".tcp"))){
	    Wrapper<TcpServerProcessor> proc = _procsTCP.get (endpoint.getProcessor ());
	    if (proc != null){
		initTcpServer (proc, namespace, config, endpoint);
	    }
	}
    }
    private synchronized void udpServerConfigUpdated (String namespace, String xml){
	ServersConfiguration config;
	try{
	    config = ServersConfiguration.parse (xml, _agent);
	}catch(Throwable t){
	    LOGGER.error ("Exception while parsing udp server configuration - configuration ignored", t);
	    return;
	}
	_configUDP.put (namespace, config);
	for (ServersConfiguration.Server endpoint : updated (namespace, config, getInternalServers (namespace+".udp"))){
	    Wrapper<UdpServerProcessor> proc = _procsUDP.get (endpoint.getProcessor ());
	    if (proc != null){
		initUdpServer (proc, namespace, config, endpoint);
	    }
	}
    }
    private synchronized void sctpServerConfigUpdated (String namespace, String xml){
	ServersConfiguration config;
	try{
	    config = ServersConfiguration.parse (xml, _agent);
	}catch(Throwable t){
	    LOGGER.error ("Exception while parsing sctp server configuration - configuration ignored", t);
	    return;
	}
	_configSCTP.put (namespace, config);
	for (ServersConfiguration.Server endpoint : updated (namespace, config, getInternalServers (namespace+".sctp"))){
	    Wrapper<SctpServerProcessor> proc = _procsSCTP.get (endpoint.getProcessor ());
	    if (proc != null){
		initSctpServer (proc, namespace, config, endpoint);
	    }
	}
    }
    private List<ServersConfiguration.Server> updated (String namespace, ServersConfiguration config, List<ServerImpl> servers){
	List<ServerImpl> removed = new ArrayList<ServerImpl> ();
	loopRem: for (ServerImpl server : servers){
	    for (ServersConfiguration.Server endpoint : config.getServers ()){
		if (endpoint.getKey ().equals (server.getKey ())){
		    server.update (getProperties (namespace, config, endpoint, null));
		    continue loopRem;
		}
	    }
	    removed.add (server);
	}
	for (ServerImpl server : removed){
	    servers.remove (server);
	    server.close (true);
	}
	List<ServersConfiguration.Server> added = new ArrayList<ServersConfiguration.Server> ();
	loopAdd: for (ServersConfiguration.Server endpoint : config.getServers ()){
	    for (ServerImpl server : servers){
		if (endpoint.getKey ().equals (server.getKey ()))
		    continue loopAdd;
	    }
	    added.add (endpoint);
	}
	return added;
    }
    
    protected synchronized void closed (ServerImpl server){
	LOGGER.info (this+" : closed : "+server+" / namespace="+server.getProperties ().get (Server.PROP_SERVER_NAMESPACE));
	if (server instanceof TcpServerImpl) {getInternalServers (server.getProperties ().get (Server.PROP_SERVER_NAMESPACE)+".tcp").remove (server); return;}
	if (server instanceof UdpServerImpl) {getInternalServers (server.getProperties ().get (Server.PROP_SERVER_NAMESPACE)+".udp").remove (server); return;}
	if (server instanceof SctpServerImpl) {getInternalServers (server.getProperties ().get (Server.PROP_SERVER_NAMESPACE)+".sctp").remove (server); return;}
    }

    public static class Wrapper<T>{
	private T _w;
	private Map<String, Object> _props;
	private Wrapper (T w, Map<String, String> props){
	    _w = w;
	    _props = new HashMap<String, Object> ();
	    Property.fillDefaultStringProperties (_props, props);
	}
	private T get (){
	    return _w;
	}
	private Map<String, Object> getProperties (){
	    return _props;
	}
    }

}
