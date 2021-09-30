/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * @author marynows
 * 
 */
public abstract class ObjectWithTag<T, S extends ObjectWithTag<?, ?>> {
	private T object;
	private ClientTag clientTag;

	public ClientTag getClientTag() {
		return this.clientTag;
	}

	public void setClientTag(final ClientTag clientTag) {
		this.clientTag = clientTag;
	}

	@SuppressWarnings("unchecked")
	public S withClientTag(final ClientTag clientTag) {
		this.clientTag = clientTag;
		return (S) this;
	}

	public T getObject() {
		return this.object;
	}

	public void setObject(final T object) {
		this.object = object;
	}

	@SuppressWarnings("unchecked")
	public S withObject(final T object) {
		this.object = object;
		return (S) this;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.object).append(this.clientTag).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof ObjectWithTag)) {
			return false;
		}
		final ObjectWithTag<?, ?> rhs = (ObjectWithTag<?, ?>) other;
		return new EqualsBuilder().append(this.object, rhs.object).append(this.clientTag, rhs.clientTag).isEquals();
	}
}
