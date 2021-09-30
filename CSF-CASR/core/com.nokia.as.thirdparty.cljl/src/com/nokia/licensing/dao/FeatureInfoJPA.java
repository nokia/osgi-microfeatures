/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.dao;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;


/**
 * Data Holder class which holds information about License feature code and feature name.
 *
 * @author Rama Manohar P
 * @version 1.0
 */
@Entity
@Table(name = "FEATUREINFO", schema = "LICENSE")
public class FeatureInfoJPA implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * FeatureCode is a unique identifier that identifies a licensed feature. Ex: 1234567890
     */
    @Column(name = "FEATURECODE")
    private long featureCode;

    /**
     * Contains encrypted data for the string which is formed by appending the all the Feature Info attributes
     */
    @Column(name = "FEATUREINFOSIGNATURE")
    private byte[] featureInfoSignature;

    /**
     * The feature name. Ex: "My Feature"
     */
    @Column(name = "FEATURENAME")
    private String featureName;

    /**
     *
     */
    @Id
    @SequenceGenerator(name = "FeatureInfoSeq", sequenceName = "FEATUREINFO_ID_SEQ")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "FeatureInfoSeq")
    private int id;

    /**
     * Modified Time of the license (NSN Standard LKG fills in time using value as "YYYY-MM-DDT23:59:59") Ex:
     * 2005-03-24T23:59:59
     */
    @Column(name = "MODIFIEDTIME")
    private Date modifiedTime;

    /**
     * Identifies the license file Serial number format: PPPPYYWKNNNNN PPPP = SAP R/3 plant identification. YY = year WK
     * = number of the week NNNNN = random number Note: Several instances of same license files may be result of
     * flexible target allocation. In NMS generated licenses following format is used: PPPAYY0NNNNNN PPP = NMS A =
     * running letter stands for NMS cluster (as in NMS generated license file name) YY = year 0 = reserved for future
     * NNNNNN = running number
     */
    @Column(name = "SERIALNUMBER")
    private String serialNbr;

    // GETTER AND SETTER METHODS

    /**
     * @return
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
}
