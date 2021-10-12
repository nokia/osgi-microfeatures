// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.router;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.log4j.Logger;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;

import org.osgi.framework.BundleContext;
import com.alcatel.as.diameter.lb.*;
import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.util.config.ConfigConstants;

public class Sessions {

    protected Map<Long, Policy> _policies = new HashMap<> ();
    protected Policy _defPolicy, _uniquePolicy;
    protected boolean _fastAny = false;

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.sessions");
    
    public Sessions (){
    }
    
    public void init (Map<String, String> conf){
	try{
	    boolean defaultNameUsed = false;
	    for (String line : ConfigHelper.getLines (conf.get (DefDiameterRouter.CONF_SESSION_POLICIES), "policy")){
		for (Policy policy : Policy.parse (line)){
		    _policies.put (policy._key, policy);
		    LOGGER.info ("Defined : "+policy);
		    defaultNameUsed |= policy._name.equals ("default");
		}
	    }
	    if (_policies.get (0L) == null){
		Policy policy = new Policy (defaultNameUsed ? null : "default", 0, 0, true, false, false, 0, 0);
		_policies.put (0L, policy); // set default : all requests stateless by def
		LOGGER.info ("Defined : "+policy);
	    }
	    _defPolicy = _policies.get (0L); // cannot be null
	    if (_policies.size () == 1) _uniquePolicy = _defPolicy; // optim if there is a single default policy : no need to look up
	    _fastAny = _uniquePolicy != null && _uniquePolicy._noState;
	}catch(Exception e){
	    LOGGER.error ("Failed to load session policies", e);
	}
	if (LOGGER.isInfoEnabled ()) LOGGER.info ("Session policies loaded : "+_policies);
    }

    public Destination getDestination (DiameterClient client, DiameterMessage msg, DestinationManager mgr){
	return getDestination (client, msg, mgr, 0, true);
    }
    public Destination getDestination (DiameterClient client, DiameterMessage msg, DestinationManager mgr, int id){
	return getDestination (client, msg, mgr, id, false);
    }
    private Destination getDestination (DiameterClient client, DiameterMessage msg, DestinationManager mgr, int id, boolean computeId){
	Destination server1 = mgr.getAny ();
	if (id == -1) return server1;
	if (_fastAny) return server1;
	if (server1 == null){
	    // there is no destination available - no need to go further
	    return null;
	}
	if (computeId){
	    Policy policy = _uniquePolicy; // if not null --> then stick with it
	    if (policy == null){
		long application = msg.getApplicationID () << 32;
		long key = application | (long) msg.getCommandCode ();
		policy = _policies.get (key);
		if (policy == null){
		    policy = _policies.get (application);
		    if (policy == null){
			policy = _defPolicy;
		    }
		    _policies.put (key, policy); // so later reqs with same key are found right away - the total nb cannot be huge (no risk of oomemory)
		}
	    }
	    // policy cannot be null from here
	    if (policy._noState) return server1;
	    id = getId (msg);
	}
	return mgr.get (id);
    }

    public int getId (DiameterMessage msg){
	if (_fastAny) return -1;
	Policy policy = _uniquePolicy; // if not null --> then stick with it
	if (policy == null){
	    long application = msg.getApplicationID () << 32;
	    long key = application | (long) msg.getCommandCode ();
	    policy = _policies.get (key);
	    if (policy == null){
		policy = _policies.get (application);
		if (policy == null){
		    policy = _defPolicy;
		}
		_policies.put (key, policy); // so later reqs with same key are found right away - the total nb cannot be huge (no risk of oomemory)
	    }
	}
	// policy cannot be null from here
	if (policy._noState) return -1;
	int idFrom, idLen;
	byte[] bytes = msg.getBytes ();
	if (policy._transState){
	    // we point to the end to end identifier (which may be more reliable for retrans than hop by hop)
	    idFrom = 16;
	    idLen = 4;
	} else {
	    int[] messageId = msg.indexOf (policy._avpId, policy._avpV);
	    if (messageId == null){
		LOGGER.debug ("Not found session id AVP : using transaction state");
		idFrom = 16;
		idLen = 4;
	    } else {
		idFrom = messageId[2];
		idLen = messageId[3];
		if (ImsiDecoder.isUsernameAvpCode (policy._avpId)){
		    String username = ImsiDecoder.getUsername (msg, bytes, idFrom, idLen);
		    bytes = username.getBytes (DiameterUtils.ASCII);
		    idFrom = 0;
		    idLen = bytes.length;
		}
	    }
	}
	return hash (bytes, idFrom, idLen);
    }

    public static int hash (byte[] bytes, int off, int len){
	int id = 0;
	int to = off + len;
	for (int k = off; k<to; k++){
	    // we lower-case on the fly
	    int b = bytes[k] & 0xFF;
	    if (b <= (int)'Z' && b >= (int)'A')
		b += 32;
	    id = 31 * id + b;
	}
	if (id == -1) id = 0; // avoid -1 here
	return id;
    }

    private static class Policy {
	private long _key;
	private String _name, _keyS, _toString;
	protected boolean _noState, _transState, _sessionState;
	protected int _avpV, _avpId;
	private Policy (String name, int app, int command, boolean noState, boolean transactionState, boolean sessionState, int avpV, int avpId){
	    _key = ((long)app) << 32;
	    _key |= (long) command;
	    _keyS = app+"/"+command;
	    _name = name != null ? name : _keyS;
	    _noState = noState;
	    _transState = transactionState;
	    _sessionState = sessionState;
	    _avpV = avpV;
	    _avpId = avpId;
	    String state = noState ? "NO_STATE" : (transactionState ? "TRANS_STATE" : ("SESSION_STATE/"+avpV+"/"+avpId));
	    _toString = "Policy[name="+_name+" app="+app+" cmd="+command+" "+state+"]";
	}
	public String toString (){
	    return _toString;
	}
	private static List<Policy> parse (String line){
	    // policy -name def -app 0 -noState
	    // policy -name p1 -app 123 -cmd 1 -cmd 2 -transactionState
	    // policy -name p2 -app 123 -cmd 3 -sessionState -avp 10415/45
	    // policy -name p2 -app 123 -cmd 0 -sessionState
	    List<Policy> policies = new ArrayList<> ();
	    int app = 0;
	    String appS = ConfigHelper.getParam (line, true, "-app", "-application");
	    List<String> codeL = ConfigHelper.getParams (line, false, "-cmd", "-command");
	    if (codeL.isEmpty ()) codeL.add ("0"); // all commands by default
	    if (appS.startsWith ("0x"))
		app= Integer.parseInt (appS.substring (2), 16);
	    else
		app= Integer.parseInt (appS);
	    for (String codeS : codeL){
		int code = 0;
		if (codeS.startsWith ("0x"))
		    code= Integer.parseInt (codeS.substring (2), 16);
		else
		    code= Integer.parseInt (codeS);
		String avp = ConfigHelper.getParam (line, "0/263", "-avp");
		policies.add (new Policy (ConfigHelper.getParam (line, null, "-name"), app, code,
					  ConfigHelper.getFlag (line, false, "-noState"),
					  ConfigHelper.getFlag (line, false, "-transactionState"),
					  ConfigHelper.getFlag (line, false, "-sessionState"),
					  Integer.parseInt (avp.substring (0, avp.indexOf ('/'))),
					  Integer.parseInt (avp.substring (avp.indexOf ('/')+1))
					  ));
	    }
	    return policies;
	}
    }

}
