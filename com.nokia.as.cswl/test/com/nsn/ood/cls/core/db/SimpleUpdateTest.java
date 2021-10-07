/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class SimpleUpdateTest {

	@Test
	public void testUpdate() throws Exception {
		final SimpleUpdate update = new SimpleUpdate("sql") {
			@Override
			public void prepare(final PreparedStatement statement) throws SQLException {
			}
		};

		assertEquals("sql", update.sql());
		assertNull(update.next());
		update.handle(0);
	}
}
