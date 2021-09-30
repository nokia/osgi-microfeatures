/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.internal.json;

import static org.easymock.EasyMock.cmp;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectNew;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.util.ArrayList;

import org.easymock.LogicalOperator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.model.json.EmbeddedDeserializer;
import com.nsn.ood.cls.model.test.TypeReferenceComparator;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	InternalHalModule.class })
public class InternalHalModuleTest {
	private static final TypeReferenceComparator COMPARATOR = new TypeReferenceComparator();

	@Test
	public void testGetEmbeddedDeserializer() throws Exception {
		final EmbeddedDeserializer deserializerMock = createMock(EmbeddedDeserializer.class);

		expectNew(EmbeddedDeserializer.class).andReturn(deserializerMock).atLeastOnce();

		expect(deserializerMock.addType(eq("reservations"), cmp(new TypeReference<ArrayList<Reservation>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(null).atLeastOnce();
		expect(deserializerMock.addType(eq("activities"), cmp(new TypeReference<ArrayList<Activity>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(null).atLeastOnce();
		expect(deserializerMock.addType(eq("activityDetails"), cmp(new TypeReference<ArrayList<ActivityDetail>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(null).atLeastOnce();
		expect(deserializerMock.addType(eq("settings"), cmp(new TypeReference<ArrayList<Setting>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(null).atLeastOnce();
		expect(deserializerMock.addType(eq("licensedFeatures"), cmp(new TypeReference<ArrayList<LicensedFeature>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(null).atLeastOnce();
		expect(deserializerMock.addType(eq("storedLicenses"), cmp(new TypeReference<ArrayList<StoredLicense>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(null).atLeastOnce();
		expect(deserializerMock.addType(eq("filterValues"), cmp(new TypeReference<ArrayList<String>>() {
		}, COMPARATOR, LogicalOperator.EQUAL))).andReturn(null).atLeastOnce();

		replayAll();
		assertEquals(deserializerMock, new InternalHalModule().getEmbeddedDeserializer());
		verifyAll();
	}
}
