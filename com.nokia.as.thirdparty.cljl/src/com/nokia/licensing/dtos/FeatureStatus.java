/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.dtos;

import java.io.Serializable;


/**
 * Transfer Object for returning licensing information. LicenseInfo object contains license information for one specific
 * feature identified by the feature code.
 *
 * @version 1.0
 */
public class FeatureStatus extends FeatureInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * A constant for capacity which is not limited or is not applicable. It is equal to the value of "-1".
     */
    public static final long CAPACITY_UNLIMITED = -1;
    /**
     * Status of license for the feature code. Set to true when the feature code is licensed for usage, false when there
     * is no valid license for the feature code.
     *
     */
    private boolean valid;
    /**
     * Capacity is used for limiting the license capacity in units specific to the feature code.
     */
    private long capacity = CAPACITY_UNLIMITED;

    private String digest;

    /**
     * Gets the feature status value
     *
     * @return the valid
     */
    public boolean isValid() {
        return this.valid;
    }

    /**
     * Sets the Feature status value
     *
     * @param valid
     *            the valid to set
     */
    public void setValid(final boolean valid) {
        this.valid = valid;
    }

    /**
     * Gets the License capacity value
     *
     * @return the capacity
     */
    public long getCapacity() {
        return this.capacity;
    }

    /**
     * Sets the License capacity value
     *
     * @param capacity
     *            the capacity to set
     */
    public void setCapacity(final long capacity) {
        this.capacity = capacity;
    }

    public void setDigest(final String digest) {
        this.digest = digest;
    }

    public String getDigest() {
        return this.digest;
    }
}
