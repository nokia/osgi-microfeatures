// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.test;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.net.*;
import java.io.*;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.DiameterUtils.Avp;
import com.alcatel.as.diameter.lb.impl.*;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import com.alcatel_lucent.as.management.annotation.config.*;

import com.alcatel.as.service.metering.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;

public class TestContext extends TcpMessageProcessor<DiameterMessage> {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.test.server");
    protected static final byte[] _2001 = DiameterUtils.setIntValue (2001, new byte[4], 0);
    protected static final byte[] _3001 = DiameterUtils.setIntValue (3001, new byte[4], 0);
    protected static final DiameterUtils.Avp _result2001 = new DiameterUtils.Avp (268, 0, true, _2001);
    protected static final DiameterUtils.Avp _result3001 = new DiameterUtils.Avp (268, 0, true, _3001);

    private boolean _respondDWR = true;
    private boolean _respondReq = true;
    private boolean _respondCER = true;
    private boolean _respondDPR = true;
    private boolean _closeOnDPR = false;
    private int _loadFactor = -1;

    private AtomicLong SERVER_IN_REQUESTS;

    private Map<String, Object> _props;

    public TestContext (Map<String, Object> props){
	super (new DiameterParser ());
	_props = props;
	SERVER_IN_REQUESTS = (AtomicLong) props.get ("SERVER_IN_REQUESTS");
    }

    public void messageReceived(TcpChannel cnx,
				DiameterMessage msg){
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug ("received : "+msg);
	byte[] sleep = msg.getAvp (DiameterTestApplication.AVP_SLEEP, (int)DiameterTestApplication.APP_ID);
	if (sleep != null){
	    int value = msg.getIntValue (sleep, 0, 4);
	    try{Thread.sleep (value);}catch(Exception e){}
	}
	if (msg.isRequest ()){
	    switch(msg.getType ()){
	    case Application:
		SERVER_IN_REQUESTS.incrementAndGet ();
		receivedRequest (cnx, msg); return;
	    case Capabilities: receivedCER (cnx, msg); return;
	    case Watchdog: receivedDWR (cnx, msg); return;
	    case Disconnection: receivedDPR (cnx, msg); return;
	    }
	}else{
	    switch(msg.getType ()){
	    case Application: receivedResponse (cnx, msg); return;
	    case Capabilities: receivedCEA (cnx, msg); return;
	    case Watchdog: receivedDWA (cnx, msg); return;
	    case Disconnection: receivedDPA (cnx, msg); return;
	    }
	}
    }

    private void receivedRequest (TcpChannel cnx, DiameterMessage req){
	if (_respondReq && DiameterTestApplication.process (req)){
	    DiameterMessage resp = makeResponse (req);
	    send (cnx, resp);
	}
	byte[] loadFactor = req.getAvp (DiameterTestApplication.AVP_LOAD_FACTOR, (int)DiameterTestApplication.APP_ID);
	if (loadFactor != null){
	    ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	    byte[] value = null;
	    try{
		new Avp (73, 1751, false, loadFactor).writeTo (baos);
		value = baos.toByteArray ();
	    }catch(IOException e){}
	    DiameterMessage dwr = DiameterUtils.makeRequest (0, 280,
							     new Avp (264, 0, true, req.getAvp (264, 0)),
							     new Avp (296, 0, true, req.getAvp (296, 0)),
							     new Avp (72, 1751, false, value)
							     );
	    send (cnx, dwr);
	}
	if (req.getApplicationID () == DiameterTestApplication.APP_ID){
	    switch (req.getCommandCode ()){
	    case DiameterTestApplication.COMMAND_IGNORE_REQ: _respondReq = false; break;
	    case DiameterTestApplication.COMMAND_PROCESS_REQ: _respondReq = true; break;
	    case DiameterTestApplication.COMMAND_IGNORE_CER: _respondCER = false; break;
	    case DiameterTestApplication.COMMAND_PROCESS_CER: _respondCER = true; break;
	    case DiameterTestApplication.COMMAND_IGNORE_DWR: _respondDWR = false; break;
	    case DiameterTestApplication.COMMAND_PROCESS_DWR: _respondDWR = true; break;
	    case DiameterTestApplication.COMMAND_IGNORE_DPR: _respondDPR = false; break;
	    case DiameterTestApplication.COMMAND_PROCESS_DPR: _respondDPR = true; break;
	    case DiameterTestApplication.COMMAND_CLOSE_ON_DPR: _closeOnDPR = true; break;
	    case DiameterTestApplication.COMMAND_SEND_REQ_1:
		    DiameterMessage newreq = DiameterUtils.makeRequest ((int)DiameterTestApplication.APP_ID, DiameterTestApplication.COMMAND_SEND_REQ_1);
		    send (cnx, newreq);
		    break;
	    }
	    doSendDPR (cnx, req);
	}
    }

    private void receivedCER (TcpChannel cnx, DiameterMessage req){
	if (_respondCER && DiameterTestApplication.process (req)){
	    DiameterUtils.Avp resp = _result2001;
	    String tmp;
	    if (_props.get ("test.cer.close") != null){
		cnx.close ();
		return;
	    }
	    if (_props.get ("test.cer.reject") != null){
		resp = _result3001;
	    }
	    if ((tmp=(String)_props.get ("test.cer.sleep")) != null){
		try{Thread.sleep (Integer.parseInt (tmp));}catch(Exception e){}
	    }
	    //DiameterMessage cea = DiameterUtils.makeResponse (req, _result2001); // we dont use echo and fillback
	    DiameterMessage cea = DiameterUtils.makeMessage (false,
							     req.getApplicationID (),
							     req.getCommandCode (),
							     req.getHopIdentifier (), req.getEndIdentifier (),
							     new Avp (264, 0, true, "host.domain.com".getBytes ()), // set orighost with desthost
							     new Avp (296, 0, true, "domain.com".getBytes ()), // set origrealm with destrealm
							     resp,
							     new Avp (257, 0, true, req.getAvp (257, 0)),
							     new Avp (266, 0, true, req.getAvp (266, 0))
							     );	
	    send (cnx, cea);
	    doSendDPR (cnx, req);
	}
    }
    private void receivedDWR (TcpChannel cnx, DiameterMessage req){
	if (_respondDWR && DiameterTestApplication.process (req)){
	    DiameterMessage dwa = makeResponse (req);
	    send (cnx, dwa);
	}
    }
    private void receivedDPR (TcpChannel cnx, DiameterMessage req){
	if (_closeOnDPR){
	    cnx.close ();
	    return;
	}
	if (_respondDPR && DiameterTestApplication.process (req)){
	    DiameterMessage dpa = makeResponse (req);
	    send (cnx, dpa);
	}
    }

    private void receivedResponse (TcpChannel cnx, DiameterMessage resp){
	if (resp.getApplicationID () == DiameterTestApplication.APP_ID){
	    switch (resp.getCommandCode ()){
	    case DiameterTestApplication.COMMAND_SEND_REQ_1:
		DiameterMessage req = DiameterUtils.makeRequest ((int)DiameterTestApplication.APP_ID, DiameterTestApplication.COMMAND_SEND_REQ_2);
		send (cnx, req);
	    }
	}
    }
    private void receivedCEA (TcpChannel cnx, DiameterMessage req){
    }
    private void receivedDWA (TcpChannel cnx, DiameterMessage req){
    }
    private void receivedDPA (TcpChannel cnx, DiameterMessage req){
	    if (DiameterTestApplication.process (req))
		    cnx.close ();
    }

    private DiameterMessage makeResponse (DiameterMessage req){
	List<DiameterUtils.Avp> avps = getResponseAvps (req);
	return DiameterUtils.makeResponse (req, avps.toArray (new DiameterUtils.Avp[avps.size ()]));
    }
    private List<DiameterUtils.Avp> getResponseAvps (DiameterMessage req){
	List<DiameterUtils.Avp> avps = new ArrayList<DiameterUtils.Avp> ();
	byte[] echo = req.getAvp (DiameterTestApplication.AVP_ECHO, (int)DiameterTestApplication.APP_ID);
	if (echo != null)
	    avps.add (new DiameterUtils.Avp (DiameterTestApplication.AVP_ECHO, (int)DiameterTestApplication.APP_ID, false, echo));
	byte[] fillback = req.getAvp (DiameterTestApplication.AVP_FILLBACK, (int)DiameterTestApplication.APP_ID);
	if (fillback != null)
	    avps.add (new DiameterUtils.Avp (DiameterTestApplication.AVP_FILLBACK, (int)DiameterTestApplication.APP_ID, false, new byte[req.getIntValue (fillback, 0, 4)]));
	byte[] buffer = req.getAvp (DiameterTestApplication.AVP_TCP_BUFFER_BACK, (int)DiameterTestApplication.APP_ID);
	if (buffer != null)
	    avps.add (new DiameterUtils.Avp (DiameterTestApplication.AVP_TCP_BUFFER, (int)DiameterTestApplication.APP_ID, false, buffer));
	avps.add (_result2001);
	return avps;
    }

    private void send (TcpChannel cnx, DiameterMessage msg){
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug ("sending : "+msg);	
	cnx.send (msg.getBytes (), false);
    }

    private void doSendDPR (TcpChannel cnx, DiameterMessage msg){
	if (msg.getAvp (DiameterTestApplication.AVP_SEND_DPR, (int) DiameterTestApplication.APP_ID) != null){
	    int reason = msg.getIntAvp (DiameterTestApplication.AVP_SEND_DPR, (int) DiameterTestApplication.APP_ID, 0);
	    try{Thread.sleep (100);}catch(Exception e){}
	    DiameterMessage dpr = DiameterUtils.makeRequest (0, 282, new DiameterUtils.Avp (273, 0, true, DiameterUtils.getIntValue(reason)));
	    send (cnx, dpr);
	}
    }
}
