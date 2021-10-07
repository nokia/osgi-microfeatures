/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * @author marynows
 * 
 */
public class LicenseFile {
	private String serialNumber;
	private String fileName;
	private String content;

	public String getSerialNumber() {
		return this.serialNumber;
	}

	public void setSerialNumber(final String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public LicenseFile withSerialNumber(final String serialNumber) {
		setSerialNumber(serialNumber);
		return this;
	}

	public String getFileName() {
		return this.fileName;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public LicenseFile withFileName(final String fileName) {
		setFileName(fileName);
		return this;
	}

	public String getContent() {
		return this.content;
	}

	public void setContent(final String content) {
		this.content = content;
	}

	public LicenseFile withContent(final String content) {
		setContent(content);
		return this;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(this.serialNumber).append(this.fileName).append(this.content).toHashCode();
	}

	@Override
	public boolean equals(final Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof LicenseFile)) {
			return false;
		}
		final LicenseFile rhs = (LicenseFile) other;
		return new EqualsBuilder().append(this.serialNumber, rhs.serialNumber).append(this.fileName, rhs.fileName)
				.append(this.content, rhs.content).isEquals();
	}
}
