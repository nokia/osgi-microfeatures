// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.headers;

public class HeaderNameCode {
    public static final int METHOD = 62;
    public static final int PATH = 63;
    public static final int SCHEME = 64;
    public static final int STATUS = 65;
    public static final int ACCEPT_ENCODING = 66;

    public static final int AUTHORITY = 1;
    public static final int ACCEPT_CHARSET = 15;
    public static final int ACCEPT_LANGUAGE = 17;
    public static final int ACCEPT_RANGES = 18;
    public static final int ACCEPT = 19;
    public static final int ACCESS_CONTROL_ALLOW_ORIGIN = 20;
    public static final int AGE = 21;
    public static final int ALLOW = 22;
    public static final int AUTHORIZATION = 23;
    public static final int CACHE_CONTROL = 24;
    public static final int CONTENT_DISPOSITION = 25;
    public static final int CONTENT_ENCODING = 26;
    public static final int CONTENT_LANGUAGE = 27;
    public static final int CONTENT_LENGTH = 28;
    public static final int CONTENT_LOCATION = 29;
    public static final int CONTENT_RANGE = 30;
    public static final int CONTENT_TYPE = 31;
    public static final int COOKIE = 32;
    public static final int DATE = 33;
    public static final int ETAG = 34;
    public static final int EXPECT = 35;
    public static final int EXPIRES = 36;
    public static final int FROM = 37;
    public static final int HOST = 38;
    public static final int IF_MATCH = 39;
    public static final int IF_MODIFIED_SINCE = 40;
    public static final int IF_NONE_MATCH = 41;
    public static final int IF_RANGE = 42;
    public static final int IF_UNMODIFIED_SINCE = 43;
    public static final int LAST_MODIFIED = 44;
    public static final int LINK = 45;
    public static final int LOCATION = 46;
    public static final int MAX_FORWARDS = 47;
    public static final int PROXY_AUTHENTICATE = 48;
    public static final int PROXY_AUTHORIZATION = 49;
    public static final int RANGE = 50;
    public static final int REFERER = 51;
    public static final int REFRESH = 52;
    public static final int RETRY_AFTER = 53;
    public static final int SERVER = 54;
    public static final int SET_COOKIE = 55;
    public static final int STRICT_TRANSPORT_SECURITY = 56;
    public static final int TRANSFER_ENCODING = 57;
    public static final int USER_AGENT = 58;
    public static final int VARY = 59;
    public static final int VIA = 60;
    public static final int WWW_AUTHENTICATE = 61;

    public static int [] values() {
        int [] values = new int [] { METHOD, PATH, };
        return  values;
    }

}
