// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless;

import io.cloudevents.CloudEvent;

@FunctionalInterface
public interface Function {
    
    public static final String PROP_NAME = "name";
    
    java.util.concurrent.CompletableFuture<CloudEvent> apply (CloudEvent event, EventContext ctx);
    
}
