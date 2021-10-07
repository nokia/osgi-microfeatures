/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.dtos;

import java.io.Serializable;
import java.util.Date;


/**
 * Data Holder class which holds information about License feature code and feature name.
 *
 * @version 1.0
 */
public class FeatureInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * FeatureCode is a unique identifier that identifies a licensed feature. Ex: 1234567890
     */
    private long featureCode;
    /**
     * The feature name. Ex: "My Feature"
     */
    private String featureName;
    /**
     * Contains encrypted data for the string which is formed by appending the all the Feature Info attributes
     */
    private byte[] featureInfoSignature;
    /**
     * Modified Time of the license (NSN Standard LKG fills in time using value as "YYYY-MM-DDT23:59:59") Ex:
     * 2005-03-24T23:59:59
     */
    private Date modifiedTime;

    // GETTER AND SETTER METHODS
    /**
     * Gets the Feature code of the License FeatureCode is a unique identifier that identifies a licensed feature. Ex:
     * 1234567890
     *
     * @return the featureCode
     */
    public long getFeatureCode() {
        return this.featureCode;
    }

    /**
     * Sets the License Feature Code FeatureCode is a unique identifier that identifies a licensed feature. Ex:
     * 1234567890
     *
     * @param featureCode
     *            the featureCode to set
     */
    public void setFeatureCode(final long featureCode) {
        this.featureCode = featureCode;
    }

    /**
     * Gets the License Feature Name The feature name. Ex: "My Feature"
     *
     * @return the featureName
     */
    public String getFeatureName() {
        return this.featureName;
    }

    /**
     * Sets the License Feature Name The feature name. Ex: "My Feature"
     *
     * @param featureName
     *            the featureName to set
     */
    public void setFeatureName(final String featureName) {
        this.featureName = featureName;
    }

    /**
     * Gets the Encrypted Data which is formed by appending the all the Feature Info attributes and encrypted it.
     *
     * @return featureInfo Signature
     */
    public byte[] getFeatureInfoSignature() {
        return this.featureInfoSignature;
    }

    /**
     * Sets the Encrypted Data which is formed by appending the all the Feature Info attributes and encrypted it.
     *
     * @param featureInfo
     *            Signature
     */
    public void setFeatureInfoSignature(final byte[] featureInfoSignature) {
        this.featureInfoSignature = featureInfoSignature;
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
        result = prime * result + (int) (this.featureCode ^ (this.featureCode >>> 32));
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
        final FeatureInfo other = (FeatureInfo) obj;
        if (this.featureCode != other.featureCode) {
            return false;
        }
        return true;
    }
}
