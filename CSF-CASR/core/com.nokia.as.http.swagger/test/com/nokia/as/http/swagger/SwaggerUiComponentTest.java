package com.nokia.as.http.swagger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
@RunWith(MockitoJUnitRunner.class)
public class SwaggerUiComponentTest {

	private SwaggerUiComponent _swagger;

	@Mock
	private UriInfo _uriInfo;
	
	@Mock
	private SwaggerConfig _swaggerConfig;
	
	@Before
	public void initDefaultConfig() throws Exception {
		_swagger = new SwaggerUiComponent();
		when(_swaggerConfig.getSwaggerDefaultDocumentationAlias()).thenReturn("services");
		when(_swaggerConfig.getSwaggerConfig()).thenReturn("instance/openapi.json");
	}

	@Test
	public void index_default_alias() throws Exception {
		when(_uriInfo.getBaseUri()).thenReturn(new URI(""));
		_swagger.updated(_swaggerConfig);
		assertThat(toHTML(_swagger.index(_uriInfo))).contains("SwaggerJson.url = \"/services/openapi.json\"");
	}
	
	@Test
	public void index_no_alias() throws Exception {
		when(_swaggerConfig.getSwaggerDefaultDocumentationAlias()).thenReturn("/");
		_swagger.updated(_swaggerConfig);
		_swagger.index(_uriInfo);
		assertThat(toHTML(_swagger.index(_uriInfo))).contains("SwaggerJson.url = \"/openapi.json\"");
	}
	
	@Test
	public void index_no_alias_2() throws Exception {
		when(_swaggerConfig.getSwaggerDefaultDocumentationAlias()).thenReturn("/");
		_swagger.updated(_swaggerConfig);
		_swagger.index(_uriInfo);
		assertThat(toHTML(_swagger.index(_uriInfo))).contains("SwaggerJson.url = \"/openapi.json\"");
	}

	@Test
	public void index_contains_default_url_placeholder() throws Exception {
		assertThat(toHTML(_swagger.resource("index.html"))).contains("SwaggerJson.url = \"%DEFAULT-SWAGGER-URL%\"");
	}
	
	private String toHTML(InputStream in) throws IOException {
		int n = in.available();
		byte[] bytes = new byte[n];
		in.read(bytes, 0, n);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
