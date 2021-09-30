/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
public class QueryLicenseExistTest {

	@Test
	public void testQuery() throws Exception {
		final QueryLicenseExist query = new QueryLicenseExist(null);
		assertEquals("select 1 from cls.licenses where serialnumber = ?", query.sql());
		assertFalse(query.getValue());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setString(1, "serial");

		replayAll();
		new QueryLicenseExist("serial").prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandleValue() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getBoolean(1)).andReturn(true);

		replayAll();
		assertTrue(new QueryLicenseExist(null).handleValue(resultSetMock));
		verifyAll();
	}
}
