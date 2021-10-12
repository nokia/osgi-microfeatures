// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.parser;

public class HttpStatuses {

    private static final String[] _reasonsByCode = new String[600];

    private static final String register (int code, String reason){
	return _reasonsByCode[code] = reason;
    }

    // 1×× Informational
    public static final String REASON_100 = register (100, "Continue");
    public static final String REASON_101 = register (101, "Switching Protocols");
    public static final String REASON_102 = register (102, "Processing");
    public static final String REASON_103 = register (103, "Early Hints");
    
    //2×× Success
    public static final String REASON_200 = register (200, "OK");
    public static final String REASON_201 = register (201, "Created");
    public static final String REASON_202 = register (202, "Accepted");
    public static final String REASON_203 = register (203, "Non-authoritative Information");
    public static final String REASON_204 = register (204, "No Content");
    public static final String REASON_205 = register (205, "Reset Content");
    public static final String REASON_206 = register (206, "Partial Content");
    public static final String REASON_207 = register (207, "Multi-Status");
    public static final String REASON_208 = register (208, "Already Reported");
    public static final String REASON_226 = register (226, "IM Used");
    
    //3×× Redirection
    public static final String REASON_300 = register (300, "Multiple Choices");
    public static final String REASON_301 = register (301, "Moved Permanently");
    public static final String REASON_302 = register (302, "Found");
    public static final String REASON_303 = register (303, "See Other");
    public static final String REASON_304 = register (304, "Not Modified");
    public static final String REASON_305 = register (305, "Use Proxy");
    public static final String REASON_307 = register (307, "Temporary Redirect");
    public static final String REASON_308 = register (308, "Permanent Redirect");
    
    //4×× Client Error
    public static final String REASON_400 = register (400, "Bad Request");
    public static final String REASON_401 = register (401, "Unauthorized");
    public static final String REASON_402 = register (402, "Payment Required");
    public static final String REASON_403 = register (403, "Forbidden");
    public static final String REASON_404 = register (404, "Not Found");
    public static final String REASON_405 = register (405, "Method Not Allowed");
    public static final String REASON_406 = register (406, "Not Acceptable");
    public static final String REASON_407 = register (407, "Proxy Authentication Required");
    public static final String REASON_408 = register (408, "Request Timeout");
    public static final String REASON_409 = register (409, "Conflict");
    public static final String REASON_410 = register (410, "Gone");
    public static final String REASON_411 = register (411, "Length Required");
    public static final String REASON_412 = register (412, "Precondition Failed");
    public static final String REASON_413 = register (413, "Payload Too Large");
    public static final String REASON_414 = register (414, "Request-URI Too Long");
    public static final String REASON_415 = register (415, "Unsupported Media Type");
    public static final String REASON_416 = register (416, "Requested Range Not Satisfiable");
    public static final String REASON_417 = register (417, "Expectation Failed");
    public static final String REASON_418 = register (418, "I'm a teapot");
    public static final String REASON_421 = register (421, "Misdirected Request");
    public static final String REASON_422 = register (422, "Unprocessable Entity");
    public static final String REASON_423 = register (423, "Locked");
    public static final String REASON_424 = register (424, "Failed Dependency");
    public static final String REASON_426 = register (426, "Upgrade Required");
    public static final String REASON_428 = register (428, "Precondition Required");
    public static final String REASON_429 = register (429, "Too Many Requests");
    public static final String REASON_431 = register (431, "Request Header Fields Too Large");
    public static final String REASON_444 = register (444, "Connection Closed Without Response");
    public static final String REASON_451 = register (451, "Unavailable For Legal Reasons");
    public static final String REASON_499 = register (499, "Client Closed Request");
    
    //5×× Server Error
    public static final String REASON_500 = register (500, "Internal Server Error");
    public static final String REASON_501 = register (501, "Not Implemented");
    public static final String REASON_502 = register (502, "Bad Gateway");
    public static final String REASON_503 = register (503, "Service Unavailable");
    public static final String REASON_504 = register (504, "Gateway Timeout");
    public static final String REASON_505 = register (505, "HTTP Version Not Supported");
    public static final String REASON_506 = register (506, "Variant Also Negotiates");
    public static final String REASON_507 = register (507, "Insufficient Storage");
    public static final String REASON_508 = register (508, "Loop Detected");
    public static final String REASON_509 = register (509, "Bandwidth Limit Exceeded");
    public static final String REASON_510 = register (510, "Not Extended");
    public static final String REASON_511 = register (511, "Network Authentication Required");
    public static final String REASON_598 = register (598, "Network read timeout error");
    public static final String REASON_599 = register (599, "Network Connect Timeout Error");
    

    public static final String getReason (int code, String def){
	if (code >= 600) return def;
	String reason = _reasonsByCode[code];
	return reason != null ? reason : def;
    }

    // best effort for a default
    public static final String getReason (int code){
	String reason = getReason (code, null);
	if (reason != null) return reason;
	switch (code / 100){
	case 1: return "Informational";
	case 2: return "Success";
	case 3: return "Redirection";
	case 4: return "Client Error";
	case 5: return "Server Error";
	default: return "Response"; // ....
	}
    }
    
}
