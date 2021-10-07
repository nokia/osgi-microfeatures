/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class DeleteFeatureTest {

	@Test
	public void testSql() throws Exception {
		assertEquals("delete from cls.features where featurecode = ?"
				+ " and (select count(*) from cls.licenses where featurecode = ?) = 0", new DeleteFeature(null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setLong(1, 1234L);
		statementMock.setLong(2, 1234L);

		replayAll();
		new DeleteFeature(1234L).prepare(statementMock);
		verifyAll();
	}
}
