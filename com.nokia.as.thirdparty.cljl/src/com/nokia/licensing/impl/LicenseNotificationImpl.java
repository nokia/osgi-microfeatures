package com.nokia.licensing.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.nokia.licensing.dtos.FeatureInfo;
import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.LicenseChange;
import com.nokia.licensing.dtos.StoredLicense;
import com.nokia.licensing.dtos.TargetSystem;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.interfaces.LicenseNotification;
import com.nokia.licensing.storages.jdbc.StoredLicenseUtil;
import com.nokia.licensing.utils.LicenseEncrypt;


public class LicenseNotificationImpl implements LicenseNotification {
    LicenseCancelDataStorage cancelDataStorage;
    LicenseDataStorage dataStorage;

    public LicenseNotificationImpl(final LicenseDataStorage dataStorage,
            final LicenseCancelDataStorage cancelDataStorage) {
        this.dataStorage = dataStorage;
        this.cancelDataStorage = cancelDataStorage;
    }

    /**
     * This method fetches all licenses which will expire in x days from now. This x days is passed as argument to the
     * method. It returns a list of license objects which will expire in x days
     * 
     * @param days
     *            -- The days after which license expires
     * @return -- Returns a list of licenses which are about to expire
     * @throws LicenseException
     */
    @Override
    public List<StoredLicense> getExpiredLicenses(final int days) throws LicenseException {
        List<StoredLicense> storedLicensList = null;

        // ArrayList<StoredLicense> storedLicenseTempList = new ArrayList<StoredLicense>();
        final Date today = new Date();
        final Calendar cal = Calendar.getInstance();

        cal.setTime(today);
        cal.add(Calendar.DATE, +days);

        final Date dateAfterDays = cal.getTime();

        storedLicensList = this.dataStorage.getExpiredLicenses(today, dateAfterDays);

        return returnStoredLicenseList(storedLicensList);
    }

    /**
     * This method checks each license from the time "startTime" till "endTime". It queries for License information
     * between "startTime" till "endTime". The modifiedDate column in database for License table will be used for
     * querying. This modifiedDate column will be updated whenever there is any changes to the License information. For
     * all the Licenses retrieved it checks the following 1. Whether one of the featureCode of a license has been
     * canceled between "startTime" and "endTime" 2. Checks whether one of the featureCode of a license has been
     * installed between "startTime" and "endTime". 3. Checks whether License has been activated between "startTime" and
     * "endTime" 4. Checks whether License has expired between "startTime" and "endTime"
     *
     * The changes are wrapped into the LicenseChange object and a List of LicenseChange is returned
     * 
     * @param startTime
     *            -- Represents the start time.
     * @param endTime
     *            -- Represents the end time
     * @return List<LicenseChange> -- List of changes in License between startTime and endTime
     * @throws LicenseException
     */
    @Override
    public List<LicenseChange> getLicenseChanges(final Date startTime, final Date endTime) throws LicenseException {
        final List<LicenseChange> licenseChangeList = new ArrayList<LicenseChange>();
        final List<LicenseCancelInfo> licenseCancelInfoList = this.cancelDataStorage.getCanceledLicense(startTime,
                endTime);

        for (final LicenseCancelInfo licenseCancelInfo : licenseCancelInfoList) {
            final LicenseChange licenseChange = new LicenseChange();

            licenseChange.setLicenseCancelInfo(licenseCancelInfo);
            licenseChange.setStatus(LicenseChange.licenseEnum.CANCELLED);
            licenseChangeList.add(licenseChange);
        }

        final List<StoredLicense> modifiedStoredLicenseList = this.dataStorage.getModifiedLicenses(startTime, endTime);
        final List<StoredLicense> validStoredLicenseList = returnStoredLicenseList(modifiedStoredLicenseList);
        final Date date = new Date();

        for (final StoredLicense storedLicense : validStoredLicenseList) {
            final LicenseChange licenseChange = new LicenseChange();
            final Date endTimeTemp = storedLicense.getEndTime();

            if ((endTimeTemp != null) && date.after(endTimeTemp)) {
                licenseChange.setStatus(LicenseChange.licenseEnum.EXPIRED);
            } else if (date.after(storedLicense.getStartTime())) {
                licenseChange.setStatus(LicenseChange.licenseEnum.ACTIVE);
            } else if (date.after(storedLicense.getLicenseFileImportTime())) {
                licenseChange.setStatus(LicenseChange.licenseEnum.INSTALLED);
            }

            licenseChange.setNewLicense(storedLicense);
            licenseChangeList.add(licenseChange);
        }

        return licenseChangeList;
    }

    private List<StoredLicense> returnStoredLicenseList(final List<StoredLicense> storedLicensList) {
        final List<StoredLicense> storedLicenseTempList = new ArrayList<StoredLicense>();

        // List<StoredLicense> invalidStoredLicenseTempList = new ArrayList<StoredLicense>();

        for (final StoredLicense storedLicense : storedLicensList) {

            // StoredLicense invalidStoredLicense = storedLicense;
            // boolean invalidFeatureCode = false;
            // boolean invalidTargetID = false;
            // boolean partialInvalidStoredLIcense = false;
            final byte[] key = storedLicense.getKey();
            final byte[] encryptedStoredLicense = storedLicense.getStoredLicenseSignature();
            final String originalStoredLicense = LicenseEncrypt.decryptData(encryptedStoredLicense, key);
            final String actualStoredLicens = StoredLicenseUtil.getAppendedString(storedLicense);

            if (originalStoredLicense.equals(actualStoredLicens)) {
                final List<FeatureInfo> featureList = storedLicense.getFeatureInfoList();
                final List<FeatureInfo> tempFeatureList = new ArrayList<FeatureInfo>();

                // List<FeatureInfo> invlaidFeatureList = new ArrayList<FeatureInfo>();

                for (final FeatureInfo feature : featureList) {
                    final byte[] featureKey = feature.getFeatureInfoSignature();
                    final String decryptedFeature = LicenseEncrypt.decryptData(featureKey, key);
                    final String featureText = storedLicense.getSerialNbr() + feature.getFeatureCode()
                            + feature.getFeatureName() + feature.getModifiedTime();

                    if (featureText.equals(decryptedFeature)) {
                        tempFeatureList.add(feature);
                    }

                    // else{
                    // invalidFeatureCode = true;
                    // invlaidFeatureList.add(feature);
                    // }
                }

                // if (invalidFeatureCode){
                // partialInvalidStoredLIcense = true;
                // invalidStoredLicense.setFeatureInfoList(invlaidFeatureList);
                // }
                storedLicense.setFeatureInfoList(tempFeatureList);

                final List<TargetSystem> systems = storedLicense.getTargetIds();
                final List<TargetSystem> tempSystems = new ArrayList<TargetSystem>();

                // List<TargetSystem> invlidSystems = new ArrayList<TargetSystem>();
                for (final TargetSystem targetSystem : systems) {
                    final byte[] encryptedTarget = targetSystem.getTargetSystemSignature();
                    final String decryptedTarget = LicenseEncrypt.decryptData(encryptedTarget, key);
                    final String availableTarget = targetSystem.getTargetId() + targetSystem.getModifiedTime()
                            + storedLicense.getSerialNbr();

                    if (availableTarget.equals(decryptedTarget)) {
                        tempSystems.add(targetSystem);
                    }

                    // else{
                    // invalidTargetID = true;
                    // invlidSystems.add(targetSystem);
                    // }
                }

                // if (invalidTargetID){
                // partialInvalidStoredLIcense = true;
                // invalidStoredLicense.setTargetIds(invlidSystems);
                // }
                storedLicense.setTargetIds(tempSystems);

                // if (partialInvalidStoredLIcense){
                // invalidStoredLicenseTempList.add(invalidStoredLicense);
                // }
                storedLicenseTempList.add(storedLicense);
            }

            // else{
            // invalidStoredLicenseTempList.add(storedLicense);
            // }
        }

        // if (validity){
        return storedLicenseTempList;

        // }
        // else{
        // return invalidStoredLicenseTempList;
        // }
    }
}
