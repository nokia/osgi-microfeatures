/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.features.Feature;


/**
 * @author marynows
 * 
 */
public class FeaturesWithTagTest {
	private static final List<Feature> FEATURES = new ArrayList<>();
	private static final ClientTag CLIENT_TAG = new ClientTag();
	private static final FeaturesWithTag FEATURE_WITH_TAG = new FeaturesWithTag().withClientTag(CLIENT_TAG).withObject(
			FEATURES);

	@Test
	public void testFeaturesWithTag() throws Exception {
		final FeaturesWithTag clientWithTag = new FeaturesWithTag();
		assertNull(clientWithTag.getClientTag());
		assertNull(clientWithTag.getObject());

		clientWithTag.setClientTag(CLIENT_TAG);
		assertSame(CLIENT_TAG, clientWithTag.getClientTag());

		clientWithTag.setObject(FEATURES);
		assertSame(FEATURES, clientWithTag.getObject());
	}

	@Test
	public void testBuilders() throws Exception {
		assertSame(CLIENT_TAG, FEATURE_WITH_TAG.getClientTag());
		assertSame(FEATURES, FEATURE_WITH_TAG.getObject());
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(FEATURE_WITH_TAG.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertFalse(FEATURE_WITH_TAG.equals(null));
		assertFalse(FEATURE_WITH_TAG.equals("test"));
		assertEquals(FEATURE_WITH_TAG, FEATURE_WITH_TAG);

		assertFalse(FEATURE_WITH_TAG.equals(new FeaturesWithTag()));
		assertNotEquals(FEATURE_WITH_TAG.hashCode(), new FeaturesWithTag().hashCode());

		final FeaturesWithTag featuresWithTag = new FeaturesWithTag().withClientTag(CLIENT_TAG).withObject(FEATURES);
		assertEquals(FEATURE_WITH_TAG, featuresWithTag);
		assertEquals(FEATURE_WITH_TAG.hashCode(), featuresWithTag.hashCode());
	}
}
