package com.nextenso.diameter.agent.dictionary;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nextenso.proxylet.diameter.dictionary.FlagPolicy;

public class FlagPolicyAdapter implements JsonSerializer<FlagPolicy>,
	JsonDeserializer<FlagPolicy> {

	@Override
	public FlagPolicy deserialize(JsonElement value, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		if(!value.isJsonPrimitive()) {
			throw new JsonParseException("expecting a String for flag policy");
		}
		
		JsonPrimitive prim = value.getAsJsonPrimitive();
		if(!prim.isString()) {
			throw new JsonParseException("expecting a String for flag policy");
		}
		
		String strPolicy = prim.getAsString();
		switch(strPolicy) {
		case "forbidden":
			return FlagPolicy.FORBIDDEN;
		case "optional":
			return FlagPolicy.OPTIONAL;
		case "required":
			return FlagPolicy.REQUIRED;
		default:
			throw new JsonParseException("unknown flag policy " + strPolicy);
		}
	}

	@Override
	public JsonElement serialize(FlagPolicy value, Type arg1, JsonSerializationContext arg2) {
		switch(value) {
			case FORBIDDEN:
				return new JsonPrimitive("forbidden");
			case OPTIONAL:
				return new JsonPrimitive("optional");
			case REQUIRED:
				return new JsonPrimitive("required");
			default:
				throw new RuntimeException("don't know how to serialize flag policy " + value);
		}
	}

}
