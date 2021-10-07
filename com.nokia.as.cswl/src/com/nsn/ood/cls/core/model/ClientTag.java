/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;


/**
 * @author marynows
 * 
 */
public class ClientTag {
	private String eTag;
	private DateTime expires;

	public String getETag() {
		return this.eTag;
	}

	public void setETag(final String eTag) {
		this.eTag = eTag;
	}

	public ClientTag withETag(final String eTag) {
		this.eTag = eTag;
		return this;
	}

	public DateTime getExpires() {
		return this.expires;
	}

	public void setExpires(final DateTime expires) {
		this.expires = expires;
	}

	public ClientTag withExpires(final DateTime expires) {
		this.expires = expires;
		return this;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.eTag).append(this.expires).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof ClientTag)) {
			return false;
		}
		final ClientTag rhs = (ClientTag) other;
		return new EqualsBuilder().append(this.eTag, rhs.eTag).append(this.expires, rhs.expires).isEquals();
	}
}
