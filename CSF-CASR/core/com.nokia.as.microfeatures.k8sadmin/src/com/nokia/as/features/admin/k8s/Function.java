package com.nokia.as.features.admin.k8s;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nokia.as.features.admin.K8sFeaturesServlet;
import com.nokia.as.k8s.sless.fwk.FunctionResource;

public class Function {
	
	public final JSONObject rawJSON;
	public final String name;
	public final String namespace;
	
	private FunctionResource parsedFunction;

	public Function(JSONObject json) throws JSONException {
		this.rawJSON = json;
		this.name = String.valueOf(json.optString("name"));
		this.namespace = K8sFeaturesServlet.CURRENT_NAMESPACE;
		this.parsedFunction = this.parseJSON(json);
	}
	
	private FunctionResource parseJSON(JSONObject json) throws JSONException {
		FunctionResource functionRes = new FunctionResource(name, namespace);
		
		/* FUNCTION */
		com.nokia.as.k8s.sless.fwk.FunctionResource.Function function = new com.nokia.as.k8s.sless.fwk.FunctionResource.Function();
		function = function.lazy(json.has("lazy") ? json.getBoolean("lazy") : true)
				   		   .timeout(json.has("timeout") ? (int) json.getDouble("timeout") : null);
			
		JSONArray locations = json.getJSONArray("locationsList");
		for(int i = 0; i < locations.length(); i++) {
			function = function.addLocation(locations.getJSONObject(i).getString("value"));
		}
		functionRes = functionRes.function(function);
			
		/* PARAMS */
		JSONArray params = json.getJSONArray("paramsList");
		for(int i = 0; i < params.length(); i++) {
			Object val = params.getJSONObject(i).get("value");
			if(String.valueOf(val).contains(",")) val = Stream.of(String.valueOf(val).split(",")).map(String::trim).collect(Collectors.toList());
			functionRes = functionRes.addParam(params.getJSONObject(i).getString("name"), val);
		}
			
		return functionRes.build();
	}
	
	public FunctionResource parsedFunction() {
		return this.parsedFunction;
	}

	public String toString(){
		String content = "{}";
		try {
			content = this.rawJSON.toString(2);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new StringBuilder("rawJSON=").append(content)
					.append(", parsedFunction=").append(parsedFunction()).toString();
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof Runtime)) return false;
		
		Function other = (Function) o;
		return (this.name.equals(other.name) && this.namespace.equals(other.namespace));
	}

}
