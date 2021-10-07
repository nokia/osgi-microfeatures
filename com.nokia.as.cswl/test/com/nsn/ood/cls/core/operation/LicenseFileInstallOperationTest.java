/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.core.test.LicenseFileTestUtil.licenseFile;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.cljl.CLJLLicenseService;
import com.nsn.ood.cls.cljl.LicenseInstallOptions;
import com.nsn.ood.cls.core.convert.LicenseInstallOptions2StringConverter;
import com.nsn.ood.cls.core.convert.StoredLicense2LicenseConverter;
import com.nsn.ood.cls.core.operation.exception.InstallException;
import com.nsn.ood.cls.core.operation.util.LicenseErrorType;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public class LicenseFileInstallOperationTest {
	private LicenseFileInstallOperation operation;

	@Before
	public void setUp() throws Exception {
		resetAll();
		this.operation = new LicenseFileInstallOperation();
	}

	@Test
	public void testInstall() throws Exception {
		final CLJLLicenseService cljlLicenseServiceMock = createMock(CLJLLicenseService.class);
		final LicenseInstallOptions licenseInstallOptionsMock = createMock(LicenseInstallOptions.class);
		Converter<StoredLicense, License> storedLicense2LicenseConverter = createMock(StoredLicense2LicenseConverter.class);
		Converter<LicenseInstallOptions, String> licenseInstallOptions2StringConverter = createMock(LicenseInstallOptions2StringConverter.class);

		final StoredLicense storedLicense = new StoredLicense();
		final Capture<InputStream> capturedContent = new Capture<>();

		expect(licenseInstallOptions2StringConverter.convertTo(licenseInstallOptionsMock)).andReturn("log");
		expect(cljlLicenseServiceMock.install(capture(capturedContent), eq("fileName"),
				same(licenseInstallOptionsMock))).andReturn(storedLicense);
		expect(storedLicense2LicenseConverter.convertTo(storedLicense)).andReturn(license("123"));

		replayAll();
		setInternalState(this.operation, cljlLicenseServiceMock, licenseInstallOptionsMock, new LicenseParserMock(true));
		setInternalState(this.operation, "storedLicense2LicenseConverter", storedLicense2LicenseConverter);
		setInternalState(this.operation, "licenseInstallOptions2StringConverter", licenseInstallOptions2StringConverter);
		assertEquals(license("123"), this.operation.install(licenseFile("fileName", "licenseContent")));
		verifyAll();

		assertEquals("licenseContent", IOUtils.toString(capturedContent.getValue()));
	}

	@Test
	public void testInstallWithInstalationException() throws Exception {
		final CLJLLicenseService cljlLicenseServiceMock = createMock(CLJLLicenseService.class);
		final LicenseInstallOptions licenseInstallOptionsMock = createMock(LicenseInstallOptions.class);
		Converter<LicenseInstallOptions, String> licenseInstallOptions2StringConverter = createMock(LicenseInstallOptions2StringConverter.class);

		expect(licenseInstallOptions2StringConverter.convertTo(licenseInstallOptionsMock)).andReturn("log");
		expect(cljlLicenseServiceMock.install(anyObject(InputStream.class), eq("fileName2"),
				same(licenseInstallOptionsMock))).andThrow(new LicenseException("errorCode", "message"));

		replayAll();
		setInternalState(this.operation, new LicenseParserMock(true), cljlLicenseServiceMock, licenseInstallOptionsMock);
		setInternalState(this.operation, "licenseInstallOptions2StringConverter", licenseInstallOptions2StringConverter);
		try {
			this.operation.install(licenseFile("fileName2", "licenseContent2"));
			fail();
		} catch (final InstallException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.CLJL, e.getErrorType());
			assertEquals("errorCode", e.getCljlErrorCode());
		}
		verifyAll();
	}

	@Test
	public void testInstallUnsupportedLicense() throws Exception {
		setInternalState(this.operation, new LicenseParserMock(false));

		try {
			this.operation.install(licenseFile("fileName", "licenseContent"));
			fail();
		} catch (final InstallException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.VERIFICATION, e.getErrorType());
			assertNull(e.getCljlErrorCode());
		}
	}

	@Test
	public void testInstallCorruptedLicense() throws Exception {
		setInternalState(this.operation, new LicenseParserMock(null));

		try {
			this.operation.install(licenseFile("fileName", "licenseContent"));
			fail();
		} catch (final InstallException e) {
			assertNotNull(e.getMessage());
			assertEquals(LicenseErrorType.VERIFICATION, e.getErrorType());
			assertNull(e.getCljlErrorCode());
		}
	}
}
