package com.nextenso.proxylet.diameter.util;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * This class wraps constants defined in RFC 3588.
 */
public class DiameterBaseConstants {

    /**
     * Constructor used by classes wishing to inherit the constants.
     */
    public DiameterBaseConstants (){}
    
    /**
     * Call this to ensure the class is loaded and the static declarations 
     * are called and inserted in the Dictionary
     */
    public static void init() {
    	
    }

    /**
     * The Common Messages Application Identifier.
     */
    public static final long APPLICATION_COMMON_MESSAGES = 0L;
    
    /**
     * The Base Accounting Application Identifier.
     */
    public static final long APPLICATION_BASE_ACCOUNTING = 3L;
    
    /**
     * The Relay Application Identifier.
     */
    public static final long APPLICATION_RELAY = 0xFFFFFFFFL;
    
    /**
     * The AVP Definition for ACCT_INTERIM_INTERVAL.
     */
    public static final DiameterAVPDefinition AVP_ACCT_INTERIM_INTERVAL = new DiameterAVPDefinition ("Acct-Interim-Interval", 85L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);
    
    /**
     * The AVP Definition for ACCOUNTING_REALTIME_REQUIRED.
     */
    public static final DiameterAVPDefinition AVP_ACCOUNTING_REALTIME_REQUIRED = new DiameterAVPDefinition ("Accounting-Realtime-Required", 483L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for ACCT_MULTI_SESSION_ID.
     */
    public static final DiameterAVPDefinition AVP_ACCT_MULTI_SESSION_ID = new DiameterAVPDefinition ("Acct-Multi-Session-Id", 50L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

    /**
     * The AVP Definition for ACCOUNTING_RECORD_NUMBER.
     */
    public static final DiameterAVPDefinition AVP_ACCOUNTING_RECORD_NUMBER = new DiameterAVPDefinition ("Accounting-Record-Number", 485L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for ACCOUNTING_RECORD_TYPE.
     */
    public static final DiameterAVPDefinition AVP_ACCOUNTING_RECORD_TYPE = new DiameterAVPDefinition ("Accounting-Record-Type", 480L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for ACCOUNTING_SESSION_ID.
     */
    public static final DiameterAVPDefinition AVP_ACCOUNTING_SESSION_ID = new DiameterAVPDefinition ("Accounting-Session-Id", 44L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

    /**
     * The AVP Definition for ACCOUNTING_SUB_SESSION_ID.
     */
    public static final DiameterAVPDefinition AVP_ACCOUNTING_SUB_SESSION_ID = new DiameterAVPDefinition ("Accounting-Sub-Session-Id", 287L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned64Format.INSTANCE);

    /**
     * The AVP Definition for ACCT_APPLICATION_ID.
     */
    public static final DiameterAVPDefinition AVP_ACCT_APPLICATION_ID = new DiameterAVPDefinition ("Acct-Application-Id", 259L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for AUTH_APPLICATION_ID.
     */
    public static final DiameterAVPDefinition AVP_AUTH_APPLICATION_ID = new DiameterAVPDefinition ("Auth-Application-Id", 258L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for AUTH_REQUEST_TYPE.
     */
    public static final DiameterAVPDefinition AVP_AUTH_REQUEST_TYPE = new DiameterAVPDefinition ("Auth-Request-Type", 274L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for AUTHORIZATION_LIFETIME.
     */
    public static final DiameterAVPDefinition AVP_AUTHORIZATION_LIFETIME = new DiameterAVPDefinition ("Authorization-Lifetime", 291L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for AUTH_GRACE_PERIOD.
     */
    public static final DiameterAVPDefinition AVP_AUTH_GRACE_PERIOD = new DiameterAVPDefinition ("Auth-Grace-Period", 276L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for AUTH_SESSION_STATE.
     */
    public static final DiameterAVPDefinition AVP_AUTH_SESSION_STATE = new DiameterAVPDefinition ("Auth-Session-State", 277L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for RE_AUTH_REQUEST_TYPE.
     */
    public static final DiameterAVPDefinition AVP_RE_AUTH_REQUEST_TYPE = new DiameterAVPDefinition ("Re-Auth-Request-Type", 285L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for CLASS.
     */
    public static final DiameterAVPDefinition AVP_CLASS = new DiameterAVPDefinition ("Class", 25L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, OctetStringFormat.INSTANCE);

    /**
     * The AVP Definition for DESTINATION_HOST.
     */
    public static final DiameterAVPDefinition AVP_DESTINATION_HOST = new DiameterAVPDefinition ("Destination-Host", 293L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for DESTINATION_REALM.
     */
    public static final DiameterAVPDefinition AVP_DESTINATION_REALM = new DiameterAVPDefinition ("Destination-Realm", 283L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for DISCONNECT_CAUSE.
     */
    public static final DiameterAVPDefinition AVP_DISCONNECT_CAUSE = new DiameterAVPDefinition ("Disconnect-Cause", 273L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE);
    
    /**
     * The AVP Definition for ERROR_MESSAGE.
     */
    public static final DiameterAVPDefinition AVP_ERROR_MESSAGE = new DiameterAVPDefinition ("Error-Message", 281L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, UTF8StringFormat.INSTANCE);

    /**
     * The AVP Definition for ERROR_REPORTING_HOST.
     */
    public static final DiameterAVPDefinition AVP_ERROR_REPORTING_HOST = new DiameterAVPDefinition ("Error-Reporting-Host", 294L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for EVENT_TIMESTAMP.
     */
    public static final DiameterAVPDefinition AVP_EVENT_TIMESTAMP = new DiameterAVPDefinition ("Event-Timestamp", 55L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, TimeFormat.INSTANCE);

    /**
     * The AVP Definition for EXPERIMENTAL_RESULT_CODE.
     */
    public static final DiameterAVPDefinition AVP_EXPERIMENTAL_RESULT_CODE = new DiameterAVPDefinition ("Experimental-Result-Code", 298L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for FIRMWARE_REVISION.
     */
    public static final DiameterAVPDefinition AVP_FIRMWARE_REVISION = new DiameterAVPDefinition ("Firmware-Revision", 267L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for HOST_IP_ADDRESS.
     */
    public static final DiameterAVPDefinition AVP_HOST_IP_ADDRESS = new DiameterAVPDefinition ("Host-IP-Address", 257L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, AddressFormat.INSTANCE);

    /**
     * The AVP Definition for INBAND_SECURITY_ID.
     */
    public static final DiameterAVPDefinition AVP_INBAND_SECURITY_ID = new DiameterAVPDefinition ("Inband-Security-Id", 299L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for MULTI_ROUND_TIME_OUT.
     */
    public static final DiameterAVPDefinition AVP_MULTI_ROUND_TIME_OUT = new DiameterAVPDefinition ("Multi-Round-Time-Out", 272L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for ORIGIN_HOST.
     */
    public static final DiameterAVPDefinition AVP_ORIGIN_HOST = new DiameterAVPDefinition ("Origin-Host", 264L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for ORIGIN_REALM.
     */
    public static final DiameterAVPDefinition AVP_ORIGIN_REALM = new DiameterAVPDefinition ("Origin-Realm", 296L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for ORIGIN_STATE_ID.
     */
    public static final DiameterAVPDefinition AVP_ORIGIN_STATE_ID = new DiameterAVPDefinition ("Origin-State-Id", 278L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for PRODUCT_NAME.
     */
    public static final DiameterAVPDefinition AVP_PRODUCT_NAME = new DiameterAVPDefinition ("Product-Name", 269L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, UTF8StringFormat.INSTANCE);

    /**
     * The AVP Definition for PROXY_HOST.
     */
    public static final DiameterAVPDefinition AVP_PROXY_HOST = new DiameterAVPDefinition ("Proxy-Host", 280L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for PROXY_STATE.
     */
    public static final DiameterAVPDefinition AVP_PROXY_STATE = new DiameterAVPDefinition ("Proxy-State", 33L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, OctetStringFormat.INSTANCE);

    /**
     * The AVP Definition for REDIRECT_HOST.
     */
    public static final DiameterAVPDefinition AVP_REDIRECT_HOST = new DiameterAVPDefinition ("Redirect-Host", 292L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for REDIRECT_HOST_USAGE.
     */
    public static final DiameterAVPDefinition AVP_REDIRECT_HOST_USAGE = new DiameterAVPDefinition ("Redirect-Host-Usage", 261L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for REDIRECT_MAX_CACHE_TIME.
     */
    public static final DiameterAVPDefinition AVP_REDIRECT_MAX_CACHE_TIME = new DiameterAVPDefinition ("Redirect-Max-Cache-Time", 262L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for RESULT_CODE.
     */
    public static final DiameterAVPDefinition AVP_RESULT_CODE = new DiameterAVPDefinition ("Result-Code", 268L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for ROUTE_RECORD.
     */
    public static final DiameterAVPDefinition AVP_ROUTE_RECORD = new DiameterAVPDefinition ("Route-Record", 282L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, IdentityFormat.INSTANCE);

    /**
     * The AVP Definition for SESSION_ID.
     */
    public static final DiameterAVPDefinition AVP_SESSION_ID = new DiameterAVPDefinition ("Session-Id", 263L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

    /**
     * The AVP Definition for SESSION_TIMEOUT.
     */
    public static final DiameterAVPDefinition AVP_SESSION_TIMEOUT = new DiameterAVPDefinition ("Session-Timeout", 27L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for SESSION_BINDING.
     */
    public static final DiameterAVPDefinition AVP_SESSION_BINDING = new DiameterAVPDefinition ("Session-Binding", 270L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for SESSION_SERVER_FAILOVER.
     */
    public static final DiameterAVPDefinition AVP_SESSION_SERVER_FAILOVER = new DiameterAVPDefinition ("Session-Server-Failover", 271L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for SUPPORTED_VENDOR_ID.
     */
    public static final DiameterAVPDefinition AVP_SUPPORTED_VENDOR_ID = new DiameterAVPDefinition ("Supported-Vendor-Id", 265L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);

    /**
     * The AVP Definition for TERMINATION_CAUSE.
     */
    public static final DiameterAVPDefinition AVP_TERMINATION_CAUSE = new DiameterAVPDefinition ("Termination-Cause", 295L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, EnumeratedFormat.INSTANCE);

    /**
     * The AVP Definition for USER_NAME.
     */
    public static final DiameterAVPDefinition AVP_USER_NAME = new DiameterAVPDefinition ("User-Name", 1L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, UTF8StringFormat.INSTANCE);

    /**
     * The AVP Definition for VENDOR_ID.
     */
    public static final DiameterAVPDefinition AVP_VENDOR_ID = new DiameterAVPDefinition ("Vendor-Id", 266L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);
    
    //public static final DiameterAVPDefinitioAVP_n E2E_SEQUENCE = new DiameterAVPDefinition ("E2E-Sequence", 300L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, true, new GroupedFormat (new DiameterAVPConstraint[]{}));

    /**
     * The AVP Definition for EXPERIMENTAL_RESULT.
     */
    public static final DiameterAVPDefinition AVP_EXPERIMENTAL_RESULT = new DiameterAVPDefinition ("Experimental-Result", 297L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE);

    /**
     * The AVP Definition for FAILED_AVP.
     */
    public static final DiameterAVPDefinition AVP_FAILED_AVP = new DiameterAVPDefinition ("Failed-AVP", 279L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE);

    /**
     * The AVP Definition for PROXY_INFO.
     */
    public static final DiameterAVPDefinition AVP_PROXY_INFO = new DiameterAVPDefinition ("Proxy-Info", 284L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, GroupedFormat.INSTANCE);

    // Error in RFC !!!
    /**
     * The AVP Definition for VENDOR_SPECIFIC_APPLICATION_ID.
     */
    public static final DiameterAVPDefinition AVP_VENDOR_SPECIFIC_APPLICATION_ID = new DiameterAVPDefinition ("Vendor-Specific-Application-Id", 260L, 0L, DiameterAVPDefinition.FORBIDDEN_FLAG, DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, GroupedFormat.INSTANCE);

    /**
     * The Command Code for CER.
     */
    public static final int COMMAND_CER = 257;
    
    /**
     * The Command Code for CEA.
     */
    public static final int COMMAND_CEA = COMMAND_CER;
    
    /**
     * The Command Code for DPR.
     */
    public static final int COMMAND_DPR = 282;

    /**
     * The Command Code for DPA.
     */
    public static final int COMMAND_DPA = COMMAND_DPR;

    /**
     * The Command Code for DWR.
     */
    public static final int COMMAND_DWR = 280;

    /**
     * The Command Code for DWA.
     */
    public static final int COMMAND_DWA = COMMAND_DWR;

    /**
     * The Command Code for RAR.
     */
    public static final int COMMAND_RAR = 258;
    
    /**
     * The Command Code for RAA.
     */
    public static final int COMMAND_RAA = COMMAND_RAR;

    /**
     * The Command Code for STR.
     */
    public static final int COMMAND_STR = 275;

    /**
     * The Command Code for STA.
     */
    public static final int COMMAND_STA = COMMAND_STR;

    /**
     * The Command Code for ASR.
     */
    public static final int COMMAND_ASR = 274;

    /**
     * The Command Code for ASA.
     */
    public static final int COMMAND_ASA = COMMAND_ASR;

    /**
     * The Command Code for ACR.
     */
    public static final int COMMAND_ACR = 271;

    /**
     * The Command Code for ACA.
     */
    public static final int COMMAND_ACA = COMMAND_ACR;
    
    /**
     * The disconnect cause for Rebooting.
     */
    public static final int VALUE_DISCONNECT_CAUSE_REBOOTING = 0;
    
    /**
     * The disconnect cause for Busy.
     */
    public static final int VALUE_DISCONNECT_CAUSE_BUSY = 1;

    /**
     * The disconnect cause for Do Not Want To Talk To You.
     */
    public static final int VALUE_DISCONNECT_CAUSE_DO_NOT_WANT_TO_TALK_TO_YOU = 2;

    /**
     * The result code for MULTI_ROUND_AUTH.
     */
    public static final long RESULT_CODE_DIAMETER_MULTI_ROUND_AUTH = 1001L;

    /**
     * The result code for SUCCESS.
     */
    public static final long RESULT_CODE_DIAMETER_SUCCESS = 2001L;

    /**
     * The result code for LIMITED_SUCCESS.
     */
    public static final long RESULT_CODE_DIAMETER_LIMITED_SUCCESS = 2002L;

    /**
     * The result code for COMMAND_UNSUPPORTED.
     */
    public static final long RESULT_CODE_DIAMETER_COMMAND_UNSUPPORTED = 3001L;

    /**
     * The result code for UNABLE_TO_DELIVER.
     */
    public static final long RESULT_CODE_DIAMETER_UNABLE_TO_DELIVER = 3002L;

    /**
     * The result code for REALM_NOT_SERVED.
     */
    public static final long RESULT_CODE_DIAMETER_REALM_NOT_SERVED = 3003L;

    /**
     * The result code for TOO_BUSY.
     */
    public static final long RESULT_CODE_DIAMETER_TOO_BUSY = 3004L;

    /**
     * The result code for LOOP_DETECTED.
     */
    public static final long RESULT_CODE_DIAMETER_LOOP_DETECTED = 3005L;

    /**
     * The result code for REDIRECT_INDICATION.
     */
    public static final long RESULT_CODE_DIAMETER_REDIRECT_INDICATION = 3006L;

    /**
     * The result code for APPLICATION_UNSUPPORTED.
     */
    public static final long RESULT_CODE_DIAMETER_APPLICATION_UNSUPPORTED = 3007L;

    /**
     * The result code for INVALID_HDR_BITS.
     */
    public static final long RESULT_CODE_DIAMETER_INVALID_HDR_BITS = 3008L;

    /**
     * The result code for INVALID_AVP_BITS.
     */
    public static final long RESULT_CODE_DIAMETER_INVALID_AVP_BITS = 3009L;

    /**
     * The result code for UNKNOWN_PEER.
     */
    public static final long RESULT_CODE_DIAMETER_UNKNOWN_PEER = 3010L;
    
    /**
     * The result code for AUTHENTICATION_REJECTED.
     */
    public static final long RESULT_CODE_DIAMETER_AUTHENTICATION_REJECTED = 4001L;

    /**
     * The result code for OUT_OF_SPACE.
     */
    public static final long RESULT_CODE_DIAMETER_OUT_OF_SPACE = 4002L;

    /**
     * The result code for LOST.
     */
    public static final long RESULT_CODE_ELECTION_LOST = 4003L;

    /**
     * The result code for AVP_UNSUPPORTED.
     */
    public static final long RESULT_CODE_DIAMETER_AVP_UNSUPPORTED = 5001L;

    /**
     * The result code for UNKNOWN_SESSION_ID.
     */
    public static final long RESULT_CODE_DIAMETER_UNKNOWN_SESSION_ID = 5002L;

    /**
     * The result code for AUTHORIZATION_REJECTED.
     */
    public static final long RESULT_CODE_DIAMETER_AUTHORIZATION_REJECTED = 5003L;

    /**
     * The result code for INVALID_AVP_VALUE.
     */
    public static final long RESULT_CODE_DIAMETER_INVALID_AVP_VALUE = 5004L;

    /**
     * The result code for MISSING_AVP.
     */
    public static final long RESULT_CODE_DIAMETER_MISSING_AVP = 5005L;

    /**
     * The result code for RESOURCES_EXCEEDED.
     */
    public static final long RESULT_CODE_DIAMETER_RESOURCES_EXCEEDED = 5006L;

    /**
     * The result code for CONTRADICTING_AVPS.
     */
    public static final long RESULT_CODE_DIAMETER_CONTRADICTING_AVPS = 5007L;

    /**
     * The result code for AVP_NOT_ALLOWED.
     */
    public static final long RESULT_CODE_DIAMETER_AVP_NOT_ALLOWED = 5008L;

    /**
     * The result code for AVP_OCCURS_TOO_MANY_TIMES.
     */
    public static final long RESULT_CODE_DIAMETER_AVP_OCCURS_TOO_MANY_TIMES = 5009L;

    /**
     * The result code for NO_COMMON_APPLICATION.
     */
    public static final long RESULT_CODE_DIAMETER_NO_COMMON_APPLICATION = 5010L;

    /**
     * The result code for UNSUPPORTED_VERSION.
     */
    public static final long RESULT_CODE_DIAMETER_UNSUPPORTED_VERSION = 5011L;

    /**
     * The result code for UNABLE_TO_COMPLY.
     */
    public static final long RESULT_CODE_DIAMETER_UNABLE_TO_COMPLY = 5012L;

    /**
     * The result code for INVALID_BIT_IN_HEADER.
     */
    public static final long RESULT_CODE_DIAMETER_INVALID_BIT_IN_HEADER = 5013L;

    /**
     * The result code for INVALID_AVP_LENGTH.
     */
    public static final long RESULT_CODE_DIAMETER_INVALID_AVP_LENGTH = 5014L;

    /**
     * The result code for INVALID_MESSAGE_LENGTH.
     */
    public static final long RESULT_CODE_DIAMETER_INVALID_MESSAGE_LENGTH = 5015L;

    /**
     * The result code for INVALID_AVP_BIT_COMBO.
     */
    public static final long RESULT_CODE_DIAMETER_INVALID_AVP_BIT_COMBO = 5016L;

    /**
     * The result code for NO_COMMON_SECURITY.
     */
    public static final long RESULT_CODE_DIAMETER_NO_COMMON_SECURITY = 5017L;
}
