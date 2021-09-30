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
public class BetweenFilter extends Filter implements Cloneable {
	private final String from;
	private final String to;

	BetweenFilter(final String name, final String from, final String to) {
		super(Type.BETWEEN, name, from + "," + to);
		this.from = from;
		this.to = to;
	}

	public String from() {
		return this.from;
	}

	public String to() {
		return this.to;
	}

	@Override
	public BetweenFilter clone() {
		return (BetweenFilter) super.clone();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().appendSuper(super.hashCode()).append(this.from).append(this.to).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof BetweenFilter)) {
			return false;
		}
		final BetweenFilter rhs = (BetweenFilter) other;
		return new EqualsBuilder().appendSuper(super.equals(other)).append(this.from, rhs.from).append(this.to, rhs.to)
				.isEquals();
	}
}
