// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.lb.plugins;

import com.alcatel.as.ioh.lb.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.nio.*;

import com.alcatel.as.ioh.client.TcpClient.Destination;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.ConfigurationPolicy;

@Component(immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL, property={"parser.id=direct"})
public class DirectParserFactory implements ParserFactory {

    public static final String PROP_DELAY = "parser.init.delay";

    @Activate
    public void start (){
    }

    public Object newParserConfig (Map<String, Object> props){
	return new DirectParserConfig (props);
    }

    public Parser newParser (Object config, int neededBuffer){
	DirectParserConfig dpg = (DirectParserConfig) config;
	if (!dpg._delaying) return INSTANCE_NO_DELAY;
	return new DirectParser (dpg);
    }
    
    public String toString (){ return "DirectParserFactory[id=direct]";}

    

    protected static class DirectParserConfig {
	
	private long _delay = -1L;
	private boolean _delaying = false;

	protected DirectParserConfig (Map<String, Object> props){
	    String delay = (String) props.get (PROP_DELAY);
	    if (delay != null){
		_delay = (long) Integer.parseInt (delay);
		_delaying = true;
	    }
	}
    }

    public static final DirectParser INSTANCE_NO_DELAY = new DirectParser ();

    protected static class DirectParser implements Parser {

	private long _delay = -1L;
	private boolean _delaying = false;
	private long _startDelay = -1L;

	protected DirectParser (){} // for INSTANCE_NO_DELAY

	protected DirectParser (DirectParserConfig config){
	    _delay = config._delay;
	    _delaying = config._delaying;
	}

	public Chunk parse (java.nio.ByteBuffer buffer){
	    if (buffer.remaining () == 0) return null;
	    boolean first = true;
	    if (_delaying){
		if (_startDelay == -1L){
		    _startDelay = System.currentTimeMillis ();
		} else {
		    if (_delay == -1L){
			// infinite delay --> shortcut
			first = false;
		    } else {
			long now = System.currentTimeMillis ();
			long delay = now - _startDelay;
			if (delay > _delay){
			    _delaying = false;
			} else {
			    first = false;
			}
		    }
		}
	    }
	    ByteBuffer duplicate = buffer.duplicate ();
	    buffer.position (buffer.limit ());
	    return new Chunk (first).setData (duplicate, false).setDescription ("Direct");
	}
	
    }
}
