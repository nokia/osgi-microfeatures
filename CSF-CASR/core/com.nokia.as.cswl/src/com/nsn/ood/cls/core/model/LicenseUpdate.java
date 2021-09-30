package com.nsn.ood.cls.core.model;

public class LicenseUpdate {
	private String serialNumber;
	private long usageDelta;

	public String getSerialNumber() {
		return this.serialNumber;
	}

	public void setSerialNumber(final String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public long getUsageDelta() {
		return this.usageDelta;
	}

	public void setUsageDelta(final long usageDelta) {
		this.usageDelta = usageDelta;
	}

	public void updateUsageDelta(final long value) {
		this.usageDelta += value;
	}
}