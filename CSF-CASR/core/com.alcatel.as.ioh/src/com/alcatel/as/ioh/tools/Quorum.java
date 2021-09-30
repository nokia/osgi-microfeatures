package com.alcatel.as.ioh.tools;

import java.util.*;
import java.net.*;
import java.nio.*;

public class Quorum {

    public static enum Transition {
	UNREACHED_REACHED(true, true),
	    REACHED_REACHED(true, false),
	    UNREACHED_UNREACHED(false, false),
	    REACHED_UNREACHED(false, true);
	private boolean _reached, _changed;
	private Transition (boolean reached, boolean changed){
	    _reached = reached;
	    _changed = changed;
	}
	public boolean changed (){ return _changed; }
	public boolean reached (){ return _reached; }
    }

    private int _present, _quorum;

    public Quorum (int quorum){
	_quorum = quorum;
    }

    public int getQuorum (){
	return _quorum;
    }

    public int present (){
	return _present;
    }

    public boolean reached (){
	return _present >= _quorum;
    }
 
    public Transition joined (){
	if (++_present == _quorum) return Transition.UNREACHED_REACHED;
	return _present < _quorum ? Transition.UNREACHED_UNREACHED : Transition.REACHED_REACHED;
    }
    
    public Transition left (){
	if (_present-- == _quorum) return Transition.REACHED_UNREACHED;
	return _present >= _quorum ? Transition.REACHED_REACHED : Transition.UNREACHED_UNREACHED;
    }

    public Transition unchanged (){
	if (reached ()) return Transition.REACHED_REACHED;
	return Transition.UNREACHED_UNREACHED;
    }
}
