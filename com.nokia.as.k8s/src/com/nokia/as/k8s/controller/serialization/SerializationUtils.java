// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.k8s.controller.serialization;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.CustomResourceDefinition;

import io.kubernetes.client.openapi.JSON;

public class SerializationUtils {
	private static Yaml yaml = new Yaml();
	private static Gson gson = new GsonBuilder()
			.registerTypeAdapter(Map.class, new MapDeserializer())
			.registerTypeAdapter(List.class, new ListDeserializer())
			.create();
	
	private static JSON k8sJson = new JSON();

	// from
	// https://stackoverflow.com/questions/17090589/gson-deserialize-integers-as-integers-and-not-as-doubles
	private static class MapDeserializer implements JsonDeserializer<Map<String, Object>> {

	    public Map<String, Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	        Map<String, Object> m = new LinkedHashMap<String, Object>();
	        JsonObject jo = json.getAsJsonObject();
	        for (Entry<String, JsonElement> mx : jo.entrySet()) {
	            String key = mx.getKey();
	            JsonElement v = mx.getValue();
	            if (v.isJsonArray()) {
	                m.put(key, context.deserialize(v, List.class));
	            } else if (v.isJsonPrimitive()) {
	                Number num = null;
	                ParsePosition position=new ParsePosition(0);
	                String vString=v.getAsString();
	                try {
	                  num = NumberFormat.getInstance(Locale.ROOT).parse(vString,position);
	                  
	                  if(num.longValue() < Integer.MAX_VALUE) {
	                	  num = new Integer(num.intValue());
	                  }
	                } catch (Exception e) {
	                }
	                //Check if the position corresponds to the length of the string
	                if(position.getErrorIndex() < 0 && vString.length() == position.getIndex()) {
	                  if (num != null) {
	                    m.put(key, num);
	                    continue;
	                  }
	                }
	                JsonPrimitive prim = v.getAsJsonPrimitive();
	                if (prim.isBoolean()) {
	                    m.put(key, prim.getAsBoolean());
	                } else if (prim.isString()) {
	                    m.put(key, prim.getAsString());
	                } else {
	                    m.put(key, null);
	                }

	            } else if (v.isJsonObject()) {
	                m.put(key, context.deserialize(v, Map.class));
	            }

	        }
	        return m;
	    }
	}

	private static class ListDeserializer implements JsonDeserializer<List<Object>> {

	    public List<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
	        List<Object> m = new ArrayList<Object>();
	        JsonArray arr = json.getAsJsonArray();
	        for (JsonElement jsonElement : arr) {
	            if (jsonElement.isJsonObject()) {
	                m.add(context.deserialize(jsonElement, Map.class));
	            } else if (jsonElement.isJsonArray()) {
	                m.add(context.deserialize(jsonElement, List.class));
	            } else if (jsonElement.isJsonPrimitive()) {
	                Number num = null;
	                try {
	                    num = NumberFormat.getInstance().parse(jsonElement.getAsString());
	                } catch (Exception e) {
	                }
	                if (num != null) {
	                    m.add(num);
	                    continue;
	                }
	                JsonPrimitive prim = jsonElement.getAsJsonPrimitive();
	                if (prim.isBoolean()) {
	                    m.add(prim.getAsBoolean());
	                } else if (prim.isString()) {
	                    m.add(prim.getAsString());
	                } else {
	                    m.add(null);
	                }
	            }
	        }
	        return m;
	    }
	}
	
	public static String toJSON(Object obj) {
		if(obj instanceof CustomResource) return gson.toJson(((CustomResource) obj).attributes());
		else return k8sJson.serialize(obj);
	}

	public static String toYAML(Object obj) {
		if(obj instanceof CustomResource) return yaml.dump(((CustomResource) obj).attributes());
		else return io.kubernetes.client.util.Yaml.dump(obj);
	}
	
	public static CustomResource fromJSON(String s, CustomResourceDefinition crd) {
		Map<String, Object> attrs = gson.fromJson(s, Map.class);
		return new CustomResource(attrs, crd);
	}

	public static CustomResource fromYAML(String s, CustomResourceDefinition crd) {
		Map<String,Object> attrs = yaml.load(s);
		if(attrs.get("kind").equals(crd.names().kind())) return new CustomResource(attrs, crd);
		else throw new RuntimeException("Not a " + crd.names().kind()); 
	}
	
	public static <T> T fromJSON(String s, Class<T> clazz) {
		return k8sJson.deserialize(s, new TypeToken<T>(){}.getType());
	}

	public static <T> T fromYAML(String s, Class<T> clazz) {
		return io.kubernetes.client.util.Yaml.loadAs(s, clazz);
	}
}
