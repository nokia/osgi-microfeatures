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
import com.nsn.ood.cls.model.json.DateTimeDeserializer;
import com.nsn.ood.cls.model.json.DateTimeSerializer;


/**
 * @author marynows
 * 
 */
@JsonPropertyOrder({
		"second", "minute", "hour", "dayOfWeek", "dayOfMonth", "month", "year", "start", "end", "timezone" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskExpression {
	@JsonProperty("dayOfMonth")
	private String dayOfMonth = "*";

	@JsonProperty("dayOfWeek")
	private String dayOfWeek = "*";

	@JsonProperty("end")
	private DateTime end;

	@JsonProperty("hour")
	private String hour = "0";

	@JsonProperty("minute")
	private String minute = "0";

	@JsonProperty("month")
	private String month = "*";

	@JsonProperty("second")
	private String second = "0";

	@JsonProperty("start")
	private DateTime start;

	@JsonProperty("timezone")
	private String timezone = "";

	@JsonProperty("year")
	private String year = "*";

	public String getDayOfMonth() {
		return this.dayOfMonth;
	}

	public void setDayOfMonth(final String dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}

	public TaskExpression withDayOfMonth(final String dayOfMonth) {
		setDayOfMonth(dayOfMonth);
		return this;
	}

	public String getDayOfWeek() {
		return this.dayOfWeek;
	}

	public void setDayOfWeek(final String dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public TaskExpression withDayOfWeek(final String dayOfWeek) {
		setDayOfWeek(dayOfWeek);
		return this;
	}

	public DateTime getEnd() {
		return this.end;
	}

	public void setEnd(final DateTime end) {
		this.end = end;
	}

	public TaskExpression withEnd(final DateTime end) {
		setEnd(end);
		return this;
	}

	public String getHour() {
		return this.hour;
	}

	public void setHour(final String hour) {
		this.hour = hour;
	}

	public TaskExpression withHour(final String hour) {
		setHour(hour);
		return this;
	}

	public String getMinute() {
		return this.minute;
	}

	public void setMinute(final String minute) {
		this.minute = minute;
	}

	public TaskExpression withMinute(final String minute) {
		setMinute(minute);
		return this;
	}

	public String getMonth() {
		return this.month;
	}

	public void setMonth(final String month) {
		this.month = month;
	}

	public TaskExpression withMonth(final String month) {
		setMonth(month);
		return this;
	}

	public String getSecond() {
		return this.second;
	}

	public void setSecond(final String second) {
		this.second = second;
	}

	public TaskExpression withSecond(final String second) {
		setSecond(second);
		return this;
	}

	public DateTime getStart() {
		return this.start;
	}

	public void setStart(final DateTime start) {
		this.start = start;
	}

	public TaskExpression withStart(final DateTime start) {
		setStart(start);
		return this;
	}

	public String getTimezone() {
		return this.timezone;
	}

	public void setTimezone(final String timezone) {
		this.timezone = timezone;
	}

	public TaskExpression withTimezone(final String timezone) {
		setTimezone(timezone);
		return this;
	}

	public String getYear() {
		return this.year;
	}

	public void setYear(final String year) {
		this.year = year;
	}

	public TaskExpression withYear(final String year) {
		setYear(year);
		return this;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.dayOfMonth).append(this.dayOfWeek).append(this.end).append(this.hour)
				.append(this.minute).append(this.month).append(this.second).append(this.start).append(this.timezone)
				.append(this.year).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof TaskExpression)) {
			return false;
		}
		final TaskExpression rhs = (TaskExpression) other;
		return new EqualsBuilder().append(this.dayOfMonth, rhs.dayOfMonth).append(this.dayOfWeek, rhs.dayOfWeek)
				.append(this.end, rhs.end).append(this.hour, rhs.hour).append(this.minute, rhs.minute)
				.append(this.month, rhs.month).append(this.second, rhs.second).append(this.start, rhs.start)
				.append(this.timezone, rhs.timezone).append(this.year, rhs.year).isEquals();
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
