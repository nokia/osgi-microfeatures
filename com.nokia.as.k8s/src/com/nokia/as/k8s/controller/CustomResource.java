package com.nokia.as.k8s.controller;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

public class CustomResource implements Externalizable {
	
	protected Map<String, Object> attributes = new HashMap<>();
	protected Map<String, Object> metadata = new HashMap<>();
	protected Map<String, Object> spec = new HashMap<>();
	protected CustomResourceDefinition crd;

	public CustomResource(CustomResourceDefinition crd) {
		this(new HashMap<>(), crd);		
	}
	
	public CustomResource(Map<String, Object> attrs, CustomResourceDefinition crd) {
		this.crd = crd;
		attributes = attrs;
		metadata   = (Map<String, Object>) attrs.get("metadata");
		spec       = (Map<String, Object>) attrs.get("spec");
		if(metadata == null) metadata = new HashMap<>();
		if(spec == null) spec = new HashMap<>();
		this.attributes.put("metadata", metadata);
		this.attributes.put("spec", spec);
	}
	
	public CustomResource(Map<String, Object> attrs, Map<String, Object> crd) {
		this(attrs, new CustomResourceDefinition(crd));
	}

	public Map<String, Object> attributes() {
		return attributes;
	}

	public Map<String, Object> metadata() {
		return metadata;
	}

	public Map<String, Object> spec() {
		return spec;
	}
    
	public CustomResource attribute (String key, Object value){
		attributes.put (key, value);
		return this;
	}

	public CustomResource metadata (String key, Object value){
		metadata.put (key, value);
		return this;
	}

	public CustomResource spec (String key, Object value){
		spec.put (key, value);
		return this;
	}

	public CustomResource apiVersion(String apiVersion) {
		attributes.put("apiVersion", apiVersion);
		return this;
	}

	public CustomResource kind(String kind) {
		attributes.put("kind", kind);
		return this;
	}

	public CustomResource name(String name) {
		metadata.put("name", name);
		return this;
	}

	public CustomResource namespace(String namespace) {
		metadata.put("namespace", namespace);
		return this;
	}

	public String apiVersion() {
		return (String) attributes.get("apiVersion");
	}

	public String kind() {
		return (String) attributes.get("kind");
	}

	public String name() {
		return (String) metadata.get("name");
	}

	public String namespace() {
		return (String) metadata.get("namespace");
	}
	
	public CustomResourceDefinition definition() {
		return crd;
	}

	public CustomResource definition (CustomResourceDefinition crd){
		this.crd = crd;
		if (crd != null){
			apiVersion(crd.group() + "/" + crd.version());
			kind(crd.names().kind());
		}
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name() == null) ? 0 : name().hashCode());
		result = prime * result + ((namespace() == null) ? 0 : namespace().hashCode());
		result = prime * result + ((kind() == null) ? 0 : kind().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!obj.getClass().isAssignableFrom(getClass()))
			return false;
		CustomResource other = (CustomResource) obj;
		if (name() == null) {
			if (other.name() != null)
				return false;
		} else if (!name().equals(other.name()))
			return false;
		if (namespace() == null) {
			if (other.namespace() != null)
				return false;
		} else if (!namespace().equals(other.namespace()))
			return false;
		if (kind() == null) {
			if (other.kind() != null)
				return false;
		} else if (!kind().equals(other.kind()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Resource [_attributes=" + attributes + ", _metadata=" + metadata + ", _spec=" + spec + ", crd=" + crd
				+ "]";
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(attributes);
		out.writeObject(crd);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		attributes = (Map<String, Object>) in.readObject();
		crd = (CustomResourceDefinition) in.readObject();
		metadata = (Map<String, Object>) attributes.get("metadata");
		spec     = (Map<String, Object>) attributes.get("spec");
	}
}
