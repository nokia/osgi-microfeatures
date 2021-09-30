/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.condition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.nsn.ood.cls.core.condition.Field.Order;
import com.nsn.ood.cls.util.Strings;


/**
 * @author marynows
 * 
 */
public class Sorting implements Cloneable {
	private Map<String, Field> fields = new LinkedHashMap<>();

	Sorting() {
	}

	public List<Field> fields() {
		return new ArrayList<>(this.fields.values());
	}

	public boolean hasFields() {
		return !this.fields.isEmpty();
	}

	public void addField(final String name, final Order order) {
		if (!Strings.isNullOrEmpty(name) && order != null && !this.fields.containsKey(name)) {
			this.fields.put(name, new Field(name, order));
		}
	}

	@Override
	public Sorting clone() {
		Sorting clone = null;
		try {
			clone = (Sorting) super.clone();
		} catch (final CloneNotSupportedException e) {
		}

		clone.fields = new LinkedHashMap<>();
		for (final Entry<String, Field> e : this.fields.entrySet()) {
			clone.fields.put(e.getKey(), e.getValue().clone());
		}
		return clone;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.fields).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Sorting)) {
			return false;
		}
		final Sorting rhs = (Sorting) other;
		return new EqualsBuilder().append(this.fields, rhs.fields).isEquals();
	}
}
