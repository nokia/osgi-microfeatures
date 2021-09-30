/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.test;

import java.util.List;

import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.model.FeaturesWithTag;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.features.Feature;


/**
 * @author marynows
 * 
 */
public class ObjectWithTagTestUtil {

	public static ClientWithTag clientWithTag(final Client client, final ClientTag clientTag) {
		return new ClientWithTag().withObject(client).withClientTag(clientTag);
	}

	public static FeaturesWithTag featuresWithTag(final List<Feature> features, final ClientTag clientTag) {
		return new FeaturesWithTag().withObject(features).withClientTag(clientTag);
	}
}
