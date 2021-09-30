/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

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
public class QueryFeatureExistTest {

	@Test
	public void testQuery() throws Exception {
		final QueryFeatureExist query = new QueryFeatureExist(null);
		assertEquals("select 1 from cls.features where featurecode = ?", query.sql());
		assertFalse(query.getValue());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setLong(1, 1234L);

		replayAll();
		new QueryFeatureExist(1234L).prepare(statementMock);
		verifyAll();
	}

	@Test
	public void testHandleValue() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getBoolean(1)).andReturn(true);

		replayAll();
		assertTrue(new QueryFeatureExist(null).handleValue(resultSetMock));
		verifyAll();
	}
}
