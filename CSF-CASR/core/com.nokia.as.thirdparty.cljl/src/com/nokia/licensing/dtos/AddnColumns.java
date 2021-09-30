package com.nokia.licensing.dtos;

public abstract class AddnColumns {

    /**
     * The type of the license e.g. on/off license or capacity license Ex: "on/off" or "Capacity"
     */
    // TODO:licenseType will hold the number, 1 for NE and 2 for POOL license.
    public enum LicenseType {

        NE, POOL, NMS, FLOATING_POOL, FLOATING_NMS;

        public int returnIntValue() {
            if (this == NE) {
                return 1;
            } else if (this == POOL) {
                return 2;
            } else if (this == NMS) {
                return 3;
            } else if (this == FLOATING_POOL) {
                return 4;
            } else if (this == FLOATING_NMS) {
                return 5;
            }
            return 0;
        }

        public static LicenseType returnEnumValue(final int intValue) {
            if (intValue == 1) {
                return NE;
            } else if (intValue == 2) {
                return POOL;
            } else if (intValue == 3) {
                return NMS;
            } else if (intValue == 4) {
                return FLOATING_POOL;
            } else if (intValue == 5) {
                return FLOATING_NMS;
            }
            return null;
        }
    }

    /**
     * The type of the license e.g. on/off license or capacity license Ex: "on/off" or "Capacity"
     */
    public enum LicenseMode {

        ONOFF, CAPACITY;

        public int returnIntValue() {
            if (this == ONOFF) {
                return 1;
            } else if (this == CAPACITY) {
                return 2;
            }
            return 0;
        }

        public static LicenseMode returnEnumValue(final int intValue) {
            if (intValue == 1) {
                return ONOFF;
            } else if (intValue == 2) {
                return CAPACITY;
            }

            return null;
        }
    }
}
