// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.httpgw;

import java.util.*;
import org.apache.log4j.Logger;
import javax.ws.rs.core.Response;

public class Headers {

    public static Logger LOGGER = DiameterRestEndpoint.LOGGER;

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
		LOGGER.debug ("DiameterRestEndpoint : defined : "+avp);
	}
	for (String line : ConfigHelper.getLines (value, "header")){
	    Header header = new Header ().parse (line, _avps);
	    if (LOGGER.isDebugEnabled ())
		LOGGER.debug ("DiameterRestEndpoint : defined : "+header);
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

    public Response.ResponseBuilder decorate (DiameterMessage msg, Response.ResponseBuilder builder){
	List<Header> list = null;
	if (_defHeadersOnly){
	    list = _defHeaders;
	}else{
	    list = _headers.get (msg.getApplicationID ());
	    if (list == null) list = _defHeaders;
	}
	for (Header header : list)
	    header.decorate (msg, builder);
	    
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

	private String _name;
	private long _app;
	private Avp _avp;
	private String _def;
	
	public Header (){
	}
	public Header parse (String value, Map<String, Avp> avps) throws Exception {
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

	public boolean decorate (DiameterMessage msg, Response.ResponseBuilder builder){
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
}
