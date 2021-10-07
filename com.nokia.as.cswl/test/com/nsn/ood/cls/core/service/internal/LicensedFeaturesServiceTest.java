/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeature;
import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeaturesList;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.operation.LicensedFeatureRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.model.metadata.MetaDataList;


/**
 * @author marynows
 * 
 */
public class LicensedFeaturesServiceTest {
	private LicensedFeatureRetrieveOperation licensedFeatureRetrieveOperationMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private LicensedFeaturesService service;

	@Before
	public void setUp() throws Exception {
		this.licensedFeatureRetrieveOperationMock = createMock(LicensedFeatureRetrieveOperation.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);

		this.service = new LicensedFeaturesService();
		setInternalState(this.service, this.licensedFeatureRetrieveOperationMock, this.serviceExceptionFactoryMock);
	}

	@Test
	public void testGetLicensedFeatures() throws Exception {
		final MetaDataList<LicensedFeature> licenseFeatures = new MetaDataList<>(licensedFeaturesList(
				licensedFeature(1L), licensedFeature(2L)), metaData());
		final Conditions conditionsMock = createMock(Conditions.class);

		expect(this.licensedFeatureRetrieveOperationMock.getList(conditionsMock)).andReturn(licenseFeatures);

		replayAll();
		assertEquals(licenseFeatures, this.service.getLicensedFeatures(conditionsMock));
		verifyAll();
	}

	@Test
	public void testGetLicensedFeaturesWithRetrieveException() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licensedFeatureRetrieveOperationMock.getList(conditionsMock)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("message"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("message"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getLicensedFeatures(conditionsMock);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetLicensedFeatureFilterValues() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();

		expect(this.licensedFeatureRetrieveOperationMock.getFilterValues("filterName", conditions)).andReturn(
				Arrays.asList("1", "2"));

		replayAll();
		assertEquals(Arrays.asList("1", "2"), this.service.getLicensedFeatureFilterValues("filterName"));
		verifyAll();
	}

	@Test
	public void testGetLicensedFeatureFilterValuesWithException() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licensedFeatureRetrieveOperationMock.getFilterValues("filterName", conditions)).andThrow(
				exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getLicensedFeatureFilterValues("filterName");
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

}
