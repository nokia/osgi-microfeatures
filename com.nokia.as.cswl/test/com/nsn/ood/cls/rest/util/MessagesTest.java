/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.getInternalState;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.model.internal.SettingKey;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	Messages.class })
public class MessagesTest {

	@Test
	public void testGetErrorDevMessage() throws Exception {
		assertEquals("Bad request", Messages.getErrorDevMessage(0L));
		assertEquals("!1337.dev!", Messages.getErrorDevMessage(1337L));
	}

	@Test
	public void testGetErrorUserMessage() throws Exception {
		assertEquals("Bad request", Messages.getErrorUserMessage(0L));
		assertEquals("!1337.user!", Messages.getErrorUserMessage(1337L));
	}

	@Test
	public void testGetSetting() throws Exception {
		assertEquals("expiring licenses threshold time", Messages.getSetting(SettingKey.EXPIRING_LICENSES_THRESHOLD));
	}

	@Test
	public void testGetWrongSetting() throws Exception {
		final ResourceBundle defaultBundle = getInternalState(Messages.class, "SETTINGS_BUNDLE");
		final ResourceBundle bundleMock = createMock(ResourceBundle.class);

		expect(bundleMock.getString("floatingReleaseTime")).andThrow(new MissingResourceException(null, null, null));

		replayAll();
		setInternalState(Messages.class, "SETTINGS_BUNDLE", bundleMock);
		assertEquals("!floatingReleaseTime!", Messages.getSetting(SettingKey.FLOATING_RELEASE_TIME));
		setInternalState(Messages.class, "SETTINGS_BUNDLE", defaultBundle);
		verifyAll();
	}

	@Test
	public void testGetValidationError() throws Exception {
		assertEquals("Resource's serial number must have from 1 to 15 digits",
				Messages.getValidationError("licenses.serialNumber"));
		assertEquals("!test!", Messages.getValidationError("test"));
	}
}
