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
		"featureCode", "featureName", "capacityUnit", "targetType", "totalCapacity", "usedCapacity",
		"remainingCapacity" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicensedFeature {
	@JsonProperty("featureCode")
	private Long featureCode;

	@JsonProperty("featureName")
	private String featureName;

	@JsonProperty("capacityUnit")
	private String capacityUnit;

	@JsonProperty("targetType")
	private String targetType;

	@JsonProperty("totalCapacity")
	private Long totalCapacity;

	@JsonProperty("usedCapacity")
	private Long usedCapacity;

	@JsonProperty("remainingCapacity")
	private Long remainingCapacity;

	public Long getFeatureCode() {
		return this.featureCode;
	}

	public void setFeatureCode(final Long featureCode) {
		this.featureCode = featureCode;
	}

	public LicensedFeature withFeatureCode(final Long featureCode) {
		setFeatureCode(featureCode);
		return this;
	}

	public String getFeatureName() {
		return this.featureName;
	}

	public void setFeatureName(final String featureName) {
		this.featureName = featureName;
	}

	public LicensedFeature withFeatureName(final String featureName) {
		setFeatureName(featureName);
		return this;
	}

	public String getCapacityUnit() {
		return this.capacityUnit;
	}

	public void setCapacityUnit(final String capacityUnit) {
		this.capacityUnit = capacityUnit;
	}

	public LicensedFeature withCapacityUnit(final String capacityUnit) {
		setCapacityUnit(capacityUnit);
		return this;
	}

	public String getTargetType() {
		return this.targetType;
	}

	public void setTargetType(final String targetType) {
		this.targetType = targetType;
	}

	public LicensedFeature withTargetType(final String targetType) {
		setTargetType(targetType);
		return this;
	}

	public Long getTotalCapacity() {
		return this.totalCapacity;
	}

	public void setTotalCapacity(final Long totalCapacity) {
		this.totalCapacity = totalCapacity;
	}

	public LicensedFeature withTotalCapacity(final Long totalCapacity) {
		setTotalCapacity(totalCapacity);
		return this;
	}

	public Long getUsedCapacity() {
		return this.usedCapacity;
	}

	public void setUsedCapacity(final Long usedCapacity) {
		this.usedCapacity = usedCapacity;
	}

	public LicensedFeature withUsedCapacity(final Long usedCapacity) {
		setUsedCapacity(usedCapacity);
		return this;
	}

	public Long getRemainingCapacity() {
		return this.remainingCapacity;
	}

	public void setRemainingCapacity(final Long remainingCapacity) {
		this.remainingCapacity = remainingCapacity;
	}

	public LicensedFeature withRemainingCapacity(final Long remainingCapacity) {
		setRemainingCapacity(remainingCapacity);
		return this;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.featureCode).append(this.featureName).append(this.capacityUnit)
				.append(this.targetType).append(this.totalCapacity).append(this.usedCapacity)
				.append(this.remainingCapacity).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof LicensedFeature)) {
			return false;
		}
		final LicensedFeature rhs = (LicensedFeature) other;
		return new EqualsBuilder().append(this.featureCode, rhs.featureCode).append(this.featureName, rhs.featureName)
				.append(this.capacityUnit, rhs.capacityUnit).append(this.targetType, rhs.targetType)
				.append(this.totalCapacity, rhs.totalCapacity).append(this.usedCapacity, rhs.usedCapacity)
				.append(this.remainingCapacity, rhs.remainingCapacity).isEquals();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
