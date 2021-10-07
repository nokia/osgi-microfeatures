/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.io.IOException;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceScope;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
@Component(provides = MultipartOutputBuilder.class, scope = ServiceScope.PROTOTYPE)
public class MultipartOutputBuilder {
	@Context
	public ObjectMapper om;
	
	private final MultiPart multipartOutput;

	public MultipartOutputBuilder() {
		this.multipartOutput = new MultiPart();
	}

	public MultiPart build() {
		return this.multipartOutput;
	}

	public MultipartOutputBuilder addJsonPart(final Object entity, final MediaType mediaType) {
		MultiPart part = this.multipartOutput.bodyPart(toJsonString(entity), mediaType);
		addHeader(part, "Content-Transfer-Encoding", "binary");
		return this;
	}

	public MultipartOutputBuilder addJsonPartAsText(final Object entity, final MediaType mediaType) {
		this.multipartOutput.bodyPart(toJsonString(entity), mediaType);
		return this;
	}

	private String toJsonString(final Object entity) {
		try {
			return om.writeValueAsString(entity);
		} catch (final IOException e) {
			throw new CLSRuntimeException(e);
		}
	}

	public MultipartOutputBuilder addFilePart(final String content, final MediaType mediaType, final String filename) {
		FormDataBodyPart body = new FormDataBodyPart();
		body.setName(filename);
		body.setValue(mediaType, content);
		body.getHeaders().add("Content-Transfer-Encoding", "binary");
		body.getHeaders().add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
		this.multipartOutput.bodyPart(body);
		return this;
	}

	private void addHeader(final MultiPart part, final String name, final String value) {
		part.getHeaders().add(name, value);
	}
}
