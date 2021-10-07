package com.nokia.as.http.swagger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OpenApiResourceComponentTest {

	@Mock
	private SwaggerConfig _cnf;
	private OpenApiResourceComponent _openApiResource;

	@Before
	public void setUp() throws Exception {
		_openApiResource = new OpenApiResourceComponent();
		_openApiResource.updated(_cnf);
	}

	@Test
	public void set_config_location() {
		String configLocation = "foo";
		when(_cnf.getSwaggerConfig()).thenReturn(configLocation);
		_openApiResource.start();
		verify(_cnf).getSwaggerConfig();
		assertThat(_openApiResource.getConfigLocation()).isEqualTo(configLocation);
	}

	@Test
	public void no_config_location() {
		when(_cnf.getSwaggerConfig()).thenReturn(null);
		_openApiResource.start();
		verify(_cnf).getSwaggerConfig();
		assertThat(_openApiResource.getConfigLocation()).isEqualTo(null);
	}

	@Test
	public void get_json_openapi_endpoint() throws Exception{
		Response openApi = _openApiResource.getOpenApi(null, null, "json");
		assertThat(openApi).isNotNull();
		assertThat(openApi.getEntity()).isInstanceOf(String.class);
		assertThat(openApi.getEntity()).isEqualTo("{\"openapi\":\"3.0.1\"}");
	}
	
	@Test
	public void get_yaml_openapi_endpoint() throws Exception{
		Response openApi = _openApiResource.getOpenApi(null, null, "yaml");
		assertThat(openApi).isNotNull();
		assertThat(openApi.getEntity()).isInstanceOf(String.class);
		assertThat(openApi.getEntity()).isEqualTo("openapi: 3.0.1\n");
	}
}
