// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.sless;

import static com.nokia.as.jaxrs.jersey.sless.HttpSlessRuntime.SPEC_HEADER;
import static com.nokia.as.jaxrs.jersey.sless.HttpSlessRuntime.SPEC_SECTION;
import static com.nokia.as.jaxrs.jersey.sless.HttpSlessRuntime.SPEC_STATUS;
import static com.nokia.as.jaxrs.jersey.sless.HttpSlessRuntime.log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.glassfish.jersey.process.Inflector;

import com.nokia.as.k8s.sless.fwk.runtime.ExecConfig;
import com.nokia.as.k8s.sless.fwk.runtime.FunctionContext;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventBuilder;
import io.cloudevents.SpecVersion;
import io.cloudevents.http.HttpTransportAttributes;
import io.cloudevents.http.V01HttpTransportMappers;

public final class HttpSlessInflector implements Inflector<ContainerRequestContext, Void> {

    public static java.nio.charset.Charset UTF_8 = null;
    static {
	try{
	    UTF_8 = java.nio.charset.Charset.forName ("utf-8");
	}catch(Exception e){}
    }    

    private static final AtomicLong SEED = new AtomicLong ();

    private final FunctionContext _function;
    private final String _defProvides;
    private int _defStatus = -1;
    private ArrayList<String[]> _headers = new ArrayList<> ();
    private boolean _requestData;
    private String _type;
    @Inject
    private javax.inject.Provider<AsyncResponse> responseProvider;

    public HttpSlessInflector(String method, FunctionContext function, String defProvides, Map<String, Object> params) {
	_requestData = "POST".equals (method) || "PUT".equals (method);
	_type = "http."+method;
	_function = function;
	_defProvides = defProvides;
	String defStatus = (String) params.get(SPEC_SECTION + SPEC_STATUS);
	if (defStatus == null) defStatus = "200";
	try{
	    _defStatus = Integer.parseInt (defStatus);
	}catch(Exception e){
	    log.error (this+" : invalid response status configured in route : "+defStatus);
	    _defStatus = 200;
	}
	// headers
	String specHeader = (String) params.get(SPEC_SECTION + SPEC_HEADER);
	if (specHeader != null){
	    String[] headers = specHeader.split(",");
	    for (String h : headers) {
		String[] kv = h.split(":");
		String key = kv[0].trim ();
		String value = kv[1].trim ();
		_headers.add (new String[]{key, value});
	    }
	}
    }

    @Override
    public String toString (){
	return "HttpSlessInflector["+_function+"]";
    }
    
    @Override
    public Void apply(ContainerRequestContext ctx) {
	return apply (ctx, responseProvider.get ());
    }
    public Void apply (ContainerRequestContext ctx, AsyncResponse response){
	CloudEventBuilder<String> builder = new CloudEventBuilder<String>();

	boolean fillDefault = true;
	if (_requestData){
	    String ceversion = ctx.getHeaderString(V01HttpTransportMappers.SPEC_VERSION_KEY);
	    HttpTransportAttributes httpMappers = HttpSlessRuntime.httpV02Mappers;
	    if (SpecVersion.V_01.toString ().equals (ceversion)) httpMappers = HttpSlessRuntime.httpV01Mappers;

	    String type = ctx.getHeaderString (httpMappers.typeKey ());
	    if (type != null){
		// this is a CloudEvent coming in
		fillDefault = false;
		builder
		    .type (type)
		    .specVersion(httpMappers.specVersionKey())
		    .source(uri(httpMappers.sourceKey()))
		    .id(httpMappers.idKey())
		    //.time(httpMappers.timeKey()) TBD
		    .schemaURL(uri(httpMappers.schemaUrlKey()));
	    }
	}
	if (fillDefault) builder
			     .type(_type)
			     .id(String.valueOf (SEED.getAndIncrement ()))
			     .source(ctx.getUriInfo().getBaseUri().resolve(ctx.getUriInfo().getPath(true)));

	if (_requestData){
	    try{
		builder.data (getData (ctx));
	    }catch(Exception e){
		if (log.isInfoEnabled ()) log.info(this+" : exception while reading the request data", e);
		response.cancel ();
		return null;
	    }
	}
	
	CloudEvent<String> event = builder.build();
	CompletableFuture<CloudEvent> cf = new CompletableFuture<CloudEvent>();
	cf.whenComplete((CloudEvent result, Throwable exception) -> {
		if (log.isInfoEnabled ()) log.info(HttpSlessInflector.this + "\u001B[33m COMPLETED = " + result + "\u001B[0m");
		if (exception != null) {
		    response.cancel();
		} else {
		    createSuccessfulResponse(response, result);
		}
	    });

	ExecConfig execConfig = new ExecConfig().cf(cf);
	if (log.isInfoEnabled ()) log.info(this + "\u001B[33m CloudEvent submit = " + event + "\u001B[0m");
	_function.exec(event, execConfig);
	
	return null;
    }

    private void createSuccessfulResponse(AsyncResponse response, CloudEvent result) {
	try {
	    ResponseBuilder build = Response.status(_defStatus);
	    for (String[] header : _headers) build.header (header[0], header[1]);
	    String responseData = result != null ? result.getData().orElse("").toString() : "";
	    if (responseData.length () > 0){
		if (_defProvides == null){
		    String ct = result.getContentType ().orElse (HttpSlessRuntime.DEFAULT_PRODUCES).toString ();
		    build.header ("Content-Type", ct);
		}
		build.entity(responseData);
	    }
	    response.resume(build.build());
	} catch (Throwable e) {
	    log.warn(this + " : creating response failed ", e);
	}
    }

    private static URI uri (String value){
	try{
	    if (value == null) return null;
	    return new URI (value);
	}catch(Exception e){
	    return null;
	}
    }

    private static String getData (ContainerRequestContext ctx) throws Exception {
	InputStream in = ctx.getEntityStream ();
	StringBuilder sb = new StringBuilder ();
	InputStreamReader reader = new InputStreamReader (in, UTF_8);
	int i;
	while ((i = reader.read ()) != -1) sb.append ((char) i);
	return sb.toString ();
    }
}
