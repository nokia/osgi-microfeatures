// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.util.license;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;


public class LicenseSaxHandlerTest {
	private LicenseStatus info;
	private LicenseSaxHandler bean;

	@Before
	public void setUp() throws Exception {
		this.info = new LicenseStatus();
		this.bean = new LicenseSaxHandler(this.info);
	}

	@Test
	public void testGetLicenseName() throws Exception {
		final String licenseName = "óóóó";
		final Attributes attributes = createMock(Attributes.class);
		expect(attributes.getValue("licenceName")).andReturn(licenseName);

		replayAll();
		this.bean.startElement(null, null, "licenceInfo", attributes);
		verifyAll();

		assertEquals(licenseName, this.info.getLicenseName());
	}

	@Test
	public void testGetNoLicenseName() throws Exception {
		final Attributes attributes = createMock(Attributes.class);

		replayAll();
		this.bean.startElement(null, null, "licenceInfo1", attributes);
		verifyAll();

		assertNull(this.info.getLicenseName());
	}
}
