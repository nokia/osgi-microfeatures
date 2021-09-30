package com.nokia.as.service.discovery.k8s.controller.objs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1HTTPIngressPath;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1HTTPIngressRuleValue;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressBackend;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressRule;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class Ingress {
	
	public static ExtensionsV1beta1Ingress create(Map<String, Object> props) {
		String name = props.get("name").toString() + "-ing";
		
		/*
		 * apiVersion: extensions/v1beta1
		 * kind: Ingress
		 * metadata:
		 *   name: %name%-ing
		 *   namespace: %namespace%
		 *   annotations:
		 *      nginx.ingress.kubernetes.io/rewrite-target: /
		 * spec:
		 *   rules:
		 *     - http:
		 *         paths:
		 *           - path: %path%
		 *             backend:
		 *               serviceName: %name%-svc
		 *               servicePort: %port%
		 */
		
		HashMap<String, String> annotations = new HashMap<>();
		annotations.put("nginx.ingress.kubernetes.io/rewrite-target", "/");
		
		List<ExtensionsV1beta1HTTPIngressPath> paths = new ArrayList<>();
		((List<Map<String, Object>>) props.get("ports")).stream().filter(p -> p.get("ingress") != null).forEach(p -> {
			Map<String, Object> ingress = (Map<String, Object>) p.get("ingress");
			int portNumber = new Double(p.get("port").toString()).intValue();
			String ingressPath = String.valueOf(ingress.get("path"));
			
			ExtensionsV1beta1HTTPIngressPath path = new ExtensionsV1beta1HTTPIngressPath()
					.path(ingressPath)
					.backend(new ExtensionsV1beta1IngressBackend()
						.serviceName(props.get("name") + "-svc")
						.servicePort(new IntOrString(portNumber)));
			paths.add(path);
		});
		
		return new ExtensionsV1beta1Ingress()
				.metadata(new V1ObjectMeta()
					.name(name)
					.annotations(annotations))
				.spec(new ExtensionsV1beta1IngressSpec()
					.addRulesItem(new ExtensionsV1beta1IngressRule()
						.http(new ExtensionsV1beta1HTTPIngressRuleValue()
							.paths(paths))));
	}
}
