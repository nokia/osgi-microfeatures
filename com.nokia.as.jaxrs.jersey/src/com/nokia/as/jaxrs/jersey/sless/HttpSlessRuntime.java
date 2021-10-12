// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.jaxrs.jersey.sless;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.core.MediaType;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.log4j.Logger;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.osgi.framework.BundleContext;

import com.nokia.as.jaxrs.jersey.common.ApplicationConfiguration;
import com.nokia.as.jaxrs.jersey.common.JaxRsResourceRegistry;
import com.nokia.as.k8s.sless.fwk.RouteResource;
import com.nokia.as.k8s.sless.fwk.runtime.FunctionContext;
import com.nokia.as.k8s.sless.fwk.runtime.SlessRuntime;

import io.cloudevents.http.V01HttpTransportMappers;
import io.cloudevents.http.V02HttpTransportMappers;

@Component(provides = SlessRuntime.class)
public class HttpSlessRuntime implements SlessRuntime {

    // DEFAULT : TBD
    static final String[] DEFAULT_METHODS = new String[] {"GET", "PUT", "DELETE", "POST"};
    static final String[] DEFAULT_CONSUMES = new String[] {MediaType.APPLICATION_JSON};
    static final MediaType DEFAULT_PRODUCES = MediaType.APPLICATION_JSON_TYPE;
    static final Integer DEFAULT_STATUS = 200;
    static final String ROLE_POLICY_ANY = "any";
    static final String ROLE_POLICY_ALL = "all";
    static final String DEFAULT_ROLE_POLICY = ROLE_POLICY_ANY;

    static final String TYPE = "http";

    static final String SPEC_SECTION = "http.";
    static final String SPEC_METHOD = "method";
    static final String SPEC_CONSUMES = "consumes";
    static final String SPEC_PRODUCES = "produces";
    static final String SPEC_HEADER = "header";
    static final String SPEC_STATUS = "status";
    static final String SPEC_ROLE = "role";
    static final String SPEC_ROLE_POLICY = "role.policy";

    static final Logger log = Logger.getLogger("sless.runtime.http");

    static final V01HttpTransportMappers httpV01Mappers = new V01HttpTransportMappers ();
    static final V02HttpTransportMappers httpV02Mappers = new V02HttpTransportMappers ();

    private Map<FunctionContext, Resource> _functions = new ConcurrentHashMap<>();
    private Map<String, Boolean> _routes = new ConcurrentHashMap<>(); // k=route.path, v=containsRoles

    @Inject
    BundleContext _bc;
    
    @ServiceDependency
    private volatile JaxRsResourceRegistry registration;
    // propagate route security info to jersey
	private ApplicationConfiguration appConfig = new ApplicationConfiguration() {
		@Override
		public Map<String, Object> getProperties() {
			Map<String, Object> properties = new HashMap<>();
			properties.put("sless.routes", _routes);
			return properties;
		}
	};

    @Override
    public String toString (){ return "[HttpSlessRuntime]";}

    @ServiceDependency(service = FunctionContext.class, filter = "(type=http)", required = false, removed = "unbindFunction")
    public void bindFunction(FunctionContext function, Map<String, String> properties) throws Exception {
	if (log.isInfoEnabled ()) log.info(this + " : binding jaxrs service to function : " + function);
	RouteResource route = function.route();
	Map<String, Object> params = (Map<String, Object>) route.route.paramsAsMap();
	Resource.Builder resourceBuilder = Resource.builder();

	// path
	resourceBuilder.path(route.route.path);
	
	// method
	for (String method : methods (params)){
	    ResourceMethod.Builder methodBuilder = resourceBuilder.addMethod(method.trim());
	    
	    // content-type
	    methodBuilder.consumes(consumes (params));
	    String produces = produces (params);
	    if (produces != null) methodBuilder.produces(produces);
	    
	    // processing
	    methodBuilder.suspended(AsyncResponse.NO_TIMEOUT, TimeUnit.SECONDS);

	    HttpSlessInflector methodHandler = new HttpSlessInflector(method, function, produces, params);
	    
	    List<String> rolesAllowed = roles (params);	    
	    if (rolesAllowed == null){
		methodBuilder.handledBy(methodHandler);
	    } else
		methodBuilder.handledBy(new AuthorizationInflector(rolesAllowed, rolePolicy (params), methodHandler));
	}
	// bind and save resource for removal
	final Resource resource = resourceBuilder.build();
	_functions.put(function, resource);
	boolean secure = params.containsKey("http.role");
	if (log.isDebugEnabled())
		log.debug(this + " add route : " + route.name + ", secure = " + secure);
	
	_routes.put(route.route.path.substring(1), secure);
	_bc.registerService(ApplicationConfiguration.class, appConfig, null);
	registration.bindJaxRsResource(resource, new HashMap<String, String>());
    }

    private static String[] methods (Map<String, Object> params){
	String methods = (String) params.get(SPEC_SECTION + SPEC_METHOD);
	return methods != null ? methods.toUpperCase ().split (",") : DEFAULT_METHODS; 
    }
    private static String[] consumes (Map<String, Object> params){
	String consumes = (String) params.get(SPEC_SECTION + SPEC_CONSUMES);
	return consumes != null ? consumes.split (",") : DEFAULT_CONSUMES; 
    }
    private static String produces (Map<String, Object> params){
	 return (String) params.get(SPEC_SECTION + SPEC_PRODUCES);
    }
    private List<String> roles(Map<String, Object> params) {
	String role = (String) params.get(SPEC_SECTION + SPEC_ROLE);
	return role != null ? Arrays.asList (role.split (",")) : null;
    }
    private String rolePolicy(Map<String, Object> params) {
	String policy = (String) params.get(SPEC_SECTION + SPEC_ROLE_POLICY);
	if (policy == null) return DEFAULT_ROLE_POLICY;
	switch (policy = policy.toLowerCase ()){
	case ROLE_POLICY_ANY:
	case ROLE_POLICY_ALL: return policy;
	default:
	    log.error (this+" : invalid role policy : "+policy+" : applying "+DEFAULT_ROLE_POLICY);
	    return DEFAULT_ROLE_POLICY;
	}
    }

    public void unbindFunction(FunctionContext function, Map<String, String> properties) {
	if (log.isInfoEnabled ()) log.info(this + " : unbinding function : " + function);
	registration.unbindJaxRsResource(_functions.remove(function), new HashMap<> ()); // TODO check the hashmap usage
	_routes.remove(function.route().route.path.substring(1));
    }

    @Override
    public String type() {
	return TYPE;
    }
}
