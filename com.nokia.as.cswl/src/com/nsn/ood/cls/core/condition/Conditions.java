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


/**
 * @author marynows
 * 
 */
public class Conditions implements Cloneable {
	private final boolean skipMetaData;
	private Map<String, Filter> filters = new LinkedHashMap<>();
	private Sorting sorting = new Sorting();
	private Pagination pagination = new Pagination(Pagination.DEFAULT_OFFSET, Pagination.DEFAULT_LIMIT);

	Conditions(final boolean skipMetaData) {
		this.skipMetaData = skipMetaData;
	}

	public List<Filter> filters() {
		return new ArrayList<>(this.filters.values());
	}

	public Sorting sorting() {
		return this.sorting;
	}

	public Pagination pagination() {
		return this.pagination;
	}

	public boolean hasFilters() {
		return !this.filters.isEmpty();
	}

	public void addFilter(final Filter filter) {
		if (filter != null && !this.filters.containsKey(filter.name())) {
			this.filters.put(filter.name(), filter);
		}
	}

	public void setSorting(final Sorting sorting) {
		if (sorting != null) {
			this.sorting = sorting;
		}
	}

	public void setPaginationOffset(final int offset) {
		this.pagination = new Pagination(offset, this.pagination.limit());
	}

	public void setPaginationLimit(final int limit) {
		this.pagination = new Pagination(this.pagination.offset(), limit);
	}

	public boolean skipMetaData() {
		return this.skipMetaData;
	}

	@Override
	public Conditions clone() {
		Conditions clone = null;
		try {
			clone = (Conditions) super.clone();
		} catch (final CloneNotSupportedException e) {
		}

		clone.filters = new LinkedHashMap<>();
		for (final Entry<String, Filter> e : this.filters.entrySet()) {
			clone.filters.put(e.getKey(), e.getValue().clone());
		}
		clone.sorting = this.sorting.clone();
		clone.pagination = this.pagination.clone();
		return clone;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.skipMetaData).append(this.filters).append(this.sorting)
				.append(this.pagination).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Conditions)) {
			return false;
		}
		final Conditions rhs = (Conditions) other;
		return new EqualsBuilder().append(this.skipMetaData, rhs.skipMetaData).append(this.filters, rhs.filters)
				.append(this.sorting, rhs.sorting).append(this.pagination, rhs.pagination).isEquals();
	}
}
