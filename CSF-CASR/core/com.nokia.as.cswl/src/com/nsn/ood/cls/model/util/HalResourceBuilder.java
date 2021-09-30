/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.util;

import java.util.Collection;
import java.util.List;

import com.nsn.ood.cls.model.gen.hal.Embedded;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Links;
import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class HalResourceBuilder<T extends Resource> {
	private final T resource;

	HalResourceBuilder(final T resource) {
		this.resource = resource;
	}

	public T build() {
		return this.resource;
	}

	public HalResourceBuilder<T> addLink(final String name, final Link link) {
		getLinks().setAdditionalProperty(name, link);
		return this;
	}

	public HalResourceBuilder<T> addLinksArray(final String name, final List<Link> links) {
		getLinks().setAdditionalProperty(name, links);
		return this;
	}

	private Links getLinks() {
		Links links = this.resource.getLinks();
		if (links == null) {
			links = new Links();
			this.resource.setLinks(links);
		}
		return links;
	}

	public HalResourceBuilder<T> addEmbedded(final String name, final Object object) {
		getEmbedded().setAdditionalProperty(name, object);
		return this;
	}

	public HalResourceBuilder<T> addEmbeddedIfNotNull(final String name, final Object object) {
		if (object != null) {
			addEmbedded(name, object);
		}
		return this;
	}

	public HalResourceBuilder<T> addEmbeddedIfNotEmpty(final String name, final Collection<?> collection) {
		if (collection != null && !collection.isEmpty()) {
			addEmbedded(name, collection);
		}
		return this;
	}

	private Embedded getEmbedded() {
		Embedded embedded = this.resource.getEmbedded();
		if (embedded == null) {
			embedded = new Embedded();
			this.resource.setEmbedded(embedded);
		}
		return embedded;
	}
}
