/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl.plugin;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		Preferences.class, CLSPreferences.class })
public class CLSPreferencesTest {

	@Before
	public void setUp() throws Exception {
		mockStatic(Preferences.class);
		setDevMode(false);
	}

	private void setDevMode(final boolean enabled) {
		System.setProperty("com.nsn.ood.cls.devMode", enabled ? "true" : "false");
	}

	@Test
	public void testGetPreferencesSystemRoot() throws Exception {
		final Preferences rootPreferences = createMock(Preferences.class);
		expect(Preferences.systemRoot()).andReturn(rootPreferences);

		final Preferences clsPreferences = mockClsPreferences(rootPreferences);

		replayAll();
		assertSame(clsPreferences, new CLSPreferences().getPreferencesSystemRoot());
		verifyAll();
	}

	@Test
	public void testGetPreferencesSystemRootDevMode() throws Exception {
		setDevMode(true);

		final Preferences rootPreferences = createMock(Preferences.class);
		expect(Preferences.userRoot()).andReturn(rootPreferences);

		final Preferences clsPreferences = mockClsPreferences(rootPreferences);

		replayAll();
		assertSame(clsPreferences, new CLSPreferences().getPreferencesSystemRoot());
		verifyAll();
	}

	@Test
	public void testGetPreferencesUserRoot() throws Exception {
		final Preferences rootPreferences = createMock(Preferences.class);
		expect(Preferences.userRoot()).andReturn(rootPreferences);

		final Preferences clsPreferences = mockClsPreferences(rootPreferences);

		replayAll();
		assertSame(clsPreferences, new CLSPreferences().getPreferencesUserRoot());
		verifyAll();
	}

	private Preferences mockClsPreferences(final Preferences rootPreferences) {
		final Preferences clsPreferences = createMock(Preferences.class);
		expect(rootPreferences.node("cls")).andReturn(clsPreferences);
		return clsPreferences;
	}

	@Test
	public void testImportPreferences() throws Exception {
		testImportPreferences("/Pref_system_LicenseInstall_CLS.xml");

		setDevMode(true);
		testImportPreferences("/Pref_system_LicenseInstall_user_CLS.xml");
	}

	private void testImportPreferences(final String resName) throws IOException, InvalidPreferencesFormatException {
		resetAll();

		final Capture<InputStream> captured = new Capture<>();
		Preferences.importPreferences(capture(captured));

		replayAll();
		CLSPreferences.importPreferences();
		verifyAll();

		assertStreams(getClass().getResourceAsStream(resName), captured.getValue());
	}

	@Test
	public void testImportPreferencesWithException() throws Exception {
		Preferences.importPreferences(anyObject(InputStream.class));
		expectLastCall().andThrow(new IOException("message"));

		replayAll();
		CLSPreferences.importPreferences();
		verifyAll();
	}

	private void assertStreams(final InputStream expected, final InputStream actual) throws IOException {
		try {
			int eByte;
			int aByte;
			do {
				eByte = expected.read();
				aByte = actual.read();
				assertEquals(eByte, aByte);
			} while (eByte != -1 || aByte != -1);
		} finally {
			expected.close();
			actual.close();
		}
	}
}
