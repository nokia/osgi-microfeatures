package com.nokia.as.k8s.sless.fwk.runtime;

import com.nokia.as.k8s.sless.fwk.*;
import io.cloudevents.CloudEvent;

public interface FunctionContext {

    public FunctionResource function ();
    public RouteResource route ();

    public ExecContext exec (CloudEvent event, ExecConfig conf);
    
}
