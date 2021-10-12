// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.transform;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.nokia.as.autoconfig.Utils;

public class EnvironmentTransformer implements Function<Map<String, Object>, Map<String, Object>> {

	//This regex matches ${<string>} and ${<string>}{<string>}
	private static final String REGEX = "\\$(\\{([^}]+?)\\})(\\{([^}]+?)\\})?";
	private static final Pattern PTN = Pattern.compile(REGEX);
	
	@Override
	public Map<String, Object> apply(Map<String, Object> t) {
		return replaceEnvironmentProperties(t);
	}
	
	private Map<String, Object> replaceEnvironmentProperties(Map<String, Object> map) {
		return map.entrySet().stream()
				   .collect(Collectors.toMap(Map.Entry::getKey,
				                             e -> e.getValue() instanceof String ? 
				                                     replace((String) e.getValue()) : 
				                                     e.getValue()));
	}
	
	private String replace (String value) {
  	    Matcher m = PTN.matcher(value);
  	    StringBuffer buf = new StringBuffer();
  	    
  	    //${<first>}{<second>}
  	    while(m.find()) {
  	        String val = Utils.getSystemProperty(m.group(2)); //first look in system properties
  	        if (val == null) val = Utils.getEnvProperty(m.group(2)); //then in environment
  	        if(val != null) {
  	            m.appendReplacement(buf, Matcher.quoteReplacement(val)); //<first> is a valid property
  	        } else if(m.group(3) != null) {
  	            m.appendReplacement(buf, Matcher.quoteReplacement(m.group(4))); //<first> is invalid and <second> is defined
  	        } else {
  	            m.appendReplacement(buf, Matcher.quoteReplacement(m.group())); //<first> is invalid and <second> is not defined
  	        }
  	    }
  	    m.appendTail(buf);
  	    return buf.toString();
	}
}
