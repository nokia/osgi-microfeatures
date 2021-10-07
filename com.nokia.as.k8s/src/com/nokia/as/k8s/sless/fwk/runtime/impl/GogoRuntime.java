package com.nokia.as.k8s.sless.fwk.runtime.impl;

import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.net.*;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.apache.log4j.Logger;
import org.osgi.framework.*;
import com.nokia.as.k8s.controller.*;
import com.nokia.as.k8s.sless.fwk.*;
import com.nokia.as.k8s.sless.fwk.runtime.*;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.felix.service.command.CommandProcessor;

import io.cloudevents.*;

@Component(service={SlessRuntime.class}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class GogoRuntime implements SlessRuntime {

    private int _id = 1;

    static final Logger LOGGER = Logger.getLogger("sless.runtime.gogo");

    private Map<String, FunctionContext> _functions = new ConcurrentHashMap<> ();

    public String type (){ return "gogo";}

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target="(type=gogo)")
    public void setFunction (FunctionContext ctx){
	LOGGER.info (this+" : setFunction : "+ctx+" : "+ctx.route ().route.path);
	_functions.put (ctx.route ().route.path, ctx);
    }
    public void unsetFunction (FunctionContext ctx){
	LOGGER.info (this+" : unsetFunction : "+ctx+" : "+ctx.route ().route.path);
	_functions.remove (ctx.route ().route.path, ctx);
    }

    public String toString (){ return "GogoRuntime";}

    @Activate
    public void activate (BundleContext osgi){
	LOGGER.info (this+" : activate");
	Hashtable<String, Object> props = new Hashtable<>();
        props.put(CommandProcessor.COMMAND_SCOPE, "casr.system.sless");
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] {
		"run", "list"
	    });
	osgi.registerService (Object.class.getName(), this, props);
    }
    
    @Descriptor ("Calls the Function")
    public void run (@Parameter(names = { "-path" }, absentValue = "/")
		     String path,
		     @Parameter(names = { "-data", "-d" }, absentValue = "")
		     String data,
		     @Parameter(names = { "-ttl" }, absentValue = "-1")
		     String ttl){
	FunctionContext ctx = _functions.get (path);
	if (ctx == null){
	    System.out.println ("Function not found for path="+path);
	    return;
	}
	
	String eventId = String.valueOf (_id++);
	URI src = URI.create(path);
	String eventType = "gogo";
		
	CloudEvent event = new CloudEventBuilder()
	    .type(eventType)
	    .id(eventId)
	    .source(src)
	    .data (data)
	    .build();
	
	CompletableFuture<CloudEvent> cf = new CompletableFuture<CloudEvent> ();
	ExecConfig conf = new ExecConfig ()
	    .ttl (Integer.parseInt (ttl) * 1000)
	    .cf (cf);
	ctx.exec (event, conf);
	
	try{
	    long start = System.currentTimeMillis ();
	    String res = cf.get ().getData().orElse ("").toString ();
	    long delay = System.currentTimeMillis () - start;
	    System.out.println (res+" / "+delay+"ms");
	}catch(Exception e){
	    LOGGER.warn ("Exception while running command", e);
	    System.out.println ("Exception while running commando : "+e);
	}
    }

    @Descriptor ("Lists the Routes/Functions")
    public void list (){
	for (FunctionContext ctx : _functions.values ())
	    System.out.println (ctx);
    }
}
