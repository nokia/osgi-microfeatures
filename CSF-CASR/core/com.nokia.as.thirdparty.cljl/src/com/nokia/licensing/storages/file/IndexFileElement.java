package com.nokia.licensing.storages.file;

/**
 * @author ajnn
 */
public class IndexFileElement {
    private String customerID;
    private String featureCode;
    private String fileName;
    private String serialNumber;
    private String status;

    /**
     * @param customerID
     * @param serialNumber
     * @param fileName
     * @param status
     * @param featureCode
     */
    public IndexFileElement(final String customerID, final String serialNumber, final String fileName,
            final String status,
            final String featureCode) {
        this.customerID = customerID;
        this.serialNumber = serialNumber;
        this.fileName = fileName;
        this.status = status;
        this.featureCode = featureCode;
    }

    /**
     * @return the customerID
     */
    public String getCustomerID() {
        return this.customerID;
    }

    /**
     * @param customerID
     *            the customerID to set
     */
    public void setCustomerID(final String customerID) {
        this.customerID = customerID;
    }

    /**
     * @return the serialNumber
     */
    public String getSerialNumber() {
        return this.serialNumber;
    }

    /**
     * @param serialNumber
     *            the serialNumber to set
     */
    public void setSerialNumber(final String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * @return the fileName
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * @param fileName
     *            the fileName to set
     */
    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return this.status;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     * @return the featureCode
     */
    public String getFeatureCode() {
        return this.featureCode;
    }

    /**
     * @param featureCode
     *            the featureCode to set
     */
    public void setFeatureCode(final String featureCode) {
        this.featureCode = featureCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof IndexFileElement) {
            if (this.hashCode() == obj.hashCode()) {
                return true;
            }
        }

        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return this.featureCode.hashCode() + this.fileName.hashCode() + this.serialNumber.hashCode();
    }
}
