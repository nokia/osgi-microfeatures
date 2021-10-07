/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.mail;

import static com.nsn.ood.cls.model.test.JodaTestUtil.assertNow;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.easymock.Capture;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.LicensedFeature;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
	Velocity.class })
public class MailContentGeneratorTest {

	@Test
	public void testExpiringLicenses() throws Exception {
		final List<License> licenses = licensesList(license());
		final Capture<Properties> capturedProperties = new Capture<>();
		final Capture<VelocityContext> capturedContext = new Capture<>();

		mockStatic(Velocity.class);
		Velocity.init(capture(capturedProperties));
		expect(
				Velocity.mergeTemplate(eq("/mailTemplates/expiringLicenses.vm"), eq("UTF-8"), capture(capturedContext),
						anyObject(StringWriter.class))).andReturn(true);

		replayAll();
		assertEquals("", new MailContentGenerator().expiringLicenses(licenses));
		verifyAll();

		final Properties properties = capturedProperties.getValue();
		assertEquals("classpath", properties.getProperty("resource.loader"));
		assertEquals("org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader",
				properties.getProperty("classpath.resource.loader.class"));

		final VelocityContext context = capturedContext.getValue();
		assertNow((DateTime) context.get("date"));
		assertEquals(licenses, context.get("expiringLicences"));
	}

	@Test
	public void testCapacityThreshold() throws Exception {
		final List<LicensedFeature> warnings = new ArrayList<>();
		final Capture<Properties> capturedProperties = new Capture<>();
		final Capture<VelocityContext> capturedContext = new Capture<>();

		mockStatic(Velocity.class);
		Velocity.init(capture(capturedProperties));
		expect(
				Velocity.mergeTemplate(eq("/mailTemplates/capacityThreshold.vm"), eq("UTF-8"),
						capture(capturedContext), anyObject(StringWriter.class))).andReturn(true);

		replayAll();
		assertEquals("", new MailContentGenerator().capacityThreshold(warnings));
		verifyAll();

		final Properties properties = capturedProperties.getValue();
		assertEquals("classpath", properties.getProperty("resource.loader"));
		assertEquals("org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader",
				properties.getProperty("classpath.resource.loader.class"));

		final VelocityContext context = capturedContext.getValue();
		assertNow((DateTime) context.get("date"));
		assertEquals(warnings, context.get("featureInfos"));
	}
}
