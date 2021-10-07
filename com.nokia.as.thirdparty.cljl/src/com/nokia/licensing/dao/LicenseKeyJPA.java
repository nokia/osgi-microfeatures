/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


/**
 * Data Holder class which holds information about License SerialNumber and LicenseKey.
 *
 * @author Rama Manohar P
 * @version 1.0
 */
@Entity
@Table(name = "LICENSEKEY", schema = "LICENSE")
public class LicenseKeyJPA {

    /**
     *
     */
    @Column(name = "KEY")
    private byte[] key;

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
     * @return
     */
    public byte[] getKey() {
        return this.key;
    }

    /**
     * @param key
     */
    public void setKey(final byte[] key) {
        this.key = key;
    }
}
