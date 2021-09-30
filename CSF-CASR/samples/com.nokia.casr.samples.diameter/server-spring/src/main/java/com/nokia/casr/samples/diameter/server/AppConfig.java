package com.nokia.casr.samples.diameter.server;

import org.springframework.context.annotation.*;

@Configuration
public class AppConfig {
	
	@Bean
	public TestServer testServer() {
		return new TestServer();
	}

}
