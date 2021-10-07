package com.nokia.as.features.admin.k8s;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nokia.as.features.admin.K8sFeaturesServlet;
import com.nokia.as.k8s.controller.CasrResource;
import com.nokia.as.k8s.controller.CasrResource.Configuration;
import com.nokia.as.k8s.controller.CasrResource.Configuration.Override;
import com.nokia.as.k8s.controller.CasrResource.Configuration.Prometheus;
import com.nokia.as.k8s.controller.CasrResource.Port;
import com.nokia.as.k8s.controller.CasrResource.Port.Ingress;
import com.nokia.as.k8s.controller.CasrResource.Runtime.Build;
import com.nokia.as.k8s.controller.CasrResource.Runtime.Build.Feature;

public class Runtime {
	
	public final JSONObject rawJSON;
	public final String name;
	public final String namespace;
	
	private DeployStatus status;
	private CasrResource parsedRuntime;
	private List<Pod> pods;

	public Runtime(JSONObject json) throws JSONException {
		this.rawJSON = json;
		this.pods = new ArrayList<>();
		this.name = String.valueOf(json.optString("name"));
		this.namespace = K8sFeaturesServlet.CURRENT_NAMESPACE;
		this.status = DeployStatus.UNDEPLOYED;
		this.parsedRuntime = this.parseJSON(json);
	}
	
	private CasrResource parseJSON(JSONObject json) throws JSONException {
		CasrResource casr = new CasrResource(name, namespace);
		
		/* RUNTIME */
		com.nokia.as.k8s.controller.CasrResource.Runtime runtime = new com.nokia.as.k8s.controller.CasrResource.Runtime();
		Build build = new Build();
		JSONArray features = json.getJSONArray("features");
		for(int i = 0; i < features.length(); i++) {
			build.addFeature(new Feature(features.optString(i)));
		}
		build.version(json.optString("version", null));
		build.repository(json.optString("repository", null));
		runtime.build(build);
		runtime.replicas(json.optInt("replicas"));
		casr = casr.runtime(runtime);
			
		/* PORTS */
		JSONArray ports = json.getJSONArray("portslist");
		for(int i = 0; i < ports.length(); i++) {
			JSONObject port = ports.getJSONObject(i);
			Ingress ingress = port.getBoolean("ingress") ? new Ingress(port.getString("ingressPath")) : null;
			casr = casr.addPort(new Port(port.getString("name"), 
										 port.getInt("port"), 
										 port.getString("protocol"),
										 port.getBoolean("external"),
										 ingress));
		}
			
		/* CONFIGURATION */
		Configuration configuration = new Configuration();
			
		/** LABELS **/
		JSONArray labels = json.getJSONArray("podLabelsList");
		for(int i = 0; i < labels.length(); i++) {
			JSONObject label = labels.getJSONObject(i);
			configuration = configuration.addLabel(label.optString("name"), label.optString("value"));
		}
			
		/** OVERRIDES **/			
		JSONArray overrides = json.getJSONArray("overridesList");
		for(int i = 0; i < overrides.length(); i++) {
			JSONObject overrideJSON = overrides.getJSONObject(i);
			Override override = new Override(overrideJSON.getBoolean("replace"), overrideJSON.getString("pid"));
			JSONArray props = overrideJSON.getJSONArray("props");
			for(int j = 0; j < props.length(); j++) {
				JSONObject prop = props.getJSONObject(j);
				override = override.addProperty(prop.getString("name"), prop.getString("value"));
			}
			
			configuration = configuration.addOverride(override);
		}
			
		/** FILES **/
		JSONArray files = json.getJSONArray("filesList");
		for(int i = 0; i < files.length(); i++) {
			JSONObject file = files.getJSONObject(i);
			configuration = configuration.addFile(file.optString("name"), file.optString("value"));
		}
		
		/** ENVIRONMENTS **/
		JSONArray environments = json.getJSONArray("envsList");
		for(int i = 0; i < environments.length(); i++) {
			JSONObject environment = environments.getJSONObject(i);
			configuration = configuration.addEnvironment(environment.optString("name"), environment.optString("value"));
		}
		
		/** PROMETHEUS **/
		JSONObject prometheus = json.getJSONArray("prometheusList").optJSONObject(0);
		if(prometheus != null) {
			configuration = configuration.prometheus(new Prometheus(prometheus.getInt("port"), prometheus.getString("path")));
		}
		
		/** CONFIGURATIONCONFIGMAP **/
		JSONObject configurationConfigMap = json.getJSONArray("configMap").optJSONObject(0);
		if(configurationConfigMap != null) {
			configuration = configuration.configurationConfigMap(configurationConfigMap.getString("value"));
		}
		
		/** TLSSECRET **/
		JSONObject tlsSecret = json.getJSONArray("secretsList").optJSONObject(0);
		if(tlsSecret != null) {
			configuration = configuration.tlsSecret(tlsSecret.getString("name"));
		}
		
		casr = casr.configuration(configuration);
			
		return casr.build();
	}
	
	public void status(DeployStatus status) {
		this.status = status;
	}
	
	public DeployStatus status() {
		return this.status;
	}

	public CasrResource parsedRuntime() {
		return this.parsedRuntime;
	}
	
	public void addPod(Pod pod) {
		this.pods.add(pod);
	}
	
	public List<Pod> pods() {
		return this.pods;
	}
	
	public Optional<Pod> pod(String name) {
		return pods.stream().filter(p -> p.name.equals(name)).findFirst();
	}
	
	public JSONArray getPodsUrls() throws JSONException {
		JSONArray result = new JSONArray();
		for(Pod p : pods) {
			JSONObject url = new JSONObject();
			url = url.put("name", p.name).put("url", p.name).put("status", p.status().statusStr());
			result.put(url);
		}
		return result;
	}

	public String toString(){
		String content = "{}";
		try {
			content = this.rawJSON.toString(2);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new StringBuilder("rawJSON=").append(content)
				.append(", parsedRuntime=").append(parsedRuntime())
				.append(", status=").append(status()).toString();
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof Runtime)) return false;
		
		Runtime other = (Runtime) o;
		return (this.name.equals(other.name) && this.namespace.equals(other.namespace));
	}

}
