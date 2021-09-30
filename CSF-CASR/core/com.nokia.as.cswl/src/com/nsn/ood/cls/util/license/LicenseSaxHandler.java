/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util.license;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.nsn.ood.cls.util.Strings;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @author marynows
 * 
 */
class LicenseSaxHandler extends DefaultHandler {
	private final LicenseStatus licenseInfo;

	LicenseSaxHandler(final LicenseStatus licenseInfo) {
		this.licenseInfo = licenseInfo;
	}

	AtomicBoolean isIssuer = new AtomicBoolean(false);

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes attributes)
			throws SAXException {
		switch (qName) {
			case "licenceInfo":
				this.licenseInfo.setLicenseName(attributes.getValue("licenceName"));
				break;
			case "supplementaryInfo":
				this.licenseInfo.setSupported(!Strings.isNullOrEmpty(attributes.getValue("licenceType"))
						&& Strings.isNullOrEmpty(attributes.getValue("additionalInfo"))
						&& !Strings.isNullOrEmpty(attributes.getValue("usageType")));
				break;
			case "X509IssuerName":
				isIssuer.set(true);
				break;

			default:
				break;
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		switch (qName) {
			case "X509IssuerName":
				isIssuer.set(false);
				break;

			default:
				break;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if(isIssuer.get()){
			String myIssuer= new String(ch, start, length);
//			System.out.println("#)(#)(#)(#)(#)(#)(#)(#)(#)(#)(#)(#) ISSUER = "+ mateubsurlatable);
//			System.out.println(mateubsurlatable.contains("CN=LICENSESIGNER"));
			this.licenseInfo.setSupported(myIssuer.contains("CN=LICENSESIGNER"));
		}
	}
}