/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Links;


/**
 * @author marynows
 * 
 */
public class LinkTestUtil {

	public static Links links() {
		return new Links();
	}

	public static Link link() {
		return new Link();
	}

	public static Link link(final String href) {
		return link().withHref(href);
	}

	public static Link link(final String href, final String type) {
		return link(href).withType(type);
	}

	public static List<Link> linksList(final Link... links) {
		return Arrays.asList(links);
	}

	public static void assertLink(final Link link, final String href) {
		assertEquals(href, link.getHref());
		assertNull(link.getHreflang());
		assertNull(link.getName());
		assertNull(link.getProfile());
		assertNull(link.getTemplated());
		assertNull(link.getTitle());
		assertNull(link.getType());
	}
}
