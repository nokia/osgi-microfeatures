/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.mail;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.model.CLSConst;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = MailSender.class)
@Loggable
public class MailSender {
	private static final Logger LOG = LoggerFactory.getLogger(MailSender.class);
	private static final String MIME_TYPE = "text/html;charset=" + CLSConst.CHARSET;

	@ServiceDependency
	private MailConfiguration mailConfiguration;
	@ServiceDependency(filter = "(&(from=message)(to=string))")
	private Converter<Message, String> message2StringConverter;

	public void send(final String content, final String subject) throws MessagingException {
		final MimeMessage message = createMessage(content, subject);
		LOG.debug("Sending email: {}", message2StringConverter.convertTo(message));
		Transport.send(message);
	}

	private MimeMessage createMessage(final String content, final String subject) throws MessagingException {
		final MimeMessage message = new MimeMessage(obtainSession());
		message.setFrom(this.mailConfiguration.getSender());
		message.setRecipients(RecipientType.TO, this.mailConfiguration.getRecipients());
		message.setSubject(this.mailConfiguration.getSubjectPrefix() + " " + subject);
		message.setSentDate(new Date());
		message.setContent(content, MIME_TYPE);
		return message;
	}

	private Session obtainSession() {
		final Properties props = new Properties();
		props.put("mail.smtp.host", this.mailConfiguration.getServer());
		return Session.getInstance(props);
	}
}
