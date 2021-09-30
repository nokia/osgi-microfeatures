/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import static com.nsn.ood.cls.core.condition.ConditionsTestUtil.pagination;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Pagination;


/**
 * @author marynows
 * 
 */
public class PaginationParserTest {

	@Test
	public void testPagination() throws Exception {
		assertEquals("", new PaginationParser(pagination(Pagination.DEFAULT_OFFSET, Pagination.DEFAULT_LIMIT)).sql());
		assertEquals(" offset 1", new PaginationParser(pagination(1, Pagination.DEFAULT_LIMIT)).sql());
		assertEquals(" limit 2", new PaginationParser(pagination(Pagination.DEFAULT_OFFSET, 2)).sql());
		assertEquals(" limit 10 offset 20", new PaginationParser(pagination(20, 10)).sql());
	}
}
