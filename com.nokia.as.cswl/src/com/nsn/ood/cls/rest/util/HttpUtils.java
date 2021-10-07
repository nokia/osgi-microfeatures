/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;

import org.apache.felix.dm.annotation.api.Component;
import org.glassfish.jersey.media.multipart.BodyPart;

import com.nsn.ood.cls.util.CollectionUtils;


/**
 * @author marynows
 * 
 */
@Component(provides = HttpUtils.class)
public class HttpUtils {
	private static final Pattern FILENAME_PATTERN = Pattern.compile(".*filename=\"(.*?)\".*");

	public String extractFileName(final BodyPart part) {
		final List<String> headers = part.getHeaders().get(HttpHeaders.CONTENT_DISPOSITION);
		if (CollectionUtils.isNotEmpty(headers)) {
			for (final String header : headers) {
				if (header.startsWith("form-data;") || header.startsWith("attachment;")) {
					return getFileName(header);
				}
			}
		}
		return null;
	}

	private String getFileName(final String header) {
		final Matcher matcher = FILENAME_PATTERN.matcher(header);
		if (matcher.matches()) {
			return matcher.group(1);
		}
		return null;
	}
}
