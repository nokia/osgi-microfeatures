/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.dao;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.nokia.licensing.dtos.AddnColumns;
import com.nokia.licensing.dtos.FeatureInfo;


/**
 * This class is a data holder class and holds License information. This information is per License File. The assumption
 * is that one License File will hold only one License Data Element as per SFRS
 *
 * @author Rama Manohar P
 * @version 1.0
 * @see SFRS for License Management https://sharenet-ims.inside.nsn.com/Download/394817326
 *
 */
@Entity
@Table(name = "STOREDLICENSE", schema = "LICENSE")
public class StoredLicenseJPA extends AddnColumns implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * Identifies the license file Serial number format: PPPPYYWKNNNNN PPPP = SAP R/3 plant identification. YY = year WK
     * = number of the week NNNNN = random number Note: Several instances of same license files may be result of
     * flexible target allocation. In NMS generated licenses following format is used: PPPAYY0NNNNNN PPP = NMS A =
     * running letter stands for NMS cluster (as in NMS generated license file name) YY = year 0 = reserved for future
     * NNNNNN = running number
     */
    @Id
    @Column(name = "SERIALNUMBER")
    private String serialNbr;
    /**
     * Identifies the order related to license purchase. Ex: 193428793827
     */
    @Column(name = "ORDERID")
    private String orderId;
    /**
     * Identifies the licensed application. Example: BSS1072
     */
    @Column(name = "LICENSECODE")
    private String licenseCode;
    /**
     * Name of the license Ex: "The Ultimate Service"
     */
    @Column(name = "LICENSENAME")
    private String licenseName;
    /**
     * Identifies the Customer Ex: FlyAgaric
     */
    @Column(name = "CUSTOMERNAME")
    private String customerName;
    /**
     * Identifies the Customer Ex: 12345
     */
    @Column(name = "CUSTOMERID")
    private String customerId;
    /**
     * The type of the license e.g. on/off license or capacity license Ex: "on/off" or "Capacity"
     */
    @Column(name = "LICENSETYPE")
    private int licenseType;
    /**
     * The number used as a threshold for capacity Ex: 50
     */
    @Column(name = "MAXVALUE")
    private long maxValue;
    /**
     * Start time of the license validity period (NSN Standard LKG fills in time using value as "YYYY-MM-DDT00:00:00")
     * Ex: 2004-09-24T00:00:00
     */
    @Column(name = "STARTTIME")
    private Date startTime;
    /**
     * End time of the license validity period (NSN Standard LKG fills in time using value as "YYYY-MM-DDT23:59:59") Ex:
     * 2005-03-24T23:59:59
     */
    @Column(name = "ENDTIME")
    private Date endTime;
    /**
     * The capacity unit of measurement Ex: "trx"
     */
    @Column(name = "CAPACITYUNIT")
    private String capacityUnit;
    /**
     * Origin omc
     */
    @Column(name = "ORIGINOMC")
    private String originOMC;
    /**
     * Pool license, denotes parent pool licenses in case it is nms otherwise null
     */
    @Column(name = "POOL")
    private String pool;
    /**
     * Supplementary product information Ex: "parentNMS"
     */
    @Column(name = "ADDITIONALINFO")
    private String additionalInfo;
    /**
     * License File Name
     */
    @Column(name = "LICENSEFILENAME")
    private String licenseFileName;
    /**
     * License File Path
     */
    @Column(name = "LICENSEFILEPATH")
    private String licenseFilePath;
    /**
     * Software release base version for license validity Ex: "3.5"
     */
    @Column(name = "SWRELEASEBASE")
    private String swReleaseBase;
    /**
     * License validity in relation to the Software release base version Ex: up to/only/from
     */
    @Column(name = "SWRELEASERELATION")
    private String swReleaseRelation;
    /**
     * Network element type (e.g. RNC). Ex: "RNC"
     */
    @Column(name = "TARGETNETYPE")
    private String targetNEType;
    /**
     * The purpose of the license file Ex: "commercial"
     */
    @Column(name = "USAGETYPE")
    private String usageType;
    /**
     * 
     */
    @Column(name = "ISVALID")
    private String isValid;
    /**
     * Holds a list of Feature Info objects which inturn contain Feature Name and Feature code for a License
     */
    @OneToMany(cascade = CascadeType.ALL)
    @MapKey(name = "SERIALNUMBER")
    @JoinColumn(name = "SERIALNUMBER", referencedColumnName = "SERIALNUMBER")
    protected List<FeatureInfoJPA> featureInfoList;
    /**
     * Holds a list of target ids which are associated with a License
     */
    @OneToMany(cascade = CascadeType.ALL)
    @MapKey(name = "SERIALNUMBER")
    @JoinColumn(name = "SERIALNUMBER", referencedColumnName = "SERIALNUMBER")
    protected List<TargetSystemJPA> targetIds;
    /**
     * Contains encrypted data for the string which is formed by appending the all the stored license attributes
     */
    @Column(name = "STOREDLICENSESIGNATURE")
    private byte[] storedLicenseSignature;
    /**
     * License Mode says if the license is ON/OFF or Capacity based license.
     * 
     */
    @Column(name = "LICENSEMODE")
    private int licenseMode;
    /**
     * The time and date when the license is installed.
     */
    @Column(name = "LICENSE_FILE_IMPORT_TIME")
    private Date licenseFileImportTime;
    /**
     * The user who installed the license.
     */
    @Column(name = "LICENSE_FILE_IMPORT_USER")
    private String licenseFileImportUser;

    // GETTER AND SETTER METHODS
    /**
     * Gets the Serial Number of the License Identifies the license file Serial number format: PPPPYYWKNNNNN PPPP = SAP
     * R/3 plant identification. YY = year WK = number of the week NNNNN = random number Note: Several instances of same
     * license files may be result of flexible target allocation. In NMS generated licenses following format is used:
     * PPPAYY0NNNNNN PPP = NMS A = running letter stands for NMS cluster (as in NMS generated license file name) YY =
     * year 0 = reserved for future NNNNNN = running number
     * 
     * @return serialNbr -- Serial Number of the license
     */
    public String getSerialNbr() {
        return this.serialNbr;
    }

    /**
     * Sets the serial number of the License * Gets the Serial Number of the License Identifies the license file Serial
     * number format: PPPPYYWKNNNNN PPPP = SAP R/3 plant identification. YY = year WK = number of the week NNNNN =
     * random number Note: Several instances of same license files may be result of flexible target allocation. In NMS
     * generated licenses following format is used: PPPAYY0NNNNNN PPP = NMS A = running letter stands for NMS cluster
     * (as in NMS generated license file name) YY = year 0 = reserved for future NNNNNN = running number
     * 
     * @param serialNbr
     *            the serialNbr to set
     */
    public void setSerialNbr(final String serialNbr) {
        this.serialNbr = serialNbr;
    }

    /**
     * Gets the Order Id related to License purchase Identifies the order related to license purchase. Ex: 193428793827
     * 
     * @return orderId -- Order Identifier of the License
     */
    public String getOrderId() {
        return this.orderId;
    }

    /**
     * Sets the Order Id related to License purchase Identifies the order related to license purchase. Ex: 193428793827
     * 
     * @param orderId
     *            the orderId to set
     */
    public void setOrderId(final String orderId) {
        this.orderId = orderId;
    }

    /**
     * Get the License Code for the License Identifies the licensed application. Example: BSS1072
     * 
     * @return licenceCode -- License Code
     */
    public String getLicenseCode() {
        return this.licenseCode;
    }

    /**
     * Sets the License code of the License Identifies the licensed application. Example: BSS1072
     * 
     * @param licenceCode
     *            the licenceCode to set
     */
    public void setLicenseCode(final String licenseCode) {
        this.licenseCode = licenseCode;
    }

    /**
     * Gets the Name of the License Name of the licence Ex: "The Ultimate Service"
     * 
     * @return licenceName -- License Name
     */
    public String getLicenseName() {
        return this.licenseName;
    }

    /**
     * Sets the name of the License Name of the licence Ex: "The Ultimate Service"
     * 
     * @param licenceName
     *            the licenceName to set
     */
    public void setLicenseName(final String licenseName) {
        this.licenseName = licenseName;
    }

    /**
     * Gets the Name of the Customer associated with the License Identifies the Customer Ex: FlyAgaric
     * 
     * @return customerName -- Customer Name
     */
    public String getCustomerName() {
        return this.customerName;
    }

    /**
     * Sets the Name of the Customer associated with the License Identifies the Customer Ex: FlyAgaric
     * 
     * @param customerName
     *            the customerName to set
     */
    public void setCustomerName(final String customerName) {
        this.customerName = customerName;
    }

    /**
     * Gets the Customer Id associated with License Identifies the Customer Ex: 12345
     * 
     * @return customerId -- Customer Id
     */
    public String getCustomerId() {
        return this.customerId;
    }

    /**
     * Sets the Customer Id associated with the License Identifies the Customer Ex: 12345
     * 
     * @param customerId
     *            the customerId to set
     */
    public void setCustomerId(final String customerId) {
        this.customerId = customerId;
    }

    /**
     * Gets the type of the License File The type of the license e.g. on/off license or capacity license Ex: "on/off" or
     * "Capacity"
     * 
     * @return licenceType -- License Type
     */
    public int getLicenseType() {
        return this.licenseType;
    }

    /**
     * Sets the type of the License File The type of the license e.g. on/off license or capacity license Ex: "on/off" or
     * "Capacity"
     * 
     * @param licenceType
     *            the licenceType to set
     */
    public void setLicenseType(final int licenseType) {
        this.licenseType = licenseType;
    }

    /**
     * Gets the Maximum Value The number used as a threshold for capacity Ex: 50
     * 
     * @return maxValue -- Maximum use used as threshold
     */
    public long getMaxValue() {
        return this.maxValue;
    }

    /**
     * Sets the Maximum Value The number used as a threshold for capacity Ex: 50
     * 
     * @param maxValue
     *            the Maximum Value to set as threshold
     */
    public void setMaxValue(final long maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the Start time of the license validity period Start time of the license validity period (NSN Standard LKG
     * fills in time using value as "YYYY-MM-DDT00:00:00") Ex: 2004-09-24T00:00:00
     * 
     * @return startTime -- Start Time
     */
    public Date getStartTime() {
        return this.startTime;
    }

    /**
     * Sets the Start time of the license validity period Start time of the license validity period (NSN Standard LKG
     * fills in time using value as "YYYY-MM-DDT00:00:00") Ex: 2004-09-24T00:00:00
     * 
     * @param startTime
     *            the startTime to set
     */
    public void setStartTime(final Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the End time of the license validity period End time of the license validity period (NSN Standard LKG fills
     * in time using value as "YYYY-MM-DDT23:59:59") Ex: 2005-03-24T23:59:59
     * 
     * @return endTime -- End Time
     */
    public Date getEndTime() {
        return this.endTime;
    }

    /**
     * Sets the End time of the license validity period End time of the license validity period (NSN Standard LKG fills
     * in time using value as "YYYY-MM-DDT23:59:59") Ex: 2005-03-24T23:59:59
     * 
     * @param endTime
     *            the endTime to set
     */
    public void setEndTime(final Date endTime) {
        this.endTime = endTime;
    }

    /**
     * Gets the capacity unit associated with the License The capacity unit of measurement Ex: "trx"
     * 
     * @return capacityUnit -- Capacity Unit associated with the License
     */
    public String getCapacityUnit() {
        return this.capacityUnit;
    }

    /**
     * Sets the License capacity Unit The capacity unit of measurement Ex: "trx"
     * 
     * @param capacityUnit
     *            the capacityUnit to set
     */
    public void setCapacityUnit(final String capacityUnit) {
        this.capacityUnit = capacityUnit;
    }

    /**
     * Gets additional information associated with the License Supplementary product information Ex: "parentNMS"
     * 
     * @return additionalInfo -- Additional information
     */
    public String getAdditionalInfo() {
        return this.additionalInfo;
    }

    /**
     * Sets additional information associated with License. Supplementary product information Ex: "parentNMS"
     * 
     * @param additionalInfo
     *            the additionalInfo to set
     */
    public void setAdditionalInfo(final String additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    /**
     * @param originOMC
     */
    public void setOriginOMC(final String originOMC) {
        this.originOMC = originOMC;
    }

    /**
     * @return the originOMC
     */
    public String getOriginOMC() {
        return this.originOMC;
    }

    /**
     * @param pool
     */
    public void setPool(final String pool) {
        this.pool = pool;
    }

    /**
     * @return parent license pool if has one
     */
    public String getPool() {
        return this.pool;
    }

    /**
     * Gets the License File Name
     * 
     * @return licenceFileName -- License File Name
     */
    public String getLicenseFileName() {
        return this.licenseFileName;
    }

    /**
     * Sets the License File Name
     * 
     * @param licenceFileName
     *            the licenceFileName to set
     */
    public void setLicenseFileName(final String licenseFileName) {
        this.licenseFileName = licenseFileName;
    }

    /**
     * Gets the License File Path
     * 
     * @return licenceFilePath -- License File Path
     */
    public String getLicenseFilePath() {
        return this.licenseFilePath;
    }

    /**
     * Sets the License File Path
     * 
     * @param licenceFilePath
     *            the licenceFilePath to set
     */
    public void setLicenseFilePath(final String licenseFilePath) {
        this.licenseFilePath = licenseFilePath;
    }

    /**
     * Gets the Software release base version for license validity License validity in relation to the Software release
     * base version Ex: up to/only/from
     * 
     * @return swReleaseBase -- Software Release Base version
     */
    public String getSwReleaseBase() {
        return this.swReleaseBase;
    }

    /**
     * Sets the Software release base version for license validity License validity in relation to the Software release
     * base version Ex: up to/only/from
     * 
     * @param swReleaseBase
     *            the swReleaseBase to set
     */
    public void setSwReleaseBase(final String swReleaseBase) {
        this.swReleaseBase = swReleaseBase;
    }

    /**
     * Gets the License validity in relation to the Software release base version Network element type (e.g. RNC). Ex:
     * "RNC"
     * 
     * @return swReleaseRelation -- Software Release Base Relation in relation to base version
     */
    public String getSwReleaseRelation() {
        return this.swReleaseRelation;
    }

    /**
     * Sets the License validity in relation to the Software release base version Network element type (e.g. RNC). Ex:
     * "RNC"
     * 
     * @param swReleaseRelation
     *            the swReleaseRelation to set
     */
    public void setSwReleaseRelation(final String swReleaseRelation) {
        this.swReleaseRelation = swReleaseRelation;
    }

    /**
     * Gets the Target NE of the License. Network NE Type Network element type (e.g. RNC). Ex: "RNC"
     * 
     * @return targetNEType -- Target NE Type
     */
    public String getTargetNEType() {
        return this.targetNEType;
    }

    /**
     * Sets the Target NE of the License. Network NE Type Network element type (e.g. RNC). Ex: "RNC"
     * 
     * @param targetNEType
     *            the targetNEType to set
     */
    public void setTargetNEType(final String targetNEType) {
        this.targetNEType = targetNEType;
    }

    /**
     * Gets the License Usage Type. The purpose of the license file Ex: "commercial"
     * 
     * @return usageType -- Usage Type
     */
    public String getUsageType() {
        return this.usageType;
    }

    /**
     * Sets the License Usage Type. The purpose of the license file Ex: "commercial"
     * 
     * @param usageType
     *            the usageType to set
     */
    public void setUsageType(final String usageType) {
        this.usageType = usageType;
    }

    /**
     * @return
     */
    public String getIsValid() {
        return this.isValid;
    }

    /**
     * @param isValid
     */
    public void setIsValid(final String isValid) {
        this.isValid = isValid;
    }

    /**
     * Gets the FeatureInfo list of the License Feature contains feature name and feature code
     * 
     * @return featureInfoList -- List of Feature Info
     * @see FeatureInfo
     */
    public java.util.List<FeatureInfoJPA> getFeatureInfoList() {
        return this.featureInfoList;
    }

    /**
     * Sets the FeatureInfo list of the License Feature contains feature name and feature code
     * 
     * @param featureInfoList
     *            the featureInfoList to set
     * @see FeatureInfo
     */
    public void setFeatureInfoList(final List<FeatureInfoJPA> featureInfoList) {
        this.featureInfoList = featureInfoList;
    }

    /**
     * Gets the Target IDs on which License is valid
     * 
     * @return targetIds -- List of TargetIds
     */
    public List<TargetSystemJPA> getTargetIds() {
        return this.targetIds;
    }

    /**
     * Sets the Target IDs on which License is valid
     * 
     * @param targetIds
     *            the targetIds to set
     */
    public void setTargetIds(final List<TargetSystemJPA> targetIds) {
        this.targetIds = targetIds;
    }

    /**
     * Gets the Encrypted Data which is formed by appending the all the stored license attributes and encrypted it.
     * 
     * @return StoredLicense Signature
     */
    public byte[] getStoredLicenseSignature() {
        return this.storedLicenseSignature;
    }

    /**
     * Sets the Encrypted Data which is formed by appending the all the stored license attributes and encrypted it.
     * 
     * @param storedLicenseSignature
     */
    public void setStoredLicenseSignature(final byte[] storedLicenseSignature) {
        this.storedLicenseSignature = storedLicenseSignature;
    }

    public String getLicenseFileImportUser() {
        return this.licenseFileImportUser;
    }

    public void setLicenseFileImportUser(final String licenseFileImportUser) {
        this.licenseFileImportUser = licenseFileImportUser;
    }

    public int getLicenseMode() {
        return this.licenseMode;
    }

    public void setLicenseMode(final int licenseMode) {
        this.licenseMode = licenseMode;
    }

    public Date getLicenseFileImportTime() {
        return this.licenseFileImportTime;
    }

    public void setLicenseFileImportTime(final Date licenseFileImportTime) {
        this.licenseFileImportTime = licenseFileImportTime;
    }
}
