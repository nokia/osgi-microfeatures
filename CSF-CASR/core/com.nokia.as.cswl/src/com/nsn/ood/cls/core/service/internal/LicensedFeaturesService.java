/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service.internal;

import java.util.List;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.operation.LicensedFeatureRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * @author marynows
 * 
 */
@Component(provides = LicensedFeaturesService.class)
@Loggable
public class LicensedFeaturesService {
	@ServiceDependency
	private LicensedFeatureRetrieveOperation licensedFeatureRetrieveOperation;
	@ServiceDependency
	private ServiceExceptionFactory serviceExceptionFactory;

	public MetaDataList<LicensedFeature> getLicensedFeatures(final Conditions conditions) throws ServiceException {
		try {
			return this.licensedFeatureRetrieveOperation.getList(conditions);
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}

	public List<String> getLicensedFeatureFilterValues(final String filterName) throws ServiceException {
		try {
			return this.licensedFeatureRetrieveOperation.getFilterValues(filterName,//
					ConditionsBuilder.createAndSkipMetaData().build());
		} catch (final RetrieveException e) {
			throw this.serviceExceptionFactory.violation(e, e.getError());
		}
	}
}
