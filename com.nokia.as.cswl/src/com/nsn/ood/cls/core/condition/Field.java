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
public class Field implements Cloneable {

	public static enum Order {
		ASC, DESC
	}

	private final String name;
	private final Order order;

	Field(final String name, final Order order) {
		this.name = name;
		this.order = order;
	}

	public Order order() {
		return this.order;
	}

	public String name() {
		return this.name;
	}

	@Override
	public Field clone() {
		Field clone = null;
		try {
			clone = (Field) super.clone();
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
		return new HashCodeBuilder().append(this.name).append(this.order).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Field)) {
			return false;
		}
		final Field rhs = (Field) other;
		return new EqualsBuilder().append(this.name, rhs.name).append(this.order, rhs.order).isEquals();
	}
}
