/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.license;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.felix.dm.annotation.api.Component;
import org.xml.sax.SAXException;

import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author wro50095
 * 
 */
@Component(provides = LicenseParser.class)
public class LicenseParser {
	private static final SAXParserFactory SAX_PARSER_FACTORY = SAXParserFactory.newInstance();

	public LicenseStatus parse(final InputStream licenseContent) {
		final LicenseStatus licenseInfo = new LicenseStatus();
		try {
			SAX_PARSER_FACTORY.newSAXParser().parse(licenseContent, new LicenseSaxHandler(licenseInfo));
		} catch (SAXException | IOException | ParserConfigurationException e) {
			throw new CLSRuntimeException(e);
		}
		return licenseInfo;
	}
}
