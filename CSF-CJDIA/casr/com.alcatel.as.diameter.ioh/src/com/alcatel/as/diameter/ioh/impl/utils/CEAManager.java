package com.alcatel.as.diameter.ioh.impl.utils;

import com.alcatel.as.diameter.ioh.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.diameter.parser.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;

public class CEAManager {

    private int _active, _closed, _quorum, _maxClosed;
    private DiameterMessage _error;
    
    public CEAManager (int total, int quorum, int closed){
	_quorum = quorum;
	_closed = closed;
	_maxClosed = total - quorum;
    }
    public DiameterMessage getErrorCEA (){
	return _error;
    }
    // return 1 to send cea to client
    // return 0 to wait for more CEA
    // return -1 to ignore
    public int receivedCEA (MuxClient server, DiameterMessage cea){
	if (cea.getResultCode () == 2001){
	    return ++_active == _quorum ? 1 : 0;
	}
	_error = cea;
	server.getChannel ().close ();
	return -1;
    }
    // return true to terminate all
    // return false to ignore
    public boolean serverClosed (){
	return ++_closed > _maxClosed;
    }

    public String toString (){
	return new StringBuilder ()
	    .append (_maxClosed+_quorum) // total
	    .append ('-')
	    .append (_quorum)
	    .append ('-')
	    .append (_active)
	    .append ('-')
	    .append (_closed)
	    .toString ();
    }
}
