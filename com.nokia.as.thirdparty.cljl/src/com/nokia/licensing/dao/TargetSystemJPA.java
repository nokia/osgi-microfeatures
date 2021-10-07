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
 * Data Holder class which holds information about License Target Id.
 *
 * @author Rama Manohar P
 * @version 1.0
 */
@Entity
@Table(name = "TARGETSYSTEM", schema = "LICENSE")
public class TargetSystemJPA implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     *
     */
    @Id
    @SequenceGenerator(name = "TargetSystemSeq", sequenceName = "TARGETSYSTEM_ID_SEQ")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "TargetSystemSeq")
    private int id;
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

    /**
     * The target id of the client.
     */
    @Column(name = "TARGETID")
    private String targetId;

    /**
     * Contains encrypted data for the string which is formed by appending the all the Target System attributes
     */
    @Column(name = "TARGETSYSTEMSIGNATURE")
    private byte[] targetSystemSignature;

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
}
