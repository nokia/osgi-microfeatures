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
public class Filter implements Cloneable {

	public static enum Type {
		EQUAL, BETWEEN, WILDCARD
	}

	private final Type type;
	private final String name;
	private final String value;

	Filter(final Type type, final String name, final String value) {
		this.type = type;
		this.name = name;
		this.value = value;
	}

	public String name() {
		return this.name;
	}

	public Type type() {
		return this.type;
	}

	public String value() {
		return this.value;
	}

	@Override
	public Filter clone() {
		Filter clone = null;
		try {
			clone = (Filter) super.clone();
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
		return new HashCodeBuilder().append(this.type).append(this.name).append(this.value).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Filter)) {
			return false;
		}
		final Filter rhs = (Filter) other;
		return new EqualsBuilder().append(this.type, rhs.type).append(this.name, rhs.name)
				.append(this.value, rhs.value).isEquals();
	}
}
