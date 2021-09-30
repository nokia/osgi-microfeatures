package com.nextenso.diameter.agent.dictionary;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinition;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDictionary;

public class DiameterCommandDictionaryAdapter implements JsonSerializer<DiameterCommandDictionary>, 
	JsonDeserializer<DiameterCommandDictionary>{

	private static final String DEFS_PROP = "commandDefinitions";
	
	@Override
	public JsonElement serialize(DiameterCommandDictionary arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		JsonArray array = new JsonArray();
		
		for(DiameterCommandDefinition def : arg0.getDefinitionSet()) {
			array.add(arg2.serialize(def));
		}
		
		obj.add(DEFS_PROP, array);
		return obj;
	}

	@Override
	public DiameterCommandDictionary deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		if(!arg0.isJsonObject()) {
			throw new JsonParseException("expected a JSON object");
		}
		
		JsonObject obj = arg0.getAsJsonObject();
		JsonArray array = obj.getAsJsonArray(DEFS_PROP);
		if(array == null) {
			throw new JsonParseException(DEFS_PROP + " array not found");
		}
		List<DiameterCommandDefinition> defs = new ArrayList<>();
		for(JsonElement el : array) {
			defs.add(arg2.deserialize(el, DiameterCommandDefinition.class));
		}
		
		return new DiameterCommandDictionary(defs);
	}

}
