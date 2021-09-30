/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.filter.certificates;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.dm.annotation.api.Component;


/**
 * @author wro50095
 * 
 */
@Component(provides = CertificateInfoProviderThankYouPowerMock.class)
public class CertificateInfoProviderThankYouPowerMock {
	private static final String CLS_INSTALLATION_PROPERTY_NAME = "com.nsn.ood.cls.cert.installation";
	private static final String CLS_SERVER_PROPERTY_NAME = "com.nsn.ood.cls.cert.server";

	private static final String CLS_INSTALLATION_DEFAULT_LOCATION = "/opt/nokia/global/cls-backend/CLS-Server-CA/CLS_Installation_CA.crt";
	private static final String CLS_SERVER_DEFAULT_LOCATION = "/opt/nokia/global/cls-backend/CLS-Server-CA/CLS_Server_CA.crt";

	String getRootCACrtLocation() {
		return "/CLS_Root_CA.crt";
	}

	String getRootCACertificateThumbprint() {
		return "7bdb0400fe3b38c7eb6e8c9ab8bc49d69b20176d";
	}

	List<String> getIntermediateCertLocations() {
		return Arrays.asList(new String[] {
				getInstallationCertificatePath(), getServerCertificatePath() });
	}

	private String getInstallationCertificatePath() {
		return System.getProperty(CLS_INSTALLATION_PROPERTY_NAME, CLS_INSTALLATION_DEFAULT_LOCATION);
	}

	private String getServerCertificatePath() {
		return System.getProperty(CLS_SERVER_PROPERTY_NAME, CLS_SERVER_DEFAULT_LOCATION);
	}
}
