package com.nokia.as.gpto.controller.impl;

import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;

@Config(section="GPTO")
public @interface GPTOConfiguration {
	
	@FileDataProperty(title = "GPTO MuxMesh Config", 
			dynamic = true,
			required = true, 
			fileData = "defGPTOMuxMeshServer.txt", 
			help = "Configure GPTO MuxMesh")
	public final static String MUX_MESH_SERVER_CONFIG = "mux.mesh.server.config";
	

	String getMuxMeshServerConfig();
}
