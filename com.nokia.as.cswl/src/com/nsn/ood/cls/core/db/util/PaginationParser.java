/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.util;

import com.nsn.ood.cls.core.condition.Pagination;


/**
 * @author marynows
 * 
 */
class PaginationParser {
	private String sql = "";

	public PaginationParser(final Pagination pagination) {
		if (pagination.limit() != Pagination.DEFAULT_LIMIT) {
			this.sql += " limit " + pagination.limit();
		}
		if (pagination.offset() != Pagination.DEFAULT_OFFSET) {
			this.sql += " offset " + pagination.offset();
		}
	}

	public String sql() {
		return this.sql;
	}
}
