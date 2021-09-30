/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * @author marynows
 * 
 */
public class Pagination implements Cloneable {
	public static final int DEFAULT_OFFSET = 0;
	public static final int DEFAULT_LIMIT = Integer.MAX_VALUE;

	private final int offset;
	private final int limit;

	Pagination(final int offset, final int limit) {
		this.offset = offset;
		this.limit = limit;
	}

	public int offset() {
		return this.offset;
	}

	public int limit() {
		return this.limit;
	}

	public boolean isLimited() {
		return (this.offset != DEFAULT_OFFSET) || (this.limit != DEFAULT_LIMIT);
	}

	@Override
	public Pagination clone() {
		Pagination clone = null;
		try {
			clone = (Pagination) super.clone();
		} catch (final CloneNotSupportedException e) {
		}
		return clone;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.offset).append(this.limit).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Pagination)) {
			return false;
		}
		final Pagination rhs = (Pagination) other;
		return new EqualsBuilder().append(this.offset, rhs.offset).append(this.limit, rhs.limit).isEquals();
	}
}
