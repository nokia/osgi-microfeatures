package com.nextenso.proxylet.diameter.dictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.nextenso.proxylet.diameter.util.AddressFormat;
import com.nextenso.proxylet.diameter.util.DerivedFormat;
import com.nextenso.proxylet.diameter.util.DiameterAVPFormat;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.Float32Format;
import com.nextenso.proxylet.diameter.util.Float64Format;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.IPFilterRuleFormat;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.Integer64Format;
import com.nextenso.proxylet.diameter.util.OctetStringFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.URIFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.diameter.util.Unsigned64Format;

/**
 * A dictionary for Diameter Formats. Let you retrieve Diameter Formats
 * by their name 
 */
public class DiameterTypeDictionary {
	
	private Map<String,DiameterAVPFormat> mapping;
	
	public DiameterTypeDictionary() {
		mapping = new HashMap<>();
		putStandardTypes();
	}
	
	public DiameterAVPFormat getFormatForTypeName(String type) {
		return mapping.get(type);
	}
	
	public void registerCustomFormat(DerivedFormat format) {
		Objects.requireNonNull(format);
		mapping.put(format.getName(), format);
	}

	private void putStandardTypes() {
		mapping.put(AddressFormat.INSTANCE.getName(), AddressFormat.INSTANCE);
		mapping.put(EnumeratedFormat.INSTANCE.getName(), EnumeratedFormat.INSTANCE);
		mapping.put(Float32Format.INSTANCE.getName(), Float32Format.INSTANCE);
		mapping.put(Float64Format.INSTANCE.getName(), Float64Format.INSTANCE);
		mapping.put(GroupedFormat.INSTANCE.getName(), GroupedFormat.INSTANCE);
		mapping.put(IdentityFormat.INSTANCE.getName(), IdentityFormat.INSTANCE);
		mapping.put(Integer32Format.INSTANCE.getName(), Integer32Format.INSTANCE);
		mapping.put(Integer64Format.INSTANCE.getName(), Integer64Format.INSTANCE);
		mapping.put(IPFilterRuleFormat.INSTANCE.getName(), IPFilterRuleFormat.INSTANCE);
		mapping.put(OctetStringFormat.INSTANCE.getName(), OctetStringFormat.INSTANCE);
		mapping.put(TimeFormat.INSTANCE.getName(), TimeFormat.INSTANCE);
		mapping.put(Unsigned32Format.INSTANCE.getName(), Unsigned32Format.INSTANCE);
		mapping.put(Unsigned64Format.INSTANCE.getName(), Unsigned64Format.INSTANCE);
		mapping.put(URIFormat.INSTANCE.getName(), URIFormat.INSTANCE);
		mapping.put(UTF8StringFormat.INSTANCE.getName(), UTF8StringFormat.INSTANCE);
	}
}
