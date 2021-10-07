/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model;

import javax.ws.rs.core.MediaType;


/**
 * @author marynows
 * 
 */
public interface CLSMediaType {

	/** <code>application/*+json</code> */
	String APPLICATION_JSON = "application/*+json";

	/** <code>application/*+json</code> */
	MediaType APPLICATION_JSON_TYPE = new MediaType("application", "*+json");

	/** <code>application/vnd.nokia-error-response.text+json</code> */
	String APPLICATION_ERROR_JSON = "application/vnd.nokia-error-response.text+json";

	/** <code>application/vnd.nokia-error-response.text+json</code> */
	MediaType APPLICATION_ERROR_JSON_TYPE = new MediaType("application", "vnd.nokia-error-response.text+json");

	/** <code>application/vnd.nokia-versioning.text+json</code> */
	String APPLICATION_VERSIONING_JSON = "application/vnd.nokia-versioning.text+json";

	/** <code>application/vnd.nokia-versioning.text+json</code> */
	MediaType APPLICATION_VERSIONING_JSON_TYPE = new MediaType("application", "vnd.nokia-versioning.text+json");

	/** <code>application/vnd.nokia-cls.text+json</code> */
	String APPLICATION_CLS_JSON = "application/vnd.nokia-cls.text+json";

	/** <code>application/vnd.nokia-cls.text+json</code> */
	MediaType APPLICATION_CLS_JSON_TYPE = new MediaType("application", "vnd.nokia-cls.text+json");

	/** <code>application/vnd.nokia-cls-client.text+json</code> */
	String APPLICATION_CLIENT_JSON = "application/vnd.nokia-cls-client.text+json";

	/** <code>application/vnd.nokia-cls-client.text+json</code> */
	MediaType APPLICATION_CLIENT_JSON_TYPE = new MediaType("application", "vnd.nokia-cls-client.text+json");

	/** <code>application/vnd.nokia-cls-feature.text+json</code> */
	String APPLICATION_FEATURE_JSON = "application/vnd.nokia-cls-feature.text+json";

	/** <code>application/vnd.nokia-cls-feature.text+json</code> */
	MediaType APPLICATION_FEATURE_JSON_TYPE = new MediaType("application", "vnd.nokia-cls-feature.text+json");

	/** <code>application/vnd.nokia-cls-license.text+json</code> */
	String APPLICATION_LICENSE_JSON = "application/vnd.nokia-cls-license.text+json";

	/** <code>application/vnd.nokia-cls-license.text+json</code> */
	MediaType APPLICATION_LICENSE_JSON_TYPE = new MediaType("application", "vnd.nokia-cls-license.text+json");
}
