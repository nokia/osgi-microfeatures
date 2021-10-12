// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.recorder.impl;

import com.alcatel.as.service.recorder.*;
import java.util.*;
import java.time.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
    
public class RecordImpl implements Record {

    private String _name;
    private Map<String, Object> _props;
    private Event[] _events;
    private int _index = 0, _max, _inc, _preserveHead, _preserveTail;
    private RecorderServiceImpl _recorder;
    private ReentrantReadWriteLock _lock = new ReentrantReadWriteLock ();

    public RecordImpl (RecorderServiceImpl recorder, String name, Map<String, Object> props){
	_recorder = recorder;
	_name = name;
	if (props == null) props = new HashMap<> ();
	_props = props;

	int init = intProperty (RecorderService.RECORD_CAPACITY_INIT);
	_max = intProperty (RecorderService.RECORD_CAPACITY_MAX);
	_inc = _max / 8;
	_events = new Event[init];
	_preserveHead = intProperty (RecorderService.RECORD_PRESERVE_HEAD);
	_preserveTail = intProperty (RecorderService.RECORD_PRESERVE_TAIL);
    }

    public RecorderService service (){ return _recorder;}

    public String name (){ return _name;}

    public Map<String, Object> properties (){ return _props;}

    public Record record (Event event){
	try{
	    _lock.writeLock ().lock ();
	    if (_index == _events.length){ // need to expand
		if (_events.length == _max){
		    // max reached : need to truncate
		    int from = _preserveHead;
		    int to = _events.length - _preserveTail;
		    dismiss (from, to);
		    _events[_index++] = new Event ("Record compacted");
		} else {
		    // expand by _inc
		    int size = Math.min (_max, _events.length + _inc);
		    Event[] copy = new Event[size];
		    System.arraycopy (_events, 0, copy, 0, _index);
		    _events = copy;
		}
	    }
	    _events[_index++] = event;
	    return this;
	} finally {
	    _lock.writeLock ().unlock ();
	}
    }
    
    private Record dismiss (int from, int to){
	if (from == to) return this;
	if (from > to) throw new IllegalArgumentException (name ()+" : cannot dismiss : "+from+">"+to);
	if (from >= _index) return this;
	if (to >= _index){
	    _index = from;
	} else {
	    System.arraycopy (_events,
			      to,
			      _events,
			      from,
			      _events.length - to);
	    _index -= to - from;
	}
	for (int i = _index; i<_events.length; i++)
	    _events[i] = null;
	return this;
    }

    public Record dismiss (int index){ return dismiss (index, index+1);}

    public Record dismissBefore (LocalDateTime time){
	try{
	    _lock.writeLock ().lock ();
	    int firstok = _index;
	    for (int i=0; i<_index; i++){
		Event ev = _events[i];
		if (ev.time ().isAfter (time)){
		    firstok = i;
		    break;
		}
	    }
	    return dismiss (0, firstok);
	} finally {
	    _lock.writeLock ().unlock ();
	}
    }

    public Record dismissBefore (Duration duration){
	return dismissBefore (LocalDateTime.now ().minus (duration));
    }
    
    public Record destroy (){
	_recorder.destroy (this);
	return this;
    }
    
    public void iterate (java.util.function.BiConsumer<Integer, Event> f){
	try{
	    _lock.readLock ().lock ();
	    for (int i=0; i<_index; i++)
		f.accept (i, _events[i]);
	} finally {
	    _lock.readLock ().unlock ();
	}
    }

    private int intProperty (String prop){
	return _recorder.intProperty (_name, prop, _props);
    }

}
