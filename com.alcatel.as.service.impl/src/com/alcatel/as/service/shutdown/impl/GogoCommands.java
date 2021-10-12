// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.shutdown.impl;

import java.util.*;

import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.shutdown.*;
import org.apache.felix.service.command.*;

@Component(service={Object.class}, property = {
	CommandProcessor.COMMAND_SCOPE + "=casr.service.shutdown",
	CommandProcessor.COMMAND_FUNCTION + "=shutdown",
	CommandProcessor.COMMAND_FUNCTION + "=halt",
	CommandProcessor.COMMAND_FUNCTION + "=shutdownEvent",
	"casr."+CommandProcessor.COMMAND_SCOPE+".alias" + "=asr.service"
})
@Descriptor("ShutdownService")
public class GogoCommands {
    
    private ShutdownService _shutdownService;

    @Reference
    public void setShutdownService (ShutdownService service){
	_shutdownService = service;
    }

    @Activate
    public void activate (){
    }
    
    @Descriptor("Triggers a shutdown")
    public void shutdown(CommandSession session,
			 @Descriptor("Indicates the exit status")
			 @Parameter(names = { "-exit"}, absentValue = "0")
			 int exit
			 ){
	System.out.println (_shutdownService.shutdown ("Gogo", exit)? "OK - shutting down" : "KO - already shutting down");
    }
    @Descriptor("Sends a shutdown event")
    public void shutdownEvent(CommandSession session,
			      @Descriptor("Indicates the instance name involved")
			      @Parameter(names = { "-instance", "-i"}, absentValue = "") 
			      String instanceName,
			      @Descriptor("Indicates the instance id involved")
			      @Parameter(names = { "-id"}, absentValue = "") 
			      String instanceId,
			      @Descriptor("Indicates the shutdown delay in seconds")
			      @Parameter(names = { "-delay"}, absentValue = "") 
			      String delay
			      ){
	HashMap<String, String> opts = new HashMap<> ();
	if (instanceName.length () > 0) opts.put (ShutdownService.SHUTDOWN_TARGET_INSTANCE_NAME, instanceName);
	if (instanceId.length () > 0) opts.put (ShutdownService.SHUTDOWN_TARGET_INSTANCE_ID, instanceId);
	if (delay.length () > 0) opts.put (ShutdownService.SHUTDOWN_DELAY, delay);
	_shutdownService.sendShutdownEvent (opts);
	System.out.println ("Sent shutdown event : "+opts);
    }
    @Descriptor("Triggers a halt")
    public void halt(CommandSession session,
		     @Descriptor("Indicates the exit status")
		     @Parameter(names = { "-exit"}, absentValue = "0")
		     int exit,
		     @Descriptor("Indicates if threads should be dumped")
		     @Parameter(names = { "-dump"}, absentValue = "true") 
		     boolean dump
		     ){
	_shutdownService.halt (exit, dump);
    }
}
