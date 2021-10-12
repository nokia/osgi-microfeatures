// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.dictionary;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition.Dictionary;
import com.nextenso.proxylet.diameter.dictionary.AbstractDiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterAVPDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinition;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinitionBuilder;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDefinitionElement;
import com.nextenso.proxylet.diameter.dictionary.DiameterCommandDictionary;
import com.nextenso.proxylet.diameter.dictionary.DiameterTypeDictionary;
import com.nextenso.proxylet.diameter.dictionary.FlagPolicy;
import com.nextenso.proxylet.diameter.nasreq.NASApplicationConstants;
import com.nextenso.proxylet.diameter.util.DerivedFormat;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

public class DiameterDictionaryTest {

	@Test
	public void testAVPDictionarySerialization() throws Exception {
		
		DiameterDictionaryFactoryImpl fac = new DiameterDictionaryFactoryImpl();
		
		DiameterAVPDefinition avp1 = DiameterBaseConstants.AVP_ACCOUNTING_REALTIME_REQUIRED;
		DiameterAVPDefinition avp2 = DiameterBaseConstants.AVP_ACCOUNTING_SESSION_ID;
		DiameterAVPDefinition avp3 = new DiameterAVPDefinition("My-Custom-AVP", 666L, 420L, 
				DiameterAVPDefinition.OPTIONAL_FLAG, 
				DiameterAVPDefinition.OPTIONAL_FLAG, 
				DiameterAVPDefinition.OPTIONAL_FLAG, false, 
				new DerivedFormat("MyCustomFormat", OctetStringFormat.INSTANCE), 
				false);
		
		List<DiameterAVPDefinition> list = Arrays.asList(avp1, avp2, avp3);
		DiameterAVPDictionary dic = new DiameterAVPDictionary(list);
		
		GsonBuilder builder = new GsonBuilder(); 
		
		DiameterTypeDictionary typeDic = new DiameterTypeDictionary();

		builder
			.registerTypeAdapter(FlagPolicy.class, new FlagPolicyAdapter())
			.registerTypeAdapter(DerivedFormat.class, new DerivedFormatAdapter(typeDic))
			.registerTypeAdapter(AbstractDiameterAVPDictionary.class, new DiameterAVPDictionaryAdapter(typeDic))
			.registerTypeAdapter(DiameterAVPDictionary.class, new DiameterAVPDictionaryAdapter(typeDic))
			.registerTypeAdapter(DiameterAVPDefinition.class, new DiameterAVPDefinitionAdapter(typeDic));
		
	    Gson gson = builder.create(); 
	    
	    String json = gson.toJson(dic);
	    System.out.println(json);

	    AbstractDiameterAVPDictionary deserialized = gson.fromJson(json, AbstractDiameterAVPDictionary.class);
	    Assert.assertEquals(dic.getAVPDefList(), deserialized.getAVPDefList());
	
		DiameterAVPDictionary deserialized2 = 
				fac.loadAVPDictionaryFromJSON(json);
		
		Assert.assertEquals(deserialized2.getAVPDefList(), dic.getAVPDefList());
		
	}
	
	@Test
	public void testCommandDictionarySerialization() throws Exception {
		DiameterDictionaryFactoryImpl fac = new DiameterDictionaryFactoryImpl();

		DiameterCommandDefinitionBuilder builder = new DiameterCommandDefinitionBuilder();
		
		DiameterAVPDefinition eapPayload = new DiameterAVPDefinition
				("EAP-Payload", 462, 0, DiameterAVPDefinition.FORBIDDEN_FLAG,
						DiameterAVPDefinition.OPTIONAL_FLAG,
						DiameterAVPDefinition.OPTIONAL_FLAG, 
						false, 
						OctetStringFormat.INSTANCE,
						false);

		DiameterAVPDictionary avpDico = new DiameterAVPDictionary(Arrays.asList(eapPayload));

		builder.name("Diameter-EAP-Request", "Diameter-EAP-Answer")
			.abbreviation("DER", "DEA")
			.code(268)
			.applicationId(0)
			.proxiableBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
			.errorBitPolicy(FlagPolicy.OPTIONAL, FlagPolicy.OPTIONAL)
			.requestAVP(DiameterBaseConstants.AVP_SESSION_ID, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_ORIGIN_HOST, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_ORIGIN_REALM, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_DESTINATION_REALM, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_AUTH_REQUEST_TYPE, 1, 1)
			.requestAVP(DiameterBaseConstants.AVP_DESTINATION_HOST, 0, 1)
			.requestAVP(NASApplicationConstants.AVP_NAS_IDENTIFIER, 0, 1)
			.requestAVP(eapPayload, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_PROXY_INFO, 0, -1)
			.answerAVP(DiameterBaseConstants.AVP_SESSION_ID, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_ORIGIN_HOST, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_ORIGIN_REALM, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_DESTINATION_REALM, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_AUTH_REQUEST_TYPE, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_DESTINATION_HOST, 0, 1)
			.answerAVP(NASApplicationConstants.AVP_NAS_IDENTIFIER, 0, 1)
			.answerAVP(eapPayload, 1, 1)
			.answerAVP(DiameterBaseConstants.AVP_PROXY_INFO, 0, -1);
		DiameterCommandDefinition command = builder.build();
		
		DiameterCommandDictionary commandDic = 
				new DiameterCommandDictionary(Arrays.asList(command));
		
		GsonBuilder gsonBuilder = new GsonBuilder(); 
		
		gsonBuilder
			.registerTypeAdapter(DiameterCommandDefinitionElement.class, new DiameterCommandDefinitionElementAdapter(avpDico))
			.registerTypeAdapter(DiameterCommandDictionary.class, new DiameterCommandDictionaryAdapter());
		
		Gson gson = gsonBuilder.create();		
		
		String json = gson.toJson(commandDic);
		
		System.out.println(json);
		
		DiameterCommandDictionary deserialized = 
				gson.fromJson(json, DiameterCommandDictionary.class);
		
		DiameterCommandDefinition deserializedDef = deserialized.getCommandDefinitionByName("Diameter-EAP-Request");
		Assert.assertNotNull(deserializedDef);
		
		Assert.assertEquals(command, deserializedDef);
		
		Assert.assertEquals(deserialized.getDefinitionSet(), 
				commandDic.getDefinitionSet());
		
		DiameterCommandDictionary deserialized2 
			= fac.loadCommandDictionaryFromJSON(json, avpDico);
		
		Assert.assertEquals(deserialized2.getDefinitionSet(),
				commandDic.getDefinitionSet());
				
	}
	
	@Test
	public void testDuplicatesInAVPDictionary() throws Exception {
		@SuppressWarnings("unused")
		DiameterAVPDefinition def1 = new DiameterAVPDefinition("My-Command", 210, 1, 0, 0, 0, false, 
				UTF8StringFormat.INSTANCE);
		
		DiameterAVPDefinition def2 = new DiameterAVPDefinition("My-Command", 215, 1, 0, 0, 0, false,
				Integer32Format.INSTANCE);
		
		
		DiameterAVPDefinition obtained = Dictionary.getDiameterAVPDefinition(215, 1);
		DiameterAVPDefinition obtained2 = Dictionary.getBackingAVPDictionary().getAVPDefinitionByName("My-Command");
		Assert.assertEquals(def2, obtained);
		Assert.assertEquals(def2, obtained2);
		
	}

}
