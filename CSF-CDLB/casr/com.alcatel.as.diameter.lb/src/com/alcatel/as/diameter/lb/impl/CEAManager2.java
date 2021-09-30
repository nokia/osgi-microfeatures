package com.alcatel.as.diameter.lb.impl;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.ioh.client.*;

public class CEAManager2 {

    private DiameterMessage _koCEA, _okCEA;
    private int _expected, _ok, _ko, _quorum;

    public CEAManager2 (int expected, int quorum){ // use quorum=-1 to disable it (more responsive)
	_expected = expected;
	_quorum = quorum;
    }
    public DiameterMessage getKOCEA (){
	return _koCEA;
    }
    public DiameterMessage getOKCEA (){
	return _okCEA;
    }
    public int getOKSize (){
	return _ok;
    }
    
    // return 1 to return OK CEA
    // return 0 to wait for more callbacks
    // return -1 if the cea was an error
    public int receivedCEA (TcpClient.Destination server, DiameterMessage cea){
	if (cea.getResultCode () == 2001){
	    _ok++;
	    _okCEA = cea;
	    if ((_ok + _ko) == _expected) // all callbacks received
		return 1;
	    if (_ok == _quorum) // quorum nb of OK
		return 1;
	    return 0;
	}
	_koCEA = cea;
	server.getChannel ().close ();
	return -1;
    }
    // return 1 to return OK CEA
    // return 0 to wait for more callbacks
    // return -1 to return KO CEA
    public int serverClosed (boolean wasActive){
	_ko++;
	if (wasActive) _ok--;
	if ((_ok + _ko) == _expected)
	    return _okCEA != null ? 1 : -1;
	return 0;
    }

    public String toString (){
	return new StringBuilder ()
	    .append (_expected)
	    .append ('-')
	    .append (_ok)
	    .append ('-')
	    .append (_ko)
	    .toString ();
    }
}
