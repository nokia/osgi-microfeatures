package com.nextenso.diameter.agent.dictionary;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nextenso.proxylet.diameter.dictionary.DiameterTypeDictionary;
import com.nextenso.proxylet.diameter.util.DerivedFormat;
import com.nextenso.proxylet.diameter.util.DiameterAVPFormat;

public class DerivedFormatAdapter implements JsonSerializer<DerivedFormat>,
	JsonDeserializer<DerivedFormat> {

	private static final String NAME_PROP = "name";
	private static final String TYPE_PROP = "baseType";
	
	private DiameterTypeDictionary dic;
	
	public DerivedFormatAdapter(DiameterTypeDictionary dic) {
		this.dic = dic;
	}
	
	@Override
	public DerivedFormat deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		if(!arg0.isJsonObject()) {
			throw new JsonParseException(arg0 + " is not a JSON Object");
		}
		
		JsonObject obj = arg0.getAsJsonObject();
		JsonPrimitive namePrimitive = obj.getAsJsonPrimitive(NAME_PROP);
		
		if(namePrimitive == null) {
			throw new JsonParseException(NAME_PROP + " property not found");
		} else if(!namePrimitive.isString()) {
			throw new JsonParseException(NAME_PROP + " property should be a String");
		}
		
		String name = namePrimitive.getAsString();
		
		JsonPrimitive typePrimitive = obj.getAsJsonPrimitive(TYPE_PROP);
		
		if(typePrimitive == null) {
			throw new JsonParseException(TYPE_PROP + " property not found");
		} else if(!typePrimitive.isString()) {
			throw new JsonParseException(TYPE_PROP + " should be a String");
		}
		
		String typeName = typePrimitive.getAsString();
		DiameterAVPFormat type = dic.getFormatForTypeName(typeName);
		if(type == null) {
			throw new JsonParseException(typeName + " is not a known Diameter AVP format");
		}
		
		return new DerivedFormat(name,  type);
	}

	@Override
	public JsonElement serialize(DerivedFormat arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		obj.add(NAME_PROP, new JsonPrimitive(arg0.getName()));
		obj.add(TYPE_PROP, new JsonPrimitive(arg0.getBaseFormat().getName()));
		
		return obj;
	}
	

}
