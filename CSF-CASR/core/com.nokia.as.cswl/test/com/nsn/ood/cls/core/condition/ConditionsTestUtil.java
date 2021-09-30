/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.core.condition.Filter.Type;


/**
 * @author marynows
 * 
 */
public class ConditionsTestUtil {

	public static Conditions conditions(final boolean skipMetaData) {
		return new Conditions(skipMetaData);
	}

	public static Filter filter(final Type type, final String name, final String value) {
		return new Filter(type, name, value);
	}

	public static BetweenFilter betweenFilter(final String name, final String from, final String to) {
		return new BetweenFilter(name, from, to);
	}

	public static Pagination pagination(final int offset, final int limit) {
		return new Pagination(offset, limit);
	}

	public static Sorting sorting() {
		return new Sorting();
	}

	public static Field field(final String name, final Order order) {
		return new Field(name, order);
	}
}
