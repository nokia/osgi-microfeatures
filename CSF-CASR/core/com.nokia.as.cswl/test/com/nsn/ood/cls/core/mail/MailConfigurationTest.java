/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.mail;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.mail.internet.InternetAddress;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.config.Configuration;


/**
 * @author marynows
 * 
 */
public class MailConfigurationTest {
	private Configuration configurationMock;
	private MailConfiguration mailConfig;

	@Before
	public void setUp() throws Exception {
		this.configurationMock = createMock(Configuration.class);

		this.mailConfig = new MailConfiguration();
		setInternalState(this.mailConfig, this.configurationMock);
	}

	@Test
	public void testGetServer() throws Exception {
		expect(this.configurationMock.getEmailServer()).andReturn("server");

		replayAll();
		assertEquals("server", this.mailConfig.getServer());
		assertEquals("server", this.mailConfig.getServer());
		verifyAll();
	}

	@Test
	public void testGetSubjectPrefix() throws Exception {
		expect(this.configurationMock.getEmailSubjectPrefix()).andReturn("subject");

		replayAll();
		assertEquals("subject", this.mailConfig.getSubjectPrefix());
		assertEquals("subject", this.mailConfig.getSubjectPrefix());
		verifyAll();
	}

	@Test
	public void testGetSender() throws Exception {
		expect(this.configurationMock.getEmailSender()).andReturn("test@nokia.com");

		replayAll();
		assertEquals(new InternetAddress("test@nokia.com"), this.mailConfig.getSender());
		assertEquals(new InternetAddress("test@nokia.com"), this.mailConfig.getSender());
		verifyAll();
	}

	@Test
	public void testGetRecipients() throws Exception {
		expect(this.configurationMock.getEmailRecipients()).andReturn("test@nokia.com");

		replayAll();
		assertArrayEquals(InternetAddress.parse("test@nokia.com"), this.mailConfig.getRecipients());
		assertArrayEquals(InternetAddress.parse("test@nokia.com"), this.mailConfig.getRecipients());
		verifyAll();
	}
}
