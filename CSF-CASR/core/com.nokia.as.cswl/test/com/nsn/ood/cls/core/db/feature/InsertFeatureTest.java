/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
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
public class InsertFeatureTest {

	@Test
	public void testSql() throws Exception {
		assertEquals(
				"insert into cls.features (featurecode, featurename, capacityunit, targettype, total, used, remaining)"
						+ " values (?, ?, ?, ?, 0, 0, 0)", new InsertFeature(null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		statementMock.setLong(1, 1234L);
		statementMock.setString(2, "name");
		statementMock.setString(3, "unit");
		statementMock.setString(4, "target");

		replayAll();
		new InsertFeature(license()//
				.withCapacityUnit("unit")//
				.withTargetType("target")//
				.withFeatures(featuresList(feature(1234L, "name")))).prepare(statementMock);
		verifyAll();
	}
}
