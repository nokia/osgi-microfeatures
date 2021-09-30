/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl;

import java.io.InputStream;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseInstall;
import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.util.log.Loggable;
import com.nsn.ood.cls.util.osgi.transaction.TransactionService;


/**
 * @author marynows
 *
 */
@Component(provides = CLJLLicenseService.class)
@Loggable
public class CLJLLicenseService {
	
	@ServiceDependency
	private BasicPrincipal principal;

	@ServiceDependency
	private CLJLProxy proxy;
	
	@ServiceDependency
	private TransactionService txService;

	public StoredLicense install(final InputStream content, final String fileName, final LicenseInstallOptions options)
			throws LicenseException {
		final LicenseInstallOptions installOptions = (options == null ? getInstallOptions() : options);

		return
		txService.txControl().notSupported(() -> {
			return getLicenseInstall().installLicense(content, fileName, installOptions.isForce(),
					installOptions.getTargetId(), installOptions.getUsername());
		});
	}

	private LicenseInstallOptions getInstallOptions() {
		return new LicenseInstallOptions() {
			@Override
			public boolean isForce() {
				return true;
			}

			@Override
			public String getUsername() {
				return CLJLLicenseService.this.principal.getUser();
			}

			@Override
			public String getTargetId() {
				return null;
			}
		};
	}

	public StoredLicense cancel(final LicenseCancelInfo licenseCancelInfo) throws LicenseException {
		return
		txService.txControl().notSupported(() -> {
			return getLicenseInstall().cancelLicenseWithFeedback(licenseCancelInfo);
		});
	}

	private LicenseInstall getLicenseInstall() throws LicenseException {
		return this.proxy.getLicenseInstall();
	}
}
