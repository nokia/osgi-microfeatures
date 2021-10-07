package com.alcatel.as.diameter.ioh.impl.utils;

import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

import java.util.concurrent.*;

public class DPRManager {
    
    private static enum State {RECV_DPR, SENT_DPA, CLOSED
    }

    private DiameterMessage _dpr;
    private State _state = State.RECV_DPR;
    private Future _dpaFuture;
    private MuxClient _server;
    private boolean _active; // indicates if the server was active when the DPR arrived : maybe the server responded non-2001 to CER
    private int _reason;

    public DPRManager (MuxClient server, DiameterMessage dpr, boolean active){
	_server = server;
	_dpr = dpr;
	_active = active;
	_reason = dpr.getIntAvp (273, 0, 0);
    }

    public boolean isActive (){
	return _active;
    }

    public void serverClosed (){
	_state = State.CLOSED;
	if (_dpaFuture != null){
	    _dpaFuture.cancel (false);
	    _dpaFuture = null;
	}
    }
    public void scheduledDPA (Future f){
	_dpaFuture = f;
    }
    public void sentDPA (PlatformExecutor exec){
	_dpaFuture = null;
	_state = State.SENT_DPA;
    }

    public DiameterMessage makeDpa (DiameterMessage dwr){
	return DiameterUtils.makeDpa (dwr, _dpr);
    }

    // not interested in clientClosed --> close server connection
    
    // not interested in clientRequest --> request cannot be sent to this server
    
    // used to know if a clientResponse may be forwarded
    public boolean dpaSent (){
	return _state != State.RECV_DPR;
    }
    
    // not interested in clientCER, clientDPR, clientDPA
    // not interested in serverRequest, serverResponse, serverCEA : clientcontext --> forward
    // not interested in serverDPR : clientcontext -> close server connection
    // not interested in serverDPA : clientcontext -> close server connection

}
