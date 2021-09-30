///* ========================================== */
///* Copyright (c) 2009 Nokia                   */
///*          All rights reserved.              */
///*          Company Confidential              */
///* ========================================== */
package com.nokia.licensing.dtos;

import java.io.Serializable;


/**
 * This class holds license change details. It holds information about changed data as well as the new data.
 */
public class LicenseChange implements Serializable {
    private static final long serialVersionUID = 1L;
    private licenseEnum status;
    /**
     * This field gives the cumulative capacity.
     *
     * This is capacities from each featureCode added up.
     */
    private int CUMULATIVE_CAPACITY;
    /**
     * Indicates whether the capacity is changed.
     */
    private boolean CAPACITYCHANGED;
    /**
     * Holds the information of canceled license.
     */
    private LicenseCancelInfo licenseCancelInfo;
    /**
     * License information
     */
    private StoredLicense newLicense;

    /**
     * Enum which determines what has changed
     */
    public enum licenseEnum {

        INSTALLED, ACTIVE, EXPIRED, INVALID, CANCELLED
    }

    /**
     * Returns true if capacity is changed.
     *
     * @return TRUE/FALSE
     */
    public boolean isCAPACITYCHANGED() {
        return this.CAPACITYCHANGED;
    }

    // GETTERS & SETTERS
    public int getCUMULATIVE_CAPACITY() {
        return this.CUMULATIVE_CAPACITY;
    }

    public void setCUMULATIVE_CAPACITY(final int cumulative_capacity) {
        this.CUMULATIVE_CAPACITY = cumulative_capacity;
    }

    public StoredLicense getNewLicense() {
        return this.newLicense;
    }

    public void setNewLicense(final StoredLicense newLicense) {
        this.newLicense = newLicense;
    }

    /**
     * Sets the value if the capacity is changed.
     *
     * @param capacitychanged
     */
    public void setCAPACITYCHANGED(final boolean capacitychanged) {
        this.CAPACITYCHANGED = capacitychanged;
    }

    /**
     * Gets the canceled license information.
     *
     * @return {@link LicenseCancelInfo}
     */
    public LicenseCancelInfo getLicenseCancelInfo() {
        return this.licenseCancelInfo;
    }

    /**
     * Sets the canceled license information.
     *
     * @param licenseCancelInfo
     */
    public void setLicenseCancelInfo(final LicenseCancelInfo licenseCancelInfo) {
        this.licenseCancelInfo = licenseCancelInfo;
    }

    public void setStatus(final licenseEnum status) {
        // TODO Auto-generated method stub
        this.status = status;
    }

    public licenseEnum getStatus() {
        return this.status;
    }
}
