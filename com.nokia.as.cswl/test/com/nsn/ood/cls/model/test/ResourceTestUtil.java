/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class ResourceTestUtil {

	public static Resource resource() {
		return new Resource();
	}

	public static void assertResource(final Resource resource, final int linksCount, final int embeddedCount) {
		if (linksCount == 0) {
			assertNull(resource.getLinks());
		} else {
			assertEquals(linksCount, resource.getLinks().getAdditionalProperties().size());
		}
		if (embeddedCount == 0) {
			assertNull(resource.getEmbedded());
		} else {
			assertEquals(embeddedCount, resource.getEmbedded().getAdditionalProperties().size());
		}
	}
}
