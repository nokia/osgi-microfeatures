/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * @author marynows
 * 
 */
@JsonPropertyOrder({
		"settingskey", "value" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Setting {
	public static final int VALUE_MAX_LENGTH = 10000;

	private static final int ELLIPSIS_CHAR_COUNT = 3;

	@JsonProperty("settingskey")
	private SettingKey key;

	@JsonProperty("value")
	private Object value;

	public SettingKey getKey() {
		return this.key;
	}

	public void setKey(final SettingKey key) {
		this.key = key;
	}

	public Setting withKey(final SettingKey key) {
		setKey(key);
		return this;
	}

	public Object getValue() {
		return this.value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public Setting withValue(final Object value) {
		setValue(value);
		return this;
	}

	@JsonIgnore
	public String getValueAsString() {
		final String s = Objects.toString(this.value, null);
		if (s == null || s.isEmpty()) {
			return null;
		}
		if (s.length() <= VALUE_MAX_LENGTH) {
			return s;
		}
		return s.substring(0, VALUE_MAX_LENGTH - ELLIPSIS_CHAR_COUNT) + "...";
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.key).append(this.value).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Setting)) {
			return false;
		}
		final Setting rhs = (Setting) other;
		return new EqualsBuilder().append(this.key, rhs.key).append(this.value, rhs.value).isEquals();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@JsonIgnore
	public void validate() {
		if (this.key == null) {
			throw new IllegalArgumentException("Key must be not null");
		}
		this.key.validate(this.value);
	}
}
