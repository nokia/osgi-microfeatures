/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.cljl.CLJLLicenseService;
import com.nsn.ood.cls.cljl.LicenseInstallOptions;
import com.nsn.ood.cls.core.model.LicenseFile;
import com.nsn.ood.cls.core.operation.exception.InstallException;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;
import com.nsn.ood.cls.util.license.LicenseParser;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = LicenseFileInstallOperation.class)
@Loggable
public class LicenseFileInstallOperation {
	private static final Logger LOG = LoggerFactory.getLogger(LicenseFileInstallOperation.class);

	@ServiceDependency
	private LicenseParser licenseParser;
	@ServiceDependency
	private CLJLLicenseService cljlLicenseService;
	@ServiceDependency
	private LicenseInstallOptions licenseInstallOptions;
	@ServiceDependency(filter = "(&(from=storedLicense)(to=license))")
	private Converter<StoredLicense, License> storedLicense2LicenseConverter;
	@ServiceDependency(filter = "(&(from=licenseInstallOptions)(to=string))")
	private Converter<LicenseInstallOptions, String> licenseInstallOptions2StringConverter;

	public License install(final LicenseFile licenseFile) throws InstallException {
		verifyLicense(licenseFile.getContent());
		System.out.println("#!#!#!#!#!#!#!#!#!#!#! License Verified ");

		try {
			LOG.info("License installation options: {}", licenseInstallOptions2StringConverter.convertTo(this.licenseInstallOptions));
			System.out.println("#!#!#!#!#!#!#!#!#!#!#! License converted ");
			LOG.debug("License content: {}", licenseFile.getContent());
			final InputStream content = IOUtils.toInputStream(licenseFile.getContent());
			final StoredLicense storedLicense = this.cljlLicenseService.install(content, licenseFile.getFileName(),
					this.licenseInstallOptions);
			System.out.println("#!#!#!#!#!#!#!#!#!#!#! License Installed ");
			return storedLicense2LicenseConverter.convertTo(storedLicense);
		} catch (final LicenseException e) {
			LOG.debug("Aborting operation: Cannot install license: {}", e.getMessage());
			throw new InstallException(e);
		}
	}

	private void verifyLicense(final String licenseContent) throws InstallException {
		final InputStream content = IOUtils.toInputStream(licenseContent);
		try {
			if (!this.licenseParser.parse(content).isSupported()) {
				LOG.debug("Aborting operation: License is not supported");
				throw new InstallException("License is not supported.", null);
			}
		} catch (final CLSRuntimeException e) {
			LOG.debug("Aborting operation: Cannot parse license: {}", e.getMessage());
			throw new InstallException("Cannot parse license.", e);
		}
	}
}
