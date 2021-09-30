/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import static com.nsn.ood.cls.core.test.LicenseFileTestUtil.licenseFile;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class QueryLicenseFileTest {

	@Test
	public void testQuery() throws Exception {
		final QueryLicenseFile query = new QueryLicenseFile("12345");
		assertEquals("select licensefilename, licensefilepath from cls.storedlicense where serialnumber = ?",
				query.sql());
		assertNull(query.getValue());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, "12345");

		replayAll();
		new QueryLicenseFile("12345").prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandleValue() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("licensefilename")).andReturn("fff");
		expect(resultSetMock.getString("licensefilepath")).andReturn("ppp");

		replayAll();
		assertEquals(licenseFile("12345", "fff", "ppp"), new QueryLicenseFile("12345").handleValue(resultSetMock));
		verifyAll();
	}
}
