/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.convert;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.util.DescriptionBuilder;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
@Component
@Property(name = "from", value = "message")
@Property(name = "to", value = "string")
public class Message2StringConverter implements Converter<Message, String> {

	@ServiceDependency(filter = "(&(from=date)(to=dateTime))")
	private Converter<Date, DateTime> converter;

	@Override
	public String convertTo(final Message message) {
		if (message == null) {
			throw new CLSIllegalArgumentException("Message must not be null");
		}

		try {
			return new DescriptionBuilder()//
					.append("from", Arrays.toString(message.getFrom()))//
					.append("to", Arrays.toString(message.getRecipients(RecipientType.TO)))//
					.append("date", converter.convertTo(message.getSentDate()))//
					.append("subject", message.getSubject())//
					.append("content", message.getContent().toString())//
					.build();
		} catch (final MessagingException | IOException e) {
			throw new CLSRuntimeException("Error during conversion", e);
		}
	}

	@Override
	public Message convertFrom(final String string) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
