/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

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

import com.nsn.ood.cls.core.model.LicenseState;


/**
 * @author marynows
 * 
 */
public class UpdateFeatureCapacityTest {
	private static final String EXPECTED_SQL = "update cls.features f,"
			+ " (select featurecode, coalesce(l.total, 0) as total, coalesce(r.used, 0) as used from cls.features"
			+ " left join (select featurecode, sum(total) as total from cls.licenses where state = '"
			+ LicenseState.ACTIVE.toString() + "' group by featurecode) l using (featurecode)"
			+ " left join (select featurecode, sum(capacity) as used from cls.reservations group by featurecode) r"
			+ " using (featurecode)) c set f.total = c.total, f.used = c.used, f.remaining = c.total - c.used where f.featurecode = c.featurecode";

	@Test
	public void testUpdate() throws Exception {
		testUpdate(new UpdateFeatureCapacity(1234L), Arrays.asList(1234L));
		testUpdate(new UpdateFeatureCapacity(Arrays.asList(1L, 2L)), Arrays.asList(1L, 2L));
	}

	private void testUpdate(final UpdateFeatureCapacity update, final List<Long> expectedFeatures) throws Exception {
		assertNull(update.next());
		update.handle(0);
		assertEquals(expectedFeatures, getInternalState(update, List.class));
	}

	@Test
	public void testSql() throws Exception {
		assertEquals(EXPECTED_SQL + " and f.featurecode = ?", new UpdateFeatureCapacity(1L).sql());

		assertEquals(EXPECTED_SQL, new UpdateFeatureCapacity((List<Long>) null).sql());
		assertEquals(EXPECTED_SQL, new UpdateFeatureCapacity(Collections.<Long> emptyList()).sql());
		assertEquals(EXPECTED_SQL + " and f.featurecode = ?", new UpdateFeatureCapacity(Arrays.asList(1L)).sql());
		assertEquals(EXPECTED_SQL + " and f.featurecode in (?,?)",
				new UpdateFeatureCapacity(Arrays.asList(1L, 2L)).sql());
	}

	@Test
	public void testPrepare() throws Exception {
		testPrepare(new UpdateFeatureCapacity(1L), Arrays.asList(1L));

		testPrepare(new UpdateFeatureCapacity((List<Long>) null), Collections.<Long> emptyList());
		testPrepare(new UpdateFeatureCapacity(Collections.<Long> emptyList()), Collections.<Long> emptyList());
		testPrepare(new UpdateFeatureCapacity(Arrays.asList(1L)), Arrays.asList(1L));
		testPrepare(new UpdateFeatureCapacity(Arrays.asList(2L, 1L)), Arrays.asList(2L, 1L));
	}

	private void testPrepare(final UpdateFeatureCapacity update, final List<Long> expectedCodes) throws SQLException {
		final PreparedStatement statementMock = createMock(PreparedStatement.class);

		for (int i = 0; i < expectedCodes.size(); i++) {
			statementMock.setLong(i + 1, expectedCodes.get(i));
		}

		replayAll();
		update.prepare(statementMock);
		verifyAll();
	}
}
