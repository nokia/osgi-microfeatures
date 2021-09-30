/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.util;

import java.util.List;
import java.util.Map;

import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public final class HalResource {

	private HalResource() {
	}

	public static <T extends Resource> HalResourceBuilder<T> builder(final T resource) {
		checkIsNullResource(resource);
		return new HalResourceBuilder<T>(resource);
	}

	public static Link getLink(final Resource resource, final String name) {
		checkIsNullResource(resource);
		if (resource.getLinks() == null) {
			return null;
		}
		return (Link) getLinkMap(resource).get(name);
	}

	@SuppressWarnings("unchecked")
	public static List<Link> getLinksArray(final Resource resource, final String name) {
		checkIsNullResource(resource);
		if (resource.getLinks() == null) {
			return null;
		}
		return (List<Link>) getLinkMap(resource).get(name);
	}

	public static boolean containsLink(final Resource resource, final String name) {
		checkIsNullResource(resource);
		if (resource.getLinks() == null) {
			return false;
		}
		return getLinkMap(resource).containsKey(name);
	}

	public static boolean isLinksArray(final Resource resource, final String name) {
		checkIsNullResource(resource);
		if (resource.getLinks() == null) {
			return false;
		}
		return (getLinkMap(resource).get(name) instanceof List);
	}

	private static Map<String, Object> getLinkMap(final Resource resource) {
		return resource.getLinks().getAdditionalProperties();
	}

	@SuppressWarnings("unchecked")
	public static <T> T getEmbedded(final Resource resource, final String name) {
		checkIsNullResource(resource);
		if (resource.getEmbedded() == null) {
			return null;
		}
		return (T) getEmbeddedMap(resource).get(name);
	}

	public static boolean containsEmbedded(final Resource resource, final String name) {
		checkIsNullResource(resource);
		if (resource.getEmbedded() == null) {
			return false;
		}
		return getEmbeddedMap(resource).containsKey(name);
	}

	private static Map<String, Object> getEmbeddedMap(final Resource resource) {
		return resource.getEmbedded().getAdditionalProperties();
	}

	private static void checkIsNullResource(final Resource resource) {
		if (resource == null) {
			throw new IllegalArgumentException("Resource cannot be null.");
		}
	}
}
