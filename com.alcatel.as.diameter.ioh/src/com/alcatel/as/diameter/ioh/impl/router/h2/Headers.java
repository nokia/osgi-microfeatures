package com.alcatel.as.diameter.ioh.impl.router.h2;

import java.util.*;
import java.net.*;
import org.apache.log4j.Logger;

import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.http2.client.api.*;
import alcatel.tess.hometop.gateways.reactor.*;

public class Headers {

    public static Logger LOGGER = H2DiameterIOHRouterFactory.LOGGER;

    private Map<Long, List<Header>> _headers = new HashMap<> ();
    private List<Header> _defHeaders = new ArrayList<> ();
    private boolean _defHeadersOnly;
    
    public Headers (){
    }

    public Headers init (String value) throws Exception {
	Map<String, Avp> _avps = new HashMap<> ();
	for (String line : ConfigHelper.getLines (value, "avp")){
	    Avp avp = new Avp ().parse (line);
	    _avps.put (avp._name, avp);
	    if (LOGGER.isDebugEnabled ())
		LOGGER.debug ("H2DiameterIOHRouterFactory : defined : "+avp);
	}
	for (String line : ConfigHelper.getLines (value, "header")){
	    Header header = new Header ().parse (line, _avps);
	    if (LOGGER.isDebugEnabled ())
		LOGGER.debug ("H2DiameterIOHRouterFactory : defined : "+header);
	    List<Header> list = null;
	    if (header._app == 0){
		list = _defHeaders;
	    }else{
		if (_headers == null) _headers = new HashMap<> ();
		list = _headers.get (header._app);
		if (list == null) _headers.put (header._app, list = new ArrayList<Header> ());
	    }
	    list.add (header);
	}
	_defHeadersOnly = (_headers == null);
	return this;
    }

    public HttpRequest.Builder decorate (DiameterIOHChannel client, DiameterMessage msg, HttpRequest.Builder builder){
	List<Header> list = null;
	if (_defHeadersOnly){
	    list = _defHeaders;
	}else{
	    list = _headers.get (msg.getApplicationID ());
	    if (list == null) list = _defHeaders;
	}
	for (Header header : list)
	    header.decorate (client, msg, builder);
	    
	return builder;
    }
    

    public static class Avp {

	private static enum Format {UTF8, UINT32, INT32, INT64}

	private Format _format;
	private String _name;
	private int _code;
	private int _vid;
	
	public Avp (){
	}
	public Avp parse (String value) throws Exception {
	    switch (ConfigHelper.getParam (value, "UTF8", "-f", "-format").toUpperCase ()){
	    case "UTF8":
	    case "UTF-8": _format = Format.UTF8; break;
	    case "UINT32": _format = Format.UINT32; break;
		//case "INT32": _format = Format.INT32; break;
		//case "INT64": _format = Format.INT64; break;
	    default: throw new IllegalArgumentException ("Invalid AVP format : "+ConfigHelper.getParam (value, "UTF8", "-f", "-format"));
	    }
	    
	    _name = ConfigHelper.getParam (value, true, "-name");
	    _code = ConfigHelper.getIntParam (value, "-code");
	    _vid = ConfigHelper.getIntParam (value, 0, "-vid", "-vendorid");
	    
	    return this;
	}

	public String extract (DiameterMessage msg){
	    byte[] data = msg.getBytes ();
	    int[] index = DiameterMessage.indexOf (_code, _vid, data, 0, data.length, true);
	    if (index == null) return null;
	    switch (_format){
	    case UTF8:
		if (index[3] == 0) return null;
		return new String (data, index[2], index[3], DiameterUtils.UTF8);
	    case UINT32:
		if (index[3] != 4) return null;
		return String.valueOf (DiameterParser.getUnsigned32 (data, index[2]));
	    }
	    return null;
	}

	public String toString (){
	    return new StringBuilder ()
		.append ("Avp[name=").append (_name).append (", code=").append (_code)
		.append (", vid=").append (_vid).append (", format=").append (_format)
		.append (']')
		.toString ();
	}
    }

    public static class Header {

	protected String _name;
	protected long _app;
	protected Avp _avp;
	protected String _def;
	
	public Header (){
	}
	public Header parse (String value, Map<String, Avp> avps) throws Exception {
	    if (ConfigHelper.getFlag (value, false, "-OriginHost")) return new OriginHostHeader ().parse (value);
	    if (ConfigHelper.getFlag (value, false, "-OriginRealm")) return new OriginRealmHeader ().parse (value);
	    if (ConfigHelper.getFlag (value, false, "-remoteIP")) return new RemoteIPHeader ().parse (value);
	    if (ConfigHelper.getFlag (value, false, "-remotePort")) return new RemotePortHeader ().parse (value);
	    if (ConfigHelper.getFlag (value, false, "-remoteProto")) return new RemoteProtoHeader ().parse (value);
	    _name = ConfigHelper.getParam (value, true, "-name");
	    _app = ConfigHelper.getIntParam (value, 0, "-app", "-appid", "-appId");
	    _def = ConfigHelper.getParam (value, null, "-def", "-default");
	    
	    // we may allow no avp if there is a default: this is to mark the request with a static header, for ex : X-Diameter: true
	    String s = ConfigHelper.getParam (value, _def == null, "-avp"); // required if def is null
	    if (s == null){
		return this;
	    }
	    _avp = avps.get (s);
	    if (_avp == null) throw new IllegalArgumentException ("Unknown avp in : "+value+" : "+s);
	    return this;
	}

	public String toString (){
	    return new StringBuilder ()
		.append ("Header[name=").append (_name).append (", app=").append (_app)
		.append (", avp=").append (_avp).append (", def=").append (_def)
		.append (']')
		.toString ();
	}

	public boolean decorate (DiameterIOHChannel client, DiameterMessage msg, HttpRequest.Builder builder){
	    String s = _avp != null ? _avp.extract (msg) : null;
	    if (s == null) s = _def;
	    if (s != null){
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug ("Adding header name="+_name+", value="+s);
		builder.header (_name, s);
		return true;
	    }
	    return false;
	}
    }

    public static class OriginHostHeader extends Header {
	public OriginHostHeader (){ super ();}
	public Header parse (String value) throws Exception {
	    _name = ConfigHelper.getParam (value, "x-diameter-originhost", "-name");
	    _app = ConfigHelper.getIntParam (value, 0, "-app", "-appid", "-appId");
	    return this;
	}
	@Override
	public String toString (){
	    return new StringBuilder ()
		.append ("OriginHostHeader[name=").append (_name)
		.append (']')
		.toString ();
	}
	@Override
	public boolean decorate (DiameterIOHChannel client, DiameterMessage msg, HttpRequest.Builder builder){
	    builder.header (_name, client.getOriginHost ());
	    return true;
	}
    }
    public static class OriginRealmHeader extends Header {
	public OriginRealmHeader (){ super ();}
	public Header parse (String value) throws Exception {
	    _name = ConfigHelper.getParam (value, "x-diameter-originrealm", "-name");
	    _app = ConfigHelper.getIntParam (value, 0, "-app", "-appid", "-appId");
	    return this;
	}
	@Override
	public String toString (){
	    return new StringBuilder ()
		.append ("OriginRealmHeader[name=").append (_name)
		.append (']')
		.toString ();
	}
	@Override
	public boolean decorate (DiameterIOHChannel client, DiameterMessage msg, HttpRequest.Builder builder){
	    builder.header (_name, client.getOriginRealm ());
	    return true;
	}
    }
    public static class RemoteIPHeader extends Header {
	public RemoteIPHeader (){ super ();}
	public Header parse (String value) throws Exception {
	    _name = ConfigHelper.getParam (value, "x-diameter-ip", "-name");
	    _app = ConfigHelper.getIntParam (value, 0, "-app", "-appid", "-appId");
	    return this;
	}
	@Override
	public String toString (){
	    return new StringBuilder ()
		.append ("RemoteIPHeader[name=").append (_name)
		.append (']')
		.toString ();
	}
	@Override
	public boolean decorate (DiameterIOHChannel client, DiameterMessage msg, HttpRequest.Builder builder){
	    if (client.getType () == DiameterIOHChannel.TYPE.TCP){
		TcpChannel channel = client.getChannel ();
		builder.header (_name, channel.getRemoteAddress ().getHostString ());
	    } else {
		try{
		    SctpChannel channel = client.getChannel ();
		    StringBuilder sb = new StringBuilder ();
		    boolean first = true;
		    Iterator<SocketAddress> it = (Iterator<SocketAddress>) channel.getRemoteAddresses ().iterator ();
		    while (it.hasNext ()){
			InetSocketAddress addr = (InetSocketAddress) it.next ();
			if (first) {
			    first = false;
			} else {
			    sb.append (',');
			}
			sb.append (addr.getHostString ());
		    }
		    builder.header (_name, sb.toString ());
		}catch(Exception e){
		    throw new RuntimeException ("Failed to retrieve remote sctp address", e);
		}
	    }
	    return true;
	}
    }
    public static class RemotePortHeader extends Header {
	public RemotePortHeader (){ super ();}
	public Header parse (String value) throws Exception {
	    _name = ConfigHelper.getParam (value, "x-diameter-port", "-name");
	    _app = ConfigHelper.getIntParam (value, 0, "-app", "-appid", "-appId");
	    return this;
	}
	@Override
	public String toString (){
	    return new StringBuilder ()
		.append ("RemotePortHeader[name=").append (_name)
		.append (']')
		.toString ();
	}
	@Override
	public boolean decorate (DiameterIOHChannel client, DiameterMessage msg, HttpRequest.Builder builder){
	    if (client.getType () == DiameterIOHChannel.TYPE.TCP){
		TcpChannel channel = client.getChannel ();
		builder.header (_name, String.valueOf (channel.getRemoteAddress ().getPort ()));
	    } else {
		SctpChannel channel = client.getChannel ();
		builder.header (_name, String.valueOf (channel.getRemotePort ()));
	    }
	    return true;
	}
    }
    public static class RemoteProtoHeader extends Header {
	public RemoteProtoHeader (){ super ();}
	public Header parse (String value) throws Exception {
	    _name = ConfigHelper.getParam (value, "x-diameter-proto", "-name");
	    _app = ConfigHelper.getIntParam (value, 0, "-app", "-appid", "-appId");
	    return this;
	}
	@Override
	public String toString (){
	    return new StringBuilder ()
		.append ("RemoteProtoHeader[name=").append (_name)
		.append (']')
		.toString ();
	}
	@Override
	public boolean decorate (DiameterIOHChannel client, DiameterMessage msg, HttpRequest.Builder builder){
	    if (client.getType () == DiameterIOHChannel.TYPE.TCP){
		TcpChannel channel = client.getChannel ();
		builder.header (_name, channel.isSecure () ? "tls" : "tcp");
	    } else {
		SctpChannel channel = client.getChannel ();
		builder.header (_name, channel.isSecure () ? "dtls" : "sctp");
	    }
	    return true;
	}
    }
}
