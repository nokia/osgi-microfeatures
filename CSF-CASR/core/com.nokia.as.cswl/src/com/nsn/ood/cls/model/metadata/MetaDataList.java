/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.metadata;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.nsn.ood.cls.model.gen.metadata.MetaData;


/**
 * @author marynows
 * 
 */
@JsonPropertyOrder({
		"list", "metaData" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaDataList<T> {
	@JsonProperty("list")
	private final List<T> list;
	@JsonProperty("metaData")
	private final MetaData metaData;

	public MetaDataList() {
		this.list = Collections.<T> emptyList();
		this.metaData = new MetaData().withTotal(0L).withFiltered(0L);
	}

	public MetaDataList(final List<T> list, final MetaData metaData) {
		this.list = list;
		this.metaData = metaData;
	}

	public List<T> getList() {
		return this.list;
	}

	public MetaData getMetaData() {
		return this.metaData;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.list).append(this.metaData).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof MetaDataList)) {
			return false;
		}
		final MetaDataList<?> rhs = (MetaDataList<?>) other;
		return new EqualsBuilder().append(this.list, rhs.list).append(this.metaData, rhs.metaData).isEquals();
	}
}
