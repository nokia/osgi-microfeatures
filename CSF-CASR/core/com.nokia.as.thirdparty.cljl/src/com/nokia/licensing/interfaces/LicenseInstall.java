/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.interfaces;

import java.io.InputStream;

import com.nokia.licensing.dtos.LicenseCancelInfo;
import com.nokia.licensing.dtos.StoredLicense;


/**
 * Install Interfaces exposes methods to install/delete/cancel License files. The interface provides various methods
 * through which above mentioned operations can be performed. The methods internally invoke the Data Storage Interfaces
 * to actually perform License Installation into the Repository. The implementation of the methods below will call
 * License Logic Function, License Parser Function and License Validation Function
 *
 * @version 1.0
 */
public interface LicenseInstall {

    /**
     * This functions parses the license file and performs the validation of the same.
     *
     * @param licenseFileStream
     * @param licenseFileName
     * @return StoredLicense
     * @throws LicenseException
     */
    public StoredLicense parseAndValidate(InputStream licenseFileStream, String licenseFileName)
            throws LicenseException;

    /**
     * Installs the License File into system. Check for the validity of the License File by invoking the Validation
     * Function. Then invokes the Parser Function which parses the License File. Data Storage is invoked to store the
     * license data into the Repository. The License will be in installed into the system and state will be set to
     * Installed. The second argument (forceInstall) when set to true will make sure that old license is protected. If a
     * license has to be re-installed then this parameter has to be set to true.
     *
     * @param licenseFileName
     *            -- Name of the License File
     * @param forceInstall
     *            -- When set to true enables license re-install
     * @throws LicenseException
     */
    public StoredLicense installLicense(java.io.File licenseFileName, boolean forceInstall) throws LicenseException;

    /**
     * Installs the License into system. Check for the validity of the License by invoking the Validation Function. Then
     * invokes the Parser Function which parses the License information passed in the Stream. Data Storage is invoked to
     * store the license data into the Repository. The License will be in installed into the system and state will be
     * set to Installed. The second argument (forceInstall) when set to true will make sure that old license is
     * protected. If a license has to be re-installed then this parameter has to be set to true.
     *
     * @param licenseFileStream
     *            -- License File Stream
     * @param forceInstall
     *            -- When set to true enables license re-install
     * @throws LicenseException
     */
    public StoredLicense installLicense(java.io.InputStream licenseFileStream, boolean forceInstall)
            throws LicenseException;

    /**
     * Installs the License into system. Check for the validity of the License by invoking the Validation Function. Then
     * invokes the Parser Function which parses the License information passed in the Stream. Data Storage is invoked to
     * store the license data into the Repository. The License will be in installed into the system and state will be
     * set to Installed. The third argument (forceInstall) when set to true will make sure that old license is
     * protected. If a license has to be re-installed then this parameter has to be set to true.
     *
     * @param licenseFileStream
     *            -- License File Stream
     * @param licenseFileName
     *            -- License File Name
     * @param forceInstall
     *            -- When set to true enables license re-install
     * @param targetId
     *            -- expected target id
     * @throws LicenseException
     */
    public StoredLicense installLicense(java.io.InputStream licenseFileStream, String licenseFileName,
            boolean forceInstall, String targetId) throws LicenseException;

    /**
     * Installs the License into system. Check for the validity of the License by invoking the Validation Function. Then
     * invokes the Parser Function which parses the License information passed in the Stream. Data Storage is invoked to
     * store the license data into the Repository. The License will be in installed into the system and state will be
     * set to Installed. The third argument (forceInstall) when set to true will make sure that old license is
     * protected. If a license has to be re-installed then this parameter has to be set to true.
     *
     * @param licenseFileStream
     *            -- License File Stream
     * @param licenseFileName
     *            -- License File Name
     * @param forceInstall
     *            -- When set to true enables license re-install
     * @param targetId
     *            -- expected target id
     * @param username
     *            -- user who is installing license
     * @throws LicenseException
     */
    public StoredLicense installLicense(java.io.InputStream licenseFileStream, String licenseFileName,
            boolean forceInstall, String targetId, String username) throws LicenseException;

    /**
     * Deletes the License information based on Serial Number provided. License Information from the Storage Repository
     * will be deleted. License File will also be deleted from the Repository
     *
     * @param serialNbr
     *            -- Serial Number associated with License
     * @return deleted license
     * @throws LicenseException
     */
    public StoredLicense deleteLicenseWithFeedback(String serialNbr) throws LicenseException;

    /**
     * Deletes the License information based on Serial Number provided. License Information from the Storage Repository
     * will be deleted. License File will also be deleted from the Repository
     *
     * @param serialNbr
     *            -- Serial Number associated with License
     * @throws LicenseException
     */
    public void deleteLicenseBySerialNumber(String serialNbr) throws LicenseException;

    /**
     * Cancels the License File based on the information provided.A single License File stores only one LicenseData
     * Element so canceling a License is same as canceling the entire License File. The License state is set to
     * CANCELLED. This License will be moved to canceled list.
     *
     * @param license
     *            -- License Cancel Information
     * @return canceled license
     * @throws LicenseException
     */
    public StoredLicense cancelLicenseWithFeedback(LicenseCancelInfo license) throws LicenseException;

    /**
     * Cancels the License File based on the information provided.A single License File stores only one LicenseData
     * Element so canceling a License is same as canceling the entire License File. The License state is set to
     * CANCELLED. This License will be moved to canceled list.
     *
     * @param license
     *            -- License Cancel Information
     * @throws LicenseException
     */
    public void cancelLicense(LicenseCancelInfo license) throws LicenseException;

    /**
     * This method validates the license by comparison between the license file content stored in the license repository
     * and the DB tables entries describing the license to make sure that the DB values haven't been changed. If there
     * is any difference an exception is thrown.
     *
     * @param serialNbr
     *            -- validated license serial number
     * @throws LicenseException
     *             with message describing the detected difference, and error code CLJL129
     */
    public void validate(String serialNbr) throws LicenseException;
}
