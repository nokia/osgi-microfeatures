/*
 * Copyright (c) 2016 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.reservation;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.core.db.Update;
import com.nsn.ood.cls.core.model.FeatureLicenseUsageDelta;


/**
 * @author wro50095
 *
 */
public class UpdateFeatureUsage extends SimpleUpdate {
	private final FeatureLicenseUsageDelta featureLicenseUpdate;

	/**
	 * @param sql
	 */
	public UpdateFeatureUsage(final FeatureLicenseUsageDelta featureLicenseUpdate) {
		super("update cls.features set used=used+?, remaining=remaining-? where featurecode=?");
		this.featureLicenseUpdate = featureLicenseUpdate;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		final long usageDelta = this.featureLicenseUpdate.getFeature().getUsageDelta();
		statement.setLong(1, usageDelta);
		statement.setLong(2, usageDelta);
		statement.setLong(3, this.featureLicenseUpdate.getFeature().getFeatureCode());
	}

	@Override
	public Update next() {
		return new UpdateLicenseUsage(this.featureLicenseUpdate.getLicense());
	}

}
