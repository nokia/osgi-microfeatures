/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.PreparedStatement;

import org.junit.Test;

import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class DeleteReservationsByLicenseTypeTest {
	private static final String EXPECTED_SQL = "delete from cls.reservations where clientid = ? and type = ?";

	@Test
	public void testSql() throws Exception {
		assertEquals(EXPECTED_SQL, new DeleteReservationsByLicenseType(null, null, null).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);
		final Converter<License.Type, Integer> converterMock = createMock(LicenseType2IntegerConverter.class);

		statementMock.setString(1, "12345");
		expect(converterMock.convertTo(Type.FLOATING_POOL)).andReturn(4);
		statementMock.setInt(2, 4);

		replayAll();
		new DeleteReservationsByLicenseType(client("12345"), Type.FLOATING_POOL, converterMock).prepare(statementMock);
		verifyAll();
	}
}
