// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime;

import com.nokia.as.k8s.sless.fwk.*;
import io.cloudevents.CloudEvent;

public interface FunctionContext {

    public FunctionResource function ();
    public RouteResource route ();

    public ExecContext exec (CloudEvent event, ExecConfig conf);
    
}
