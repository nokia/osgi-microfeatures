package com.nokia.as.k8s.controller;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomResourceDefinition implements Externalizable {
	
	static final String API_VERSION = "apiextensions.k8s.io/v1beta1";
	static final String KIND = "CustomResourceDefinition";
	
	private Map<String, Object> attributes = new HashMap<>();
	private Map<String, Object> metadata = new HashMap<>();
	private Map<String, Object> spec = new HashMap<>();
	
	public CustomResourceDefinition() {
		attributes.put("spec", spec);
		attributes.put("metadata", metadata);
	}
	
    public CustomResourceDefinition(Map<String, Object> crd) {
    	this();
    	metadata = (Map<String, Object>) crd.get("metadata");
		spec     = (Map<String, Object>) crd.get("spec");
		if(metadata == null) metadata = new HashMap<>();
		if(spec == null) spec = new HashMap<>();
    }
    
    public Map<String, Object> attributes() {
    	return attributes;
    }
    
	public CustomResourceDefinition namespaced(boolean namespaced){
    	spec.put("scope", namespaced ? "Namespaced" : "Cluster");
    	return this;
    }

    public CustomResourceDefinition names(Names names){
    	spec.put("names", names);
    	return this;
    }

    public CustomResourceDefinition group(String group){
    	spec.put("group", group);
    	return this;
    }

    public CustomResourceDefinition version(String version){
    	spec.put("version", version);
    	return this;
    }

    public boolean namespaced() { 
    	return "Namespaced".equals(spec.get("scope"));
    }

    public Names names() { 
    	Object names = spec.get("names");
    	if(names instanceof Names) {
    		return (Names) names;
    	} else if (names instanceof Map<?,?>) {
    		spec.put("names", new Names((Map<String, Object>) names));
    	}
    	return (Names) spec.get("names");
    }

    public String group () { 
    	return (String) spec.get("group");
    }

    public String version() { 
    	return (String) spec.get("version");
    }

    public CustomResourceDefinition build() {
    	String name = new StringBuilder()
    			.append(names().plural())
    			.append('.')
    			.append(group())
    			.toString();
    	metadata.put("name", name);
    	return this;
    }

    public String name() { 
    	return (String) metadata.get("name");
    }

    public static class Names implements Externalizable {
		private Map<String, Object> _attributes = new HashMap<>();
		
		public Names(){}
		
		public Map<String, Object> getAttributes() {
			return _attributes;
		}
		
		public void setAttributes(Map<String, Object> obj) {
			this._attributes = obj;
		}
		
		public Names(Map<String, Object> attrs){ 
			this._attributes = attrs;
		}

		public Map<String, Object> attributes() { 
			return _attributes; 
		}
	
		public String plural () {
			return (String) _attributes.get("plural");
		}
		
		public String singular() { 
			return (String) _attributes.get("singular");
		}
		
		public String kind() { 
			return (String) _attributes.get("kind");
		}
		
		public List<String> shortNames() {
			return (List<String>) _attributes.get("shortNames");
		}
		
		public Names plural (String plural) {
		    _attributes.put("plural", plural);
		    return this;
		}
	
		public Names singular (String singular) {
		    _attributes.put("singular", singular);
		    return this;
		}
	
		public Names kind (String kind) {
		    _attributes.put("kind", kind);
		    return this;
		}
	
		public Names shortName (String shortName) {
		    List<String> shortNames = shortNames();
		    if (shortNames == null) _attributes.put("shortNames", shortNames = new ArrayList<>());
		    shortNames.add(shortName);
		    return this;
		}

		@Override
		public String toString() {
			return "Names [plural()=" + plural() + ", singular()=" + singular()
					+ ", kind()=" + kind() + ", shortNames()=" + shortNames() + "]";
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(_attributes);
			
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			_attributes = (Map<String, Object>) in.readObject();
		}
    }

	@Override
	public String toString() {
		return "CustomResourceDefinition [namespaced()=" + namespaced() + ", names()=" + names() + ", group()="
				+ group() + ", version()=" + version() + ", name()=" + name() + "]";
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(attributes);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		attributes = (Map<String, Object>) in.readObject();
		metadata = (Map<String, Object>) attributes.get("metadata");
		spec     = (Map<String, Object>) attributes.get("spec");
	}
	
}
