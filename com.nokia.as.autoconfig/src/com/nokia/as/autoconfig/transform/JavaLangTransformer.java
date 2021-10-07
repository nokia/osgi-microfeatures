package com.nokia.as.autoconfig.transform;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JavaLangTransformer implements Function<Map<String, Object>, Map<String, Object>> {

	@SuppressWarnings("serial")
	private static final Map<String , Function<String, Object>> SWAP = 
			new HashMap<String , Function<String, Object>>() {{
	    put("java.lang.Integer", 	Integer::valueOf);
	    put("java.lang.Boolean", 	Boolean::valueOf);
	    put("java.lang.Byte", 		Byte::valueOf);
	    put("java.lang.Character",	s -> s.charAt(0));
	    put("java.lang.Short", 		Short::valueOf);
	    put("java.lang.Long", 		Long::valueOf);
	    put("java.lang.Float", 		Float::valueOf);
	    put("java.lang.Double", 	Double::valueOf);
	    put("java.lang.String", 	String::valueOf);
	}};
	
	@Override
	public Map<String, Object> apply(Map<String, Object> t) {
		return replaceJavaLangs(t);
	}
	
	private Map<String, Object> replaceJavaLangs(Map<String, Object> map) {
		return
    		map.entrySet().stream()
    		   .map(this::replace)
    		   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
	
	//example: "java.lang.Integer-foo" = "1" -> "foo" = 1
	private Map.Entry<String, Object> replace(Map.Entry<String, Object> entry) {
		if(!(entry.getValue() instanceof String)) return entry;
	    String v = (String) entry.getValue();
	    String[] split = entry.getKey().split("-");
	    
	    //if the class is defined, then we apply the corresponding function
	    return SWAP.containsKey(split[0]) ? new AbstractMap.SimpleEntry<>(split[1], SWAP.get(split[0]).apply(v))
	                                       : entry;
	}

}
