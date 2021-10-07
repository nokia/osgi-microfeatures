/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorException;
import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorExceptionsList;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.bigString;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.settingsList;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.errorsList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
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

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.convert.Setting2StringConverter;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ActivityService;
import com.nsn.ood.cls.core.service.internal.ConfigurationService;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.SettingKey;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.convert.ErrorException2StringConverter;
import com.nsn.ood.cls.rest.exception.ViolationException;
import com.nsn.ood.cls.rest.resource.AbstractResourceTest;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class ConfigurationResourceTest extends AbstractResourceTest {
	private ConfigurationResource resource;
	private ConfigurationService configurationServiceMock;
	private ActivityService activityServiceMock;
	private BaseResource base;
	private Converter<Setting, String> setting2StringConverter;

	@Before
	public void setUp() throws Exception {
		this.configurationServiceMock = createMock(ConfigurationService.class);
		this.activityServiceMock = createMock(ActivityService.class);
		this.base = createMock(BaseResource.class);
		init(base);

		this.resource = new ConfigurationResource();
		this.setting2StringConverter = createMock(Setting2StringConverter.class);
		setInternalState(this.resource, this.configurationServiceMock, this.activityServiceMock, base);
		setInternalState(this.resource, "uriInfo2ConditionsConverter", uriInfo2ConditionsConverter);
		setInternalState(this.resource, "errorException2ErrorConverter", errorException2ErrorConverter);
		setInternalState(this.resource, "setting2StringConverter", setting2StringConverter);
		this.base.init(EasyMock.anyObject(), EasyMock.eq("configurations"));
	}

	@Test
	public void testGetSettings() throws Exception {
		final Resource resourceMock = createMock(Resource.class);
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final Capture<Links> capturedLinks = new Capture<>();
		final List<Setting> settings = settingsList(setting());
		final MetaData metaData = metaData(20L, 10L);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.configurationServiceMock.getSettings(conditions)).andReturn(new MetaDataList<>(settings, metaData));
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.metaData(capture(capturedLinks), eq(metaData))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.settings(settings)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.links(conditions, metaData.getFiltered())).andReturn(new Links(new Link().withHref(("/configurations?query")), null, null));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getSettings(uriInfoMock));
		verifyAll();

		assertLinks(capturedLinks.getValue(), "/configurations?query");
	}

	@Test
	public void testGetSettingsAndExpectError() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final ServiceException exceptionMock = createMock(ServiceException.class);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.configurationServiceMock.getSettings(conditions)).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getSettings(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testGetSetting() throws Exception {
		final Resource resourceMock = createMock(Resource.class);
		final Capture<Conditions> capturedConditions = new Capture<>();
		final Capture<Link> capturedLink = new Capture<>();
		final List<Setting> settings = settingsList(setting(SettingKey.FLOATING_RELEASE_TIME, 100L));

		expect(this.configurationServiceMock.getSettings(capture(capturedConditions))).andReturn(
				new MetaDataList<>(settings, metaData()));
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.settings(settings)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("floatingReleaseTime")).andReturn(new Link().withHref("/configurations/floatingReleaseTime"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getSetting("floatingReleaseTime"));
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("key", "floatingReleaseTime").build(),
				capturedConditions.getValue());
		assertLink(capturedLink.getValue(), "/configurations/floatingReleaseTime");
	}

	@Test
	public void testGetSettingAndExpectError() throws Exception {
		final Capture<Conditions> capturedConditions = new Capture<>();
		final ServiceException exceptionMock = createMock(ServiceException.class);

		expect(this.configurationServiceMock.getSettings(capture(capturedConditions))).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getSetting("floatingReleaseTime"));
		verifyAll();

		assertEquals(ConditionsBuilder.createAndSkipMetaData().equalFilter("key", "floatingReleaseTime").build(),
				capturedConditions.getValue());
	}

	@Test
	public void testGetSettingAndExpectException() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.valueException("configurations.unknownSetting", "test"))
				.andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.getSetting("test");
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testSetSettings() throws Exception {
		final List<Setting> settings = settingsList(setting(SettingKey.EMAIL_SUBJECT, "subject"));

		this.base.logInit(EasyMock.eq("Update settings"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Update settings"));
		this.base.logSuccess(EasyMock.eq("Add activity"));
		settings.forEach(s -> expect(this.setting2StringConverter.convertTo(s)).andReturn("S"));
		this.configurationServiceMock.setSettings(settings);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.noContent()).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		this.activityServiceMock.addSettingUpdateActivity(settings, Collections.<ErrorException> emptyList());
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.setSettings(settings));
		verifyAll();
	}

	@Test
	public void testSetSettingsAndExpectException() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(this.violationExceptionBuilderFactoryMock.exception("configurations.oneOrMoreSettings")).andReturn(
				exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.setSettings(settingsList());
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testSetSetting() throws Exception {
		final List<Setting> settings = settingsList(setting(SettingKey.FLOATING_RELEASE_TIME, 33L));

		this.base.logInit(EasyMock.eq("Update settings"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Update settings"));
		this.base.logSuccess(EasyMock.eq("Add activity"));
		settings.forEach(s -> expect(this.setting2StringConverter.convertTo(s)).andReturn("S"));
		this.configurationServiceMock.setSettings(settings);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.noContent()).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		this.activityServiceMock.addSettingUpdateActivity(settings, Collections.<ErrorException> emptyList());
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.setSetting("floatingReleaseTime", setting(null, 33L)));
		verifyAll();
	}

	@Test
	public void testSetSettingAndExpectErrorDuringSettingSetting() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		ErrorException ee = errorException();
		final List<ErrorException> errorExceptions = errorExceptionsList(ee);
		final List<Setting> settings = settingsList(setting(SettingKey.FLOATING_RELEASE_TIME, 33L));

		this.base.logInit(EasyMock.eq("Update settings"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));
		settings.forEach(s -> expect(this.setting2StringConverter.convertTo(s)).andReturn("S"));
		this.configurationServiceMock.setSettings(settings);
		expectLastCall().andThrow(exceptionMock);
		Converter<ErrorException, String> converter = createMock(ErrorException2StringConverter.class);
		errorExceptionsList().forEach(e -> expect(converter.convertTo(e)).andReturn("error"));
		this.base.logFailure("Update settings", exceptionMock);
		this.base.logSuccess("Add activity");
		expect(exceptionMock.getExceptions()).andReturn(errorExceptions);
		this.activityServiceMock.addSettingUpdateActivity(settings, errorExceptions);

		final Response responseMock = createMock(Response.class);
		errorExceptionsList(ee).forEach(e -> expect(errorException2ErrorConverter.convertTo(e)).andReturn(error(2L)));
		expect(this.base.getRestUtil()).andReturn(this.restUtilMock);
		expect(this.restUtilMock.errorResponse(Status.BAD_REQUEST, errorsList(error(2L)))).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.setSetting("floatingReleaseTime", setting(null, 33L)));
		verifyAll();
	}

	@Test
	public void testSetSettingAndExpectErrorDuringActivityUpdate() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final List<Setting> settings = settingsList(setting(SettingKey.FLOATING_RELEASE_TIME, 33L));

		this.base.logInit(EasyMock.eq("Update settings"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Update settings"));
		this.base.logFailure("Add activity", exceptionMock);
		settings.forEach(s -> expect(this.setting2StringConverter.convertTo(s)).andReturn("S"));
		this.configurationServiceMock.setSettings(settings);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.noContent()).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		this.activityServiceMock.addSettingUpdateActivity(settings, Collections.<ErrorException> emptyList());
		expectLastCall().andThrow(exceptionMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.setSetting("floatingReleaseTime", setting(null, 33L)));
		verifyAll();
	}

	@Test
	public void testSetSettingAndExpectExceptionDuringKeyValidation() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(
				this.violationExceptionBuilderFactoryMock.settingKeyException("configurations.unexpectedKey",
						SettingKey.FLOATING_RELEASE_TIME)).andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.setSetting("floatingReleaseTime", setting(SettingKey.FLOATING_RELEASE_TIME, null));
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testSetSettingAndExpectExceptionDuringValueValidation() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(
				this.violationExceptionBuilderFactoryMock.settingValueException("configurations.invalidValue", null,
						"test")).andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.setSetting("floatingReleaseTime", setting(null, "test"));
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testSetSettingAndExpectExceptionWhenValueIsTooLong() throws Exception {
		final ViolationException exceptionMock = createMock(ViolationException.class);
		final String value = bigString(Setting.VALUE_MAX_LENGTH + 1);
		expect(base.getViolationFactory()).andReturn(violationExceptionBuilderFactoryMock);
		expect(
				this.violationExceptionBuilderFactoryMock.settingValueException("configurations.valueTooLong", null,
						value)).andReturn(exceptionMock);

		replayAll();
		try {
			this.resource.start();
			this.resource.setSetting("floatingReleaseTime", setting(null, value));
			fail();
		} catch (final ViolationException e) {
			assertEquals(exceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetTargetId() throws Exception {
		final Resource resourceMock = createMock(Resource.class);
		final Capture<Link> capturedLink = new Capture<>();

		expect(this.configurationServiceMock.getTargetId()).andReturn("tttt");
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.embedded("targetId", "tttt")).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("targetId")).andReturn(new Link().withHref("/configurations/targetId"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getTargetId());
		verifyAll();

		assertLink(capturedLink.getValue(), "/configurations/targetId");
	}
}
