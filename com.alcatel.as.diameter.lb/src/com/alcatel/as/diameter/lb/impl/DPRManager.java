package com.alcatel.as.diameter.lb.impl;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.ioh.client.*;

import com.alcatel.as.service.concurrent.*;

import java.util.concurrent.*;

public class DPRManager {
    
    private static enum State {RECV_DPR, SENT_DPA, CLOSED
    }

    private DiameterMessage _dpr;
    private State _state = State.RECV_DPR;
    private Future _closeFuture, _dpaFuture;
    private TcpClient.Destination _server;
    private boolean _active; // indicates if the server was active when the DPR arrived : maybe the server responded non-2001 to CER
    private int _reason;

    public DPRManager (TcpClient.Destination server, DiameterMessage dpr, boolean active){
	_server = server;
	_dpr = dpr;
	_active = active;
	_reason = dpr.getIntAvp (273, 0, 0);
	if (doNotWantToTalkToYou ())
	    server.getTcpClient ().getDestinations ().remove (server);
    }

    public boolean doNotWantToTalkToYou (){ return _reason == 2;} // DO_NOT_WANT_TO_TALK_TO_YOU

    public boolean isActive (){
	return _active;
    }

    public void serverClosed (){
	_state = State.CLOSED;
	if (_dpaFuture != null){
	    _dpaFuture.cancel (false);
	    _dpaFuture = null;
	}
	if (_closeFuture != null){
	    _closeFuture.cancel (false);
	    _closeFuture = null;
	}
    }
    public void scheduledDPA (Future f){
	_dpaFuture = f;
    }
    public void sentDPA (PlatformExecutor exec){
	_dpaFuture = null;
	_state = State.SENT_DPA;
	scheduleClose (exec);
    }
    public void scheduleClose (PlatformExecutor exec){
	Runnable r = new Runnable (){
		public void run (){
		    _server.getChannel ().close ();
		    _closeFuture = null;
		}
	    };
	// we schedule a safety close for 100ms after we sent the DPA
	_closeFuture = exec.schedule (r, 100L, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public DiameterMessage makeDpa (DiameterMessage dwr){
	return DiameterUtils.makeDpa (dwr, _dpr);
    }

    // not interested in clientClosed --> close server connection
    
    // not interested in clientRequest --> request cannot be sent to this server
    
    // used to know if a clientResponse may be forwarded
    protected boolean dpaSent (){
	return _state != State.RECV_DPR;
    }
    
    // not interested in clientCER, clientDPR, clientDPA
    // not interested in serverRequest, serverResponse, serverCEA : clientcontext --> forward
    // not interested in serverDPR : clientcontext -> close server connection
    // not interested in serverDPA : clientcontext -> close server connection

}
