// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.ioh.impl;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.felix.service.command.CommandProcessor;

import org.osgi.framework.BundleContext;

import java.util.Hashtable;
import java.util.Vector;
import java.util.List;
import jsr166e.CompletableFuture;
import java.util.concurrent.Phaser;

import com.alcatel.as.ioh.engine.*;

public class DiameterIOHGogoCommands {

    private DiameterIOHEngine _engine;

    public DiameterIOHGogoCommands (DiameterIOHEngine engine){
	_engine = engine;
    }

    public void register (BundleContext osgi){
	Hashtable<String, Object> props = new Hashtable<>();
        props.put(CommandProcessor.COMMAND_SCOPE, _engine.fullName ());
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] {
		"disconnect", "prepareShutdown"
	    });
	osgi.registerService (Object.class.getName(), this, props);
    }

    @Descriptor("Prepares a shutdown.")
    public void prepareShutdown (
			 ) {
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    _engine.history ("prepare shutdown");
		    _engine.getProperties ().put (IOHEngine.PROP_EXT_SERVER_MIN, "0");
		    cf.complete ("Done");
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }

    @Descriptor("Disconnects diameter channels.")
    public void disconnect (@Descriptor("A channel Type to filter channels : all, tcp, sctp, tcp-in, tcp-out, sctp-in, sctp-out") 
		       @Parameter(names = { "-type" }, absentValue = "") 
		       final String type,
		       @Descriptor("A channel id") 
		       @Parameter(names = { "-id" }, absentValue = "") 
		       final String id
		       ) {
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    _engine.history ("disconnect "+type);
		    StringBuilder sb = new StringBuilder ();
		    for (final IOHChannel channel : _engine.getDiameterTcpChannels ().values ()){
			if (match (channel, type, id)){
			    sb.append ("Closing : ").append (channel).append ('\n');
			    _engine.closeConnection (channel, "Connection closed by IOH via gogo");
			}
		    }
		    for (final IOHChannel channel : _engine.getDiameterSctpChannels ().values ()){
			if (match (channel, type, id)){
			    sb.append ("Closing : ").append (channel).append ('\n');
			    _engine.closeConnection (channel, "Connection closed by IOH via gogo");
			}
		    }
		    cf.complete (sb.toString ());
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }
    
    private boolean match (IOHChannel channel, String type, String id){
	if (type.length () > 0){
	    switch (type.toLowerCase ()){
	    case "all" : return true;
	    case "tcp" : return (channel instanceof IOHTcpChannel || channel instanceof IOHTcpClientChannel);
	    case "tcp-in" : return (channel instanceof IOHTcpChannel);
	    case "tcp-out" : return (channel instanceof IOHTcpClientChannel);
	    case "sctp" : return (channel instanceof IOHSctpChannel || channel instanceof IOHSctpClientChannel);
	    case "sctp-in" : return (channel instanceof IOHSctpChannel);
	    case "sctp-out" : return (channel instanceof IOHSctpClientChannel);
	    }
	    return false;
	}
	if (id.length () > 0){
	    return String.valueOf (channel.getSockId ()).equals (id);
	}
	return true;
    }
}
