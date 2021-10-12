// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.sless.fwk.runtime;

import java.util.Map;
import java.util.function.*;
import java.util.concurrent.Executor;
import java.util.concurrent.CompletableFuture;

import io.cloudevents.CloudEvent;

public class ExecConfig {

    private static final BiConsumer<ExecContext, CloudEvent> ON_SUCCESS_VOID = (ec, r) -> {};
    private static final BiConsumer<ExecContext, Throwable> ON_ERROR_VOID = (ec, t) -> {};

    private long _ttl, _maxttl;
    private Executor _exec;
    private Map<String, Object> _eventParams, _functionParams;
    private BiConsumer<ExecContext, CloudEvent> _onSuccess = ON_SUCCESS_VOID;
    private BiConsumer<ExecContext, Throwable> _onError = ON_ERROR_VOID;
    private Object _attachment;

    public ExecConfig (){
    }

    public ExecConfig maxttl (long ttl){ _maxttl = ttl; return this;}
    public long maxttl (){ return _maxttl;}

    public ExecConfig ttl (long ttl){ _ttl = ttl; return this;}
    public long ttl (){ return _ttl;}

    public ExecConfig executor (Executor ex){ _exec = ex; return this;}
    public Executor executor (){ return _exec;}

    public ExecConfig eventParameters (Map<String, Object> params){ _eventParams = params; return this;}
    public Map<String, Object> eventParameters (){ return _eventParams;}

    public ExecConfig functionParameters (Map<String, Object> params){ _functionParams = params; return this;}
    public Map<String, Object> functionParameters (){ return _functionParams;}

    public ExecConfig attach (Object attachment){ _attachment = attachment; return this;}
    public <T> T attachment (){ return (T) _attachment;}

    public ExecConfig onSuccess (BiConsumer<ExecContext, CloudEvent> callback){ _onSuccess = callback; return this;}
    public BiConsumer<ExecContext, CloudEvent> onSuccess (){ return _onSuccess;}

    public ExecConfig onError (BiConsumer<ExecContext, Throwable> callback){ _onError = callback; return this;}
    public BiConsumer<ExecContext, Throwable> onError (){ return _onError;}

    public ExecConfig cf (CompletableFuture<CloudEvent> cf){
	onSuccess ((ec, r) -> cf.complete (r));
	onError ((ec, t) -> cf.completeExceptionally (t));
	return this;
    }
}
