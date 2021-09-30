package com.nsn.ood.cls.core.model;

import java.util.List;


public class FeatureLicenseUsageDelta {
	private FeatureUpdate feature;
	private List<LicenseUpdate> license;

	public FeatureUpdate getFeature() {
		return this.feature;
	}

	public void setFeature(final FeatureUpdate feature) {
		this.feature = feature;
	}

	public List<LicenseUpdate> getLicense() {
		return this.license;
	}

	public void setLicense(final List<LicenseUpdate> license) {
		this.license = license;
	}

}
