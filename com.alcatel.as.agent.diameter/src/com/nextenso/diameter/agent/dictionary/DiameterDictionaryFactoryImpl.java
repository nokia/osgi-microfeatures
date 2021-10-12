// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.dictionary;

import java.io.Reader;
import java.util.Objects;

import org.apache.felix.dm.annotation.api.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.dictionary.AbstractDiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinitionElement;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterDictionaryFactory;
import com.nextenso.proxylet.diameter.dictionary.DiameterTypeDictionary;
import com.nextenso.proxylet.diameter.dictionary.FlagPolicy;
import com.nextenso.proxylet.diameter.nasreq.NASApplicationConstants;
import com.nextenso.proxylet.diameter.util.DerivedFormat;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;

@Component
public class DiameterDictionaryFactoryImpl implements DiameterDictionaryFactory {

	@Override
	public DiameterAVPDictionary loadAVPDictionaryFromJSON(Reader reader) throws LoadingException {
		Objects.requireNonNull(reader);
		
		DiameterTypeDictionary typeDic = new DiameterTypeDictionary();

		GsonBuilder builder = new GsonBuilder(); 

		builder
			.registerTypeAdapter(FlagPolicy.class, new FlagPolicyAdapter())
			.registerTypeAdapter(DerivedFormat.class, new DerivedFormatAdapter(typeDic))
			.registerTypeAdapter(DiameterAVPDictionary.class, new DiameterAVPDictionaryAdapter(typeDic))
			.registerTypeAdapter(DiameterAVPDefinition.class, new DiameterAVPDefinitionAdapter(typeDic));
		
		Gson gson = builder.create(); 
		try {
			return gson.fromJson(reader, DiameterAVPDictionary.class);
		} catch (Exception e) {
			throw new LoadingException("Failed to load AVP Dictionary", e);
		}
	}

	@Override
	public DiameterCommandDictionary loadCommandDictionaryFromJSON(Reader reader, AbstractDiameterAVPDictionary dico)
			throws LoadingException {
		Objects.requireNonNull(reader);
		GsonBuilder gsonBuilder = new GsonBuilder(); 
		
		DiameterBaseConstants.init();
		NASApplicationConstants.init();
		
		gsonBuilder
			.registerTypeAdapter(DiameterCommandDefinitionElement.class, 
					new DiameterCommandDefinitionElementAdapter(dico))
			.registerTypeAdapter(DiameterCommandDictionary.class, new DiameterCommandDictionaryAdapter());
		
		Gson gson = gsonBuilder.create();	
		try {
			return gson.fromJson(reader, DiameterCommandDictionary.class);
		} catch(Exception e) {
			throw new LoadingException("Failed to load Command Dictionary", e);
		}
	}
}
