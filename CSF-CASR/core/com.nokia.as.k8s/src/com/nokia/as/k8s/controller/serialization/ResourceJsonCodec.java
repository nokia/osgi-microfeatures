package com.nokia.as.k8s.controller.serialization;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nokia.as.k8s.controller.CustomResource;

public class ResourceJsonCodec implements JsonSerializer<CustomResource>, JsonDeserializer<CustomResource> {

	@Override
	public CustomResource deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		if(!arg0.isJsonObject()) {
			throw new JsonParseException("should be an object");
		}
		
		JsonObject jsObj = arg0.getAsJsonObject();
		Map<String, Object> attrs = arg2.deserialize(jsObj.get("attributes"), Map.class);
		Map<String, Object> crd = arg2.deserialize(jsObj.get("crd"), Map.class);
		
		return new CustomResource(attrs, crd);
	}

	@Override
	public JsonElement serialize(CustomResource arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		
		obj.add("attributes", arg2.serialize(arg0.attributes()));
		if(arg0.definition() != null) {
			obj.add("crd", arg2.serialize(arg0.definition().attributes()));
		}
		
		return obj;
	}



}
