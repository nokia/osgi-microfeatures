// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

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
