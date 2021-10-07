package com.nsn.ood.cls.core.model;

public class FeatureUpdate {
	private long featureCode;
	private long usageDelta;

	public long getFeatureCode() {
		return this.featureCode;
	}

	public void setFeatureCode(final long featureCode) {
		this.featureCode = featureCode;
	}

	public long getUsageDelta() {
		return this.usageDelta;
	}

	public void setUsageDelta(final long usageDelta) {
		this.usageDelta = usageDelta;
	}

}
