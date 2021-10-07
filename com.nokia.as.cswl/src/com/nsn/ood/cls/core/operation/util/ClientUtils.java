/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation.util;

import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.core.config.Configuration;
import com.nsn.ood.cls.core.platform.PlatformPreferences;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = ClientUtils.class)
@Loggable
public class ClientUtils {
	private static final Random RND = new Random();
	private static final int ID_HEX_SIZE = 8;
	private static final int ETAG_HEX_SIZE = 12;

	@ServiceDependency
	private Configuration configuration;
	@ServiceDependency
	private PlatformPreferences platformPreferences;

	public String createNewId(final long id) {
		return this.platformPreferences.getTargetId() + "_" + hexString(id, ID_HEX_SIZE);
	}

	public long getDefaultKeepAliveTime() {
		return this.configuration.getDefaultFloatingReleaseTime();
	}

	public String generateETag() {
		return hexString(randomValue(), ETAG_HEX_SIZE);
	}

	public DateTime calculateDefaultExpiresTime() {
		return calculateExpiresTime(getDefaultKeepAliveTime());
	}

	public DateTime calculateExpiresTime(final long keepAliveTime) {
		return now().plusSeconds((int) keepAliveTime);
	}

	private static String hexString(final long value, final int digits) {
		return StringUtils.leftPad(StringUtils.right(Long.toHexString(value), digits), digits, '0');
	}

	protected DateTime now() {
		return DateTime.now();
	}

	protected long randomValue() {
		return RND.nextLong();
	}
}
