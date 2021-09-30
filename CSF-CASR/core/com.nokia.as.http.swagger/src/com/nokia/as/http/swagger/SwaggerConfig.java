package com.nokia.as.http.swagger;

import com.alcatel_lucent.as.management.annotation.config.StringProperty;

public interface SwaggerConfig {

	@StringProperty(title="Swagger configuration",
		      help="Enter Swagger configuration path",
		      required=false,
		      dynamic=false,
		      section="Swagger",
		      defval="${INSTALL_DIR}/${instance.name}/openapi.json")		      
	String getSwaggerConfig();
	
	@StringProperty(title="Swagger UI default doc alias",
		      help="Enter alias path for the index page Swagger documentation",
		      required=false,
		      dynamic=false,
		      section="Swagger",
		      defval="services")		      
	String getSwaggerDefaultDocumentationAlias();
}
