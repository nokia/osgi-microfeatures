// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.storages.jpa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.nokia.licensing.dao.FeatureInfoJPA;
import com.nokia.licensing.dao.TargetSystemJPA;
import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.querycache.QueryRetrieval;
import com.nokia.licensing.utils.LicenseConstants;


/**
 *
 * @author twozniak
 */
public class StoredLicenseUtil {

	/**
	 * This method is for connecting to the data base and getting Feature Info List and Target System List for the
	 * corresponding Feature Name,Feature Code,Target Id and Serial Number. This method uses JPA implementation to
	 * create query and execute it for getting data.
	 *
	 * @param storedLicense
	 *            -- Containing License details without Feature Info & Target System List
	 * @param connection
	 *            -- connection object to get the data from Data Base
	 * @param featureName
	 *            -- Feature Name
	 * @param FeatureCode
	 *            -- Feature Code
	 * @param TargetId
	 *            -- Target ID of the Licensing system
	 * @return StoredLicense -- Containing License details with Feature Info & Target System List
	 * @throws Exception
	 */
	public static StoredLicense populateCompletStoredLicense(final StoredLicense storedLicense,
			final EntityManager entityManager, final String featureName, final long featureCode, final String targetId)
					throws Exception {
		String strquery = null;
		FeatureInfoJPA featureInfoJPA = null;
		TargetSystemJPA targetSystemJPA = null;
		List<FeatureInfoJPA> listForFeatureInfoJPA = null;
		List<TargetSystemJPA> listForTargetSystemJPA = null;
		Iterator<FeatureInfoJPA> iteratorForFeatureInfoJPA = null;
		Iterator<TargetSystemJPA> iteratorForTargetSystemJPA = null;
		FeatureInfo featureInfo = null;
		TargetSystem targetSystem = null;
		List<FeatureInfo> listForFeatureInfo = null;
		List<TargetSystem> listForTargetSystem = null;
		Query query = null;

		LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
				"Getting the connection,making the statement and executing the statement to get Complete data for storedlicense so it will contain FeaureInfo and TargetSystem...");

		try {
			if ((null != featureName) || (0l != featureCode)) {
				if (featureName != null) {
					strquery = QueryRetrieval
							.getSQLData(LicenseConstants.SELECTFEATUREINFOBYSERIALNUMBERANDFEATURENAME);
				} else if (0l != featureCode) {
					strquery = QueryRetrieval
							.getSQLData(LicenseConstants.SELECTFEATUREINFOBYSERIALNUMBERANDFEATURECODE);
				}

				if (null != entityManager) {
					query = entityManager.createNativeQuery(strquery, FeatureInfoJPA.class);
				}

				query.setParameter(1, storedLicense.getSerialNbr());

				if (null != featureName) {
					query.setParameter(2, featureName);
				} else if (0l != featureCode) {
					query.setParameter(2, featureCode);
				}

				listForFeatureInfoJPA = new ArrayList<FeatureInfoJPA>();
				listForFeatureInfoJPA = query.getResultList();
				iteratorForFeatureInfoJPA = listForFeatureInfoJPA.iterator();
				listForFeatureInfo = new ArrayList<FeatureInfo>();

				while (iteratorForFeatureInfoJPA.hasNext()) {
					featureInfo = new FeatureInfo();
					featureInfoJPA = iteratorForFeatureInfoJPA.next();
					featureInfo.setFeatureName(featureInfoJPA.getFeatureName());
					featureInfo.setFeatureCode(featureInfoJPA.getFeatureCode());
					featureInfo.setFeatureInfoSignature(featureInfoJPA.getFeatureInfoSignature());
					featureInfo.setModifiedTime(featureInfoJPA.getModifiedTime());
					listForFeatureInfo.add(featureInfo);
				}

				storedLicense.setFeatureInfoList(listForFeatureInfo);
				LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
						"FeatureInfo Data is Added to StoredLicense Object.");
			}

			if (null != targetId) {
				strquery = QueryRetrieval.getSQLData(LicenseConstants.SELECTTARGETSYSTEMBYSERIALNUMBERANDTARGETID);

				if (null != entityManager) {
					query = entityManager.createNativeQuery(strquery, TargetSystemJPA.class);
				}

				query.setParameter(1, storedLicense.getSerialNbr());

				if (null != targetId) {
					query.setParameter(2, targetId);
				}

				listForTargetSystemJPA = query.getResultList();
				iteratorForTargetSystemJPA = listForTargetSystemJPA.iterator();
				listForTargetSystem = new ArrayList<TargetSystem>();

				while (iteratorForTargetSystemJPA.hasNext()) {
					targetSystem = new TargetSystem();
					targetSystemJPA = iteratorForTargetSystemJPA.next();
					targetSystem.setTargetId(targetSystemJPA.getTargetId());
					targetSystem.setTargetSystemSignature(targetSystemJPA.getTargetSystemSignature());
					targetSystem.setModifiedTime(targetSystemJPA.getModifiedTime());
					listForTargetSystem.add(targetSystem);
				}

				storedLicense.setTargetIds(listForTargetSystem);
				LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
						"targetSystem Data is Added to StoredLicense Object.");
				LicenseLogger.getInstance().finest(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
						"Complet Stored License Object is populated. ");
			}
		} catch (final Exception e) {
			LicenseLogger.getInstance().error(StoredLicenseUtil.class.getName(), "populateCompletStoredLicense",
					"Retrieving Data from Data Base is failed." + e.getMessage());
			throw e;
		}
		return storedLicense;
	}
}
