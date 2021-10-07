/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.mail;

import static com.nsn.ood.cls.model.test.JodaTestUtil.assertNow;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createMockAndExpectNew;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.easymock.Capture;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.convert.Message2StringConverter;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		MailSender.class, Session.class, Transport.class })
public class MailSenderTest {
	private static final String SERVER = "server";
	private static final String SUBJECT_PREFIX = "prefix";
	private static final String SUBJECT = "subject";
	private static final String CONTENT = "content";
	private static InternetAddress SENDER;
	private static InternetAddress[] RECIPIENTS;

	static {
		try {
			SENDER = new InternetAddress("test1@nokia.com");
			RECIPIENTS = InternetAddress.parse("test2@nokia.com");
		} catch (final AddressException e) {
		}
	}

	private MailSender sender;
	private MailConfiguration mailConfigurationMock;
	private Converter<Message, String> converterMock;

	@Before
	public void setUp() throws Exception {
		this.mailConfigurationMock = createMock(MailConfiguration.class);
		this.converterMock = createMock(Message2StringConverter.class);

		this.sender = new MailSender();
		setInternalState(this.sender, this.mailConfigurationMock, this.converterMock);

		mockStatic(Session.class);
		mockStatic(Transport.class);
	}

	@Test
	public void testSend() throws Exception {
		final Capture<Properties> capturedProperties = new Capture<>();
		final Capture<Date> capturedDate = new Capture<>();

		final Session sessionMock = mockObtainSession(capturedProperties);
		final MimeMessage messageMock = mockCreateMessage(sessionMock, capturedDate);
		expect(this.converterMock.convertTo(messageMock)).andReturn("log");
		Transport.send(messageMock);

		replayAll();
		this.sender.send(CONTENT, SUBJECT);
		verifyAll();

		assertEquals(SERVER, capturedProperties.getValue().get("mail.smtp.host"));
		assertNow(new DateTime(capturedDate.getValue()));
	}

	private Session mockObtainSession(final Capture<Properties> capturedProperties) {
		final Session sessionMock = createMock(Session.class);
		expect(this.mailConfigurationMock.getServer()).andReturn(SERVER);
		expect(Session.getInstance(capture(capturedProperties))).andReturn(sessionMock);
		return sessionMock;
	}

	private MimeMessage mockCreateMessage(final Session sessionMock, final Capture<Date> capturedDate)
			throws Exception, AddressException, MessagingException {
		final MimeMessage messageMock = createMockAndExpectNew(MimeMessage.class, sessionMock);
		expect(this.mailConfigurationMock.getSender()).andReturn(SENDER);
		messageMock.setFrom(SENDER);
		expect(this.mailConfigurationMock.getRecipients()).andReturn(RECIPIENTS);
		messageMock.setRecipients(javax.mail.Message.RecipientType.TO, RECIPIENTS);
		expect(this.mailConfigurationMock.getSubjectPrefix()).andReturn(SUBJECT_PREFIX);
		messageMock.setSubject(SUBJECT_PREFIX + " " + SUBJECT);
		messageMock.setSentDate(capture(capturedDate));
		messageMock.setContent(CONTENT, "text/html;charset=UTF-8");
		return messageMock;
	}
}
