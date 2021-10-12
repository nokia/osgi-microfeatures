// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.controller.objs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;

public class Deployment {
	
	public static V1Deployment create(Map<String, Object> props, V1PodSpec pod) {
		/*
		 * apiVersion: extensions/v1beta1
		 * kind: Deployment
		 * metadata:
		 *   name: %name%
		 *   labels:
		 *     app: %name%
		 * spec:
		 *   replicas: %replicas%
		 *   template:
		 *     (if %prometheus%)
	     *     metadata:
	     *       labels:
	     *         %labels%
		 *       annotations:
		 *         prometheus.io/scrape: "true"
		 *         prometheus.io/port: %prometheus-port%
		 *     (end)
		 *   spec:
		 *     %pod%
		 */
		V1Deployment deploy = 
				new V1Deployment()
				  .apiVersion("apps/v1")
				  .kind("Deployment")
				  .spec(new V1DeploymentSpec()
						  .replicas((int) props.get("runtime.replicas")));
		
		/** METADATA **/
		String name = props.get("name").toString() + "-deploy";
		Map<String, String> labels = new HashMap<>();
		labels.put("app", name);

		List<Map<String, Object>> definedLabels = (List<Map<String, Object>>) props.get("configuration.labels");
		definedLabels.forEach(l -> {
			labels.put(l.get("name").toString(), l.get("value").toString());
		});

		V1ObjectMeta metadata = new V1ObjectMeta().name(name).labels(labels);
		
		deploy = deploy.metadata(metadata);
		
		HashMap<String, String> annotations = new HashMap<>();
		if(props.containsKey("configuration.prometheus.port")) {
			annotations.put("prometheus.io/scrape", "true");
			//else we have 8080.0, who knows why...
			Integer prometheusPort = Double.valueOf(String.valueOf(props.get("configuration.prometheus.port"))).intValue();
			annotations.put("prometheus.io/port", prometheusPort.toString());
			annotations.put("prometheus.io/path", String.valueOf(props.get("configuration.prometheus.path")));
		}
		
		/** POD TEMPLATE **/
		V1PodTemplateSpec podSpec = new V1PodTemplateSpec()
									  .metadata(new V1ObjectMeta()
									    .labels(labels)
									    .annotations(annotations))
									  .spec(pod);

		deploy = deploy.spec(deploy.getSpec().template(podSpec));
		return deploy;
	}
}
