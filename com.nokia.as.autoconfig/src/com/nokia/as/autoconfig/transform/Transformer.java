// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.transform;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.nokia.as.autoconfig.Configuration;

public class Transformer {
	
	public Configuration transform(Configuration original, Function<Map<String, Object>, Map<String, Object>> valueMapper) {
		Map<String, Map<String, Object>> transformedConfig = 
		original.config.entrySet().stream()
		               .collect(Collectors.toMap(Map.Entry::getKey, 
		                                         e -> transform(e.getValue(), valueMapper)));
		
		Map<String, List<Map<String, Object>>> transformedFactoryConfig =
		original.factoryConfig.entrySet().stream()
		                      .collect(Collectors.toMap(Map.Entry::getKey, 
		                                                e -> e.getValue().stream()
		                                                                 .map(m -> transform(m, valueMapper))
		                                                                 .collect(Collectors.toList()))); 
		
		return new Configuration(transformedConfig, transformedFactoryConfig);
	}
	
	private Map<String, Object> transform(Map<String, Object> map, Function<Map<String, Object>, Map<String, Object>> valueMapper) {
		return valueMapper.apply(map);
	}

}
