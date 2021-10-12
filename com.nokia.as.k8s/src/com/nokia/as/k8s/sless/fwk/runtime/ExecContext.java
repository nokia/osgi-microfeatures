// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime;

import org.osgi.annotation.versioning.ProviderType;
import com.nokia.as.k8s.sless.EventContext;
import io.cloudevents.CloudEvent;

@ProviderType
public interface ExecContext {
    
    public void interrupt (String cause);

    public ExecConfig config ();

    public EventContext eventContext ();

    public CloudEvent event ();

    public long id ();
    
}
