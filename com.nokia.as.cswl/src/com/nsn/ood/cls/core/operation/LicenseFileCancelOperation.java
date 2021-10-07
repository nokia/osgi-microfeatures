/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.cljl.CLJLLicenseService;
import com.nsn.ood.cls.core.operation.exception.CancelException;
import com.nsn.ood.cls.core.security.BasicPrincipal;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 *
 */
@Component(provides = LicenseFileCancelOperation.class)
@Loggable
public class LicenseFileCancelOperation {
	private static final Logger LOG = LoggerFactory.getLogger(LicenseFileCancelOperation.class);

	@ServiceDependency
	private CLJLLicenseService cljlLicenseService;
	@ServiceDependency
	private BasicPrincipal basicPrincipal;
	@ServiceDependency(filter = "(&(from=licenseCancelInfo)(to=string))")
	private Converter<LicenseCancelInfo, String> licenseCancelInfo2StringConverter;
	@ServiceDependency(filter = "(&(from=storedLicense)(to=license))")
	private Converter<StoredLicense, License> storedLicense2LicenseConverter;
	@ServiceDependency(filter = "(&(from=license)(to=licenseCancelInfo))")
	private Converter<License, LicenseCancelInfo> license2LicenseCancelInfoConverter;

	public License cancel(final License license) throws CancelException {
		try {
			final LicenseCancelInfo licenseCancelInfo = convert(license);
			LOG.info("Cancelling license: {}", licenseCancelInfo2StringConverter.convertTo(licenseCancelInfo));
			final StoredLicense storedLicense = this.cljlLicenseService.cancel(licenseCancelInfo);
			return storedLicense2LicenseConverter.convertTo(storedLicense);
		} catch (final LicenseException e) {
			LOG.debug("Aborting operation: Cannot cancel license: {}", e.getMessage());
			throw new CancelException(e);
		}
	}

	private LicenseCancelInfo convert(final License license) {
		final LicenseCancelInfo licenseCancelInfo = license2LicenseCancelInfoConverter.convertTo(license);
		licenseCancelInfo.setUserName(this.basicPrincipal.getUser());
		licenseCancelInfo.setCancelReason("Deleted by user");
		licenseCancelInfo.setCancelDate(new DateTime().toDate());
		licenseCancelInfo.setFeaturecode(-1L);
		return licenseCancelInfo;
	}
}
