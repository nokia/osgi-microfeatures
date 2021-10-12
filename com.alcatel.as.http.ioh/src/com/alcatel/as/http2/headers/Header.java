// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.headers;

public enum Header {
    METHOD (62, ":method", true, true),
    PATH (63, ":path", true, true),
    SCHEME (64, ":scheme", true, true),
    STATUS (65, ":status", true, true),
    ACCEPT_ENCODING (66, "accept-encoding", false, true),

    AUTHORITY(1,":authority",true),
    METHOD_GET(2,"GET",METHOD),
    METHOD_POST(3,"POST",METHOD),
    PATH_SLASH(4,"/",PATH),
    PATH_SLASH_INDEX_HTML(5,"/index.html",PATH),
    SCHEME_HTTP(6,"http",SCHEME),
    SCHEME_HTTPS(7,"https",SCHEME),
    STATUS_200(8,"200",STATUS),
    STATUS_204(9,"204",STATUS),
    STATUS_206(10,"206",STATUS),
    STATUS_304(11,"304",STATUS),
    STATUS_400(12,"400",STATUS),
    STATUS_404(13,"404",STATUS),
    STATUS_500(14,"500",STATUS),
    ACCEPT_CHARSET(15,"accept-charset",false),
    ACCEPT_ENCODING_GZIP_DEFLATE(16,"gzip, deflate",ACCEPT_ENCODING),
    ACCEPT_LANGUAGE(17,"accept-language",false),
    ACCEPT_RANGES(18,"accept-ranges",false),
    ACCEPT(19,"accept",false),
    ACCESS_CONTROL_ALLOW_ORIGIN(20,"access-control-allow-origin",false),
    AGE(21,"age",false),
    ALLOW(22,"allow",false),
    AUTHORIZATION(23,"authorization",false),
    CACHE_CONTROL(24,"cache-control",false),
    CONTENT_DISPOSITION(25,"content-disposition",false),
    CONTENT_ENCODING(26,"content-encoding",false),
    CONTENT_LANGUAGE(27,"content-language",false),
    CONTENT_LENGTH(28,"content-length",false),
    CONTENT_LOCATION(29,"content-location",false),
    CONTENT_RANGE(30,"content-range",false),
    CONTENT_TYPE(31,"content-type",false),
    COOKIE(32,"cookie",false),
    DATE(33,"date",false),
    ETAG(34,"etag",false),
    EXPECT(35,"expect",false),
    EXPIRES(36,"expires",false),
    FROM(37,"from",false),
    HOST(38,"host",false),
    IF_MATCH(39,"if-match",false),
    IF_MODIFIED_SINCE(40,"if-modified-since",false),
    IF_NONE_MATCH(41,"if-none-match",false),
    IF_RANGE(42,"if-range",false),
    IF_UNMODIFIED_SINCE(43,"if-unmodified-since",false),
    LAST_MODIFIED(44,"last-modified",false),
    LINK(45,"link",false),
    LOCATION(46,"location",false),
    MAX_FORWARDS(47,"max-forwards",false),
    PROXY_AUTHENTICATE(48,"proxy-authenticate",false),
    PROXY_AUTHORIZATION(49,"proxy-authorization",false),
    RANGE(50,"range",false),
    REFERER(51,"referer",false),
    REFRESH(52,"refresh",false),
    RETRY_AFTER(53,"retry-after",false),
    SERVER(54,"server",false),
    SET_COOKIE(55,"set-cookie",false),
    STRICT_TRANSPORT_SECURITY(56,"strict-transport-security",false),
    TRANSFER_ENCODING(57,"transfer-encoding",false),
    USER_AGENT(58,"user-agent",false),
    VARY(59,"vary",false),
    VIA(60,"via",false),
    WWW_AUTHENTICATE(61,"www-authenticate",false)
    ;

    public static final int DO_NOT_USE = -1;
    /**
     * Has nothing to do with {@link Enum#name()}
     */
    public final String      name;
    public final String      value;
    public final int         name_value_code;
    public final boolean     pseudo;
    public final int         name_code;
    public final boolean     valued_variants_exist;
    public final Header      variant_parent;

    Header(int name_code, String name, boolean pseudo, boolean valued_variants_exist){
        this.name_code = name_code;
        this.name = name;
        this.pseudo = pseudo;
        this.value = null;
        this.name_value_code = DO_NOT_USE;
        this.valued_variants_exist = valued_variants_exist;
        this.variant_parent = null;
    }
    Header(int name_code, String name, boolean pseudo){
        this.name_code = name_code;
        this.name = name;
        this.pseudo = pseudo;
        this.value = null;
        this.name_value_code = DO_NOT_USE;
        this.valued_variants_exist = false;
        this.variant_parent = null;
    }
    Header(int code, String value, Header header){
        this.name_value_code = code;
        this.name = header.name;
        this.value = value;
        this.pseudo = header.pseudo;
        this.name_code=header.name_code;
        this.valued_variants_exist = true;
        this.variant_parent = header;
    }
    Header(int name_code, String name, String value, boolean pseudo){
        this.name_code = name_code;
        this.name = name;
        this.value = value;
        this.pseudo = pseudo;
        this.name_value_code = DO_NOT_USE;
        this.valued_variants_exist = false;
        this.variant_parent = null;
    }

    public int getNameCode () { return name_code;}
    public int getNameValueCode () {
        assert name_value_code != DO_NOT_USE : "Value is not known, use switchNameCode";
        return name_value_code;
    }
    public int getUniqueCode () {
        if (name_value_code == DO_NOT_USE)
            return name_code;
        return name_value_code;
    }
    /**
     * Has nothing to do with {@link Enum#name()}
     */
    public String getName (){ return name;}
    public String getValue(){
        assert name_value_code != DO_NOT_USE : "Value is not known, use switchNameCode";
        return value;
    }
    public boolean isPseudo (){ return pseudo;}

    final private static Header [] hpack_static_table= new Header[] {
            null,
        AUTHORITY,
                METHOD_GET,
                METHOD_POST,
                PATH_SLASH,
                PATH_SLASH_INDEX_HTML,
                SCHEME_HTTP,
                SCHEME_HTTPS,
                STATUS_200,
                STATUS_204,
                STATUS_206,
                STATUS_304,
                STATUS_400,
                STATUS_404,
                STATUS_500,
                ACCEPT_CHARSET,
                ACCEPT_ENCODING_GZIP_DEFLATE,
                ACCEPT_LANGUAGE,
                ACCEPT_RANGES,
                ACCEPT,
                ACCESS_CONTROL_ALLOW_ORIGIN,
                AGE,
                ALLOW,
                AUTHORIZATION,
                CACHE_CONTROL,
                CONTENT_DISPOSITION,
                CONTENT_ENCODING,
                CONTENT_LANGUAGE,
                CONTENT_LENGTH,
                CONTENT_LOCATION,
                CONTENT_RANGE,
                CONTENT_TYPE,
                COOKIE,
                DATE,
                ETAG,
                EXPECT,
                EXPIRES,
                FROM,
                HOST,
                IF_MATCH,
                IF_MODIFIED_SINCE,
                IF_NONE_MATCH,
                IF_RANGE,
                IF_UNMODIFIED_SINCE,
                LAST_MODIFIED,
                LINK,
                LOCATION,
                MAX_FORWARDS,
                PROXY_AUTHENTICATE,
                PROXY_AUTHORIZATION,
                RANGE,
                REFERER,
                REFRESH,
                RETRY_AFTER,
                SERVER,
                SET_COOKIE,
                STRICT_TRANSPORT_SECURITY,
                TRANSFER_ENCODING,
                USER_AGENT,
                VARY,
                VIA,
                WWW_AUTHENTICATE

    };

    public static Header get_static_table(int position) {
        assert position > 0 && position < 62 : "not a static entry: "+position;
        assert hpack_static_table[position].getUniqueCode() == position ;
        return hpack_static_table[position];
    }

}
