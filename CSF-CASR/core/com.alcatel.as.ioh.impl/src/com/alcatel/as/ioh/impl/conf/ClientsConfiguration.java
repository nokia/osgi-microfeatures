package com.alcatel.as.ioh.impl.conf;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import com.thoughtworks.xstream.security.NoTypePermission;

@XStreamAlias("clients")
public class ClientsConfiguration {
	int i;

    @XStreamImplicit(itemFieldName="property")
    public List<Property> properties;

    @XStreamImplicit(itemFieldName="client")
    private List<Client> clients;

    public static ClientsConfiguration parse (String xml, String thisAgent){
	XStream stream = new XStream(null, new DomDriver (), ClientsConfiguration.class.getClassLoader());
	// the two following lines deactivate xstream security which is now enforced by default using xstream 1.4.10
	stream.addPermission(NoTypePermission.NONE);
	stream.addPermission(AnyTypePermission.ANY);	
	stream.processAnnotations (ClientsConfiguration.class);
	stream.processAnnotations (ServersConfiguration.class);
	stream.registerConverter (new PropertyConverter ());
	ClientsConfiguration conf = (ClientsConfiguration) stream.fromXML (xml);
	return conf.clean (thisAgent);
    }

    private ClientsConfiguration clean (String thisAgent){
	if (thisAgent != null){
	    Property.clean (getProperties (), thisAgent);
	    for (Client client : getClients ()){
		Property.clean (client.getProperties (), thisAgent);
		client.getServers ().clean (thisAgent);
	    }
	}
	return this;
    }

    public List<Client> getClients (){
	if (clients == null) clients = new ArrayList<Client> ();
	return clients;
    }
    
    public List<Property> getProperties (){
	if (properties == null) properties = new ArrayList<Property> ();
	return properties;
    }

    public static class Client {

	@XStreamAsAttribute
	public String id;

	@XStreamAsAttribute
	public String version;
	
	@XStreamImplicit(itemFieldName="property")
	public List<Property> properties;

	public ServersConfiguration servers;
    
	public String getId (){
	    return id;
	}

	public String getVersion (){
	    return version;
	}

	public List<Property> getProperties (){
	    if (properties == null) properties = new ArrayList<Property> ();
	    return properties;
	}

	public ServersConfiguration getServers (){
	    return servers;
	}
    }
}