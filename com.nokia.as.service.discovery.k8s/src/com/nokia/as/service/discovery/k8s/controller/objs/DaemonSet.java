// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.controller.objs;

import java.util.HashMap;
import java.util.Map;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1Toleration;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1DaemonSetSpec;

public class DaemonSet {
	
	public static V1DaemonSet create(Map<String, Object> props, V1PodSpec pod) {
		/*
		 * apiVersion: extensions/v1beta1
		 * kind: DaemonSet
		 * metadata:
		 *   name: %name%
		 *   labels:
		 *     app: %name%
		 * spec:
		 *   template:
		 *     spec:
		 *       %pod%
		 *         nodeSelector: {is_edge: 'true'}
		 *         tolerations:
		 *           - key: 'is_edge'
		 *             operator: 'Equal'
		 *             value: 'true'
		 *             effect: 'NoExecute'
		 */
		V1DaemonSet ds = 
				new V1DaemonSet()
				  .apiVersion("apps/v1")
				  .kind("DaemonSet");
		
		/** METADATA **/
		String name = props.get("name").toString() + "-ds";
		Map<String, String> labels = new HashMap<>();
		labels.put("app", name);
		V1ObjectMeta metadata = new V1ObjectMeta().name(name).labels(labels);
		
		ds = ds.metadata(metadata);
		
		/** POD TEMPLATE **/
		Map<String, String> nodeSelector = new HashMap<>();
		nodeSelector.put("is_edge", "true");
		
		V1PodTemplateSpec podSpec = new V1PodTemplateSpec()
									  .metadata(new V1ObjectMeta()
									    .labels(labels))
									  .spec(pod
								        .nodeSelector(nodeSelector)
								        .addTolerationsItem(new V1Toleration()
										  .key("is_edge")
									   	  .operator("Equal")
										  .value("true")
									      .effect("NoExecute")));
		
		ds = ds.spec(new V1DaemonSetSpec().template(podSpec));
		return ds;
	}
}
