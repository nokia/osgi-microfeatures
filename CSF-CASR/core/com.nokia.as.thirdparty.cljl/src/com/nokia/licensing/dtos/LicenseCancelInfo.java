/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.dtos;

import java.io.Serializable;
import java.util.Date;


/**
 * This class represents the cancellation list, by means of containing all necessary information about canceled license
 * files. Whenever a license file is canceled, it is not possible to install the same license file again. The License
 * Access Interface provides API to fetch the canceled List
 *
 * @version 1.0
 */
// TODO : MAKE THIS CLASS MEMBER OF LICENSE
public class LicenseCancelInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    /**
     * Serial number of the canceled License File
     */
    private String serialNbr;
    /**
     * Date and time when License file was canceled
     */
    private long featureCode;
    private Date cancelDate;
    /**
     * Reason for License File cancellation
     */
    private String cancelReason;
    /**
     * User who performed the Cancellation operation
     */
    private String userName;
    /**
     * License File Name
     */
    private String licenseFileName;
    /**
     * Signifies whether the License has been canceled or not True = Canceled False == Not Canceled
     */
    private boolean isCanceled;
    /**
     * Contains encrypted data for the string which is formed by appending the all the Cancel List attributes
     */
    private byte[] cancelListSignature;

    // GETTER AND SETTER METHODS
    /**
     * @return id
     */
    public int getId() {
        return this.id;
    }

    /**
     * @param id
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     * Gets the serial number associated with the License
     *
     * @return the serialNbr
     */
    public String getSerialNbr() {
        return this.serialNbr;
    }

    /**
     * Sets the serial number associated with the License
     *
     * @param serialNbr
     *            the serialNbr to set
     */
    public void setSerialNbr(final String serialNbr) {
        this.serialNbr = serialNbr;
    }

    /**
     * Gets the License canceled date
     *
     * @return the cancelDate
     */
    public Date getCancelDate() {
        return this.cancelDate;
    }

    /**
     * Sets the License canceled date
     *
     * @param cancelDate
     *            the cancelDate to set
     */
    public void setCancelDate(final Date cancelDate) {
        this.cancelDate = cancelDate;
    }

    /**
     * Gets the License Cancel Reason
     *
     * @return the cancelReason
     */
    public String getCancelReason() {
        return this.cancelReason;
    }

    /**
     * Sets the License Cancel Reason
     *
     * @param cancelReason
     *            the cancelReason to set
     */
    public void setCancelReason(final String cancelReason) {
        this.cancelReason = cancelReason;
    }

    /**
     * Gets the User Name
     *
     * @return the userName
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Sets the User Name
     *
     * @param userName
     *            the userName to set
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Checks if License is Canceled or not True = Canceled False == Not Canceled
     *
     * @return the isCanceled
     */
    public boolean isCanceled() {
        return this.isCanceled;
    }

    /**
     * Sets Licensed canceled state True = Canceled False == Not Canceled
     *
     * @param isCanceled
     *            the isCanceled to set
     */
    public void setCanceled(final boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    /**
     * Gets License file name
     *
     * @return the license filename
     */
    public String getLicenseFileName() {
        return this.licenseFileName;
    }

    /**
     * Sets the License File Name
     *
     * @param licenceFileName
     *            -- License File Name
     */
    public void setLicenseFileName(final String licenseFileName) {
        this.licenseFileName = licenseFileName;
    }

    /**
     * Gets the Feature code of the License FeatureCode is a unique identifier that identifies a licensed feature. Ex:
     * 1234567890
     *
     * @return the featureCode
     */
    public long getFeaturecode() {
        return this.featureCode;
    }

    /**
     * Sets the License Feature Code FeatureCode is a unique identifier that identifies a licensed feature. Ex:
     * 1234567890
     *
     * @param featureCode
     *            the featureCode to set
     */
    public void setFeaturecode(final long featurecode) {
        this.featureCode = featurecode;
    }

    /**
     * Gets the Encrypted Data which is formed by appending the all the Cancel List attributes and encrypted it.
     *
     * @return Cancel List Signature
     */
    public byte[] getCancelListSignature() {
        return this.cancelListSignature;
    }

    /**
     * Sets the Encrypted Data which is formed by appending the all the Cancel List attributes and encrypted it.
     *
     * @param cancelListSignature
     */
    public void setCancelListSignature(final byte[] cancelListSignature) {
        this.cancelListSignature = cancelListSignature;
    }
}
