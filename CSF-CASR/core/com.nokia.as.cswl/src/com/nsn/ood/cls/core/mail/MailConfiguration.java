/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.mail;

import java.util.Arrays;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.ServiceScope;

import com.nsn.ood.cls.core.config.Configuration;


/**
 * @author marynows
 * 
 */
@Component(provides = MailConfiguration.class, scope = ServiceScope.PROTOTYPE)
public class MailConfiguration {
	@ServiceDependency
	private Configuration configuration;

	private String server;
	private String subject;
	private InternetAddress sender;
	private InternetAddress[] recipients;

	public String getServer() {
		if (this.server == null) {
			this.server = this.configuration.getEmailServer();
		}
		return this.server;
	}

	public String getSubjectPrefix() {
		if (this.subject == null) {
			this.subject = this.configuration.getEmailSubjectPrefix();
		}
		return this.subject;
	}

	public InternetAddress getSender() throws AddressException {
		if (this.sender == null) {
			this.sender = new InternetAddress(this.configuration.getEmailSender());
		}
		return this.sender;
	}

	public InternetAddress[] getRecipients() throws AddressException {
		if (this.recipients == null) {
			this.recipients = InternetAddress.parse(this.configuration.getEmailRecipients());
		}
		return Arrays.copyOf(this.recipients, this.recipients.length);
	}
}
