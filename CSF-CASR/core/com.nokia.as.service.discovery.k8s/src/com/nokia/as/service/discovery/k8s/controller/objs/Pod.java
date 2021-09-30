package com.nokia.as.service.discovery.k8s.controller.objs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.V1ConfigMapVolumeSource;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1ContainerPort;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1PodSecurityContext;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1Probe;
import io.kubernetes.client.openapi.models.V1SecretVolumeSource;
import io.kubernetes.client.openapi.models.V1ServiceAccount;
import io.kubernetes.client.openapi.models.V1TCPSocketAction;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.openapi.models.V1VolumeMount;

public class Pod {
	
	@SuppressWarnings("unchecked")
	public static V1PodSpec create(Map<String, Object> props, V1ServiceAccount sa) {
		/*
		 * spec:
		 *   securityContext:
		 *     runAsUser: 7777
		 *     fsGroup: 7777
		 *   volumes:
		 *   (if %configMapName%)
		 *     - name: configuration-volume
		 *       configMap:
		 *         name: %configMapName%
		 *   (end)
		 *   (if %tlsSecret%)
		 *     - name: tls-secret
		 *       secret:
		 *         secretName: %tlsSecret%
		 *   (end)
		 *   containers:
		 *     - name: %name%
		 *       image: "%registry%/%imageRepo%:%imageTag%"
		 *       imagePullPolicy: IfNotPresent
		 *     env: %env%
		 *     ports:
		 *       %ports%
         *       - name: gogo
         *         containerPort: 17000
         *         protocol: TCP
         *     livenessProbe:
         *       tcpSocket:
         *         port: gogo
         *       initialDelaySeconds: 5
         *       periodSeconds: 10
         *     readinessProbe:
         *       tcpSocket:
         *         port: gogo
         *       initialDelaySeconds: 15
         *       periodSeconds: 20
         *     volumeMounts:
         *     (if %tlsSecret%)
         *       - name: tls-secret
         *         mountPath: "/casr/tls"
         *     (end)
         *     (if %configMapName%)
         *       - name: configuration-volume
         *         mountPath: /tmp/conf
         *     (end)
         *  serviceAccountName: %saName%
		 */
		V1PodSpec pod = new V1PodSpec();
		
		/** SECURITY CONTEXT **/
		V1PodSecurityContext securityContext = new V1PodSecurityContext().runAsUser(7777l).runAsGroup(7777l);
		pod = pod.securityContext(securityContext);
		
		/** VOLUME DECLARATIONS **/
		String configMapName = props.get("configuration.configMapName").toString();
		String secretName = props.get("configuration.tlsSecret").toString();
		
		if(!(configMapName.isEmpty())) {
			V1Volume cm = new V1Volume()
					        .name("configuration-volume")
					        .configMap(new V1ConfigMapVolumeSource().name(configMapName));
			pod = pod.addVolumesItem(cm);
		}
		
		if(!(secretName.isEmpty())) {
			V1Volume secret = new V1Volume()
		                        .name("tls-secret")
		                        .secret(new V1SecretVolumeSource().secretName(secretName));
			pod = pod.addVolumesItem(secret);
		}
		
		/** CONTAINER **/
		String name = props.get("name").toString();
		String image = props.get("runtime.features.registry") + "/" + "casr" + ":" + props.get("runtime.features.imageTag");
		if((boolean) props.get("docker")) {
			image = props.get("runtime.docker.registry") + "/" + props.get("runtime.docker.imageRepo") + ":" + props.get("runtime.docker.imageTag");
		}
		
		V1Container container = new V1Container()
								  .name(name)
								  .image(image)
								  .imagePullPolicy("Always");
		
		/*** ENVIRONMENT ***/
		List<Map<String, Object>> env = (List<Map<String, Object>>) props.get("configuration.env");
		for(Map<String, Object> e : env) {
			String envName = e.get("name").toString();
			String envValue = e.get("value").toString();
			V1EnvVar envVar = new V1EnvVar().name(envName).value(envValue);
			container = container.addEnvItem(envVar);
		};
		
		if(!((boolean) props.get("docker"))) {
			String features = ((List<String>) props.get("runtime.features")).stream().collect(Collectors.joining(","));
			container = container.addEnvItem(new V1EnvVar().name("INSTANCE_NAME").value("casr"))
								 .addEnvItem(new V1EnvVar().name("INSTANCE_VERSION").value("1.0.0"))
								 .addEnvItem(new V1EnvVar().name("CASR_VERSION").value(props.get("runtime.features.casrVersion").toString()))
								 .addEnvItem(new V1EnvVar().name("CASR_REPO").value(props.get("runtime.features.casrRepo").toString()))
								 .addEnvItem(new V1EnvVar().name("OBR_URL").value(props.getOrDefault("runtime.features.obrUrl", "").toString()))
								 .addEnvItem(new V1EnvVar().name("FEATURES").value(features));
		}
		
		/*** PORTS ***/
		AtomicBoolean deployment = new AtomicBoolean(true);
		AtomicBoolean ingress = new AtomicBoolean(false);
		List<V1ContainerPort> ports = new ArrayList<>();
		
		((List<Map<String, Object>>) props.get("ports")).forEach(p -> {
			String portName = p.get("name").toString();
			int portNumber = new Double(p.get("port").toString()).intValue();
			String protocol = p.getOrDefault("protocol", "TCP").toString();
			boolean external = Boolean.parseBoolean(p.getOrDefault("external", "false").toString());
			boolean ingressPath = (p.get("ingress") != null);
			
			if(external) deployment.set(false); //if one is has an external port, we should do a daemonset instead of a deployment
			if(ingressPath) ingress.set(true);
			
			V1ContainerPort containerPort = new V1ContainerPort()
					.name(portName)											 
					.containerPort(portNumber)
					.protocol(protocol);
			
			if(external) containerPort = containerPort.hostPort(portNumber);
			ports.add(containerPort);
		});
		
		props.put("deployment", deployment.get());
		props.put("ports.ingress", ingress.get());
		ports.add(new V1ContainerPort().name("gogo").containerPort(17000).protocol("TCP")); //gogo my man should always be here
		ports.add(new V1ContainerPort().name("gogows").containerPort(17001).protocol("TCP")); //gogo my man should always be here, ft. hadrien
		
		container = container.ports(ports);
		
		/*** LIVENESS & READINESS ***/
		int delay = 15;
		if(!((boolean) props.get("docker"))) delay = 60;
		
		V1Probe probe = new V1Probe()
						  .tcpSocket(new V1TCPSocketAction()
						    .port(new IntOrString("gogo")))
				          .initialDelaySeconds(delay)
				          .periodSeconds(15);
		
		container = container.livenessProbe(probe);
		container = container.readinessProbe(probe);
		
		/*** VOLUME MOUNTS ***/
		if(!(configMapName.isEmpty())) {
			container = container.addVolumeMountsItem(new V1VolumeMount()
								   .name("configuration-volume")
								   .mountPath("/tmp/conf"));
		}
		
		if(!(secretName.isEmpty())) {
			container = container.addVolumeMountsItem(new V1VolumeMount()
						  		   .name("tls-secret")
						  		   .mountPath("/tmp/tls"));
		}
		
		/*** SERVICE ACCOUNT ***/
		if(sa != null) {
			pod = pod.serviceAccountName(sa.getMetadata().getName());
		}
		
		pod = pod.addContainersItem(container);
		return pod;
	}
}