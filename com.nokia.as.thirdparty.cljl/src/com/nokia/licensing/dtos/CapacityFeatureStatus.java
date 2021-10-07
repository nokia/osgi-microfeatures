/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */
package com.nokia.licensing.dtos;

/**
 * This class extends from the FeatureStatus and holds additional information required for Reserving License Capacity
 *
 * @version 1.0
 */
public class CapacityFeatureStatus extends FeatureStatus {
    private static final long serialVersionUID = 1L;
    // Start date from when the Capacity is available
    private java.util.Date startDate;
    // End date till when the capacity is available
    private java.util.Date endDate;
    // Reservervation ID of the Licenses which have been reserved by the applications
    private String reservationID;

    /**
     * Gets the Start Date Date from which Capacity is available for external clients
     *
     * @return the startDate
     */
    public java.util.Date getStartDate() {
        return this.startDate;
    }

    /**
     * Sets the Start Date Date from which Capacity is available for external clients
     *
     * @param startDate
     *            the startDate to set
     */
    public void setStartDate(final java.util.Date startDate) {
        this.startDate = startDate;
    }

    /**
     * Fetches the End Date Date from which Capacity ceases to exist for external clients
     *
     * @return the endDate
     */
    public java.util.Date getEndDate() {
        return this.endDate;
    }

    /**
     * Sets the End Date Date from which Capacity ceases to exist for external clients
     *
     * @param endDate
     *            the endDate to set
     */
    public void setEndDate(final java.util.Date endDate) {
        this.endDate = endDate;
    }

    /**
     * Fetches the reservation ID Reservation ID of the reserved Capacity
     *
     * @return the reservationID
     */
    public String getReservationID() {
        return this.reservationID;
    }

    /**
     * Sets the reservation ID Reserved ID of the reserved Capacity
     *
     * @param reservationID
     *            the reservationID to set
     */
    public void setReservationID(final String reservationID) {
        this.reservationID = reservationID;
    }

    /**
     * Compares if instances of 2 objects are equal by comparing their attributes. Here ReservationID attribute is
     * compared
     *
     * @param Instance
     *            of CapacityFeatureStatus object
     * @return boolean -- True if objects are equal, otherwise false
     */
    public boolean equals(final CapacityFeatureStatus o) {
        if (o instanceof CapacityFeatureStatus) {
            if (this.reservationID.equals(o.reservationID)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Returns the hashCode of the object. Here it considers hash code of reservationID attribute
     */
    @Override
    public int hashCode() {
        return this.reservationID.hashCode();
    }
}
