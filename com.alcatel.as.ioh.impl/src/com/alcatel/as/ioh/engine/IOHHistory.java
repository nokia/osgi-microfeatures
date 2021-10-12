// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine;

import java.util.*;
import java.time.format.DateTimeFormatter;
import com.alcatel.as.service.recorder.Event;

import static com.alcatel.as.util.helper.AnsiFormatter.*;

public class IOHHistory {

    protected DateTimeFormatter _dateFormat;
    protected ArrayList<Event> _history;
    protected boolean _active;

    public IOHHistory (IOHEngine engine, boolean active){
	_dateFormat = engine.getIOHServices ().getRecorderService ().getDateFormatter ();
	_active = active;
	if (_active){
	    _history = new ArrayList<> ();
	}
    }
    public boolean active (){ return _active;}

    public void history (String s){
	if (!_active) return;
	_history.add (new Event (s));
	checkSize ();
    }
    protected void checkSize (){
	if (_history.size () > 1024){ // for now, we limit to 1024 entries to avoid mem leak
	    _history.subList (256, 768).clear (); // we remove 512 in the middle (we keep the init and the recent entries)
	    history ("(History Compacted)");
	}
    }
    public StringBuilder getHistory (boolean prefix, StringBuilder sb){
	return getHistory (prefix, sb, false);
    }
    public StringBuilder getHistory (boolean prefix, StringBuilder sb, boolean pretty){
	if (sb == null) sb = new StringBuilder ();
	if (_active){
	    if (prefix){
		if (pretty)
		    sb.append (BOLD).append ("History :").append (BOLD_OFF).append ('\n');
		else
		    sb.append ("History :\n");
	    }
	    int index = 0;
	    for (Event event : _history){
		sb.append ('#').append (index++).append (":\t")
		    .append (_dateFormat.format (event.time ()))
		    .append (":\t")
		    .append (event.message ())
		    .append ('\n');
	    }
	}
	return sb;
    }
    
    
}
