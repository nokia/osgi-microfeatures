// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.util.resteasy;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.felix.dm.annotation.api.Component;

@Component
@Provider
public class JacksonFeature implements Feature {
	@Override
    public boolean configure(FeatureContext context) {
        context.register(JacksonProvider.class, MessageBodyReader.class, MessageBodyWriter.class);
        return true;
    }
}
