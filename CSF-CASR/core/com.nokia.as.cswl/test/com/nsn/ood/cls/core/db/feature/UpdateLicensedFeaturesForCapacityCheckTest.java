/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeature;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class UpdateLicensedFeaturesForCapacityCheckTest {
	private static final DateTime TIME = new DateTime(2015, 8, 26, 16, 12);

	@Test
	public void testSql() throws Exception {
		assertEquals("update cls.features set capacitychecktime = ? where featurecode = ?",
				new UpdateLicensedFeaturesForCapacityCheck(null, null, null).sql());
	}

	@Test
	public void testPrepareRow() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<Timestamp, DateTime> converterMock = createMock(Timestamp2DateTimeConverter.class);

		expect(converterMock.convertFrom(TIME)).andReturn(new Timestamp(12345678L));
		statementMock.setTimestamp(1, new Timestamp(12345678L));
		statementMock.setLong(2, 1234L);

		replayAll();
		new UpdateLicensedFeaturesForCapacityCheck(null, TIME, converterMock).prepareRow(statementMock,
				licensedFeature(1234L));
		verifyAll();
	}
}
