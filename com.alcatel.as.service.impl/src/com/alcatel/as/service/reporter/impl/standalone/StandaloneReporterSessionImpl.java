// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.reporter.impl.standalone;

import java.io.Serializable;

import org.apache.felix.dm.annotation.api.Component;

import com.alcatel.as.service.reporter.api.ReporterSession;

import alcatel.tess.hometop.gateways.utils.Log;

@Component
public class StandaloneReporterSessionImpl implements ReporterSession {
    final static Log _log = Log.getLogger(StandaloneReporterSessionImpl.class);
    
    @Override
    public void setAttribute(String attributeName, Serializable attributeValue) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setState(int state) {
        switch (state) {
        case ReporterSession.ACTIVE:
            _log.info("Application is fully active.");
        default:
        }        
    }

    @Override
    public void setMessage(String message) {
        // TODO Auto-generated method stub
        
    }

}
