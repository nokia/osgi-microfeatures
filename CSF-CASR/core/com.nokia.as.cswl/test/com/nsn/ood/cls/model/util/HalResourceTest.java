/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.util;

import static com.nsn.ood.cls.model.test.LinkTestUtil.link;
import static com.nsn.ood.cls.model.test.LinkTestUtil.links;
import static com.nsn.ood.cls.model.test.LinkTestUtil.linksList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Embedded;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class HalResourceTest {

	@Test
	public void testBuilder() {
		assertNotNull(HalResource.builder(new Resource()));
		assertNotNull(HalResource.builder(new Error()));

		try {
			HalResource.builder(null);
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}

	@Test
	public void testLink() {
		final Link link1 = link("link1");
		final Link link2 = link("link2");
		final List<Link> links1 = linksList(link("link1_1"), link("link1_2"));
		final List<Link> links2 = linksList(link("link2_1"), link("link2_2"));
		final Resource resource = new Resource().withLinks(links()//
				.withAdditionalProperty("link1", link1)//
				.withAdditionalProperty("link2", link2)//
				.withAdditionalProperty("links1", links1)//
				.withAdditionalProperty("links2", links2));

		assertLinkExists(link1, resource, "link1");
		assertLinkExists(link2, resource, "link2");
		assertLinksArrayExists(links1, resource, "links1");
		assertLinksArrayExists(links2, resource, "links2");
		assertLinkNotExists(resource, "test");
		assertLinkNotExists(resource, null);
	}

	@Test
	public void testLinkWhenResourceHasNoLinks() {
		final Resource resource = new Resource();
		assertLinkNotExists(resource, "link");
		assertLinksArrayNotExists(resource, "links");

		final Resource emptyLinks = resource.withLinks(links());
		assertLinkNotExists(emptyLinks, "link");
		assertLinksArrayNotExists(emptyLinks, "links");
	}

	@Test
	public void testLinkForNullResource() {
		try {
			HalResource.getLink(null, "link");
			fail();
		} catch (final IllegalArgumentException e) {
		}
		try {
			HalResource.getLinksArray(null, "links");
			fail();
		} catch (final IllegalArgumentException e) {
		}
		try {
			HalResource.containsLink(null, "link");
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}

	private void assertLinkExists(final Link link, final Resource resource, final String name) {
		assertTrue(HalResource.containsLink(resource, name));
		assertFalse(HalResource.isLinksArray(resource, name));
		assertEquals(link, HalResource.getLink(resource, name));
	}

	private void assertLinkNotExists(final Resource resource, final String name) {
		assertFalse(HalResource.containsLink(resource, name));
		assertFalse(HalResource.isLinksArray(resource, name));
		assertNull(HalResource.getLink(resource, name));
	}

	private void assertLinksArrayExists(final List<Link> links, final Resource resource, final String name) {
		assertTrue(HalResource.containsLink(resource, name));
		assertTrue(HalResource.isLinksArray(resource, name));
		assertEquals(links, HalResource.getLinksArray(resource, name));
	}

	private void assertLinksArrayNotExists(final Resource resource, final String name) {
		assertFalse(HalResource.containsLink(resource, name));
		assertFalse(HalResource.isLinksArray(resource, name));
		assertNull(HalResource.getLinksArray(resource, name));
	}

	@Test
	public void testEmbedded() {
		final String object1 = "object1";
		final String object2 = "object2";
		final Resource resource = new Resource().withEmbedded(new Embedded()//
				.withAdditionalProperty("name1", object1)//
				.withAdditionalProperty("name2", object2));

		assertEmbeddedExists(object1, resource, "name1");
		assertEmbeddedExists(object2, resource, "name2");
		assertEmbeddedNotExists(resource, "test");
		assertEmbeddedNotExists(resource, null);
	}

	@Test
	public void testEmbeddedWhenResourceHasNoLinks() {
		assertEmbeddedNotExists(new Resource().withEmbedded(new Embedded()), "name");
		assertEmbeddedNotExists(new Resource(), "name");
	}

	@Test
	public void testEmbeddedForNullResource() {
		try {
			HalResource.getEmbedded(null, "name");
			fail();
		} catch (final IllegalArgumentException e) {
		}
		try {
			HalResource.containsEmbedded(null, "name");
			fail();
		} catch (final IllegalArgumentException e) {
		}
	}

	private void assertEmbeddedExists(final Object object, final Resource resource, final String name) {
		assertTrue(HalResource.containsEmbedded(resource, name));
		assertEquals(object, HalResource.getEmbedded(resource, name));
	}

	private void assertEmbeddedNotExists(final Resource resource, final String name) {
		assertFalse(HalResource.containsEmbedded(resource, name));
		assertNull(HalResource.getEmbedded(resource, name));
	}
}
