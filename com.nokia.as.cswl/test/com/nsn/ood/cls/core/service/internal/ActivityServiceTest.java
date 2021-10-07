/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorException;
import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorExceptionsList;
import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetail;
import static com.nsn.ood.cls.model.internal.test.ActivityDetailTestUtil.activityDetailsList;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activitiesList;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.activity;
import static com.nsn.ood.cls.model.internal.test.ActivityTestUtil.assertActivity;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.settingsList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.List;

import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.core.convert.ErrorException2ActivityDetailConverter;
import com.nsn.ood.cls.core.convert.License2ActivityDetailConverter;
import com.nsn.ood.cls.core.convert.Setting2ActivityDetailConverter;
import com.nsn.ood.cls.core.operation.ActivityCreateOperation;
import com.nsn.ood.cls.core.operation.ActivityDetailRetrieveOperation;
import com.nsn.ood.cls.core.operation.ActivityRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.Activity.OperationType;
import com.nsn.ood.cls.model.internal.Activity.Result;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.convert.Converter;

/**
 * @author marynows
 * 
 */
public class ActivityServiceTest {
	private static final long ACTIVITY_ID = 77L;
	private static final ActivityDetail ACTIVITY_DETAIL1 = activityDetail("1");
	private static final ActivityDetail ACTIVITY_DETAIL2 = activityDetail("2");

	private ActivityRetrieveOperation activityRetrieveOperationMock;
	private ActivityDetailRetrieveOperation activityDetailRetrieveOperationMock;
	private ActivityCreateOperation activityCreateOperationMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private Converter<ErrorException, ActivityDetail> errorException2ActivityDetailConverter;
	private Converter<License, ActivityDetail> license2ActivityDetailConverter;
	private Converter<Setting, ActivityDetail> setting2ActivityDetailConverter;
	private ActivityService service;

	@Before
	public void setUp() throws Exception {
		this.activityRetrieveOperationMock = createMock(ActivityRetrieveOperation.class);
		this.activityDetailRetrieveOperationMock = createMock(ActivityDetailRetrieveOperation.class);
		this.activityCreateOperationMock = createMock(ActivityCreateOperation.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);
		this.errorException2ActivityDetailConverter = createMock(ErrorException2ActivityDetailConverter.class);
		this.license2ActivityDetailConverter = createMock(License2ActivityDetailConverter.class);
		this.setting2ActivityDetailConverter = createMock(Setting2ActivityDetailConverter.class);

		this.service = new ActivityService();
		setInternalState(this.service, this.activityRetrieveOperationMock, this.activityDetailRetrieveOperationMock,
				this.activityCreateOperationMock, this.serviceExceptionFactoryMock);
		setInternalState(this.service, "errorException2ActivityDetailConverter", errorException2ActivityDetailConverter);
		setInternalState(this.service, "license2ActivityDetailConverter", license2ActivityDetailConverter);
		setInternalState(this.service, "setting2ActivityDetailConverter", setting2ActivityDetailConverter);
	}

	@Test
	public void testGetActivities() throws Exception {
		final MetaDataList<Activity> activities = new MetaDataList<>(activitiesList(activity(1L), activity(2L)),
				metaData());
		final Conditions conditionsMock = createMock(Conditions.class);

		expect(this.activityRetrieveOperationMock.getList(conditionsMock)).andReturn(activities);

		replayAll();
		assertEquals(activities, this.service.getActivities(conditionsMock));
		verifyAll();
	}

	@Test
	public void testGetActivitiesWithRetrieveException() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.activityRetrieveOperationMock.getList(conditionsMock)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("message"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("message"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getActivities(conditionsMock);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetActivityDetails() throws Exception {
		final MetaDataList<ActivityDetail> activityDetails = new MetaDataList<>(activityDetailsList(ACTIVITY_DETAIL1,
				ACTIVITY_DETAIL2), metaData());
		final Conditions conditionsMock = createMock(Conditions.class);
		final Capture<Filter> capturedFilter = new Capture<>();

		conditionsMock.addFilter(capture(capturedFilter));
		expect(this.activityDetailRetrieveOperationMock.getList(conditionsMock)).andReturn(activityDetails);

		replayAll();
		assertEquals(activityDetails, this.service.getActivityDetails(ACTIVITY_ID, conditionsMock));
		verifyAll();

		assertFilter(capturedFilter.getValue(), "activityId", String.valueOf(ACTIVITY_ID));
	}

	@Test
	public void testGetActivityDetailsWithRetrieveException() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);
		final Capture<Filter> capturedFilter = new Capture<>();

		conditionsMock.addFilter(capture(capturedFilter));
		expect(this.activityDetailRetrieveOperationMock.getList(conditionsMock)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("message"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("message"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getActivityDetails(ACTIVITY_ID, conditionsMock);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();

		assertFilter(capturedFilter.getValue(), "activityId", String.valueOf(ACTIVITY_ID));
	}

	private void assertFilter(final Filter filter, final String expectedName, final String expectedValue) {
		assertEquals(expectedName, filter.name());
		assertEquals(expectedValue, filter.value());
		assertEquals(Type.EQUAL, filter.type());
	}

	@Test
	public void testGetActivityFilterValues() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();

		expect(this.activityRetrieveOperationMock.getFilterValues("filterName", conditions)).andReturn(
				Arrays.asList("1", "2"));

		replayAll();
		assertEquals(Arrays.asList("1", "2"), this.service.getActivityFilterValues("filterName"));
		verifyAll();
	}

	@Test
	public void testGetActivityFilterValuesWithException() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.activityRetrieveOperationMock.getFilterValues("filterName", conditions)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getActivityFilterValues("filterName");
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetActivityDetailFilterValues() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().equalFilter("activityId", "2").build();

		expect(this.activityDetailRetrieveOperationMock.getFilterValues("filterName", conditions)).andReturn(
				Arrays.asList("1", "2"));

		replayAll();
		assertEquals(Arrays.asList("1", "2"), this.service.getActivityDetailFilterValues(2L, "filterName"));
		verifyAll();
	}

	@Test
	public void testGetActivityDetailFilterValuesWithException() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().equalFilter("activityId", "2").build();
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.activityDetailRetrieveOperationMock.getFilterValues("filterName", conditions)).andThrow(
				exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m"))).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.getActivityDetailFilterValues(2L, "filterName");
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testAddLicenseInstallActivityWithCreateException() throws Exception {
		final CreateException exceptionMock = createMock(CreateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		List<License> l = licensesList(license("1"), license("2"));
		List<ErrorException> ex = errorExceptionsList();
		expect(this.license2ActivityDetailConverter.convertTo(license("1"))).andReturn(ACTIVITY_DETAIL1);
		expect(this.license2ActivityDetailConverter.convertTo(license("2"))).andReturn(ACTIVITY_DETAIL2);
		final Capture<Activity> capturedActivity = mockAddActivity(exceptionMock, ACTIVITY_DETAIL1, ACTIVITY_DETAIL2);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.ACTIVITY_CREATION_FAIL, exceptionMock)).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.addLicenseInstallActivity(l, ex);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.LICENSE_INSTALL, Result.SUCCESS, null);
	}

	@Test
	public void testAddLicenseCancelActivitySuccess() throws Exception {
		List<License> l = licensesList(license("1"), license("2"));
		List<ErrorException> ex = errorExceptionsList();
		expect(this.license2ActivityDetailConverter.convertTo(license("1"))).andReturn(ACTIVITY_DETAIL1);
		expect(this.license2ActivityDetailConverter.convertTo(license("2"))).andReturn(ACTIVITY_DETAIL2);
		
		final Capture<Activity> capturedActivity = mockAddActivity(null, ACTIVITY_DETAIL1, ACTIVITY_DETAIL2);

		replayAll();
		this.service.addLicenseCancelActivity(l, ex);
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.LICENSE_CANCEL, Result.SUCCESS, null);
	}
	
	@Test
	public void testAddLicenseCancelActivityFailure() throws Exception {
		List<License> l = licensesList();
		List<ErrorException> ex = errorExceptionsList(errorException());
		ex.forEach(e -> expect(this.errorException2ActivityDetailConverter.convertTo(e)).andReturn(ACTIVITY_DETAIL1));
		
		final Capture<Activity> capturedActivity = mockAddActivity(null, ACTIVITY_DETAIL1);

		replayAll();
		this.service.addLicenseCancelActivity(l, ex);
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.LICENSE_CANCEL, Result.FAILURE, null);
	}
	
	@Test
	public void testAddLicenseCancelActivityPartial() throws Exception {
		List<License> l = licensesList(license());
		List<ErrorException> ex = errorExceptionsList(errorException());
		mockCreateActivityDetails(l, ex);
		
		final Capture<Activity> capturedActivity = mockAddActivity(null, ACTIVITY_DETAIL1, ACTIVITY_DETAIL2);

		replayAll();
		this.service.addLicenseCancelActivity(l, ex);
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.LICENSE_CANCEL, Result.PARTIAL, null);
	}

	@Test
	public void testAddLicenseCancelActivityWithCreateException() throws Exception {
		final CreateException exceptionMock = createMock(CreateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		List<License> l = licensesList(license("1"), license("2"));
		List<ErrorException> ex = errorExceptionsList();
		expect(this.license2ActivityDetailConverter.convertTo(license("1"))).andReturn(ACTIVITY_DETAIL1);
		expect(this.license2ActivityDetailConverter.convertTo(license("2"))).andReturn(ACTIVITY_DETAIL2);
		final Capture<Activity> capturedActivity = mockAddActivity(exceptionMock, ACTIVITY_DETAIL1, ACTIVITY_DETAIL2);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.ACTIVITY_CREATION_FAIL, exceptionMock)).andReturn(
				serviceExceptionMock);

		replayAll();
		try {
			this.service.addLicenseCancelActivity(l, ex);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.LICENSE_CANCEL, Result.SUCCESS, null);
	}

	private void mockCreateActivityDetails(final List<License> licenses, final List<ErrorException> errors) {
		licenses.forEach(l -> expect(this.license2ActivityDetailConverter.convertTo(l)).andReturn(ACTIVITY_DETAIL1));
		errors.forEach(e -> expect(this.errorException2ActivityDetailConverter.convertTo(e)).andReturn(ACTIVITY_DETAIL2));
	}

	@Test
	public void testAddSettingUpdateActivityWithSuccesState() throws Exception {
		mockCreateActivityDetails2(settingsList(), errorExceptionsList());
		final Capture<Activity> capturedActivity = mockAddActivity(null, ACTIVITY_DETAIL1);
		
		List<Setting> s = settingsList(setting());
		expect(this.setting2ActivityDetailConverter.convertTo(setting())).andReturn(ACTIVITY_DETAIL1);

		replayAll();
		this.service.addSettingUpdateActivity(s, errorExceptionsList());
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.SETTING_UPDATE, Result.SUCCESS, null);
	}

	@Test
	public void testAddSettingUpdateActivityWithFailureState() throws Exception {
		final List<ErrorException> errors = errorExceptionsList(errorException());

		mockCreateActivityDetails2(settingsList(), errors);
		final Capture<Activity> capturedActivity = mockAddActivity(null, ACTIVITY_DETAIL2);

		replayAll();
		this.service.addSettingUpdateActivity(settingsList(), errors);
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.SETTING_UPDATE, Result.FAILURE, null);
	}

	@Test
	public void testAddSettingUpdateActivityWithCreateException() throws Exception {
		final CreateException exceptionMock = createMock(CreateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);
		
		List<Setting> s = settingsList(setting());
		expect(this.setting2ActivityDetailConverter.convertTo(setting())).andReturn(ACTIVITY_DETAIL1);

		final Capture<Activity> capturedActivity = mockAddActivity(exceptionMock, ACTIVITY_DETAIL1);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.ACTIVITY_CREATION_FAIL, exceptionMock)).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.addSettingUpdateActivity(s, errorExceptionsList());
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();

		assertActivity(capturedActivity.getValue(), null, null, OperationType.SETTING_UPDATE, Result.SUCCESS, null);
	}

	private void mockCreateActivityDetails2(final List<Setting> settings, final List<ErrorException> errors) {
		if (errors.isEmpty()) {
			settings.forEach(s -> expect(this.setting2ActivityDetailConverter.convertTo(s)).andReturn(ACTIVITY_DETAIL1));
		} else {
			errors.forEach(e -> expect(this.errorException2ActivityDetailConverter.convertTo(e)).andReturn(ACTIVITY_DETAIL2));
		}
	}

	private Capture<Activity> mockAddActivity(final CreateException exceptionMock,
			final ActivityDetail... activityDetails) throws Exception {
		final Capture<Activity> capturedActivity = new Capture<>();
		this.activityCreateOperationMock.createActivity(capture(capturedActivity),
				eq(activityDetailsList(activityDetails)));
		if (exceptionMock != null) {
			expectLastCall().andThrow(exceptionMock);
		}
		return capturedActivity;
	}
	
	@After
	public void tearDown() {
		PowerMock.resetAll();
	}
}
