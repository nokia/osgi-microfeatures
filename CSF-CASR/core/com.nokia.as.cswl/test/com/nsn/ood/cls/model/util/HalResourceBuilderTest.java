/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.util;

import static com.nsn.ood.cls.model.test.LinkTestUtil.link;
import static com.nsn.ood.cls.model.test.LinkTestUtil.linksList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class HalResourceBuilderTest {

	@Test
	public void testCreatingEmptyResource() {
		final Resource resource = new HalResourceBuilder<Resource>(new Resource()).build();

		assertNotNull(resource);
		assertNull(resource.getLinks());
		assertNull(resource.getEmbedded());
	}

	@Test
	public void testCreatingResourceWithLinks() {
		final Resource resource = new HalResourceBuilder<Resource>(new Resource())//
				.addLink("self", link("selfHref"))//
				.addLink("next", link("nextHref"))//
				.addLink("null", null)//
				.addLinksArray("links", linksList(link("link1"), link("link2")))//
				.addLinksArray("empty_links", linksList())//
				.addLinksArray("null_links", null)//
				.build();

		assertNotNull(resource.getLinks());
		assertEquals(6, resource.getLinks().getAdditionalProperties().size());
		assertEquals(link("selfHref"), HalResource.getLink(resource, "self"));
		assertEquals(link("nextHref"), HalResource.getLink(resource, "next"));
		assertTrue(HalResource.containsLink(resource, "null"));
		assertNull(HalResource.getLink(resource, "null"));
		assertEquals(linksList(link("link1"), link("link2")), HalResource.getLinksArray(resource, "links"));
		assertEquals(linksList(), HalResource.getLinksArray(resource, "empty_links"));
		assertTrue(HalResource.containsLink(resource, "null_links"));
		assertNull(HalResource.getLinksArray(resource, "null_links"));
		assertFalse(HalResource.containsLink(resource, "unknown"));
	}

	@Test
	public void testCreatingResourceWithEmbeddedObjects() {
		final Resource resource = new HalResourceBuilder<Resource>(new Resource()) //
				.addEmbedded("long", new Long(123L)) //
				.addEmbedded("string", "test") //
				.addEmbedded("null", null)//
				.build();

		assertNotNull(resource.getEmbedded());
		assertEquals(new Long(123L), HalResource.getEmbedded(resource, "long"));
		assertEquals("test", HalResource.getEmbedded(resource, "string"));
		assertTrue(HalResource.containsEmbedded(resource, "null"));
		assertFalse(HalResource.containsEmbedded(resource, "unknown"));
	}

	@Test
	public void testErrorResource() {
		final Error error = new HalResourceBuilder<Error>(new Error()) //
				.addLink("self", link("selfHref")) //
				.addLink("self", link("selfHref2")) //
				.addEmbedded("error", "errorObject") //
				.addEmbedded("error", "errorObject2") //
				.build();

		assertNotNull(error);
		assertEquals(link("selfHref2"), HalResource.getLink(error, "self"));
		assertEquals("errorObject2", HalResource.getEmbedded(error, "error"));
	}

	@Test
	public void testAddingEmbeddedObjectWhenNotNullIsExpected() {
		{
			final Resource resource = new HalResourceBuilder<Resource>(new Resource())//
					.addEmbeddedIfNotNull("test1", null)//
					.build();

			assertNull(resource.getEmbedded());
		}
		{
			final Resource resource = new HalResourceBuilder<Resource>(new Resource())//
					.addEmbeddedIfNotNull("test1", null)//
					.addEmbeddedIfNotNull("test2", "test2")//
					.build();

			assertNotNull(resource.getEmbedded());
			assertFalse(HalResource.containsEmbedded(resource, "test1"));
			assertEquals("test2", HalResource.getEmbedded(resource, "test2"));
		}
	}

	@Test
	public void testAddingEmbeddedCollectionWhenNotEmptyOneIsExpected() {
		{
			final Resource resource = new HalResourceBuilder<Resource>(new Resource())//
					.addEmbeddedIfNotEmpty("test1", null)//
					.addEmbeddedIfNotEmpty("test2", Collections.emptyList())//
					.build();

			assertNull(resource.getEmbedded());
		}
		{
			final Resource resource = new HalResourceBuilder<Resource>(new Resource())//
					.addEmbeddedIfNotEmpty("test1", null)//
					.addEmbeddedIfNotEmpty("test2", Collections.emptyList())//
					.addEmbeddedIfNotEmpty("test3", Arrays.asList("test3"))//
					.build();

			assertNotNull(resource.getEmbedded());
			assertFalse(HalResource.containsEmbedded(resource, "test1"));
			assertFalse(HalResource.containsEmbedded(resource, "test2"));
			assertEquals(Arrays.asList("test3"), HalResource.getEmbedded(resource, "test3"));
		}
	}
}
