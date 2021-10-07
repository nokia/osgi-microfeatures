package com.alcatel.as.ioh.impl.conf;

import java.util.ArrayList;
import java.util.List;

import com.alcatel.as.ioh.server.SctpServer;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;

@XStreamAlias("servers")
public class ServersConfiguration {

    @XStreamImplicit(itemFieldName="property")
    public List<Property> properties;

    @XStreamImplicit(itemFieldName="server")
    private List<Server> servers;

    public static ServersConfiguration parse (String xml, String thisAgent){
	XStream stream = new XStream(null, new DomDriver (), ClientsConfiguration.class.getClassLoader());
	// the two following lines deactivate xstream security which is now enforced by default using xstream 1.4.10
	stream.addPermission(NoTypePermission.NONE);
	stream.addPermission(AnyTypePermission.ANY);	
	stream.processAnnotations (ServersConfiguration.class);
	stream.processAnnotations (ServersConfiguration.Server.class);
	stream.registerConverter (new PropertyConverter ());
	ServersConfiguration config = (ServersConfiguration) stream.fromXML (xml);
	return config.clean (thisAgent);
    }

    protected ServersConfiguration clean (String thisAgent){
	if (servers == null) servers = new ArrayList<Server> ();
	if (thisAgent != null){
	    for (int i=0; i<servers.size ();){
		List agents = Property.getStringListProperty ("system.agent", Property.fillProperties (servers.get (i).getProperties (), null));
		if (agents != null && agents.indexOf (thisAgent) == -1)
		    servers.remove (i);
		else {
		    Property.clean (servers.get (i).getProperties (), thisAgent);
		    i++;
		}
	    }
	}
	return this;
    }
    
    public List<Server> getServers (){
	if (servers == null) servers = new ArrayList<Server> ();
	return servers;
    }
    
    public List<Property> getProperties (){
	if (properties == null) properties = new ArrayList<Property> ();
	return properties;
    }

    public static class Server {

	@XStreamAsAttribute
	public String name;

	@XStreamAsAttribute
	public String ip;

	@XStreamAsAttribute
	@XStreamAlias("if")
	public String interf;

	@XStreamAsAttribute
	public int port;

	@XStreamAsAttribute
	public boolean secure;

	@XStreamAsAttribute
	public String processor;

	@XStreamAsAttribute
	public String stamp;

	@XStreamAsAttribute
	public boolean standby;

	@XStreamImplicit(itemFieldName="property")
	public List<Property> properties;

	private String key;
    
	public String getIP (){
	    return ip;
	}

	public String getInterface (){
	    return interf;
	}

	public int getPort (){
	    return port;
	}

	public String getProcessor (){
	    return processor;
	}
    
	public String getName (){
	    return name;
	}

	public boolean isSecure (){
	    return secure;
	}

	public boolean standby (){
	    return standby;
	}

	public List<Property> getProperties (){
	    if (properties == null) properties = new ArrayList<Property> ();
	    return properties;
	}

	public String getKey (){
	    if (key == null)
		key = new StringBuilder ()
		    .append (stamp)
		    .append ('/').append (interf)
		    .append ('/').append (ip)
		    .append ('/').append (port)
		    .append ('/').append (processor)
		    .append ('/').append (secure)
		    .append ('/').append (Property.getStringListProperty (SctpServer.PROP_SERVER_IP_SECONDARY, getProperties ()))
		    .toString ();
	    return key;
	}
    
	public String toString (){
	    return getKey ();
	}
    }
}
