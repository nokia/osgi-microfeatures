// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.http.swagger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ConfigurationDependency;

import io.swagger.v3.oas.annotations.Hidden;

@Component(provides = Object.class)
@Path("ui")
@Hidden
public class SwaggerUiComponent {

	private final static String SWAGGER_UI_DIR = "/META-INF/resources/webjars/swagger-ui/";

	private SwaggerConfig _cnf;

	@ConfigurationDependency
	void updated(SwaggerConfig cnf) {
		_cnf = cnf;
	}

	@GET
	@Path("index.html")
	public InputStream index(@Context UriInfo uri) throws MalformedURLException {
		InputStream index = getResource(SWAGGER_UI_DIR + "index.html");

		// set defaultJSONUrl
		String defaultJSONUrl = _cnf.getSwaggerConfig();
		String urlBase = "/";
		String aliasPath = addLastSlash(_cnf.getSwaggerDefaultDocumentationAlias());
		if (!aliasPath.equals("/")) {
			urlBase += aliasPath;
		}
		urlBase += defaultJSONUrl.substring(defaultJSONUrl.lastIndexOf('/') + 1);

		// replace defaultJSONUrl in indexHTML
		@SuppressWarnings("resource")
		Scanner s = new Scanner(index);
		s = s.useDelimiter("\\A");
		String indexHTML = s.next();
		s.close();
		String replaceFirst = indexHTML.replaceFirst("%DEFAULT-SWAGGER-URL%", Matcher.quoteReplacement(urlBase));
		return new ByteArrayInputStream(replaceFirst.getBytes(StandardCharsets.UTF_8));
	}

	private static final String addLastSlash(String alias) {
		if (alias.endsWith("/"))
			return alias;
		return alias + "/";
	}

	@GET
	@Path("{resource}")
	public InputStream resource(@PathParam("resource") String resource) {
		return getResource(SWAGGER_UI_DIR + resource);
	}

	private InputStream getResource(String uri) {
		return SwaggerUiComponent.class.getResourceAsStream(uri);
	}
}
