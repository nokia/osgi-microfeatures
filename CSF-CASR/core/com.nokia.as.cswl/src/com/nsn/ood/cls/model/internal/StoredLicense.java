/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.json.DateTimeDeserializer;
import com.nsn.ood.cls.model.json.DateTimeSerializer;


/**
 * @author marynows
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
		"customerName", "customerId", "orderId", "user", "importDate", "remainingCapacity" })
public class StoredLicense extends License {
	@JsonProperty("customerName")
	private String customerName;

	@JsonProperty("customerId")
	private String customerId;

	@JsonProperty("orderId")
	private String orderId;

	@JsonProperty("user")
	private String user;

	@JsonProperty("importDate")
	private DateTime importDate;

	@JsonProperty("remainingCapacity")
	private Long remainingCapacity;

	public String getCustomerName() {
		return this.customerName;
	}

	public void setCustomerName(final String customerName) {
		this.customerName = customerName;
	}

	public StoredLicense withCustomerName(final String customerName) {
		setCustomerName(customerName);
		return this;
	}

	public String getCustomerId() {
		return this.customerId;
	}

	public void setCustomerId(final String customerId) {
		this.customerId = customerId;
	}

	public StoredLicense withCustomerId(final String customerId) {
		setCustomerId(customerId);
		return this;
	}

	public String getOrderId() {
		return this.orderId;
	}

	public void setOrderId(final String orderId) {
		this.orderId = orderId;
	}

	public StoredLicense withOrderId(final String orderId) {
		setOrderId(orderId);
		return this;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(final String user) {
		this.user = user;
	}

	public StoredLicense withUser(final String user) {
		setUser(user);
		return this;
	}

	public DateTime getImportDate() {
		return this.importDate;
	}

	public void setImportDate(final DateTime importDate) {
		this.importDate = importDate;
	}

	public StoredLicense withImportDate(final DateTime importDate) {
		setImportDate(importDate);
		return this;
	}

	public Long getRemainingCapacity() {
		return this.remainingCapacity;
	}

	public void setRemainingCapacity(final Long remainingCapacity) {
		this.remainingCapacity = remainingCapacity;
	}

	public StoredLicense withRemainingCapacity(final Long remainingCapacity) {
		setRemainingCapacity(remainingCapacity);
		return this;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().appendSuper(super.hashCode()).append(this.customerName).append(this.customerId)
				.append(this.orderId).append(this.user).append(this.importDate).append(this.remainingCapacity)
				.toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof StoredLicense)) {
			return false;
		}
		final StoredLicense rhs = (StoredLicense) other;
		return new EqualsBuilder().appendSuper(super.equals(other)).append(this.customerName, rhs.customerName)
				.append(this.customerId, rhs.customerId).append(this.orderId, rhs.orderId).append(this.user, rhs.user)
				.append(this.importDate, rhs.importDate).append(this.remainingCapacity, rhs.remainingCapacity)
				.isEquals();
	}
}
