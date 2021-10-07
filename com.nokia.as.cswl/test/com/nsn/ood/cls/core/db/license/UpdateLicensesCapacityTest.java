/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.getInternalState;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class UpdateLicensesCapacityTest {
	private static final String EXPECTED_SQL = "update cls.licenses l,"
			+ " (select serialnumber, total, coalesce(s.used, 0) as used from cls.licenses"
			+ " left join (select serialnumber, sum(capacity) as used from cls.reservations group by serialnumber) s"
			+ " using (serialnumber)) r set l.used = r.used, l.remaining = r.total - r.used where l.serialnumber = r.serialnumber";

	@Test
	public void testUpdate() throws Exception {
		final UpdateLicensesCapacity update = new UpdateLicensesCapacity(Arrays.asList(1L, 2L));
		assertNull(update.next());
		update.handle(0);
		assertEquals(Arrays.asList(1L, 2L), getInternalState(update, List.class));
	}

	@Test
	public void testSql() throws Exception {
		assertEquals(EXPECTED_SQL, new UpdateLicensesCapacity((List<Long>) null).sql());
		assertEquals(EXPECTED_SQL, new UpdateLicensesCapacity(Collections.<Long> emptyList()).sql());
		assertEquals(EXPECTED_SQL + " and l.featurecode = ?", new UpdateLicensesCapacity(Arrays.asList(1L)).sql());
		assertEquals(EXPECTED_SQL + " and l.featurecode in (?,?)",
				new UpdateLicensesCapacity(Arrays.asList(1L, 2L)).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		testPrepare(new UpdateLicensesCapacity((List<Long>) null), Collections.<Long> emptyList());
		testPrepare(new UpdateLicensesCapacity(Collections.<Long> emptyList()), Collections.<Long> emptyList());
		testPrepare(new UpdateLicensesCapacity(Arrays.asList(1L)), Arrays.asList(1L));
		testPrepare(new UpdateLicensesCapacity(Arrays.asList(2L, 1L)), Arrays.asList(2L, 1L));
	}

	private void testPrepare(final UpdateLicensesCapacity update, final List<Long> expectedCodes) throws SQLException {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		for (int i = 0; i < expectedCodes.size(); i++) {
			statementMock.setLong(i + 1, expectedCodes.get(i));
		}

		replayAll();
		update.prepare(statementMock);
		verifyAll();
	}
}
