package com.nextenso.diameter.agent.dictionary;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.dictionary.DiameterTypeDictionary;
import com.nextenso.proxylet.diameter.util.DiameterAVPFormat;

public class DiameterAVPDefinitionAdapter implements JsonSerializer<DiameterAVPDefinition>,
	JsonDeserializer<DiameterAVPDefinition> {

	private static final String CODE_PROP_NAME = "code";
	private static final String VENDOR_PROP_NAME = "vendorId";
	private static final String VFLAG_PROP_NAME = "vendorFlagPolicy";
	private static final String MFLAG_PROP_NAME = "mandatoryFlagPolicy";
	private static final String PFLAG_PROP_NAME = "protectedFlagPolicy";
	private static final String SENSITIVE_PROP_NAME = "needsEncryption";
	private static final String NAME_PROP_NAME = "name";
	private static final String TYPE_PROP_NAME = "type";
	
	private static final String MANDATORY_FLAG_POLICY = "required";
	private static final String OPTIONAL_FLAG_POLICY = "optional";
	private static final String FORBIDDEN_FLAG_POLICY = "forbidden";
	
	
	private DiameterTypeDictionary typeDic;
	
	public DiameterAVPDefinitionAdapter(DiameterTypeDictionary dic) {
		this.typeDic = dic;
	}

	@Override
	public JsonElement serialize(DiameterAVPDefinition arg0, Type arg1, JsonSerializationContext arg2) {
		JsonObject obj = new JsonObject();
		obj.addProperty(NAME_PROP_NAME,  arg0.getAVPName());
		obj.addProperty(CODE_PROP_NAME, arg0.getAVPCode());
		obj.addProperty(VENDOR_PROP_NAME, arg0.getVendorId());
		obj.addProperty(VFLAG_PROP_NAME, policyCodeToLabel(arg0.getVFlag()));
		obj.addProperty(MFLAG_PROP_NAME, policyCodeToLabel(arg0.getMFlag()));
		obj.addProperty(PFLAG_PROP_NAME, policyCodeToLabel(arg0.getPFlag()));
		obj.addProperty(SENSITIVE_PROP_NAME, arg0.needsEncryption());
		
		if(arg0.getVendorId() != -1 ) {
			obj.addProperty(VENDOR_PROP_NAME, arg0.getVendorId());
		}
		
		obj.addProperty(TYPE_PROP_NAME, arg0.getDiameterAVPFormat().getName());
		
		
		return obj;
	}
	
	private String policyCodeToLabel(int code) {
		switch (code) {
		
		case DiameterAVPDefinition.FORBIDDEN_FLAG:
			return FORBIDDEN_FLAG_POLICY;
		case DiameterAVPDefinition.OPTIONAL_FLAG:
			return OPTIONAL_FLAG_POLICY;
		case DiameterAVPDefinition.REQUIRED_FLAG:
			return MANDATORY_FLAG_POLICY;
		default:
			throw new RuntimeException("unknown policy code!");
		}
	}
	
	private int labelToPolicyCode(String label) {
		switch(label) {
		case MANDATORY_FLAG_POLICY:
			return DiameterAVPDefinition.REQUIRED_FLAG;
		case FORBIDDEN_FLAG_POLICY:
			return DiameterAVPDefinition.FORBIDDEN_FLAG;
		case OPTIONAL_FLAG_POLICY:
			return DiameterAVPDefinition.OPTIONAL_FLAG;
		default:
			throw new RuntimeException("unknown policy label!");
		}
	}
	
	

	@Override
	public DiameterAVPDefinition deserialize(JsonElement arg0, Type arg1, JsonDeserializationContext arg2)
			throws JsonParseException {
		JsonObject object = arg0.getAsJsonObject();
		String name = object.get(NAME_PROP_NAME).getAsString();
		Long code = object.get(CODE_PROP_NAME).getAsLong();
		Long vendorId = object.get(VENDOR_PROP_NAME).getAsLong();
		int vFlag = labelToPolicyCode(object.get(VFLAG_PROP_NAME).getAsString());
		int mFlag = labelToPolicyCode(object.get(MFLAG_PROP_NAME).getAsString());
		int pFlag = labelToPolicyCode(object.get(PFLAG_PROP_NAME).getAsString());
		String typeName = object.get(TYPE_PROP_NAME).getAsString();
		Boolean needsEncryption = object.get(SENSITIVE_PROP_NAME).getAsBoolean();
		
		DiameterAVPFormat type = typeDic.getFormatForTypeName(typeName);
		
		DiameterAVPDefinition avpDef = new DiameterAVPDefinition(name, code, vendorId, vFlag, mFlag, pFlag, needsEncryption, type, false);
		
		return avpDef;
	}

}
