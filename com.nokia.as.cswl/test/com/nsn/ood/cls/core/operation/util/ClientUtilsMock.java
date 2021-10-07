/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import org.joda.time.DateTime;


/**
 * @author marynows
 * 
 */
public class ClientUtilsMock extends ClientUtils {
	public static final String CLIENT_ID = "12345";
	public static final long KEEP_ALIVE_TIME = 1800L;
	public static final String ETAG = "abc123";
	public static final DateTime EXPIRES_TIME = DateTime.parse("2014-12-16T13:19:23+01:00");

	private long id;

	@Override
	public String createNewId(final long id) {
		this.id = id;
		return CLIENT_ID;
	}

	public boolean verifyCreateNewId(final long id) {
		return (this.id == id);
	}

	@Override
	public long getDefaultKeepAliveTime() {
		return KEEP_ALIVE_TIME;
	}

	@Override
	public String generateETag() {
		return ETAG;
	}

	@Override
	public DateTime calculateDefaultExpiresTime() {
		return EXPIRES_TIME;
	}

	@Override
	public DateTime calculateExpiresTime(final long keepAliveTime) {
		return EXPIRES_TIME.plusSeconds((int) keepAliveTime);
	}
}
