package com.nokia.licensing.impl;

import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.utils.LicensesFilesRepository;


public class LicenseIntegrityValidator {

    private final LicenseDataStorage dataStorage;

    private final LicensesFilesRepository filesRepository;

    public LicenseIntegrityValidator(final LicenseDataStorage dataStorage,
            final LicensesFilesRepository filesRepository) {
        this.dataStorage = dataStorage;
        this.filesRepository = filesRepository;
    }

    public void validate(final String serialNbr) throws LicenseException {
        final StoredLicense dbLicense = this.dataStorage.getLicenseBySerialNo(serialNbr, false);

        final StoredLicense fileLicense = this.filesRepository.readStoredLicense(dbLicense.getLicenseFileName());
        this.filesRepository.isValid(fileLicense, false, false);

        compare(fileLicense, dbLicense);
    }

    public void validate(final StoredLicense dbLicense) throws LicenseException {
        final StoredLicense fileLicense = this.filesRepository.readStoredLicense(dbLicense.getLicenseFileName());
        this.filesRepository.isValid(fileLicense, false, false);

        compare(fileLicense, dbLicense);
    }

    private void compare(final StoredLicense orginalLicense, final StoredLicense storedLicense)
            throws LicenseException {
        final String errorCode = "CLJL129";

        if (!isEqual(orginalLicense.getSerialNbr(), storedLicense.getSerialNbr())) {
            throw new LicenseException(errorCode, "License serial number field is modified !");
        }
        if (!isEqual(orginalLicense.getOrderId(), storedLicense.getOrderId())) {
            throw new LicenseException(errorCode, "License order id field is modified !");
        }
        if (!isEqual(orginalLicense.getLicenseCode(), storedLicense.getLicenseCode())) {
            throw new LicenseException(errorCode, "License code field is modified !");
        }
        if (!isEqual(orginalLicense.getLicenseName(), storedLicense.getLicenseName())) {
            throw new LicenseException(errorCode, "License name field is modified !");
        }
        if (!isEqual(orginalLicense.getCustomerName(), storedLicense.getCustomerName())) {
            throw new LicenseException(errorCode, "License customer name field is modified !");
        }
        if (!isEqual(orginalLicense.getCustomerId(), storedLicense.getCustomerId())) {
            throw new LicenseException(errorCode, "License customer id field is modified !");
        }
        if (!isEqual(orginalLicense.getLicenseType(), storedLicense.getLicenseType())) {
            throw new LicenseException(errorCode, "License type field is modified !");
        }
        if (!isEqual(orginalLicense.getMaxValue(), storedLicense.getMaxValue())) {
            throw new LicenseException(errorCode, "License max value field is modified !");
        }
        if (!isEqual(orginalLicense.getStartTime(), storedLicense.getStartTime())) {
            throw new LicenseException(errorCode, "License start time field is modified !");
        }
        if (!isEqual(orginalLicense.getEndTime(), storedLicense.getEndTime())) {
            throw new LicenseException(errorCode, "License end time field is modified !");
        }
        if (!isEqual(orginalLicense.getCapacityUnit(), storedLicense.getCapacityUnit())) {
            throw new LicenseException(errorCode, "License capacity unit field is modified !");
        }
        if (!isEqual(orginalLicense.getAdditionalInfo(), storedLicense.getAdditionalInfo())) {
            throw new LicenseException(errorCode, "License additional info field is modified !");
        }
        if (!isEqual(orginalLicense.getLicenseFileName(), storedLicense.getLicenseFileName())) {
            throw new LicenseException(errorCode, "License file name field is modified !");
        }
        if (!isEqual(orginalLicense.getLicenseFilePath(), storedLicense.getLicenseFilePath())) {
            throw new LicenseException(errorCode, "License file path field is modified !");
        }
        if (!isEqual(orginalLicense.getSwReleaseBase(), storedLicense.getSwReleaseBase())) {
            throw new LicenseException(errorCode, "License software release field is modified !");
        }
        if (!isEqual(orginalLicense.getSwReleaseRelation(), storedLicense.getSwReleaseRelation())) {
            throw new LicenseException(errorCode, "License software release releation field is modified !");
        }
        if (!isEqual(orginalLicense.getTargetNEType(), storedLicense.getTargetNEType())) {
            throw new LicenseException(errorCode, "License target NE type field is modified !");
        }
        if (!isEqual(orginalLicense.getUsageType(), storedLicense.getUsageType())) {
            throw new LicenseException(errorCode, "License usage type field is modified !");
        }
        if (!isEqual(orginalLicense.getIsValid(), storedLicense.getIsValid())) {
            throw new LicenseException(errorCode, "License valid field is modified !");
        }
        if (!isEqual(orginalLicense.getLicenseMode(), storedLicense.getLicenseMode())) {
            throw new LicenseException(errorCode, "License mode field is modified !");
        }
        if (!isEqual(orginalLicense.getOriginOMC(), storedLicense.getOriginOMC())) {
            throw new LicenseException(errorCode, "License origin OMC field is modified !");
        }
        if (!isEqual(orginalLicense.getPool(), storedLicense.getPool())) {
            throw new LicenseException(errorCode, "License pool field is modified !");
        }

        if (!orginalLicense.getTargetIds().containsAll(storedLicense.getTargetIds())) {
            throw new LicenseException(errorCode, "License target ids are modified !");
        }

        if (!orginalLicense.getFeatureInfoList().containsAll(storedLicense.getFeatureInfoList())) {
            throw new LicenseException(errorCode, "License feature infos are modified !");
        }
    }

    private boolean isEqual(final Object valueA, final Object valueB) {
        return valueA == null ? valueB == null : valueA.equals(valueB);
    }

}
