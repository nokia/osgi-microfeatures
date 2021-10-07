/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * @author marynows
 * 
 */
@JsonPropertyOrder({
		"releaseCapacityForExpiredClients", "releaseCapacityForExpiredLicenses", "updateLicensesState",
		"sendExpiringLicensesEmail", "sendCapacityThresholdEmail" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tasks {
	@JsonProperty("releaseCapacityForExpiredClients")
	private TaskExpression releaseCapacityForExpiredClients;

	@JsonProperty("releaseCapacityForExpiredLicenses")
	private TaskExpression releaseCapacityForExpiredLicenses;

	@JsonProperty("updateLicensesState")
	private TaskExpression updateLicensesState;

	@JsonProperty("sendExpiringLicensesEmail")
	private TaskExpression sendExpiringLicensesEmail;

	@JsonProperty("sendCapacityThresholdEmail")
	private TaskExpression sendCapacityThresholdEmail;

	public TaskExpression getReleaseCapacityForExpiredClients() {
		return this.releaseCapacityForExpiredClients;
	}

	public void setReleaseCapacityForExpiredClients(final TaskExpression clients) {
		this.releaseCapacityForExpiredClients = clients;
	}

	public Tasks withReleaseCapacityForExpiredClients(final TaskExpression clients) {
		setReleaseCapacityForExpiredClients(clients);
		return this;
	}

	public TaskExpression getReleaseCapacityForExpiredLicenses() {
		return this.releaseCapacityForExpiredLicenses;
	}

	public void setReleaseCapacityForExpiredLicenses(final TaskExpression licenses) {
		this.releaseCapacityForExpiredLicenses = licenses;
	}

	public Tasks withReleaseCapacityForExpiredLicenses(final TaskExpression licenses) {
		setReleaseCapacityForExpiredLicenses(licenses);
		return this;
	}

	public TaskExpression getUpdateLicensesState() {
		return this.updateLicensesState;
	}

	public void setUpdateLicensesState(final TaskExpression updateLicensesState) {
		this.updateLicensesState = updateLicensesState;
	}

	public Tasks withUpdateLicensesState(final TaskExpression updateLicensesState) {
		setUpdateLicensesState(updateLicensesState);
		return this;
	}

	public TaskExpression getSendExpiringLicensesEmail() {
		return this.sendExpiringLicensesEmail;
	}

	public void setSendExpiringLicensesEmail(final TaskExpression licensesEmail) {
		this.sendExpiringLicensesEmail = licensesEmail;
	}

	public Tasks withSendExpiringLicensesEmail(final TaskExpression licensesEmail) {
		setSendExpiringLicensesEmail(licensesEmail);
		return this;
	}

	public TaskExpression getSendCapacityThresholdEmail() {
		return this.sendCapacityThresholdEmail;
	}

	public void setSendCapacityThresholdEmail(final TaskExpression capacityThreshold) {
		this.sendCapacityThresholdEmail = capacityThreshold;
	}

	public Tasks withSendCapacityThresholdEmail(final TaskExpression capacityThreshold) {
		setSendCapacityThresholdEmail(capacityThreshold);
		return this;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.releaseCapacityForExpiredClients)
				.append(this.releaseCapacityForExpiredLicenses).append(this.updateLicensesState)
				.append(this.sendExpiringLicensesEmail).append(this.sendCapacityThresholdEmail).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Tasks)) {
			return false;
		}
		final Tasks rhs = (Tasks) other;
		return new EqualsBuilder().append(this.releaseCapacityForExpiredClients, rhs.releaseCapacityForExpiredClients)
				.append(this.releaseCapacityForExpiredLicenses, rhs.releaseCapacityForExpiredLicenses)
				.append(this.updateLicensesState, rhs.updateLicensesState)
				.append(this.sendExpiringLicensesEmail, rhs.sendExpiringLicensesEmail)
				.append(this.sendCapacityThresholdEmail, rhs.sendCapacityThresholdEmail).isEquals();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
