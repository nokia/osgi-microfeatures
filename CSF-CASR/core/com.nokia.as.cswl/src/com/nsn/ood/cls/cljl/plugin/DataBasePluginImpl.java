/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl.plugin;

import java.sql.Connection;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nokia.licensing.interfaces.DataBasePlugin;
import com.nokia.licensing.interfaces.LicenseException;
import com.nsn.ood.cls.util.osgi.transaction.TransactionService;


/**
 * @author marynows
 *
 */
@Component
public class DataBasePluginImpl implements DataBasePlugin {
	private static final Logger LOG = LoggerFactory.getLogger(DataBasePluginImpl.class);
	
	@ServiceDependency
	protected volatile TransactionService txService;

	@Override
	public synchronized Connection getConnection() throws LicenseException {
		try {
			if(txService != null) {
				return txService.getConnection();
			} else {
				BundleContext bc = FrameworkUtil.getBundle(TransactionService.class).getBundleContext();
				return bc.getService(bc.getServiceReference(TransactionService.class)).getConnection();
			}
		} catch (Exception e) {
			LOG.error("Cannot retrieve connection to data base.", e);
		}
		return null;
	}
}
