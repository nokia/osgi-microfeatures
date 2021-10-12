// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.dictionary;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.dictionary.AbstractDiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterTypeDictionary;
import com.nextenso.proxylet.diameter.util.DerivedFormat;

public class DiameterAVPDictionaryAdapter implements 
	JsonSerializer<DiameterAVPDictionary>,
	JsonDeserializer<DiameterAVPDictionary> {
	
	private static final String CUSTOM_TYPES_PROP = "customFormats";
	private static final String AVPS_PROP = "avpDefinitions";
	
	
	private DiameterTypeDictionary typeDic;
	
	public DiameterAVPDictionaryAdapter(DiameterTypeDictionary dic) {
		Objects.requireNonNull(dic);
		typeDic = dic;
	}

	@Override
	public DiameterAVPDictionary deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		if(!arg0.isJsonObject()) {
			throw new JsonParseException("Expected to deserialize a JSON Object");
		}
		JsonObject obj = arg0.getAsJsonObject();
		JsonArray customTypeArray = obj.getAsJsonArray(CUSTOM_TYPES_PROP);
		
		if(customTypeArray != null) {
			for(JsonElement elem : customTypeArray) {
				DerivedFormat format = arg2.deserialize(elem, DerivedFormat.class);
				typeDic.registerCustomFormat(format);
			}
		}
		
		List<DiameterAVPDefinition> defs = new ArrayList<>();
		JsonArray avpDefArray = obj.getAsJsonArray(AVPS_PROP);
		
		if(avpDefArray != null) {
			for(JsonElement elem : avpDefArray) {
				DiameterAVPDefinition def = arg2.deserialize(elem, DiameterAVPDefinition.class);
				defs.add(def);
			}
		}
		
		return new DiameterAVPDictionary(defs);
	}

	@Override
	public JsonElement serialize(DiameterAVPDictionary arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		
		JsonArray typeArray = new JsonArray();
		for(DerivedFormat format : arg0.getCustomFormats()) {
			typeArray.add(arg2.serialize(format));
		}
		
		obj.add(CUSTOM_TYPES_PROP, typeArray);
		
		JsonArray avpDefArray = new JsonArray();
		for(DiameterAVPDefinition def : arg0.getAVPDefList()) {
			avpDefArray.add(arg2.serialize(def));
		}
		
		obj.add(AVPS_PROP, avpDefArray);
		
		return obj;
	}

}
