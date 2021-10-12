// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.httpgw;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import org.osgi.service.component.annotations.*;
import org.apache.log4j.Logger;
import com.alcatel_lucent.as.management.annotation.config.*;

import com.nextenso.proxylet.diameter.*;
import com.nextenso.proxylet.diameter.client.*;
import com.nextenso.proxylet.diameter.util.*;

import com.nextenso.diameter.agent.impl.DiameterRequestFacade;

@Component(service = DiameterRestEndpoint.class, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate=true)
@Path("diameter")
public class DiameterRestEndpoint {
    
    public static Logger LOGGER = Logger.getLogger ("as.ioh.diameter.rest");

    public static final String ATTR_HEADER = "http.header.";

    @FileDataProperty(title="Http Response Headers",
		      fileData="httpgwHeaders.txt",
		      required=true,
		      dynamic=false,
		      section="General",
		      help="Describes the http headers to add to the http responses.")
    public final static String CONF_RESP_HEADERS = "httpgw.resp.headers";

    @BooleanProperty(title = "Instantiate a Client Peer", section = "Client Peer Management", help = "Specifies if a dummy Client Peer should be instantiated per request. If not, the Local Peer is used as Client.", required = false, dynamic = false, defval = true)
    public static final String CONF_CLIENT_PEER_NEW = "httpgw.peer.new";
    @StringProperty(title = "Http Header for Client Peer OriginHost", section = "Client Peer Management", help = "Specifies the http header carrying the OriginHost.", required = true, dynamic = false, defval = "x-diameter-originhost")
    public static final String CONF_CLIENT_PEER_ORIGIN_HOST = "httpgw.peer.diameter.origin.host";
    @StringProperty(title = "Http Header for Client Peer OriginRealm", section = "Client Peer Management", help = "Specifies the http header carrying the OriginRealm.", required = true, dynamic = false, defval = "x-diameter-originrealm")
    public static final String CONF_CLIENT_PEER_ORIGIN_REALM = "httpgw.peer.diameter.origin.realm";
    @StringProperty(title = "Http Header for Client Peer IPs", section = "Client Peer Management", help = "Specifies the http header carrying the peer IP(s).", required = true, dynamic = false, defval = "x-diameter-ip")
    public static final String CONF_CLIENT_PEER_IP = "httpgw.peer.connection.ip";
    @StringProperty(title = "Http Header for Client Peer Port", section = "Client Peer Management", help = "Specifies the http header carrying the peer port.", required = true, dynamic = false, defval = "x-diameter-port")
    public static final String CONF_CLIENT_PEER_PORT = "httpgw.peer.connection.port";
    @StringProperty(title = "Http Header for Client Peer Protocol", section = "Client Peer Management", help = "Specifies the http header carrying the peer protocol.", required = true, dynamic = false, defval = "x-diameter-proto")
    public static final String CONF_CLIENT_PEER_PROTO = "httpgw.peer.connection.proto";

    @BooleanProperty(title = "Enable Redirection", section = "General", help = "Specifies if diameter redirections should be reflected in http.", required = true, dynamic = false, defval = true)
    public static final String CONF_RESP_REDIRECT = "httpgw.resp.redirect";


    private DiameterClientFactory _clientF;
    private DiameterClient _client;
    private DiameterPeerTable _diamPT;
    private Headers _headers;
    private String _originHostHeader, _originRealmHeader, _ipHeader, _portHeader, _protoHeader;
    private boolean _newClientPeer, _redirectEnabled;

    @Reference
    public void setDiameterClientFactory (DiameterClientFactory clientF){
	_clientF = clientF;
    }
    @Reference
    public void setDiameterPeerTable (DiameterPeerTable pt){
	_diamPT = pt;
    }
    
    @Activate
    public void activate (Map<String, String> conf){
	try{
	    _headers = new Headers ().init ((String) conf.get (CONF_RESP_HEADERS));
	    _newClientPeer = Boolean.parseBoolean (getConfString (CONF_CLIENT_PEER_NEW, conf, "true"));
	    _originHostHeader = getConfString (CONF_CLIENT_PEER_ORIGIN_HOST, conf, "x-diameter-originhost");
	    _originRealmHeader = getConfString (CONF_CLIENT_PEER_ORIGIN_REALM, conf, "x-diameter-originrealm");
	    _ipHeader = getConfString (CONF_CLIENT_PEER_IP, conf, "x-diameter-ip");
	    _portHeader = getConfString (CONF_CLIENT_PEER_PORT, conf, "x-diameter-port");
	    _protoHeader = getConfString (CONF_CLIENT_PEER_PROTO, conf, "x-diameter-proto");
	    _redirectEnabled = Boolean.parseBoolean (getConfString (CONF_RESP_REDIRECT, conf, "true"));
	}catch(Exception e){
	    LOGGER.error (this+" : invalid configuration", e);
	}
    }
    private static final String getConfString  (String name, Map<String, String> conf, String def){
	Object o = conf.get (name);
	if (o == null) return def;
	String s = o.toString ();
	if (s.length () == 0) return def;
	return s.toLowerCase ();
    }
    

    @POST
    @Produces("application/octet-stream")
    public void doClientRequest(@QueryParam("local") String local,
				@Context HttpHeaders headers,
				byte[] data,
				@Suspended AsyncResponse asyncResponse) {

	MultivaluedMap<String,String> headersMap = headers.getRequestHeaders ();
	String destOriginHost = headersMap.getFirst ("destOriginHost");
	String destRealm = headersMap.getFirst ("destRealm");
	
	DiameterPeer localPeer = (local == null || local.length () == 0) ? _diamPT.getLocalDiameterPeer () : _diamPT.getLocalDiameterPeer (local);
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug ("DiameterRestEndpoint :  doClientRequest : local peer = "+localPeer+" destOriginHost="+destOriginHost+" destRealm="+destRealm);
	if (localPeer == null){
	    asyncResponse.resume(Response
				 .status(404)
				 .header ("Warning", "Local peer not found : "+local)
				 .build());
	    return;
	}

	int offset = 1; // skip version
	int length = DiameterParser.getUnsigned24 (data, offset);
	offset += 3;
	int flags = data[offset++] & 0xFF;
	int code = DiameterParser.getUnsigned24 (data, offset);
	offset += 3;

	final int fcode = code;
	final long appId = DiameterParser.getUnsigned32(data, offset);
	offset += 4;
	final int hopIdentifier = (int) DiameterParser.getUnsigned32(data, offset);
	offset += 4;
	final int endIdentifier = (int) DiameterParser.getUnsigned32(data, offset);
	offset += 4;

	try{
	    DiameterClient client = _clientF.newDiameterClient (localPeer,
								destOriginHost != null ? destOriginHost : localPeer.getOriginHost (),
								destRealm != null ? destRealm : localPeer.getOriginRealm (),
								0, appId, DiameterClient.TYPE_ALL, true, 0);
	    
	    DiameterClientRequest req = client.newRequest (code, true);
	    
	    MyHandler handler = new MyHandler (req);
	    DiameterParser.parseMessage (data, 0, data.length, handler);

	    for(String header : headersMap.keySet()){
		req.setAttribute (ATTR_HEADER+header, headersMap.getFirst (header));
	    }

	    if (_newClientPeer)
		((DiameterRequestFacade) req).setApplicationClientPeer (new ClientPeer (headersMap.getFirst (_originHostHeader),
											headersMap.getFirst (_originRealmHeader),
											headers.getRequestHeader (_ipHeader),
											headersMap.getFirst (_portHeader),
											headersMap.getFirst (_protoHeader)
											));

	    req.execute (new DiameterClientListener (){
		    public void handleResponse(DiameterClientRequest request,
					       DiameterClientResponse response){
			if (LOGGER.isDebugEnabled ()) LOGGER.debug ("DiameterRestEndpoint : handleResponse : result="+response.getResultCode ());
			setDefaultAVPs (request, response);
			List<DiameterUtils.Avp> list = new ArrayList<> ();
			Enumeration e = response.getDiameterAVPs ();
			while (e.hasMoreElements ()){
			    DiameterAVP avp = (DiameterAVP) e.nextElement ();
			    for (int i=0; i<avp.getValueSize (); i++){
				list.add (new DiameterUtils.Avp ((int) avp.getAVPCode (),
								 (int) avp.getVendorId (),
								 avp.mFlagSet (),
								 avp.getValue (i)
								 ));
			    }
			}
			DiameterMessage resp = DiameterUtils.makeMessage (false, appId, fcode, hopIdentifier, endIdentifier, list);
			int status = 200;
			String redirect = null;
			if (_redirectEnabled && response.getResultCode () == DiameterBaseConstants.RESULT_CODE_DIAMETER_REDIRECT_INDICATION){
			    status = 302;
			    DiameterAVP avp = response.getDiameterAVP (DiameterBaseConstants.AVP_REDIRECT_HOST);
			    if (avp != null && avp.getValueSize () == 1){
				String location = UTF8StringFormat.INSTANCE.getUtf8String (avp.getValue (0));
				if (location.startsWith ("http")){
				    redirect = location;
				    if (LOGGER.isDebugEnabled ()) LOGGER.debug ("DiameterRestEndpoint : handleResponse : redirect to : "+redirect);
				}
			    } else
				if (LOGGER.isDebugEnabled ()) LOGGER.debug ("DiameterRestEndpoint : handleResponse : redirect location not specified");
			}
			Response.ResponseBuilder builder = Response
			    .status(status)
			    .entity (resp.getBytes ());
			e = response.getAttributeNames ();
			while (e.hasMoreElements ()){
			    String name = (String) e.nextElement ().toString ();
			    if (name.startsWith (ATTR_HEADER)){
				builder.header (name.substring (ATTR_HEADER.length ()), response.getAttribute (name).toString ());
			    }
			}
			_headers.decorate (resp, builder);
			if (redirect != null) builder.header ("Location", redirect);
			asyncResponse.resume(builder.build());
		    }
		    public void handleException(DiameterClientRequest request,
						java.io.IOException ioe){
			if (LOGGER.isInfoEnabled ()) LOGGER.info ("DiameterRestEndpoint : handleException : "+ioe);
			asyncResponse.resume(Response
					     .status(500)
					     .header ("Warning", ioe.toString ())
					     .build());
		    }
		});
	    
	}catch(Exception e){
	    asyncResponse.resume(Response
				 .status(500)
				 .header ("Warning", e.toString ())
				 .build());
	    return;
	}
    }

    private static class MyHandler implements DiameterParser.Handler {
	private DiameterClientRequest _req;
	private MyHandler (DiameterClientRequest req){
	    _req = req;
	}
	public boolean newAVP (long code, long vendorId, int flags, byte[] data, int off, int len){
	    DiameterAVP avp = _req.getDiameterAVP(code, vendorId);
	    if (avp == null) {
		avp = new DiameterAVP(code, vendorId, flags);
		_req.addDiameterAVP(avp);
	    }
	    avp.addValue(data, off, len, flags, true);
	    return false;
	}
	public void flags (int flags){}
	public void code (int code){}
	public void application (long id){}
	public void hopId (long id){}
	public void endId (long id){}
    }

    private static void setDefaultAVPs(DiameterClientRequest request, DiameterClientResponse response) {
	int index = 0;
    	// add session-id AVP
	DiameterAVP avp = response.getDiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
	if (avp == null){
	    avp = request.getDiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
	    if (avp != null) {
		response.addDiameterAVP(0, (DiameterAVP) avp.clone());
		index = 1;
	    }
	} else
	    index = 1;
    	
    	// add application-id AVP
	avp = response.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
	if (avp == null){
	    avp = request.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
	    if (avp != null) {
		response.addDiameterAVP(index, (DiameterAVP) avp.clone());
		return;
	    }
	} else
	    return;

	avp = response.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
	if (avp == null){
	    avp = request.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
	    if (avp != null) {
		response.addDiameterAVP(index, (DiameterAVP) avp.clone());
		return;
	    }
	} else
	    return;

	avp = response.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
	if (avp == null){
	    avp = request.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
	    if (avp != null) {
		response.addDiameterAVP(index, (DiameterAVP) avp.clone());
		return;
	    }
	}
    }

}
