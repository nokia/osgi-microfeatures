/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class PaginationTest {
	private static final Pagination PAGINATION = new Pagination(1, 2);

	@Test
	public void testPagination() throws Exception {
		assertPagination(PAGINATION, 1, 2, true);
		assertPagination(new Pagination(Pagination.DEFAULT_OFFSET, 2), Pagination.DEFAULT_OFFSET, 2, true);
		assertPagination(new Pagination(1, Pagination.DEFAULT_LIMIT), 1, Pagination.DEFAULT_LIMIT, true);
		assertPagination(new Pagination(Pagination.DEFAULT_OFFSET, Pagination.DEFAULT_LIMIT),
				Pagination.DEFAULT_OFFSET, Pagination.DEFAULT_LIMIT, false);
	}

	private void assertPagination(final Pagination pagination, final int offset, final int limit, final boolean limited) {
		assertEquals(offset, pagination.offset());
		assertEquals(limit, pagination.limit());
		assertEquals(limited, pagination.isLimited());
	}

	@Test
	public void testClone() throws Exception {
		final Pagination pagination2 = PAGINATION.clone();

		assertNotSame(PAGINATION, pagination2);
		assertEquals(PAGINATION, pagination2);
		assertEquals(PAGINATION.hashCode(), pagination2.hashCode());
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(PAGINATION.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertEquals(PAGINATION, PAGINATION);
		assertFalse(PAGINATION.equals(null));
		assertFalse(PAGINATION.equals("test"));
	}
}
