/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


/**
 * @author marynows
 *
 */
@ApplicationPath("/")
public class CLSApplication extends Application {
	public static final String CONTEXT_ROOT = "/CLS";
	public static final String VERSION = "/v1";
	public static final String API = "/api";
	public static final String INTERNAL = "/internal";
	public static final String FEATURE_LEVEL = "Feature-Level";
	public static final String V1_1 = "v1_1";

//	@Override
//	public Set<Class<?>> getClasses() {
//		return Stream.of(ApiResource.class, ActivityDetailsResource.class, ActivityResource.class,
//				ConfigurationResource.class,LicensedFeaturesResource.class, ReservationsResource.class,
//				StoredLicensesResource.class, TestResource.class, ClientsFeaturesResource.class,
//				ClientsResource.class, LicensesResource.class, OpenApiResource.class).collect(Collectors.toSet());
//	}
}
