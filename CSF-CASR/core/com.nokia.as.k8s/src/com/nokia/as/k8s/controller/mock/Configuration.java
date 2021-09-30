package com.nokia.as.k8s.controller.mock;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Config(section = "Kubernetes Mock Custom Resource API Config")
public interface Configuration {
	
	@StringProperty(title = "Path to Config Directory", 
			help = "Path to the directory where the YAML decl of the Custom Resource will be retrieved",
			defval = "/tmp/k8s_resources", 
			required = true)
	String getPath();

}
