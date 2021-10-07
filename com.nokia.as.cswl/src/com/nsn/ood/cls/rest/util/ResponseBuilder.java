/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.model.gen.hal.Resource;


/**
 * @author marynows
 * 
 */
public class ResponseBuilder {
	private final javax.ws.rs.core.Response.ResponseBuilder responseBuilder;

	ResponseBuilder(final StatusType status) {
		this.responseBuilder = Response.status(status);
	}

	public Response build() {
		return this.responseBuilder.build();
	}

	public ResponseBuilder type(final String type) {
		this.responseBuilder.type(type);
		return this;
	}

	public ResponseBuilder header(final String name, final Object value) {
		this.responseBuilder.header(name, value);
		return this;
	}

	public ResponseBuilder resource(final Resource resource) {
		if (resource != null) {
			this.responseBuilder.entity(resource);
		}
		return this;
	}

	public ResponseBuilder tag(final ClientTag tag) {
		if (tag != null) {
			if (tag.getETag() != null) {
				this.responseBuilder.tag(tag.getETag());
			}
			if (tag.getExpires() != null) {
				this.responseBuilder.expires(tag.getExpires().toDate());
			}
		}
		return this;
	}
}
