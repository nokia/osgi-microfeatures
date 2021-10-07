package com.nokia.as.service.discovery.k8s.controller;

import java.util.List;

import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Config(section = "Kubernetes discovery configuration")
public interface ControllerConfiguration {
	
	@StringProperty(title = "Namespaces", 
					help = "List of namespaces that will be watched by the controller",
					defval = "${DISCOVERY_NAMESPACES}{$CURRENT$}", 
					required = true)
	public List<String> getNamespaces();
	
	@BooleanProperty(title = "Is RBAC enabled?", 
					help = "Is RBAC enabled in the cluster where the controller is launched?",
					defval = true, 
					required = true)
	public boolean getRbacEnabled();
	
	@StringProperty(title = "CASR image registry", 
				    help = "Registry of the CASR docker image",
					defval = "${CASR_DOCKER_REGISTRY}{csf-docker-delivered.repo.lab.pl.alcatel-lucent.com}", 
					required = true)
	public String getCasrRegistry();
	
	@StringProperty(title = "CASR image tag", 
					help = "Tag of the CASR docker image",
					defval = "${CASR_DOCKER_TAG}{1.1.0}", 
					required = true)
	public String getCasrImageTag();
	
	@StringProperty(title = "CASR version", 
					help = "Version of the CASR OBR used",
					defval = "${CASR_VERSION}{19.9.3}", 
					required = true)
	public String getCasrVersion();

	@StringProperty(title = "CASR repo", 
					help = "Repository of the CASR OBR used",
					defval = "${CASR_REPO}{csf-mvn-delivered}", 
					required = true)
	public String getCasrRepo();
	
	@StringProperty(title = "CASR OBR URL", 
			help = "Optional full URL to an OBR (takes priority over CASR repo)",
			defval = "${OBR_URL}{none}", 
			required = true)
	public String getCasrObrUrl();
	
}