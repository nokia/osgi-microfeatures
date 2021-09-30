/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.test.JodaTestUtil.assertNow;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.cljl.CLJLLicenseService;
import com.nsn.ood.cls.core.convert.License2LicenseCancelInfoConverter;
import com.nsn.ood.cls.core.convert.LicenseCancelInfo2StringConverter;
import com.nsn.ood.cls.core.convert.StoredLicense2LicenseConverter;
import com.nsn.ood.cls.core.operation.exception.CancelException;
import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 *
 */
public class LicenseFileCancelOperationTest {
	private LicenseFileCancelOperation operation;

	@Before
	public void setUp() throws Exception {
		this.operation = new LicenseFileCancelOperation();
	}

	@Test
	public void testCancel() throws Exception {
		final CLJLLicenseService cljlLicenseServiceMock = createMock(CLJLLicenseService.class);
		final Converter<License, LicenseCancelInfo> license2LicenseCancelInfoConverter = createMock(License2LicenseCancelInfoConverter.class);
		final Converter<LicenseCancelInfo, String> licenseCancelInfo2StringConverter = createMock(LicenseCancelInfo2StringConverter.class);
		final Converter<StoredLicense, License> storedLicense2LicenseConverter = createMock(StoredLicense2LicenseConverter.class);
		final BasicPrincipal basicPrincipalMock = createMock(BasicPrincipal.class);

		final LicenseCancelInfo licenseCancelInfo = new LicenseCancelInfo();
		final StoredLicense storedLicense = new StoredLicense();

		expect(license2LicenseCancelInfoConverter.convertTo(license("123"))).andReturn(licenseCancelInfo);
		expect(licenseCancelInfo2StringConverter.convertTo(licenseCancelInfo)).andReturn("log");
		expect(basicPrincipalMock.getUser()).andReturn("user");
		expect(cljlLicenseServiceMock.cancel(licenseCancelInfo)).andReturn(storedLicense);
		expect(storedLicense2LicenseConverter.convertTo(storedLicense)).andReturn(license("234"));

		replayAll();
		setInternalState(this.operation, cljlLicenseServiceMock, basicPrincipalMock);
		setInternalState(this.operation, "license2LicenseCancelInfoConverter", license2LicenseCancelInfoConverter);
		setInternalState(this.operation, "licenseCancelInfo2StringConverter", licenseCancelInfo2StringConverter);
		setInternalState(this.operation, "storedLicense2LicenseConverter", storedLicense2LicenseConverter);
		assertEquals(license("234"), this.operation.cancel(license("123")));
		verifyAll();

		assertEquals("user", licenseCancelInfo.getUserName());
		assertEquals("Deleted by user", licenseCancelInfo.getCancelReason());
		assertNotNull(licenseCancelInfo.getCancelDate());
		assertNow(new DateTime(licenseCancelInfo.getCancelDate()));
		assertEquals(-1, licenseCancelInfo.getFeaturecode());
	}

	@Test
	public void testCancelWithCancelException() throws Exception {
		final CLJLLicenseService cljlLicenseServiceMock = createMock(CLJLLicenseService.class);
		final Converter<License, LicenseCancelInfo> license2LicenseCancelInfoConverter = createMock(License2LicenseCancelInfoConverter.class);
		final Converter<LicenseCancelInfo, String> licenseCancelInfo2StringConverter = createMock(LicenseCancelInfo2StringConverter.class);
		final BasicPrincipal basicPrincipalMock = createMock(BasicPrincipal.class);

		final LicenseCancelInfo licenseCancelInfo = new LicenseCancelInfo();

		expect(license2LicenseCancelInfoConverter.convertTo(license("123"))).andReturn(licenseCancelInfo);
		expect(licenseCancelInfo2StringConverter.convertTo(licenseCancelInfo)).andReturn("log");
		expect(basicPrincipalMock.getUser()).andReturn("user");
		expect(cljlLicenseServiceMock.cancel(licenseCancelInfo)).andThrow(new LicenseException("errorCode", "message"));

		replayAll();
		setInternalState(this.operation, cljlLicenseServiceMock, basicPrincipalMock);
		setInternalState(this.operation, "license2LicenseCancelInfoConverter", license2LicenseCancelInfoConverter);
		setInternalState(this.operation, "licenseCancelInfo2StringConverter", licenseCancelInfo2StringConverter);
		try {
			this.operation.cancel(license("123"));
			fail();
		} catch (final CancelException e) {
			assertNotNull(e.getMessage());
			assertEquals("errorCode", e.getCljlErrorCode());
		}
		verifyAll();
	}
}
