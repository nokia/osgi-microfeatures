// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.engine.mesh;

import com.alcatel.as.service.concurrent.*;

import com.nextenso.mux.*;
import com.nextenso.mux.mesh.*;
import org.osgi.service.component.annotations.*;
import org.osgi.framework.*;

import com.alcatel.as.ioh.engine.*;
import java.util.*;
import org.apache.log4j.Logger;

@Component(service={MuxMeshFactory.class},  immediate=true, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class MuxMeshFactoryImpl implements MuxMeshFactory {

    protected final Logger _logger = Logger.getLogger("as.ioh.mux");

    protected IOHServices _services;
    protected BundleContext _osgi;
    protected Map<MuxMeshListener, Map> _listeners = new HashMap<> ();
    protected final SerialExecutor _exec = new SerialExecutor (_logger);

    @Reference
    public void setServices (IOHServices services){
	_services = services;
    }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public void setMuxListener (final MuxMeshListener listener, final Map properties){
	_exec.execute (new Runnable (){ public void run (){
	    if (_osgi == null){
		_listeners.put (listener, properties);
	    } else {
		activate (listener, properties);
	    }
	}});
	     
    }
    public void unsetMuxListener (MuxMeshListener listener){} // not implemented

    private void activate (MuxMeshListener listener, Map<String, String> properties){
	newMuxMesh (properties.get (MuxMeshListener.MESH_NAME), listener, properties).start ();
    }

    @Activate
    protected void activate(final BundleContext osgi) {
	_exec.execute (new Runnable (){ public void run (){
	    _osgi = osgi;
	    
	    for (MuxMeshListener listener : _listeners.keySet ()){
		activate (listener, _listeners.get (listener));
	    }
	    _listeners.clear ();
	    _listeners = null;
	}});
    }

    public MuxMesh newMuxMesh (String name, MuxMeshListener listener, java.util.Map<String, String> props){
	_logger.info ("MuxMeshFactoryImpl : newMuxMesh : "+name+" : "+listener);
	return new MuxMeshImpl (this, name, listener, props);
    }
}
