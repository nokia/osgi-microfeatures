// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.dictionary;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.dictionary.AbstractDiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinitionElement;

public class DiameterCommandDefinitionElementAdapter implements JsonSerializer<DiameterCommandDefinitionElement>,
	JsonDeserializer<DiameterCommandDefinitionElement> {
	
	private AbstractDiameterAVPDictionary dico;
	
	private static final String AVP_PROP = "avpName";
	private static final String MIN_OCCURENCE_PROP = "minOccurence";
	private static final String MAX_OCCURENCE_PROP = "maxOccurence";
	
	public DiameterCommandDefinitionElementAdapter(AbstractDiameterAVPDictionary dico) {
		Objects.requireNonNull(dico);
		this.dico = dico;
	}

	@Override
	public JsonElement serialize(DiameterCommandDefinitionElement arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		
		obj.addProperty(AVP_PROP, arg0.getAVPDefinition().getAVPName());
		obj.add(MIN_OCCURENCE_PROP, occurenceCodeToJsonPrimitive(arg0.getMinOccurence()));
		obj.add(MAX_OCCURENCE_PROP, occurenceCodeToJsonPrimitive(arg0.getMaxOccurence()));
		return obj;
	}

	@Override
	public DiameterCommandDefinitionElement deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		if(!arg0.isJsonObject()) {
			throw new JsonParseException("expected a JSON Object");
		}
		
		JsonObject obj = arg0.getAsJsonObject();
		JsonPrimitive namePrimitive = obj.getAsJsonPrimitive(AVP_PROP);
		
		if(namePrimitive == null) {
			throw new JsonParseException(AVP_PROP + " property missing");
		}
		
		String name = namePrimitive.getAsString();
		DiameterAVPDefinition def = dico.getAVPDefinitionByName(name);
		
		if(def == null) {
			throw new JsonParseException("no such AVP in Dictionary : " + name);
		}
		
		JsonElement minOccurenceElement = obj.get(MIN_OCCURENCE_PROP);
		JsonElement maxOccurenceElement = obj.get(MAX_OCCURENCE_PROP);
		int minOccurence = jsonElementToOccurenceCode(minOccurenceElement);
		int maxOccurence = jsonElementToOccurenceCode(maxOccurenceElement);
		
		DiameterCommandDefinitionElement elem = 
				new DiameterCommandDefinitionElement(def, minOccurence, maxOccurence);
		
		return elem;
	}
	
	private JsonPrimitive occurenceCodeToJsonPrimitive(int code) {
		if(code == -1) {
			return new JsonPrimitive("*");
		} else {
			return new JsonPrimitive(code);
		}
	}
	
	private int jsonElementToOccurenceCode(JsonElement el) throws JsonParseException {
		if(el == null) {
			return -1;
		}
		
		if(!el.isJsonPrimitive()) {
			throw new JsonParseException("cannot parse element " + el.toString() 
			+ ", must be either \"*\" or an integer");
		}
		
		JsonPrimitive primitive =  el.getAsJsonPrimitive();
		
		if(primitive.isString()) {
			String primAsString = primitive.getAsString();
			if("*".equals(primAsString.trim())) {
				return -1;
			} else {
				throw new JsonParseException("unrecognized string for occurence " + primAsString);
			}
		} else if(primitive.isNumber()) {
			return el.getAsInt();
		} else {
			throw new JsonParseException("cannot parse element " + el.toString() 
			+ ", must be either \"*\" or an integer");
		}
	}
}
