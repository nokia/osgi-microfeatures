// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.discovery.k8s.controller.objs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServicePort;
import io.kubernetes.client.openapi.models.V1ServiceSpec;

public class Service {
	
	public static V1Service create(Map<String, Object> props) {
		String name = props.get("name") + "-svc";
		
		/*
		 * apiVersion: v1
		 * kind: Service
		 * metadata:
		 *   name: %name%-svc
		 *   namespace: %namespace%
		 * spec:
		 *   clusterIP: None
		 *   type: ClusterIP
		 *   ports:
		 *     %ports%
		 *   selector:
		 *     app: %name%
		 */
		
		V1ObjectMeta metadata = new V1ObjectMeta()
									.name(name);
		
		List<V1ServicePort> ports = new ArrayList<>();
		((List<Map<String, Object>>) props.get("ports")).forEach(p -> {
			String portName = String.valueOf(p.get("name"));
			int portNumber = new Double(p.get("port").toString()).intValue();
			String protocol = String.valueOf(p.getOrDefault("protocol", "TCP"));
			
			V1ServicePort servicePort = new V1ServicePort()
											.name(portName)
											.port(portNumber)
											.protocol(protocol);
			ports.add(servicePort);
		});
		
		HashMap<String, String> selector = new HashMap<>();
		selector.put("app", (String) props.get("name"));
		
		return new V1Service()
				.metadata(metadata)
				.spec(new V1ServiceSpec()
					.clusterIP("None")
					.type("ClusterIP")
					.ports(ports)
					.selector(selector));
	}
}
