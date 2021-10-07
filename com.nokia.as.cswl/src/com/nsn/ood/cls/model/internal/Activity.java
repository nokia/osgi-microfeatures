/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.nsn.ood.cls.model.json.DateTimeDeserializer;
import com.nsn.ood.cls.model.json.DateTimeSerializer;


/**
 * @author wro50095
 * 
 */
@JsonPropertyOrder({
		"id", "activityTime", "clientId", "operationType", "result" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Activity {
	@JsonProperty("id")
	private Long id;

	@JsonProperty("activityTime")
	private DateTime activityTime;

	@JsonProperty("clientId")
	private String clientId;

	@JsonProperty("operationType")
	private Activity.OperationType operationType;

	@JsonProperty("result")
	private Activity.Result result;

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public Activity withId(final Long id) {
		setId(id);
		return this;
	}

	public String getClientId() {
		return this.clientId;
	}

	public void setClientId(final String clientId) {
		this.clientId = clientId;
	}

	public Activity withClientId(final String clientId) {
		setClientId(clientId);
		return this;
	}

	public DateTime getActivityTime() {
		return this.activityTime;
	}

	public void setActivityTime(final DateTime time) {
		this.activityTime = time;
	}

	public Activity withActivityTime(final DateTime time) {
		setActivityTime(time);
		return this;
	}

	public Activity.OperationType getOperationType() {
		return this.operationType;
	}

	public void setOperationType(final Activity.OperationType operationType) {
		this.operationType = operationType;
	}

	public Activity withOperationType(final Activity.OperationType operationType) {
		setOperationType(operationType);
		return this;
	}

	public Activity.Result getResult() {
		return this.result;
	}

	public void setResult(final Activity.Result result) {
		this.result = result;
	}

	public Activity withResult(final Activity.Result result) {
		setResult(result);
		return this;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.id).append(this.activityTime).append(this.clientId)
				.append(this.operationType).append(this.result).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof Activity)) {
			return false;
		}
		final Activity rhs = (Activity) other;
		return new EqualsBuilder().append(this.id, rhs.id).append(this.activityTime, rhs.activityTime)
				.append(this.clientId, rhs.clientId).append(this.operationType, rhs.operationType)
				.append(this.result, rhs.result).isEquals();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public static enum OperationType {
		LICENSE_INSTALL("license_install"), //
		LICENSE_CANCEL("license_cancel"), //
		SETTING_UPDATE("setting_update");

		private final String value;
		private static Map<String, Activity.OperationType> constants = new HashMap<>();

		static {
			for (final Activity.OperationType c : values()) {
				constants.put(c.value, c);
			}
		}

		private OperationType(final String value) {
			this.value = value;
		}

		@JsonValue
		@Override
		public String toString() {
			return this.value;
		}

		@JsonCreator
		public static Activity.OperationType fromValue(final String value) {
			final Activity.OperationType constant = constants.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}
	}

	public static enum Result {
		SUCCESS("success"), //
		FAILURE("failure"), //
		PARTIAL("partial");

		private final String value;
		private static Map<String, Activity.Result> constants = new HashMap<>();

		static {
			for (final Activity.Result c : values()) {
				constants.put(c.value, c);
			}
		}

		private Result(final String value) {
			this.value = value;
		}

		@JsonValue
		@Override
		public String toString() {
			return this.value;
		}

		@JsonCreator
		public static Activity.Result fromValue(final String value) {
			final Activity.Result constant = constants.get(value);
			if (constant == null) {
				throw new IllegalArgumentException(value);
			} else {
				return constant;
			}
		}
	}
}
