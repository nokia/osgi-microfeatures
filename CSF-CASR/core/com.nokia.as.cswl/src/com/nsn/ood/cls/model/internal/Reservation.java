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
 * @author wro50095
 *
 */
@JsonPropertyOrder({
		"featureCode", "serialNumber", "clientId", "capacity", "reservationTime", "mode", "type", "endDate",
		"fileName" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Reservation {
	@JsonProperty("featureCode")
	private Long featureCode;

	@JsonProperty("serialNumber")
	private String serialNumber;

	@JsonProperty("clientId")
	private String clientId;

	@JsonProperty("capacity")
	private Long capacity;

	@JsonProperty("reservationTime")
	private DateTime reservationTime;

	@JsonProperty("mode")
	private License.Mode mode;

	@JsonProperty("type")
	private License.Type type;

	@JsonProperty("endDate")
	private DateTime endDate;

	@JsonProperty("fileName")
	private String fileName;

	public Reservation() {
	}

	public Reservation(final Reservation toClone) {
		this.capacity = toClone.capacity;
		this.clientId = toClone.clientId;
		this.endDate = toClone.endDate;
		this.featureCode = toClone.featureCode;
		this.fileName = toClone.fileName;
		this.mode = toClone.mode;
		this.reservationTime = toClone.reservationTime;
		this.serialNumber = toClone.serialNumber;
		this.type = toClone.type;
	}

	public Long getFeatureCode() {
		return this.featureCode;
	}

	public void setFeatureCode(final Long featureCode) {
		this.featureCode = featureCode;
	}

	public Reservation withFeatureCode(final Long featureCode) {
		setFeatureCode(featureCode);
		return this;
	}

	public String getSerialNumber() {
		return this.serialNumber;
	}

	public void setSerialNumber(final String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public Reservation withSerialNumber(final String serialNumber) {
		setSerialNumber(serialNumber);
		return this;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(final String clientId) {
		this.clientId = clientId;
	}

	public Reservation withClientId(final String clientId) {
		setClientId(clientId);
		return this;
	}

	public Long getCapacity() {
		return this.capacity;
	}

	public void setCapacity(final Long capacity) {
		this.capacity = capacity;
	}

	public Reservation withCapacity(final Long capacity) {
		setCapacity(capacity);
		return this;
	}

	public DateTime getReservationTime() {
		return this.reservationTime;
	}

	public void setReservationTime(final DateTime reservationTime) {
		this.reservationTime = reservationTime;
	}

	public Reservation withReservationTime(final DateTime reservationTime) {
		setReservationTime(reservationTime);
		return this;
	}

	public License.Mode getMode() {
		return this.mode;
	}

	public void setMode(final License.Mode mode) {
		this.mode = mode;
	}

	public Reservation withMode(final License.Mode mode) {
		setMode(mode);
		return this;
	}

	public License.Type getType() {
		return this.type;
	}

	public void setType(final License.Type type) {
		this.type = type;
	}

	public Reservation withType(final License.Type type) {
		setType(type);
		return this;
	}

	public DateTime getEndDate() {
		return this.endDate;
	}

	public void setEndDate(final DateTime endDate) {
		this.endDate = endDate;
	}

	public Reservation withEndDate(final DateTime endDate) {
		setEndDate(endDate);
		return this;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public Reservation withFileName(final String fileName) {
		setFileName(fileName);
		return this;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.featureCode).append(this.serialNumber).append(this.clientId)
				.append(this.capacity).append(this.reservationTime).append(this.mode).append(this.type)
				.append(this.endDate).append(this.fileName).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Reservation)) {
			return false;
		}
		final Reservation rhs = (Reservation) other;
		return new EqualsBuilder().append(this.featureCode, rhs.featureCode).append(this.serialNumber, rhs.serialNumber)
				.append(this.clientId, rhs.clientId).append(this.capacity, rhs.capacity)
				.append(this.reservationTime, rhs.reservationTime).append(this.mode, rhs.mode)
				.append(this.type, rhs.type).append(this.endDate, rhs.endDate).append(this.fileName, rhs.fileName)
				.isEquals();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
