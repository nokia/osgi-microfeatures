/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.MessagingException;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.config.Configuration;
import com.nsn.ood.cls.core.convert.License2StringConverter;
import com.nsn.ood.cls.core.convert.LicensedFeature2StringConverter;
import com.nsn.ood.cls.core.mail.MailContentGenerator;
import com.nsn.ood.cls.core.mail.MailSender;
import com.nsn.ood.cls.core.operation.EmailSendOperation.SendException;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.util.Messages;
import com.nsn.ood.cls.model.CLSConst;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		EmailSendOperation.class, DateTime.class, Messages.class })
public class EmailSendOperationTest {
	private Configuration configurationMock;
	private MailSender mailSenderMock;
	private MailContentGenerator mailContentGeneratorMock;
	private LicenseRetrieveOperation licenseRetrieveOperationMock;
	private Converter<License, String> converterMock;
	private Converter<LicensedFeature, String> licensedFeatureConverterMock;
	private EmailSendOperation operation;
	private FeatureCapacityCheckOperation featureCheckMock;
	private String subject;

	@Before
	public void setUp() throws Exception {
		this.configurationMock = createMock(Configuration.class);
		this.mailSenderMock = createMock(MailSender.class);
		this.mailContentGeneratorMock = createMock(MailContentGenerator.class);
		this.licenseRetrieveOperationMock = createMock(LicenseRetrieveOperation.class);
		this.converterMock = createMock(License2StringConverter.class);
		this.licensedFeatureConverterMock = createMock(LicensedFeature2StringConverter.class);
		this.featureCheckMock = createMock(FeatureCapacityCheckOperation.class);
		this.subject = "Subjectó";

		this.operation = new EmailSendOperation();
		setInternalState(this.operation, this.configurationMock, this.mailSenderMock, this.mailContentGeneratorMock,
				this.licenseRetrieveOperationMock, this.featureCheckMock);
		setInternalState(this.operation, "licensedFeature2StringConverter", licensedFeatureConverterMock);
		setInternalState(this.operation, "license2StringConverter", converterMock);
		mockStatic(Messages.class);
	}

	@Test
	public void testCapacityWarningLicensesEmailDisabledEmails() throws Exception {
		expect(this.configurationMock.isEmailNotificationsEnabled()).andReturn(false);

		replayAll();
		this.operation.sendCapacityThresholdEmail();
		verifyAll();
	}

	@Test
	public void testCapacityWarningLicensesEmailNoWarnings() throws Exception {
		final long capacityThreshold = 665;
		final List<LicensedFeature> emptyList = new ArrayList<>();
		final DateTime now = new DateTime();

		expect(this.configurationMock.isEmailNotificationsEnabled()).andReturn(true);
		expect(this.configurationMock.getCapacityThreshold()).andReturn(capacityThreshold);

		mockStatic(DateTime.class);
		expect(DateTime.now()).andReturn(now);

		expect(this.featureCheckMock.retrieve(capacityThreshold, now)).andReturn(emptyList);

		replayAll();
		this.operation.sendCapacityThresholdEmail();
		verifyAll();
	}

	@Test
	public void testCapacityWarningLicensesEmail() throws Exception {
		final String messageContent = "óął";
		final long capacityThreshold = 665;

		final List<LicensedFeature> nonEmptyList = new ArrayList<>();
		final LicensedFeature mock1 = createMock(LicensedFeature.class);
		nonEmptyList.add(mock1);

		final DateTime now = new DateTime();

		expect(this.configurationMock.isEmailNotificationsEnabled()).andReturn(true);
		expect(this.configurationMock.getCapacityThreshold()).andReturn(capacityThreshold);

		mockStatic(DateTime.class);
		expect(DateTime.now()).andReturn(now);

		expect(this.featureCheckMock.retrieve(capacityThreshold, now)).andReturn(nonEmptyList);
		expect(this.mailContentGeneratorMock.capacityThreshold(nonEmptyList)).andReturn(messageContent);
		nonEmptyList.forEach(l -> expect(this.licensedFeatureConverterMock.convertTo(l)).andReturn(""));
		expect(Messages.getSubject("capacityThresholdSubject")).andReturn(this.subject);
		this.mailSenderMock.send(messageContent, this.subject);
		this.featureCheckMock.update(nonEmptyList, now);

		replayAll();
		this.operation.sendCapacityThresholdEmail();
		verifyAll();
	}

	@Test
	public void testSendExpiringLicensesEmail() throws Exception {
		final Capture<Conditions> capturedConditions = new Capture<>(CaptureType.ALL);

		expect(this.configurationMock.isEmailNotificationsEnabled()).andReturn(true);

		expect(this.configurationMock.getExpiringLicensesThreshold()).andReturn(7L);
		mockSendEmailForExpiringLicenses(capturedConditions, licensesList(license("123")));

		mockSendEmailForExpiringLicenses(capturedConditions, licensesList());

		replayAll();
		this.operation.sendExpiringLicensesEmail();
		verifyAll();

		assertEquals(
				Arrays.asList(createExpectedConditions(DateTime.now().plusDays(7)),
						createExpectedConditions(DateTime.now())), capturedConditions.getValues());
	}

	@Test
	public void testSendExpiringLicensesEmailAndExpectMessagingException() throws Exception {
		final MessagingException exception = new MessagingException("message");

		expect(this.configurationMock.isEmailNotificationsEnabled()).andReturn(true);
		expect(this.configurationMock.getExpiringLicensesThreshold()).andReturn(1L);
		expect(this.licenseRetrieveOperationMock.getList(anyObject(Conditions.class))).andReturn(
				new MetaDataList<>(licensesList(license("123")), metaData()));
		expect(this.converterMock.convertTo(license("123"))).andReturn("log");
		expect(this.mailContentGeneratorMock.expiringLicenses(licensesList(license("123")))).andReturn("content");
		expect(Messages.getSubject("licenseExpirationSubject")).andReturn(this.subject);
		this.mailSenderMock.send("content", this.subject);
		expectLastCall().andThrow(exception);

		replayAll();
		try {
			this.operation.sendExpiringLicensesEmail();
			fail();
		} catch (final SendException e) {
			assertEquals("message", e.getMessage());
			assertEquals(exception, e.getCause());
		}
		verifyAll();
	}

	@Test
	public void testSendExpiringLicensesEmailAndExpectRetrieveException() throws Exception {
		final RetrieveException exceptionMock = createMock(RetrieveException.class);

		expect(this.configurationMock.isEmailNotificationsEnabled()).andReturn(true);
		expect(this.configurationMock.getExpiringLicensesThreshold()).andReturn(1L);

		expect(this.licenseRetrieveOperationMock.getList(anyObject(Conditions.class))).andThrow(exceptionMock);
		expect(exceptionMock.getMessage()).andReturn("message");

		replayAll();
		try {
			this.operation.sendExpiringLicensesEmail();
			fail();
		} catch (final SendException e) {
			assertEquals("message", e.getMessage());
			assertEquals(exceptionMock, e.getCause());
		}
		verifyAll();
	}

	@Test
	public void testSendExpiringLicensesEmailWhenEmailNotificationsIsDisabled() throws Exception {
		expect(this.configurationMock.isEmailNotificationsEnabled()).andReturn(false);

		replayAll();
		this.operation.sendExpiringLicensesEmail();
		verifyAll();
	}

	private void mockSendEmailForExpiringLicenses(final Capture<Conditions> capturedConditions,
			final List<License> licenses) throws RetrieveException, MessagingException {
		expect(this.licenseRetrieveOperationMock.getList(capture(capturedConditions))).andReturn(
				new MetaDataList<>(licenses, metaData()));
		if (!licenses.isEmpty()) {
			licenses.forEach(l -> expect(this.converterMock.convertTo(l)).andReturn("log"));
			expect(this.mailContentGeneratorMock.expiringLicenses(licenses)).andReturn("content");
			expect(Messages.getSubject("licenseExpirationSubject")).andReturn(this.subject);
			this.mailSenderMock.send("content", this.subject);
		}
	}

	private Conditions createExpectedConditions(final DateTime date) {
		return ConditionsBuilder
				.createAndSkipMetaData()
				.betweenFilter("endDate", date.withTimeAtStartOfDay().toString(CLSConst.DATE_TIME_FORMAT),
						date.plusDays(1).withTimeAtStartOfDay().toString(CLSConst.DATE_TIME_FORMAT)).build();
	}
}
