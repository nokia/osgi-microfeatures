// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.radius;

/**
 * This class encapsulates an Extended Vendor Specific Attribute (See RFC 6929 section
 * 2.4).
 */
public class ExtendedVendorSpecificAttribute extends ExtendedTypeAttribute {

    private int _vendorId;
    private int _evsType;

    public ExtendedVendorSpecificAttribute (int type, int vendorId, int evsType){
	super (type, RadiusUtils.VENDOR_SPECIFIC);
	switch (type){
	case Extended_Type_1:
	case Extended_Type_2:
	case Extended_Type_3:
	case Extended_Type_4:
	case Long_Extended_Type_1:
	case Long_Extended_Type_2:
	    break;
	default: throw new IllegalArgumentException ("Illegal type for ExtendedVendorSpecificAttribute : "+type);
	}
	_vendorId = vendorId;
	_evsType = evsType;
    }

    /**
     * Instanciates a new ExtendedVendorSpecificAttribute of type Extended-Vendor-Specific-1
     * @param vendorId the vendorId
     * @param evsType the EVS Type
     * @return the instanciated ExtendedVendorSpecificAttribute
     */
    public static ExtendedVendorSpecificAttribute newExtendedVendorSpecificAttribute1 (int vendorId, int evsType){
	return new ExtendedVendorSpecificAttribute (Extended_Type_1, vendorId, evsType);
    }
    /**
     * Instanciates a new ExtendedVendorSpecificAttribute of type Extended-Vendor-Specific-2
     * @param vendorId the vendorId
     * @param evsType the EVS Type
     * @return the instanciated ExtendedVendorSpecificAttribute
     */
    public static ExtendedVendorSpecificAttribute newExtendedVendorSpecificAttribute2 (int vendorId, int evsType){
	return new ExtendedVendorSpecificAttribute (Extended_Type_2, vendorId, evsType);
    }
    /**
     * Instanciates a new ExtendedVendorSpecificAttribute of type Extended-Vendor-Specific-3
     * @param vendorId the vendorId
     * @param evsType the EVS Type
     * @return the instanciated ExtendedVendorSpecificAttribute
     */
    public static ExtendedVendorSpecificAttribute newExtendedVendorSpecificAttribute3 (int vendorId, int evsType){
	return new ExtendedVendorSpecificAttribute (Extended_Type_3, vendorId, evsType);
    }
    /**
     * Instanciates a new ExtendedVendorSpecificAttribute of type Extended-Vendor-Specific-4
     * @param vendorId the vendorId
     * @param evsType the EVS Type
     * @return the instanciated ExtendedVendorSpecificAttribute
     */
    public static ExtendedVendorSpecificAttribute newExtendedVendorSpecificAttribute4 (int vendorId, int evsType){
	return new ExtendedVendorSpecificAttribute (Extended_Type_4, vendorId, evsType);
    }
    /**
     * Instanciates a new ExtendedVendorSpecificAttribute of type Extended-Vendor-Specific-5
     * @param vendorId the vendorId
     * @param evsType the EVS Type
     * @return the instanciated ExtendedVendorSpecificAttribute
     */
    public static ExtendedVendorSpecificAttribute newExtendedVendorSpecificAttribute5 (int vendorId, int evsType){
	return new ExtendedVendorSpecificAttribute (Long_Extended_Type_1, vendorId, evsType);
    }
    /**
     * Instanciates a new ExtendedVendorSpecificAttribute of type Extended-Vendor-Specific-6
     * @param vendorId the vendorId
     * @param evsType the EVS Type
     * @return the instanciated ExtendedVendorSpecificAttribute
     */
    public static ExtendedVendorSpecificAttribute newExtendedVendorSpecificAttribute6 (int vendorId, int evsType){
	return new ExtendedVendorSpecificAttribute (Long_Extended_Type_2, vendorId, evsType);
    }

    /**
     * Returns the VendorId
     * @return the vendorId
     */
    public int getVendorId (){ return _vendorId;}
    /**
     * Returns the EVS Type
     * @return the EVS Type
     */
    public int getEVSType (){ return _evsType;}
    
    /**
     * @see com.nextenso.proxylet.radius.RadiusAttribute#toString()
     */
    @Override
    public String toString() {
	StringBuilder buff = new StringBuilder(super.toString());
	buff.append(" [vendor-id=");
	buff.append(String.valueOf(_vendorId));
	buff.append(']');
	buff.append(" [EVS-Type=");
	buff.append(String.valueOf(_evsType));
	buff.append(']');
	return buff.toString();
    }
}
