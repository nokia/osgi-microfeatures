// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime.impl.crd;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;
import com.nokia.as.k8s.controller.CustomResourceDefinition.Names;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.sless.fwk.runtime.ExecConfig;
import com.nokia.as.k8s.sless.fwk.runtime.FunctionContext;
import com.nokia.as.k8s.sless.fwk.runtime.SlessRuntime;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventBuilder;

@Component(service={SlessRuntime.class}, immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class CRDSlessRuntime implements SlessRuntime {

	private int _id = 1;

	static final Logger LOGGER = Logger.getLogger("sless.runtime.crd");

	private Map<String, PathContext> _pathContexts = new ConcurrentHashMap<> ();
	private ResourceService _resService;
	private PlatformExecutors _execs;

	public String type (){ return "crd";}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, target="(type=crd)")
	public void setFunction (FunctionContext ctx){
		String path = ctx.route ().route.path;
		if (LOGGER.isInfoEnabled ())
			LOGGER.info (this+" : setFunction : "+ctx+" : "+path);
		String kind = kind (path);
		execute (kind, () -> {
			try{
				PathContext pctx =  _pathContexts.get (kind);
				if (pctx == null){
					pctx = new PathContext (ctx);
					CustomResourceDefinition CRD =
							new CustomResourceDefinition ()
							.namespaced(Boolean.parseBoolean (getParameter ("namespaced", path, "true")))
							.group(getParameter ("group", path, "nokia.com"))
							.version(getParameter ("version", path, "v1beta1"))
							.names(new Names()
									.kind(kind)
									.plural(getParameter ("plural", path, kind+"s")))
							//.shortName( ).. ?
							.build ();
					pctx._watcher = _resService.watch (CRD, 
							r -> {   //RESOURCE ADDED
								execute (r.kind(), () -> {
									PathContext context = _pathContexts.get (kind);
									if (context != null){

										String eventId = String.valueOf (_id++);

										CloudEvent event = new CloudEventBuilder()
												.type("create")
												.id(eventId)
												.source(URI.create (r.kind()))
												.data (r.toString ())
												.build();

										for (FunctionContext function : context._functions){
										    ExecConfig conf = new ExecConfig ()
											.executor (_execs.getProcessingThreadPoolExecutor (r.name ()));
										    function.exec (event, conf);
										}
									}
								});
							},
							r -> {}, //RESOURCE MODIFIED
							r -> {   //RESOURCE DELETED
								execute (r.kind(), () -> {
									PathContext context = _pathContexts.get (kind);
									if (context != null){
										String eventId = String.valueOf (_id++);

										CloudEvent event = new CloudEventBuilder()
												.type("destroy")
												.id(eventId)
												.source(URI.create (r.kind()))
												.data (r.toString ())
												.build();

										for (FunctionContext function : context._functions){
										    ExecConfig conf = new ExecConfig ()
											.executor (_execs.getProcessingThreadPoolExecutor (r.name ()));
										    function.exec (event, conf);
										}
									}
								});
							});
					_pathContexts.put (kind, pctx);
				} else {
					pctx._functions.add (ctx);
				}
			}catch(Exception e){
				LOGGER.warn (CRDSlessRuntime.this+" : failed to setFunction : "+ctx, e);
			}
		});
	}	    

	public void unsetFunction (FunctionContext ctx){
		LOGGER.info (this+" : unsetFunction : "+ctx);
		String path = ctx.route ().route.path;
		String kind = kind (path);
		execute (kind, () -> {
			PathContext pctx = _pathContexts.get (kind);
			if (pctx != null){
				pctx._functions.remove (ctx);
				if (pctx._functions.size () == 0){
					_pathContexts.remove (kind);
					try{pctx._watcher.close ();}catch(Exception e){} // cannot happen
				}
			}
		});
	}

	public String toString (){ return "ResourceRuntime";}

	@Reference
	public void setResourceService (ResourceService rs){	
		_resService = rs;
	}

	@Reference
	public void setPlatformExecs (PlatformExecutors execs){
		_execs = execs;
	}

	@Activate
	public void activate (BundleContext osgi){
		LOGGER.debug(this+" : activate");
	}

	private static class PathContext {
		private List<FunctionContext> _functions = new ArrayList<> ();
		private java.io.Closeable _watcher;
		private PathContext (FunctionContext fc){ _functions.add (fc);}
	}


	private static String getParameter (String name, String path, String def){
		String key = ";"+name+"=";
		int index = path.indexOf (key);
		if (index != -1){
			index += key.length ();
			int index2 = path.indexOf (';', index);
			if (index2 == -1) index2 = path.length ();
			return path.substring (index, index2);
		}
		return def;
	}

	private static String kind (String path){
		int index = path.indexOf (';');
		return index == -1 ? path : path.substring (0, index);
	}

	private void execute (String kind, Runnable r){
		PlatformExecutor exec = _execs.getProcessingThreadPoolExecutor (kind);
		exec.execute (r);
	}
}
