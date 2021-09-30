/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * @author wro50095
 * 
 */
@JsonPropertyOrder({
		"errorCode", "status", "fileName", "featureCode", "featureName", "settingKey", "settingValue" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActivityDetail {
	@JsonProperty("errorCode")
	private String errorCode;

	@JsonProperty("status")
	private ActivityDetail.Status status;

	@JsonProperty("fileName")
	private String fileName;

	@JsonProperty("featureCode")
	private Long featureCode;

	@JsonProperty("featureName")
	private String featureName;

	@JsonProperty("settingKey")
	private String settingKey;

	@JsonProperty("settingValue")
	private String settingValue;

	public String getErrorCode() {
		return this.errorCode;
	}

	public void setErrorCode(final String errorCodee) {
		this.errorCode = errorCodee;
	}

	public ActivityDetail withErrorCode(final String errorCode) {
		setErrorCode(errorCode);
		return this;
	}

	public ActivityDetail.Status getStatus() {
		return this.status;
	}

	public void setStatus(final ActivityDetail.Status status) {
		this.status = status;
	}

	public ActivityDetail withStatus(final ActivityDetail.Status status) {
		setStatus(status);
		return this;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public ActivityDetail withFileName(final String fileName) {
		setFileName(fileName);
		return this;
	}

	public Long getFeatureCode() {
		return this.featureCode;
	}

	public void setFeatureCode(final Long featureCode) {
		this.featureCode = featureCode;
	}

	public ActivityDetail withFeatureCode(final Long featureCode) {
		setFeatureCode(featureCode);
		return this;
	}

	public String getFeatureName() {
		return this.featureName;
	}

	public void setFeatureName(final String featureName) {
		this.featureName = featureName;
	}

	public ActivityDetail withFeatureName(final String featureName) {
		setFeatureName(featureName);
		return this;
	}

	public String getSettingKey() {
		return this.settingKey;
	}

	public void setSettingKey(final String settingKey) {
		this.settingKey = settingKey;
	}

	public ActivityDetail withSettingKey(final String settingKey) {
		setSettingKey(settingKey);
		return this;
	}

	public String getSettingValue() {
		return this.settingValue;
	}

	public void setSettingValue(final String settingValue) {
		this.settingValue = settingValue;
	}

	public ActivityDetail withSettingValue(final String settingValue) {
		setSettingValue(settingValue);
		return this;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.errorCode).append(this.status).append(this.fileName)
				.append(this.featureCode).append(this.featureName).append(this.settingKey).append(this.settingValue)
				.toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof ActivityDetail)) {
			return false;
		}
		final ActivityDetail rhs = (ActivityDetail) other;
		return new EqualsBuilder().append(this.errorCode, rhs.errorCode).append(this.status, rhs.status)
				.append(this.fileName, rhs.fileName).append(this.featureCode, rhs.featureCode)
				.append(this.featureName, rhs.featureName).append(this.settingKey, rhs.settingKey)
				.append(this.settingValue, rhs.settingValue).isEquals();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public static enum Status {
		SUCCESS("success"), //
		FAILURE("failure");

		private final String value;
		private static Map<String, ActivityDetail.Status> constants = new HashMap<>();

		static {
			for (final ActivityDetail.Status c : values()) {
				constants.put(c.value, c);
			}
		}

		private Status(final String value) {
			this.value = value;
		}

		@JsonValue
		@Override
		public String toString() {
			return this.value;
		}

		@JsonCreator
		public static ActivityDetail.Status fromValue(final String value) {
			final ActivityDetail.Status constant = constants.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}
	}
}
