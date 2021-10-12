// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.features.admin.k8s;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nokia.as.features.admin.K8sFeaturesServlet;
import com.nokia.as.k8s.sless.fwk.RouteResource;
import com.nokia.as.k8s.sless.fwk.RouteResource.Exec;

public class Route {
	
	public final JSONObject rawJSON;
	public final String name;
	public final String namespace;
	
	private RouteResource parsedRoute;

	public Route(JSONObject json) throws JSONException {
		this.rawJSON = json;
		this.name = String.valueOf(json.optString("name"));
		this.namespace = K8sFeaturesServlet.CURRENT_NAMESPACE;
		this.parsedRoute = this.parseJSON(json);
	}
	
	private RouteResource parseJSON(JSONObject json) throws JSONException {
		/* ROUTE */
		com.nokia.as.k8s.sless.fwk.RouteResource.Route route = 
				new com.nokia.as.k8s.sless.fwk.RouteResource.Route(json.getString("path"), json.getString("type"));
		JSONArray rParams = json.getJSONArray("paramsList");
		for(int i = 0; i < rParams.length(); i++) {
			Object val = rParams.getJSONObject(i).get("value");
			if(String.valueOf(val).contains(",")) val = Stream.of(String.valueOf(val).split(",")).map(String::trim).collect(Collectors.toList());
			route = route.addParam(rParams.getJSONObject(i).getString("name"), val);
		}
			
		/* FUNCTION */
		com.nokia.as.k8s.sless.fwk.RouteResource.Function function = 
				new com.nokia.as.k8s.sless.fwk.RouteResource.Function(json.getString("functionId"));
		JSONArray fParams = json.getJSONArray("functionParamsList");
		for(int i = 0; i < fParams.length(); i++) {
			Object val = fParams.getJSONObject(i).get("value");
			if(String.valueOf(val).contains(",")) val = Stream.of(String.valueOf(val).split(",")).map(String::trim).collect(Collectors.toList());
			function = function.addParam(fParams.getJSONObject(i).getString("name"), val);
		}
			
		RouteResource routeRes = new RouteResource(this.name, this.namespace, route, function);
			
		/* RUNTIMES */
		JSONArray runtimes = json.getJSONArray("runtimesList");
		for(int i = 0; i < runtimes.length(); i++) {
			com.nokia.as.k8s.sless.fwk.RouteResource.Runtime r = new
					com.nokia.as.k8s.sless.fwk.RouteResource.Runtime(runtimes.getJSONObject(i).getString("name"));
			routeRes = routeRes.addRuntime(r);
		}
			
		/* EXEC */
		if(json.has("ttl")) {
			routeRes = routeRes.exec(new Exec().ttl((int) json.getDouble("ttl")));
		}
		
		return routeRes.build();
	}
	
	public RouteResource parsedRoute() {
		return this.parsedRoute;
	}

	public String toString(){
		String content = "{}";
		try {
			content = this.rawJSON.toString(2);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new StringBuilder("rawJSON=").append(content)
					.append(", parsedRoute=").append(parsedRoute()).toString();
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof Runtime)) return false;
		
		Route other = (Route) o;
		return (this.name.equals(other.name) && this.namespace.equals(other.namespace));
	}

}
