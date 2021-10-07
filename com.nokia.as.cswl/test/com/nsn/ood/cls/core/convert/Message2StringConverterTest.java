/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Date;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class Message2StringConverterTest {
	private static final DateTime DATE = new DateTime(2015, 8, 4, 16, 14, DateTimeZone.forOffsetHours(1));

	private Message2StringConverter converter;
	private Converter<Date, DateTime> dateConverter;

	@Before
	public void setUp() throws Exception {
		this.dateConverter = createMock(Date2DateTimeConverter.class);
		this.converter = new Message2StringConverter();
		setInternalState(converter, dateConverter);
	}

	@Test
	public void testConvertTo() throws Exception {
		final Message message = createMock(Message.class);

		expect(message.getFrom()).andReturn(new Address[] {
			new InternetAddress("from@test.com") });
		expect(message.getRecipients(RecipientType.TO)).andReturn(new Address[] {
				new InternetAddress("to@aa.bb"), new InternetAddress("dd@ee.ff") });
		expect(message.getSentDate()).andReturn(DATE.toDate());
		expect(dateConverter.convertTo(DATE.toDate())).andReturn(DATE);
		expect(message.getSubject()).andReturn("SSS");
		expect(message.getContent()).andReturn("CCC");

		replayAll();
		assertEquals("[from=[from@test.com], to=[to@aa.bb, dd@ee.ff], date=2015-08-04T16:14:00.000+01:00,"
				+ " subject=SSS, content=CCC]", this.converter.convertTo(message));
		verifyAll();
	}

	@Test
	public void testConvertToWithException() throws Exception {
		final Message message = createMock(Message.class);
		final MessagingException exception = new MessagingException();

		expect(message.getFrom()).andThrow(exception);

		replayAll();
		try {
			this.converter.convertTo(message);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
			assertEquals(exception, e.getCause());
		}
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
}
