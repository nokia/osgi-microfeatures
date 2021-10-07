/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.impl;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nokia.licensing.dtos.AddnColumns.LicenseMode;
import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.FeatureStatus;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;
import com.nokia.licensing.utils.LicensesFilesRepositoryImpl;


/**
 * @author chhgupta This class is used by applications to check if the featureCode is valid and obtains the capacity of
 *         the featureCode
 */
public class LicenseCheckLogic {

    private static final String GET_FEATURE_STATUS = "getFeatureStatus";

    private static final String WRONG_DIGEST = "-1";

    private static final long UNLIMITED_CAPACITY = -1;

    private LicenseDataStorage dataStorage;
    private String targetId;
    private LicenseCheckValidator validator;
    private LicenseIntegrityValidator licenseIntegrityValidator;

    private static final String CLASS_NAME = LicenseCheckLogic.class.getName();

    public LicenseCheckLogic() {
        // TODO Auto-generated constructor stub
    }

    public LicenseCheckLogic(final LicenseDataStorage dataStorage, final LicenseCancelDataStorage cancelDataStorage,
            final String targetId) {
        this.dataStorage = dataStorage;
        this.targetId = targetId;
        this.validator = new LicenseCheckValidatorImpl();
        this.licenseIntegrityValidator = new LicenseIntegrityValidator(dataStorage,
                LicensesFilesRepositoryImpl.getInstance());
    }

    /**
     * This method calls getStoredLicensesforFeatureCode which returns a List of the StoredLicences all these
     * StoredLicenses which have the 'featureCode' received as parameter
     * 
     * @param featureCode
     * @param checkDataIntegrity
     *            - to check data between license XML file and DB tables The check variable is obtained by ORing the
     *            values of checkValidity(storedLicense) called on all the StoredLicenses The capacity is set to the net
     *            capacity of all the StoredLicenses FeatureStatus is set with the value of capacity and check
     * @return an instance of FeatureStatus
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws SQLException
     * @throws LicenseException
     */
    public FeatureStatus getFeatureStatus(final long featureCode, final boolean checkDataIntegrity)
            throws SQLException, ClassNotFoundException, IOException, LicenseException {

        final List<StoredLicense> storedLicenseList = getStoredLicenseList(featureCode, checkDataIntegrity);

        return getFeatureStatus(featureCode, checkDataIntegrity, true, storedLicenseList);
    }

    public FeatureStatus getFeatureStatus(final long featureCode, final String savedDigest)
            throws SQLException, ClassNotFoundException, IOException, LicenseException {

        final Map<Long, String> featureCodeMap = new HashMap<Long, String>();
        featureCodeMap.put(featureCode, savedDigest);
        return getFeatureStatus(featureCodeMap).get(0);
    }

    public List<FeatureStatus> getFeatureStatus(final Map<Long, String> featureCodes)
            throws SQLException, ClassNotFoundException, IOException, LicenseException {
        LicenseLogger.getInstance().fine(CLASS_NAME, GET_FEATURE_STATUS, "begin method ");
        final List<FeatureStatus> featureStatusList = new ArrayList<FeatureStatus>();
        final Map<Long, List<StoredLicense>> storedLicenses = getStoredLicenseList(featureCodes.keySet());

        for (final Long featureCode : featureCodes.keySet()) {
            List<StoredLicense> storedLicenseList = storedLicenses.get(featureCode);
            if (storedLicenseList == null) {
                storedLicenseList = new ArrayList<StoredLicense>();
            }
            final String actualDigest = computeDigest(storedLicenseList);
            final boolean isDigestEqual = isDigestEqual(featureCodes.get(featureCode), actualDigest);
            final FeatureStatus featureStatus = getFeatureStatus(featureCode, !isDigestEqual, !isDigestEqual,
                    storedLicenseList);
            setDigest(actualDigest, featureStatus);
            featureStatusList.add(featureStatus);
            LicenseLogger.getInstance().fine(CLASS_NAME, GET_FEATURE_STATUS,
                    "featureCode=" + featureCode + " featureStatus=" + featureStatus.isValid() + " capacity="
                            + featureStatus.getCapacity() + " licenses retrieved from DB: " + storedLicenseList.size());
        }
        LicenseLogger.getInstance().fine(CLASS_NAME, GET_FEATURE_STATUS, "end method");
        return featureStatusList;
    }

    private void setDigest(final String actualDigest, final FeatureStatus featureStatus) {
        if (WRONG_DIGEST.equals(featureStatus.getDigest())) {
            LicenseLogger.getInstance().finest(CLASS_NAME, GET_FEATURE_STATUS, "digest is not set");
            featureStatus.setDigest(null);
        } else {
            LicenseLogger.getInstance().finest(CLASS_NAME, GET_FEATURE_STATUS, "digest is set: " + actualDigest);
            featureStatus.setDigest(actualDigest);
        }
    }

    private boolean isDigestEqual(final String savedDigest, final String actualDigest) {
        boolean result;
        if (savedDigest != null) {
            result = savedDigest.equals(actualDigest);
        } else {
            result = false;
        }
        LicenseLogger.getInstance().finest(CLASS_NAME, "isDigestEqual", "" + result);
        return result;
    }

    private String computeDigest(final List<StoredLicense> storedLicenseList) {
        if (storedLicenseList.isEmpty()) {
            return null;
        } else {
            final String licenseInfo = getLicenseInfo(storedLicenseList);
            return computeDigest(licenseInfo.toString());
        }
    }

    private String getLicenseInfo(final List<StoredLicense> storedLicenseList) {
        final StringBuilder licenseInfo = new StringBuilder();
        sortStoredLicenseList(storedLicenseList);
        for (final StoredLicense license : storedLicenseList) {
            licenseInfo.append(getFeatureInfo(license.getFeatureInfoList()));
            licenseInfo.append(license.getMaxValue());
            licenseInfo.append(license.getSerialNbr());
            if (license.getEndTime() != null) {
                licenseInfo.append(license.getEndTime().getTime());
            }
            licenseInfo.append(license.getStartTime().getTime());
            licenseInfo.append(";");
        }
        return licenseInfo.toString();
    }

    private void sortStoredLicenseList(final List<StoredLicense> storedLicenseList) {
        Collections.sort(storedLicenseList, new Comparator<StoredLicense>() {
            @Override
            public int compare(final StoredLicense object1, final StoredLicense object2) {
                return object1.getSerialNbr().compareTo(object2.getSerialNbr());
            }
        });
    }

    private String computeDigest(final String s) {
        String hex = null;
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            final byte digest[] = md5.digest(s.getBytes());
            final BigInteger bigInt = new BigInteger(1, digest);
            hex = bigInt.toString(16);
        } catch (final Exception e) {
            LicenseLogger.getInstance().error(this.getClass().getName(), "computeDigest", e.getMessage(), e);
        }
        return hex;
    }

    private String getFeatureInfo(final List<FeatureInfo> featureInfoList) {
        String result = "";
        sortFeatureInfoList(featureInfoList);
        for (final FeatureInfo featureInfo : featureInfoList) {
            result += featureInfo.getFeatureCode();
        }
        return result;
    }

    private void sortFeatureInfoList(final List<FeatureInfo> featureInfoList) {
        Collections.sort(featureInfoList, new Comparator<FeatureInfo>() {
            @Override
            public int compare(final FeatureInfo object1, final FeatureInfo object2) {
                return (new Long(object1.getFeatureCode())).compareTo(new Long(object2.getFeatureCode()));
            }
        });
    }

    private FeatureStatus getFeatureStatus(final long featureCode, final boolean checkDataIntegrity,
            final boolean checkValidity,
            final List<StoredLicense> storedLicenseList) throws LicenseException {
        final FeatureStatus featureStatus = new FeatureStatus();
        boolean featureValid = false;
        long capacity = 0;
        for (final StoredLicense storedLicense : storedLicenseList) {
            try {
                final boolean licenseValid = isLicenseValid(checkDataIntegrity, checkValidity, storedLicense);
                if (!licenseValid) {
                    featureStatus.setDigest(WRONG_DIGEST);
                }

                featureValid = featureValid | licenseValid;

                if (licenseValid) {
                    final List<FeatureInfo> featureInfoList = storedLicense.getFeatureInfoList();
                    if (featureInfoList != null && !featureInfoList.isEmpty()) {
                        featureStatus.setFeatureName(featureInfoList.get(0).getFeatureName());
                    }

                    if (capacity != UNLIMITED_CAPACITY && storedLicense.getLicenseMode().equals(LicenseMode.CAPACITY)) {
                        capacity = capacity + storedLicense.getMaxValue();
                    } else if (storedLicense.getLicenseMode().equals(LicenseMode.ONOFF)) {
                        capacity = UNLIMITED_CAPACITY;
                        break;
                    }
                }
            } catch (final SQLException se) {
                createLicenseException(" Unable to connect/disconnect to the database: ", "CLJL109", se);
            } catch (final ClassNotFoundException cnfe) {
                createLicenseException(" ClassNotFound Exception: ", "CLJL123", cnfe);
            } catch (final IOException ioe) {
                createLicenseException(" IOException Exception: ", "CLJL116", ioe);
            } catch (final ParseException e) {
                createLicenseException(" ParseException Exception ocurred: ", "CLJL119", e);
            }
        }

        featureStatus.setValid(featureValid);
        featureStatus.setCapacity(capacity);
        featureStatus.setFeatureCode(featureCode);

        return featureStatus;
    }

    private boolean isLicenseValid(final boolean checkDataIntegrity, final boolean checkValidity,
            final StoredLicense storedLicense)
                    throws LicenseException, SQLException, ClassNotFoundException, IOException, ParseException {
        boolean licenseValid;
        if (checkDataIntegrity) {
            licenseValid = this.validator.checkIntegrity(storedLicense, this.licenseIntegrityValidator)
                    && this.validator.checkValidity(storedLicense);
        } else {
            if (checkValidity) {
                licenseValid = this.validator.checkValidity(storedLicense);
            } else {
                licenseValid = true;
            }
        }
        LicenseLogger.getInstance().finer(CLASS_NAME, "isLicenseValid",
                "License filename=" + storedLicense.getLicenseFileName() + " result=" + licenseValid);
        return licenseValid;
    }

    private void createLicenseException(final String errorMsg, final String errorCode, final Exception se)
            throws LicenseException {
        final LicenseException ex = new LicenseException(errorMsg + se);
        ex.setErrorCode(errorCode);
        LicenseLogger.getInstance().error(this.getClass().getName(), GET_FEATURE_STATUS, "error code set to: " + ex.getErrorCode(),
                se);

        throw ex;
    }

    private List<StoredLicense> getStoredLicenseList(final long featureCode, final boolean checkDataIntegrity)
            throws LicenseException {
        return getLicensesByFeatureCodeAndTargetIdAndTime(featureCode, this.targetId, new Date(), checkDataIntegrity);
    }

    private Map<Long, List<StoredLicense>> getStoredLicenseList(final Set<Long> featureCodes) throws LicenseException {

        return this.dataStorage.getLicensesByFeatureCodeAndTargetIdAndTime(featureCodes, this.targetId, new Date());
    }

    private List<StoredLicense> getLicensesByFeatureCodeAndTargetIdAndTime(final long featureCode,
            final String targetId,
            final java.util.Date time, final boolean checkDataIntegrity) throws LicenseException {
        final List<StoredLicense> storedLicenseList = this.dataStorage.getLicensesByFeatureCodeAndTargetIdAndTime(
                featureCode,
                targetId, time, checkDataIntegrity);
        return storedLicenseList;
    }

}
