// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//


/**
 *
 */
package com.nokia.licensing.utils;

/**
 * @author chhgupta
 * @version 1.0
 *
 */
public class LicenseConstants {
    public static String ADDITIONALINFO = "additionalInfo";
    public static String CAPACITYUNIT = "capacityUnit";
    public static String CUSTOMER = "customer";
    public static String CUSTOMERID = "customerId";
    public static String CUSTOMERNAME = "customerName";
    public static String DELETELICENSEBASEDONSERIALNUMBER = "deleteLicenseBasedOnSerialNumber";
    public static String DELETELICENSEBASEDONSERIALNUMBERFEATURECODE = "deleteLicenseBasedOnSerialNumberAndFeatureCode";
    public static String DELETELICENSEKEYBYSERALNUMBER = "deleteLicenseKeyBySerialNumbe";
    public static String ENDTIME = "endTime";
    public static String FEATURECODE = "featureCode";
    public static String FEATUREDATA = "featureData";
    public static String FEATUREINFO = "featureInfo";
    public static String FEATURENAME = "featureName";
    public static String INSERTCANCELINFORMATION = "insertCancelInformation";
    public static String INSERTFEATUREINFO = "insertFeatureInfo";
    public static String INSERTLICENSE = "insertLicense";
    public static String INSERTLICENSEKEY = "insertLicenseKey";
    public static String INSERTTARGETSSYSTEM = "insertTargetsSystem";
    public static String LICENSECODE = "licenceCode";
    public static String LICENSEDATA = "LicenceData";
    public static String LICENSEINFO = "licenceInfo";
    public static String LICENSEMODE = "licenceMode";
    public static String LICENSENAME = "licenceName";
    public static String LICENSETYPE = "licenceType";
    public static String LICENSE_FILE_IMPORT_TIME = "licensesByLicenseFileImportTime";
    public static String LICENSE_FILE_IMPORT_USER = "licensesByLicenseFileImportUser";
    public static String MAXVALUE = "maxValue";
    public static String OBJECTLIMIT = "objectLimit";
    public static String ORDER = "order";
    public static String ORDERID = "orderId";
    public static String SELECTALLCANCELINFOS = "selectAllCancelInfos";
    public static String SELECTALLCANCELINFOSBYCANCELDATE = "selectAllCancelInfosByCancelDate";

    // Logging Constants

    public static String SELECTALLLICENSES = "selectAllLicenses";
    public static String SELECTCANCELINFOBYSERIALNUMBER = "selectCancelInfoBySerialNumber";
    public static String SELECTFEATUREINFOBYSERIALNUMBER = "selectFeatureInfoBySerialNumber";
    public static String SELECTFEATUREINFOBYSERIALNUMBERANDFEATURECODE = "selectFeatureInfoBySerialNumberAndFeatureCode";
    public static String SELECTFEATUREINFOBYSERIALNUMBERANDFEATURENAME = "selectFeatureInfoBySerialNumberAndFeatureName";
    public static String SELECTLICENSEBYFEATURECODE = "selectLicenseByFeatureCode";
    public static String SELECTLICENSEBYSERIALNUMBER = "selectLicenseBySerialNumber";
    public static String SELECTLICENSEGETTINGEXPIRED = "selectLicenseGettingExpired";
    public static String SELECTLICENSEKEYBYSERIALNUMBER = "selectLicenseKeyBySerialNumber";
    public static String SELECTLICENSEMODIFIED = "selectLicenseModified";
    public static String SELECTLICENSESBYCUSTOMERID = "selectLicensesByCustomerID";
    public static String SELECTLICENSESBYCUSTOMERNAME = "selectLicensesByCustomerName";
    public static String SELECTLICENSESBYFEATURECODEANDTARGETIDANDTIME = "selectLicensesByFeatureCodeAndTargetIdAndTime";
    public static String SELECTLICENSESBYFEATURECODEANDTARGETIDANDTIME_NO_TARGETID = "selectLicensesByFeatureCodeAndTargetIdAndTimeNoTargetId";
    public static String SELECTLICENSESBYFEATURENAME = "selectLicensesByFeatureName";
    public static String SELECTLICENSESBYFILENAME = "selectLicensesByFileName";
    public static String SELECTLICENSESBYLICENSECODE = "selectLicensesByLicenseCode";

    // /newly added columns
    public static String SELECTLICENSESBYLICENSEMODE = "selectLicensesByLicenseMode";
    public static String SELECTLICENSESBYLICENSETYPE = "selectLicensesByLicenseType";
    public static String SELECTLICENSESBYLICENSE_FILE_IMPORT_TIME = "selectLicensesByLicenseFileImportTime";
    public static String SELECTLICENSESBYLICENSE_FILE_IMPORT_USER = "selectLicensesByLicenseFileImportUser";
    public static String SELECTLICENSESBYNAME = "selectLicensesByName";
    public static String SELECTLICENSESBYORDERID = "selectLicensesByOrderID";
    public static String SELECTLICENSESBYSWBASERELEASE = "selectLicensesBySWBaseRelease";
    public static String SELECTLICENSESBYSWRELEASERELATION = "selectLicensesBySWReleaseRelation";
    public static String SELECTLICENSESBYTARGETID = "selectLicensesByTargetID";
    public static String SELECTLICENSESBYTARGETTYPE = "selectLicensesByTargetType";
    public static String SELECTLICENSESBYUSAGETYPE = "selectLicensesByUsageType";
    public static String SELECTTARGETSYSTEMBYSERIALNUMBER = "selectTargetSystemByserialNumber";
    public static String SELECTTARGETSYSTEMBYSERIALNUMBERANDTARGETID = "selectTargetSystemByserialNumberAndTargetId";
    public static String SERIAL = "serial";
    public static String SERIALNBR = "serialNbr";
    public static String SERVICE_PROPERTIES = "servicelocator.properties";
    public static String STARTTIME = "startTime";
    public static String SUPPLEMENTARYINFO = "supplementaryInfo";
    public static String TARGET = "target";
    public static String TARGETID = "targetId";
    public static String TARGETNE = "targetNe";
    public static String TARGETNETYPE = "targetNeType";
    public static String THRESHOLD = "threshold";
    public static String TIMELIMIT = "timeLimit";
    public static String UPDATELICENSEKEYBASEDONSERIALNUMBER = "updateLicenseKeyBasedOnSerialNumber";
    public static String USAGETYPE = "usageType";

    /**
     * Debug mode. If true, those log messages are logged which level is higher than specified with the constant
     * 'LOGGING_LEVEL'. If false, the constant 'LOGGING_LEVEL' is ignored and some default logging level is used.
     */
    public static final boolean IS_DEBUG_MODE = false;
    public static final String SELECTLICENSESBY_MANY_FEATURECODEANDTARGETIDANDTIME_OPTIMIZED_WITHOUT_TARGET = "selectLicensesByManyFeatureCodeAndTargetIdAndTimeOptimizedWithoutTarget";
    public static final String SELECTLICENSESBY_MANY_FEATURECODEANDTARGETIDANDTIME_OPTIMIZED = "selectLicensesByManyFeatureCodeAndTargetIdAndTimeOptimized";

    /** The level which errors are logged */

    // public static final java.util.logging.Level LOGGING_LEVEL = Level.ALL;
}
