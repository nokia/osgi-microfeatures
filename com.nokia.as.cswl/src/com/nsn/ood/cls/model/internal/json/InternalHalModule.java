/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal.json;

import java.util.ArrayList;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.model.json.EmbeddedDeserializer;
import com.nsn.ood.cls.model.json.HalModule;


/**
 * @author marynows
 * 
 */
public class InternalHalModule extends HalModule {
	private static final long serialVersionUID = 5859888799375330817L;

	@Override
	protected EmbeddedDeserializer getEmbeddedDeserializer() {
		final EmbeddedDeserializer embeddedDeserializer = new EmbeddedDeserializer();
		embeddedDeserializer.addType("reservations", new TypeReference<ArrayList<Reservation>>() {
		});
		embeddedDeserializer.addType("activities", new TypeReference<ArrayList<Activity>>() {
		});
		embeddedDeserializer.addType("activityDetails", new TypeReference<ArrayList<ActivityDetail>>() {
		});
		embeddedDeserializer.addType("settings", new TypeReference<ArrayList<Setting>>() {
		});
		embeddedDeserializer.addType("licensedFeatures", new TypeReference<ArrayList<LicensedFeature>>() {
		});
		embeddedDeserializer.addType("storedLicenses", new TypeReference<ArrayList<StoredLicense>>() {
		});
		embeddedDeserializer.addType("filterValues", new TypeReference<ArrayList<String>>() {
		});
		return embeddedDeserializer;
	}
}
