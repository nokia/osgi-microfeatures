/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.storedLicense;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.target;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.targetsList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.LicenseMode2IntegerConverter;
import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.Target;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class LicenseCreatorTest {
	private static final DateTime END_DATE = new DateTime(2014, 11, 26, 13, 48, 15);
	private static final Timestamp END_DATE_TS = new Timestamp(END_DATE.getMillis());
	private static final DateTime START_DATE = new DateTime(2014, 1, 2, 3, 8, 5);
	private static final Timestamp START_DATE_TS = new Timestamp(START_DATE.getMillis());
	private static final DateTime IMPORT_DATE = new DateTime(2014, 6, 5, 4, 3, 2);
	private static final Timestamp IMPORT_DATE_TS = new Timestamp(IMPORT_DATE.getMillis());

	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
	private Converter<License.Mode, Integer> licenseMode2IntegerConverter = createMock(LicenseMode2IntegerConverter.class);
	private Converter<License.Type, Integer> licenseType2IntegerConverter = createMock(LicenseType2IntegerConverter.class);
	private LicenseCreator creator;

	@Before
	public void setUp() throws Exception {
		this.creator = new LicenseCreator();
		setInternalState(creator, "timestamp2DateTimeConverter", timestamp2DateTimeConverter);
		setInternalState(creator, "licenseMode2IntegerConverter", licenseMode2IntegerConverter);
		setInternalState(creator, "licenseType2IntegerConverter", licenseType2IntegerConverter);
	}

	@Test
	public void testCreateLicense() throws Exception {
		testCreateLicense(null, targetsList());
		testCreateLicense("targetId", targetsList(target("targetId")));
		testCreateLicense("t1,t2", targetsList(target("t1"), target("t2")));
		testCreateLicense("t1,,t2", targetsList(target("t1"), target("t2")));
	}

	private void testCreateLicense(final String dbTargetId, final List<Target> targets) throws Exception {
		resetAll();

		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("serialnumber")).andReturn("serialnumber");
		expect(resultSetMock.getString("capacityunit")).andReturn("capacityunit");
		expect(resultSetMock.getString("code")).andReturn("code");
		expect(resultSetMock.getTimestamp("enddate")).andReturn(END_DATE_TS);
		expect(this.timestamp2DateTimeConverter.convertTo(END_DATE_TS)).andReturn(END_DATE);
		expect(resultSetMock.getString("filename")).andReturn("filename");
		expect(resultSetMock.getInt("mode")).andReturn(1);
		expect(this.licenseMode2IntegerConverter.convertFrom(1)).andReturn(License.Mode.CAPACITY);
		expect(resultSetMock.getString("name")).andReturn("name");
		expect(resultSetMock.getTimestamp("startdate")).andReturn(START_DATE_TS);
		expect(this.timestamp2DateTimeConverter.convertTo(START_DATE_TS)).andReturn(START_DATE);
		expect(resultSetMock.getString("targettype")).andReturn("targettype");
		expect(resultSetMock.getLong("total")).andReturn(1L);
		expect(resultSetMock.getInt("type")).andReturn(2);
		expect(this.licenseType2IntegerConverter.convertFrom(2)).andReturn(License.Type.POOL);
		expect(resultSetMock.getLong("used")).andReturn(23L);

		expect(resultSetMock.getLong("featurecode")).andReturn(1234L);
		expect(resultSetMock.getString("featurename")).andReturn("featurename");

		expect(resultSetMock.getString("targetid")).andReturn(dbTargetId);

		replayAll();
		final License license = this.creator.createLicense(resultSetMock);
		verifyAll();

		assertEquals(
				license("serialnumber", License.Type.POOL, License.Mode.CAPACITY, START_DATE, END_DATE, 1L, 23L,
						"capacityunit", "code", "name", "filename", "targettype").withFeatures(
						featuresList(feature(1234L, "featurename"))).withTargets(targets), license);
	}

	@Test
	public void testCreateStoredLicense() throws Exception {
		resetAll();

		final ResultSet resultSetMock = createMock(ResultSet.class);
		expect(resultSetMock.getString("customername")).andReturn("customerName");
		expect(resultSetMock.getString("customerid")).andReturn("customerId");
		expect(resultSetMock.getString("orderid")).andReturn("orderId");
		expect(resultSetMock.getString("user")).andReturn("user");
		expect(resultSetMock.getTimestamp("importdate")).andReturn(IMPORT_DATE_TS);
		expect(this.timestamp2DateTimeConverter.convertTo(IMPORT_DATE_TS)).andReturn(IMPORT_DATE);
		expect(resultSetMock.getLong("remaining")).andReturn(43L);

		expect(resultSetMock.getString("serialnumber")).andReturn("serialnumber");
		expect(resultSetMock.getString("capacityunit")).andReturn("capacityunit");
		expect(resultSetMock.getString("code")).andReturn("code");
		expect(resultSetMock.getTimestamp("enddate")).andReturn(END_DATE_TS);
		expect(this.timestamp2DateTimeConverter.convertTo(END_DATE_TS)).andReturn(END_DATE);
		expect(resultSetMock.getString("filename")).andReturn("filename");
		expect(resultSetMock.getInt("mode")).andReturn(1);
		expect(this.licenseMode2IntegerConverter.convertFrom(1)).andReturn(License.Mode.CAPACITY);
		expect(resultSetMock.getString("name")).andReturn("name");
		expect(resultSetMock.getTimestamp("startdate")).andReturn(START_DATE_TS);
		expect(this.timestamp2DateTimeConverter.convertTo(START_DATE_TS)).andReturn(START_DATE);
		expect(resultSetMock.getString("targettype")).andReturn("targettype");
		expect(resultSetMock.getLong("total")).andReturn(66L);
		expect(resultSetMock.getInt("type")).andReturn(2);
		expect(this.licenseType2IntegerConverter.convertFrom(2)).andReturn(License.Type.POOL);
		expect(resultSetMock.getLong("used")).andReturn(23L);

		expect(resultSetMock.getLong("featurecode")).andReturn(1234L);
		expect(resultSetMock.getString("featurename")).andReturn("featurename");

		expect(resultSetMock.getString("targetid")).andReturn("targetid");

		replayAll();
		final StoredLicense license = this.creator.createStoredLicense(resultSetMock);
		verifyAll();

		assertEquals(
				storedLicense("customerName", "customerId", "orderId", "user", IMPORT_DATE, 43L, "serialnumber",
						License.Type.POOL, License.Mode.CAPACITY, START_DATE, END_DATE, 66L, 23L, "capacityunit",
						"code", "name", "filename", "targettype").withFeatures(
						featuresList(feature(1234L, "featurename"))).withTargets(targetsList(target("targetid"))),
				license);
	}
}
