package com.nokia.licensing.impl;

import java.util.ArrayList;
import java.util.List;

import com.nokia.licensing.dtos.License;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.storages.jdbc.LicenseDataStorageImpl;


public class LicenseAccessLogic {
    public List<License> getAllLicensesFromDb(final boolean checkDataIntegrity) throws LicenseException {
        final LicenseDataStorageImpl dataStorageImpl = new LicenseDataStorageImpl();
        final List<License> allLicense = new ArrayList<License>();
        final List<StoredLicense> allStoredLicense = dataStorageImpl.getAllLicenses(true);

        // allLicense = convertStoredLicenseToLicense();
        for (int i = 0; i < allStoredLicense.size(); i++) {

            final License license = new License();
            // setting StoredLicense object to License object
            license.setAdditionalInfo(allStoredLicense.get(i).getAdditionalInfo());
            license.setCapacityUnit(allStoredLicense.get(i).getCapacityUnit());
            license.setCustomerId(allStoredLicense.get(i).getCustomerId());
            license.setCustomerName(allStoredLicense.get(i).getCustomerName());
            license.setEndTime(allStoredLicense.get(i).getEndTime());

            license.setFeatureInfoList(allStoredLicense.get(i).getFeatureInfoList());
            license.setIsValid(allStoredLicense.get(i).getIsValid());
            license.setEndTime(allStoredLicense.get(i).getEndTime());
            license.setLicenseCode(allStoredLicense.get(i).getLicenseCode());
            license.setLicenseFileName(allStoredLicense.get(i).getLicenseFileName());
            license.setLicenseFilePath(allStoredLicense.get(i).getLicenseFilePath());
            license.setLicenseName(allStoredLicense.get(i).getLicenseName());
            license.setLicenseType(allStoredLicense.get(i).getLicenseType());
            license.setMaxValue(allStoredLicense.get(i).getMaxValue());
            license.setOrderId(allStoredLicense.get(i).getOrderId());
            license.setSerialNbr(allStoredLicense.get(i).getSerialNbr());
            license.setStartTime(allStoredLicense.get(i).getStartTime());
            license.setSwReleaseBase(allStoredLicense.get(i).getSwReleaseBase());
            license.setSwReleaseRelation(allStoredLicense.get(i).getSwReleaseRelation());

            // license.setTargetIds(targetIds)
            license.setTargetNEType(allStoredLicense.get(i).getTargetNEType());
            license.setUsageType(allStoredLicense.get(i).getUsageType());
            allLicense.add(license);
        }

        return allLicense;
    }
}
