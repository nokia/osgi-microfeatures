// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.keycloak.admin.stest;

import java.util.Hashtable;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.testcontainers.containers.GenericContainer;

import com.github.dockerjava.api.DockerClient;

@Component(provides = Object.class)
public class EnableKeycloak {
	//private static final String KEYCLOAK_IMAGE = "csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/keycloak/2496/keycloak:6.0.0-0";
    //private static final String KEYCLOAK_IMAGE = "csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/keycloak/3370/keycloak:8.0.1-1";
    //private static final String KEYCLOAK_IMAGE = "csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/keycloak/3729/keycloak:9.0.0-3";
	//private static final String KEYCLOAK_IMAGE = "csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/keycloak/4159/keycloak-ha:10.0.1-2";
    private static final String KEYCLOAK_IMAGE = "registry1-docker-io.repo.lab.pl.alcatel-lucent.com/jboss/keycloak:10.0.1";
	//private static final String KEYCLOAK_IMAGE = "csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/keycloak/7/keycloak:11.0.2-1";
    
	@ServiceDependency
	ConfigurationAdmin admin;

	@Start
	public void start() {
		ClassLoader currentThread = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(DockerClient.class.getClassLoader());
			GenericContainer container = new GenericContainer<>(KEYCLOAK_IMAGE)	.withExposedPorts(8080)
																				.withEnv("KEYCLOAK_USER", "admin")
																				.withEnv("KEYCLOAK_PASSWORD", "admin");
			container.start();
			// create new keycloak admin client instance
			Configuration conf = admin.createFactoryConfiguration("keycloak.admin.client", "?");
			Hashtable<String, Object> props = new Hashtable<>();
			props.put("authUrl", "http://localhost:" + container.getFirstMappedPort() + "/auth");
//			props.put("authUrl", "http://localhost:8080/auth");
			props.put("realm", "master");
			props.put("username", "admin");
			props.put("password", "admin");
			props.put("clientId", "admin-cli");
			conf.update(props); // this will trigger activation of the
								// com.nokia.as.thirdparty.keycloak.admin.client.KeycoakAdminClient
								// osgi sevice component
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			Thread.currentThread().setContextClassLoader(currentThread);
		}
	}
}
