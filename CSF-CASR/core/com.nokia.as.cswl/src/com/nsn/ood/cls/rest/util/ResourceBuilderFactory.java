/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;

import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = ResourceBuilderFactory.class)
@Loggable
public class ResourceBuilderFactory {

	public ResourceBuilder create() {
		return createResourceBuilder();
	}

	public ResourceBuilder selfLink(final Link link) {
		return createResourceBuilder().selfLink(link);
	}

	public ResourceBuilder metaData(final Links links, final MetaData metaData) {
		return createResourceBuilder().selfLink(links.getSelfLink()).nextLink(links.getNextLink())
				.prevLink(links.getPrevLink()).metaData(metaData);
	}

	public ResourceBuilder errors(final Error... errors) {
		return createResourceBuilder().errors(errors);
	}

	public ResourceBuilder errors(final List<Error> errors) {
		return createResourceBuilder().errors(errors);
	}

	protected ResourceBuilder createResourceBuilder() {
		return new ResourceBuilder();
	}

	public static class Links {
		private final Link selfLink;
		private final Link nextLink;
		private final Link prevLink;

		public Links(final Link selfLink, final Link nextLink, final Link prevLink) {
			this.selfLink = selfLink;
			this.nextLink = nextLink;
			this.prevLink = prevLink;
		}

		public Link getSelfLink() {
			return this.selfLink;
		}

		public Link getNextLink() {
			return this.nextLink;
		}

		public Link getPrevLink() {
			return this.prevLink;
		}
	}
}
