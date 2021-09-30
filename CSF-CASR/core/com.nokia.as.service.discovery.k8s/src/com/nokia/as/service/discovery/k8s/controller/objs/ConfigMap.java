package com.nokia.as.service.discovery.k8s.controller.objs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import io.kubernetes.client.openapi.models.V1ConfigMap;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class ConfigMap {

	@SuppressWarnings("unchecked")
	public static V1ConfigMap create(Map<String, Object> props) {
		/*
		 * apiVersion: v1
		 * kind: ConfigMap
		 * metadata:
		 *   name: %name%-cm
		 * data:
		 *   %key%: %value%
		 */
		String name = props.get("name").toString() + "-cm";
		
		/** DATA **/
		Map<String, String> data = new HashMap<>();
		List<Map<String, Object>> override = (List<Map<String, Object>>) props.get("configuration.override");
		override.forEach(e -> {
			boolean replace = Boolean.parseBoolean(e.getOrDefault("replace", "false").toString());
			String filename = e.get("pid") + ".cfg.yaml";
			if(!replace) filename = filename + ".patch";
				
			List<Map<String, Object>> properties = (List<Map<String, Object>>) e.getOrDefault("props", new ArrayList<>());
			Map<String, String> toParse = new HashMap<>();
			properties.forEach(p -> {
				toParse.put(p.get("name").toString(), p.get("value").toString());
			});
				
			Yaml yaml = new Yaml();
			DumperOptions options = new DumperOptions();
		    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		    options.setPrettyFlow(true);
			String content = yaml.dump(toParse);
			data.put(filename, content);
		});
		
		List<Map<String, Object>> files = (List<Map<String, Object>>) props.get("configuration.files");
		files.forEach(e -> {
			String file = e.get("name").toString();
			String content = e.get("content").toString();
			data.put(file, content);
		});
		
		if(props.get("configuration.configMapName").toString().isEmpty()) {
			props.put("configuration.configMapName", name);
		}
		
		return new V1ConfigMap()
			     .metadata(new V1ObjectMeta()
			       .name(name))
			     .data(data);
	}
}
