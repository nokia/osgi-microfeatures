// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.storages.jdbc.StoredLicenseUtil;
import com.nokia.licensing.utils.LicenseEncrypt;


class LicenseCheckValidatorImpl implements LicenseCheckValidator {
    /**
     * This method compares the data between the DB tables and stored license file.
     * 
     * @param storedLicense
     *            to check
     * @return true if data is integral
     */
    @Override
    public boolean checkIntegrity(final StoredLicense storedLicense,
            final LicenseIntegrityValidator licenseIntegrityValidator)
                    throws LicenseException {

        try {
            licenseIntegrityValidator.validate(storedLicense);
        } catch (final LicenseException ex) {
            LicenseLogger.getInstance().error(this.getClass().getName(), "getFeatureStatus",
                    "License + " + storedLicense.getLicenseFileName() + " integrity check failed", ex);
            return false;
        }
        return true;
    }

    /**
     * This method validates if the StoredLicense has not expired
     * 
     * @param storedlicense
     * @return a boolean which tells if the StoredLicense has not expired
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws LicenseException
     * @throws ParseException
     */
    @Override
    public boolean checkValidity(final StoredLicense storedLicense)
            throws SQLException, ClassNotFoundException, IOException, LicenseException, ParseException {
        LicenseLogger.getInstance().finer(this.getClass().getName(), "checkValidity",
                "License filename=" + storedLicense.getLicenseFileName());

        final Date currentDateTime = new Date();
        // DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        boolean validate = false;
        final byte[] encryptedDatastoredLicense = storedLicense.getStoredLicenseSignature();
        final byte[] encryptKey = storedLicense.getKey();
        final String decryptedString = LicenseEncrypt.decryptData(encryptedDatastoredLicense, encryptKey);
        final String originalString = StoredLicenseUtil.getAppendedString(storedLicense);

        if (decryptedString.equals(originalString)) {
            final Date endDateTime = storedLicense.getEndTime();
            final Date startDateTime = storedLicense.getStartTime();

            if ((endDateTime == null) && startDateTime.before(currentDateTime)) {
                // if end time is null check only whether currentdate is after start time.
                validate = true;
                LicenseLogger.getInstance().finest(this.getClass().getName(), "checkValidity",
                        "License start date is valid, validate=" + validate);
            } else if (startDateTime.before(currentDateTime) && endDateTime.after(currentDateTime)) {
                // if the current date lies between startdate and enddate then the StoredLicense is valid, valid = true
                validate = true;
                LicenseLogger.getInstance().finest(this.getClass().getName(), "checkValidity",
                        "Current date lies between license start date and license end date, validate=" + validate);
            } else if (endDateTime.before(currentDateTime)) {
                validate = false;
                LicenseLogger.getInstance().finer(this.getClass().getName(), "checkValidity",
                        "Current date lies between license start date and license end date, validate=" + validate);
            }
        } else {
            validate = false;
            LicenseLogger.getInstance().finer(this.getClass().getName(), "checkValidity", "The DataBase has been modified.");
            return validate;
        }

        List<FeatureInfo> featureInfoList;

        featureInfoList = storedLicense.getFeatureInfoList();

        for (int i = 0; i < featureInfoList.size(); i++) {

            // Timestamp modifiedTimeFeatureInfo = null;
            final FeatureInfo featureInfo = featureInfoList.get(i);

            // modifiedTimeFeatureInfo = new Timestamp(featureInfo.getModifiedTime().getTime());
            final byte[] encryptedFeatureInfo = featureInfo.getFeatureInfoSignature();
            final String decryptedFeatureInfo = LicenseEncrypt.decryptData(encryptedFeatureInfo, encryptKey);

            // dateformat.parse(formattedCurrentDate));
            final String originalFeatureInfo = storedLicense.getSerialNbr() + featureInfo.getFeatureCode();
            LicenseLogger.getInstance().finest(this.getClass().getName(), "checkValidity",
                    String.format("Feature %s info validation, original: >%s<, decrypted: >%s<",
                            featureInfo.getFeatureCode(), originalFeatureInfo, decryptedFeatureInfo));
            final boolean check = decryptedFeatureInfo.startsWith(originalFeatureInfo);
            validate = validate & check;

            // If Feature info table has been tampered then return false.
            if (validate == false) {
                LicenseLogger.getInstance().error(this.getClass().getName(), "checkValidity",
                        "Feature info table has been tampered for license=" + storedLicense.getLicenseFileName());
                return validate;
            }
        }

        // TODO: If License file is tampered then shd we move it to cancel table??
        // TODO:
        List<TargetSystem> targetIds;

        targetIds = storedLicense.getTargetIds();

        for (int i = 0; i < targetIds.size(); i++) {

            // Timestamp modifiedTimeTargetSystem = null;
            final TargetSystem targetSystem = targetIds.get(i);
            final byte[] encryptedtargetSystem = targetSystem.getTargetSystemSignature();
            final String decryptedtargetSystem = LicenseEncrypt.decryptData(encryptedtargetSystem, encryptKey);
            final String originaltargetSystem = storedLicense.getSerialNbr() + targetSystem.getTargetId();
            LicenseLogger.getInstance().finest(this.getClass().getName(), "checkValidity",
                    String.format("Taget system validation %s, original: >%s<, decrypted: >%s<",
                            targetSystem.getTargetId(), originaltargetSystem, decryptedtargetSystem));
            final boolean check = decryptedtargetSystem.startsWith(originaltargetSystem);
            validate = validate & check;
        }

        return validate;
    }
}
