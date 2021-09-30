/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.util.List;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.config.Configuration;
import com.nsn.ood.cls.core.mail.MailContentGenerator;
import com.nsn.ood.cls.core.mail.MailSender;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.util.Messages;
import com.nsn.ood.cls.model.CLSConst;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = EmailSendOperation.class)
@Loggable
public class EmailSendOperation {
	private static final Logger LOG = LoggerFactory.getLogger(EmailSendOperation.class);
	private static final String CAPACITY_WARNING_KEY = "capacityThresholdSubject";
	private static final String EXPIRATION_WARNING_KEY = "licenseExpirationSubject";

	@ServiceDependency
	private Configuration configuration;
	@ServiceDependency
	private MailSender mailSender;
	@ServiceDependency
	private MailContentGenerator mailContentGenerator;
	@ServiceDependency
	private LicenseRetrieveOperation licenseRetrieveOperation;
	@ServiceDependency(filter = "(&(from=licensedFeature)(to=string))")
	private Converter<LicensedFeature, String> licensedFeature2StringConverter;
	@ServiceDependency(filter = "(&(from=license)(to=string))")
	private Converter<License, String> license2StringConverter;
	@ServiceDependency
	private FeatureCapacityCheckOperation featureCheckOperation;

	public void sendExpiringLicensesEmail() throws SendException {
		if (this.configuration.isEmailNotificationsEnabled()) {
			final DateTime now = DateTime.now();
			sendEmailForExpiringLicenses(now.plusDays(this.configuration.getExpiringLicensesThreshold().intValue()));
			sendEmailForExpiringLicenses(now);
		}
	}

	public void sendCapacityThresholdEmail() throws SendException, UpdateException {
		if (this.configuration.isEmailNotificationsEnabled()) {
			final DateTime now = DateTime.now();
			final List<LicensedFeature> capacityWarningList = this.featureCheckOperation
					.retrieve(this.configuration.getCapacityThreshold(), now);

			if (!capacityWarningList.isEmpty()) {
				logInfo(now, capacityWarningList);
				sendMail(this.mailContentGenerator.capacityThreshold(capacityWarningList),
						Messages.getSubject(CAPACITY_WARNING_KEY));
				this.featureCheckOperation.update(capacityWarningList, now);
			}
		}
	}

	private void logInfo(final DateTime now, final List<LicensedFeature> capacityWarningList) {
		List<String> converted = capacityWarningList.stream()
									.map(licensedFeature2StringConverter::convertTo)
									.collect(Collectors.toList());
		LOG.info("Sending email for capacity thresholds: {}",
				new DescriptionBuilder().append("day", now.toString(CLSConst.DATE_FORMAT))
						.append("features", converted.toString())
						.build());
	}

	private void sendEmailForExpiringLicenses(final DateTime date) throws SendException {
		final List<License> licenses = getExpiringLicenses(date);
		List<String> converted = licenses.stream()
									.map(license2StringConverter::convertTo)
									.collect(Collectors.toList());
		if (!licenses.isEmpty()) {
			LOG.info("Sending email for expiring licenses: {}",
					new DescriptionBuilder().append("day", date.toString(CLSConst.DATE_FORMAT))
							.append("licenses", converted.toString()).build());
			sendMail(this.mailContentGenerator.expiringLicenses(licenses), Messages.getSubject(EXPIRATION_WARNING_KEY));
		}
	}

	private List<License> getExpiringLicenses(final DateTime date) throws SendException {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData()
				.betweenFilter("endDate", date2String(date), date2String(date.plusDays(1))).build();
		try {
			return this.licenseRetrieveOperation.getList(conditions).getList();
		} catch (final RetrieveException e) {
			throw new SendException(e);
		}
	}

	private String date2String(final DateTime date) {
		return date.withTimeAtStartOfDay().toString(CLSConst.DATE_TIME_FORMAT);
	}

	private void sendMail(final String content, final String subject) throws SendException {
		try {
			this.mailSender.send(content, subject);
		} catch (final MessagingException e) {
			throw new SendException(e);
		}
	}

	public static final class SendException extends CLSException {
		private static final long serialVersionUID = -5819794684314184268L;

		private SendException(final Throwable cause) {
			super(cause.getMessage(), cause);
		}
	}
}
