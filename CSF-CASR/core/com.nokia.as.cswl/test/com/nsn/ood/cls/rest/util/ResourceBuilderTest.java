/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetail;
import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetailsList;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activitiesList;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activity;
import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeature;
import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeaturesList;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservation;
import static com.nsn.ood.cls.model.internal.test.ReservationTestUtil.reservationsList;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.settingsList;
import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.storedLicense;
import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.storedLicensesList;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.ClientTestUtil.clientsList;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.errorsList;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.feature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.link;
import static com.nsn.ood.cls.model.test.LinkTestUtil.linksList;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.model.test.ResourceTestUtil.assertResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.util.HalResource;


/**
 * @author marynows
 * 
 */
public class ResourceBuilderTest {

	@Test
	public void testBuild() throws Exception {
		final Resource resource = new ResourceBuilder().build();
		assertResource(resource, 0, 0);
	}

	@Test
	public void testPredefinedLinks() throws Exception {
		final Resource resource = new ResourceBuilder()//
				.selfLink(link("selfHref"))//
				.nextLink(link("nextHref"))//
				.prevLink(link("prevHref")).build();

		assertResource(resource, 3, 0);
		assertEquals(link("selfHref"), HalResource.getLink(resource, "self"));
		assertEquals(link("nextHref"), HalResource.getLink(resource, "next"));
		assertEquals(link("prevHref"), HalResource.getLink(resource, "previous"));
	}

	@Test
	public void testNullLinks() throws Exception {
		final Resource resource = new ResourceBuilder()//
				.selfLink(null)//
				.nextLink(null)//
				.prevLink(null)//
				.links("name", (Link) null).build();

		assertResource(resource, 0, 0);
		assertFalse(HalResource.containsLink(resource, "self"));
		assertFalse(HalResource.containsLink(resource, "next"));
		assertFalse(HalResource.containsLink(resource, "previous"));
		assertFalse(HalResource.containsLink(resource, "name"));
	}

	@Test
	public void testCustomLinks() throws Exception {
		final Resource resource = new ResourceBuilder()//
				.links("name1", link("link1"))//
				.links("name2", link("link2_1"), link("link2_2"))//
				.links("name3")//
				.build();

		assertResource(resource, 2, 0);
		assertEquals(link("link1"), HalResource.getLink(resource, "name1"));
		assertEquals(linksList(link("link2_1"), link("link2_2")), HalResource.getLinksArray(resource, "name2"));
		assertFalse(HalResource.containsLink(resource, "name3"));
	}

	@Test
	public void testErrors() throws Exception {
		assertResource(new ResourceBuilder().errors().build(), 0, 0);
		assertEmbedded(new ResourceBuilder().errors(error(1L)).build(), "errors", errorsList(error(1L)));
		assertEmbedded(new ResourceBuilder().errors(error(1L), error(2L)).build(), "errors",
				errorsList(error(1L), error(2L)));
	}

	@Test
	public void testLicenses() throws Exception {
		assertResource(new ResourceBuilder().licenses(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().licenses(licensesList()).build(), "licenses", licensesList());
		assertEmbedded(new ResourceBuilder().licenses(licensesList(license("1"))).build(), "licenses",
				licensesList(license("1")));
		assertEmbedded(new ResourceBuilder().licenses(licensesList(license("1"), license("2"))).build(), "licenses",
				licensesList(license("1"), license("2")));
	}

	@Test
	public void testClients() throws Exception {
		assertResource(new ResourceBuilder().clients(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().clients(clientsList()).build(), "clients", clientsList());
		assertEmbedded(new ResourceBuilder().clients(clientsList(client("1"))).build(), "clients",
				clientsList(client("1")));
		assertEmbedded(new ResourceBuilder().clients(clientsList(client("1"), client("2"))).build(), "clients",
				clientsList(client("1"), client("2")));
	}

	@Test
	public void testFeatures() throws Exception {
		assertResource(new ResourceBuilder().features(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().features(featuresList()).build(), "features", featuresList());
		assertEmbedded(new ResourceBuilder().features(featuresList(feature(1L))).build(), "features",
				featuresList(feature(1L)));
		assertEmbedded(new ResourceBuilder().features(featuresList(feature(1L), feature(2L))).build(), "features",
				featuresList(feature(1L), feature(2L)));
	}

	@Test
	public void testActivities() throws Exception {
		assertResource(new ResourceBuilder().activities(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().activities(activitiesList()).build(), "activities", activitiesList());
		assertEmbedded(new ResourceBuilder().activities(activitiesList(activity(1L))).build(), "activities",
				activitiesList(activity(1L)));
		assertEmbedded(new ResourceBuilder().activities(activitiesList(activity(1L), activity(2L))).build(),
				"activities", activitiesList(activity(1L), activity(2L)));
	}

	@Test
	public void testActivityDetails() throws Exception {
		assertResource(new ResourceBuilder().activityDetails(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().activityDetails(activityDetailsList()).build(), "activityDetails",
				activityDetailsList());
		assertEmbedded(new ResourceBuilder().activityDetails(activityDetailsList(activityDetail("1"))).build(),
				"activityDetails", activityDetailsList(activityDetail("1")));
		assertEmbedded(
				new ResourceBuilder().activityDetails(activityDetailsList(activityDetail("1"), activityDetail("2")))
						.build(), "activityDetails", activityDetailsList(activityDetail("1"), activityDetail("2")));
	}

	@Test
	public void testReservations() throws Exception {
		assertResource(new ResourceBuilder().reservations(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().reservations(reservationsList()).build(), "reservations",
				reservationsList());
		assertEmbedded(new ResourceBuilder().reservations(reservationsList(reservation("1"))).build(), "reservations",
				reservationsList(reservation("1")));
		assertEmbedded(
				new ResourceBuilder().reservations(reservationsList(reservation("1"), reservation("2"))).build(),
				"reservations", reservationsList(reservation("1"), reservation("2")));
	}

	@Test
	public void testLicensedFeatures() throws Exception {
		assertResource(new ResourceBuilder().licensedFeatures(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().licensedFeatures(licensedFeaturesList()).build(), "licensedFeatures",
				licensedFeaturesList());
		assertEmbedded(new ResourceBuilder().licensedFeatures(licensedFeaturesList(licensedFeature(1L))).build(),
				"licensedFeatures", licensedFeaturesList(licensedFeature(1L)));
		assertEmbedded(
				new ResourceBuilder().licensedFeatures(licensedFeaturesList(licensedFeature(1L), licensedFeature(2L)))
						.build(), "licensedFeatures", licensedFeaturesList(licensedFeature(1L), licensedFeature(2L)));
	}

	@Test
	public void testStoredLicenses() throws Exception {
		assertResource(new ResourceBuilder().storedLicenses(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().storedLicenses(storedLicensesList()).build(), "storedLicenses",
				licensedFeaturesList());
		assertEmbedded(new ResourceBuilder().storedLicenses(storedLicensesList(storedLicense("1"))).build(),
				"storedLicenses", storedLicensesList(storedLicense("1")));
		assertEmbedded(new ResourceBuilder().storedLicenses(storedLicensesList(storedLicense("1"), storedLicense("2")))
				.build(), "storedLicenses", storedLicensesList(storedLicense("1"), storedLicense("2")));
	}

	@Test
	public void testSettings() throws Exception {
		assertResource(new ResourceBuilder().settings(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().settings(settingsList()).build(), "settings", settingsList());
		assertEmbedded(new ResourceBuilder().settings(settingsList(setting(null, "1"))).build(), "settings",
				settingsList(setting(null, "1")));
		assertEmbedded(new ResourceBuilder().settings(settingsList(setting(null, "1"), setting(null, "2"))).build(),
				"settings", settingsList(setting(null, "1"), setting(null, "2")));
	}

	@Test
	public void testMetaData() throws Exception {
		assertResource(new ResourceBuilder().metaData(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().metaData(metaData()).build(), "metadata", new MetaData());
	}

	@Test
	public void testFilterValues() throws Exception {
		assertResource(new ResourceBuilder().filterValues(null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().filterValues(Collections.<String> emptyList()).build(), "filterValues",
				Collections.<String> emptyList());
		assertEmbedded(new ResourceBuilder().filterValues(Arrays.asList("1")).build(), "filterValues",
				Arrays.asList("1"));
		assertEmbedded(new ResourceBuilder().filterValues(Arrays.asList("1", "2")).build(), "filterValues",
				Arrays.asList("1", "2"));
	}

	@Test
	public void testEmbedded() throws Exception {
		assertResource(new ResourceBuilder().embedded(null, null).build(), 0, 0);
		assertResource(new ResourceBuilder().embedded("name", null).build(), 0, 0);
		assertEmbedded(new ResourceBuilder().embedded("name", "value").build(), "name", "value");
	}

	private void assertEmbedded(final Resource resource, final String name, final Object expectedObject) {
		assertResource(resource, 0, 1);
		assertEquals(expectedObject, HalResource.getEmbedded(resource, name));
	}
}
