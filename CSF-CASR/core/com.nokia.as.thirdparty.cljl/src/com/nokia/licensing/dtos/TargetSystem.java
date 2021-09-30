/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.dtos;

import java.io.Serializable;
import java.util.Date;


/**
 * Data Holder class which holds information about License Target Id.
 *
 * @version 1.0
 */
public class TargetSystem implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The target id of the client.
     */
    private String targetId;
    /**
     * Contains encrypted data for the string which is formed by appending the all the Target System attributes
     */
    private byte[] targetSystemSignature;
    /**
     * Modified Time of the license (NSN Standard LKG fills in time using value as "YYYY-MM-DDT23:59:59") Ex:
     * 2005-03-24T23:59:59
     */
    private Date modifiedTime;

    // GETTER AND SETTER METHODS
    /**
     * Gets the License Target Id The target Id.
     *
     * @return the target Id
     */
    public String getTargetId() {
        return this.targetId;
    }

    /**
     * Sets the License Target Id The target Id.
     *
     * @param targetId
     */
    public void setTargetId(final String targetId) {
        this.targetId = targetId;
    }

    /**
     * Gets the Encrypted Data which is formed by appending the all the Target System attributes and encrypted it.
     *
     * @return TargetSystem Signature
     */
    public byte[] getTargetSystemSignature() {
        return this.targetSystemSignature;
    }

    /**
     * Sets the Encrypted Data which is formed by appending the all the Target System attributes and encrypted it.
     *
     * @param targetSystemSignature
     */
    public void setTargetSystemSignature(final byte[] targetSystemSignature) {
        this.targetSystemSignature = targetSystemSignature;
    }

    public Date getModifiedTime() {
        return this.modifiedTime;
    }

    public void setModifiedTime(final Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.targetId == null) ? 0 : this.targetId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TargetSystem other = (TargetSystem) obj;
        if (this.targetId == null) {
            if (other.targetId != null) {
                return false;
            }
        } else if (!this.targetId.equals(other.targetId)) {
            return false;
        }
        return true;
    }
}
