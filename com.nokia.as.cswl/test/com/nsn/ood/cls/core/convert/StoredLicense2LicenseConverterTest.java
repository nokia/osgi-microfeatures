/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.target;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.targetsList;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.ArrayList;
import java.util.Date;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nokia.licensing.dtos.AddnColumns.LicenseMode;
import com.nokia.licensing.dtos.AddnColumns.LicenseType;
import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nsn.ood.cls.model.gen.licenses.Feature;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.License.Mode;
import com.nsn.ood.cls.model.gen.licenses.License.Type;
import com.nsn.ood.cls.model.gen.licenses.Target;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 *
 */
public class StoredLicense2LicenseConverterTest {
	private static final DateTime START_DATE = new DateTime(2014, 10, 06, 1, 12);
	private static final DateTime END_DATE = new DateTime(2014, 11, 26, 15, 39);

	private StoredLicense2LicenseConverter converter;
	
	private Converter<Date, DateTime> date2DateTimeConverter;
	private Converter<LicenseMode, License.Mode> storedLicenseMode2LicenseModeConverter;
	private Converter<LicenseType, License.Type> storedLicenseType2LicenseTypeConverter;
	private Converter<FeatureInfo, Feature> featureInfo2FeatureConverter;
	private Converter<TargetSystem, Target> targetSystem2TargetConverter;

	@Before
	public void setUp() throws Exception {
		this.date2DateTimeConverter = createMock(Date2DateTimeConverter.class);
		this.storedLicenseMode2LicenseModeConverter = createMock(StoredLicenseMode2LicenseModeConverter.class);
		this.storedLicenseType2LicenseTypeConverter = createMock(StoredLicenseType2LicenseTypeConverter.class);
		this.featureInfo2FeatureConverter = createMock(FeatureInfo2FeatureConverter.class);
		this.targetSystem2TargetConverter = createMock(TargetSystem2TargetConverter.class);
		this.converter = new StoredLicense2LicenseConverter();
		
		setInternalState(converter, "date2DateTimeConverter", date2DateTimeConverter);
		setInternalState(converter, "storedLicenseMode2LicenseModeConverter", storedLicenseMode2LicenseModeConverter);
		setInternalState(converter, "storedLicenseType2LicenseTypeConverter", storedLicenseType2LicenseTypeConverter);
		setInternalState(converter, "featureInfo2Feature", featureInfo2FeatureConverter);
		setInternalState(converter, "targetSystem2TargetConverter", targetSystem2TargetConverter);
	}

	@Test
	public void testConvertTo() throws Exception {
		final StoredLicense storedLicense = createStoredLicense(77L);

		expect(date2DateTimeConverter.convertTo(END_DATE.toDate())).andReturn(END_DATE);
		expect(storedLicenseMode2LicenseModeConverter.convertTo(LicenseMode.ONOFF)).andReturn(Mode.ON_OFF);
		expect(date2DateTimeConverter.convertTo(START_DATE.toDate())).andReturn(START_DATE);
		expect(storedLicenseType2LicenseTypeConverter.convertTo(LicenseType.FLOATING_POOL)).andReturn(Type.FLOATING_POOL);

		replayAll();
		final License license = this.converter.convertTo(storedLicense);
		verifyAll();

		assertEquals(license("serialNbr", 77L, 0L, Type.FLOATING_POOL, END_DATE)//
				.withCapacityUnit("capacityUnit")//
				.withCode("licenseCode")//
				.withFileName("licenseFileName")//
				.withMode(Mode.ON_OFF)//
				.withName("licenseName")//
				.withStartDate(START_DATE)//
				.withTargetType("targetNEType")//
				.withTargets(null)//
				.withFeatures(null), license);
	}

	@Test
	public void testConvertToWithNegativeMaxValue() throws Exception {
		final StoredLicense storedLicense = createStoredLicense(-1L);

		expect(date2DateTimeConverter.convertTo(END_DATE.toDate())).andReturn(END_DATE);
		expect(storedLicenseMode2LicenseModeConverter.convertTo(LicenseMode.ONOFF)).andReturn(Mode.ON_OFF);
		expect(date2DateTimeConverter.convertTo(START_DATE.toDate())).andReturn(START_DATE);
		expect(storedLicenseType2LicenseTypeConverter.convertTo(LicenseType.FLOATING_POOL)).andReturn(Type.FLOATING_POOL);

		replayAll();
		final License license = this.converter.convertTo(storedLicense);
		verifyAll();

		assertEquals(license("serialNbr", Long.MAX_VALUE, 0L, Type.FLOATING_POOL, END_DATE)//
				.withCapacityUnit("capacityUnit")//
				.withCode("licenseCode")//
				.withFileName("licenseFileName")//
				.withMode(Mode.ON_OFF)//
				.withName("licenseName")//
				.withStartDate(START_DATE)//
				.withTargetType("targetNEType")//
				.withTargets(null)//
				.withFeatures(null), license);
	}

	@Test
	public void testConvertToWithTargetsAndFeatures() throws Exception {
		final StoredLicense storedLicense = createStoredLicenseWithTargetsAndFeatures();

		expect(date2DateTimeConverter.convertTo(null)).andReturn(null).times(2);
		expect(storedLicenseMode2LicenseModeConverter.convertTo(null)).andReturn(null);
		expect(storedLicenseType2LicenseTypeConverter.convertTo(null)).andReturn(null);
		
		storedLicense.getTargetIds().add(new TargetSystem());
		storedLicense.getFeatureInfoList().add(new FeatureInfo());
		storedLicense.getTargetIds().forEach(t -> expect(targetSystem2TargetConverter.convertTo(t)).andReturn(target("targetId")));
		storedLicense.getFeatureInfoList().forEach(f -> expect(featureInfo2FeatureConverter.convertTo(f)).andReturn(feature(1234L, "name")));

		replayAll();
		final License license = this.converter.convertTo(storedLicense);
		verifyAll();

		assertEquals(license(0L, 0L, featuresList(feature(1234L, "name")), targetsList(target("targetId"))), license);
	}

	@Test
	public void testConvertToWithEmptyTargetsAndFeatures() throws Exception {
		final StoredLicense storedLicense = createStoredLicenseWithTargetsAndFeatures();

		expect(date2DateTimeConverter.convertTo(null)).andReturn(null).times(2);
		expect(storedLicenseMode2LicenseModeConverter.convertTo(null)).andReturn(null);
		expect(storedLicenseType2LicenseTypeConverter.convertTo(null)).andReturn(null);
		//expect(targetSystem2TargetConverter(storedLicense.getTargetIds(), Target.class)).andReturn(targetsList());
		//expect(this.converterMock.convertList(storedLicense.getFeatureInfoList(), Feature.class))
		//		.andReturn(featuresList());

		replayAll();
		final License license = this.converter.convertTo(storedLicense);
		verifyAll();

		assertEquals(license(0L, 0L, null, null), license);
	}

	@Test
	public void testConvertToEmpty() throws Exception {
		expect(date2DateTimeConverter.convertTo(null)).andReturn(null).times(2);
		expect(storedLicenseMode2LicenseModeConverter.convertTo(null)).andReturn(null);
		expect(storedLicenseType2LicenseTypeConverter.convertTo(null)).andReturn(null);

		replayAll();
		assertEquals(license(0L, 0L, null, null), this.converter.convertTo(new StoredLicense()));
		verifyAll();
	}

	@Test
	public void testConvertToNull() throws Exception {
		try {
			this.converter.convertTo(null);
			fail();
		} catch (final CLSIllegalArgumentException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	@Test
	public void testConvertFrom() throws Exception {
		try {
			this.converter.convertFrom(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}

	private StoredLicense createStoredLicense(final long maxValue) {
		final StoredLicense storedLicense = new StoredLicense();
		storedLicense.setCapacityUnit("capacityUnit");
		storedLicense.setEndTime(END_DATE.toDate());
		storedLicense.setLicenseCode("licenseCode");
		storedLicense.setLicenseFileName("licenseFileName");
		storedLicense.setLicenseMode(LicenseMode.ONOFF);
		storedLicense.setLicenseName("licenseName");
		storedLicense.setLicenseType(LicenseType.FLOATING_POOL);
		storedLicense.setMaxValue(maxValue);
		storedLicense.setSerialNbr("serialNbr");
		storedLicense.setStartTime(START_DATE.toDate());
		storedLicense.setTargetNEType("targetNEType");
		// unsupported fields
		storedLicense.setAdditionalInfo("additionalInfo");
		storedLicense.setCustomerId("customerId");
		storedLicense.setCustomerName("customerName");
		storedLicense.setIsValid("isValid");
		storedLicense.setKey(new byte[] {
				1 });
		storedLicense.setLicenseFileImportTime(new Date());
		storedLicense.setLicenseFileImportUser("licenseFileImportUser");
		storedLicense.setLicenseFilePath("licenseFilePath");
		storedLicense.setOrderId("orderId");
		storedLicense.setOriginOMC("originOMC");
		storedLicense.setPool("pool");
		storedLicense.setStoredLicenseSignature(new byte[] {
				2 });
		storedLicense.setSwReleaseBase("swReleaseBase");
		storedLicense.setSwReleaseRelation("swReleaseRelation");
		storedLicense.setUsageType("usageType");
		return storedLicense;
	}

	private StoredLicense createStoredLicenseWithTargetsAndFeatures() {
		final StoredLicense storedLicense = new StoredLicense();
		storedLicense.setTargetIds(new ArrayList<TargetSystem>());
		storedLicense.setFeatureInfoList(new ArrayList<FeatureInfo>());
		return storedLicense;
	}
}
