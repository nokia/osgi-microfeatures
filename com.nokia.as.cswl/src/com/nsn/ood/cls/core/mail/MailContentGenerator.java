/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.mail;

import java.io.StringWriter;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.joda.time.DateTime;

import com.nsn.ood.cls.model.CLSConst;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = MailContentGenerator.class)
@Loggable
public class MailContentGenerator {
	private static final Properties VELOCITY_PROPS = new Properties();

	static {
		VELOCITY_PROPS.setProperty("resource.loader", "classpath");
		VELOCITY_PROPS.setProperty("classpath.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
	}

	public String capacityThreshold(final List<LicensedFeature> capacityWarnings) {
		final VelocityContext context = new VelocityContext();
		context.put("date", DateTime.now());
		context.put("featureInfos", capacityWarnings);

		return generateContent("/mailTemplates/capacityThreshold.vm", context);
	}

	public String expiringLicenses(final List<License> licenses) {
		final VelocityContext context = new VelocityContext();
		context.put("date", DateTime.now());
		context.put("expiringLicences", licenses);

		return generateContent("/mailTemplates/expiringLicenses.vm", context);
	}

	private String generateContent(final String templateName, final VelocityContext context) {
		final StringWriter writer = new StringWriter();
		Velocity.init(VELOCITY_PROPS);
		Velocity.mergeTemplate(templateName, CLSConst.CHARSET, context, writer);
		return writer.toString();
	}
}
