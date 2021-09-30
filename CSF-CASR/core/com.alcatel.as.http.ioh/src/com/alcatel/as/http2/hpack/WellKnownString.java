package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;

public class WellKnownString {
    static public Header match_name(byte[] data) {
        int length = data.length;
        if (length <= 0)
            return null;
        switch (data[0]) {
            case 58:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 97:
                        if (length == 10 && data[2] == 117 && data[3] == 116 && data[4] == 104 && data[5] == 111 && data[6] == 114 && data[7] == 105 && data[8] == 116 && data[9] == 121)
                            return Header.AUTHORITY;

                        break;
                    case 109:
                        if (length == 7 && data[2] == 101 && data[3] == 116 && data[4] == 104 && data[5] == 111 && data[6] == 100)
                            return Header.METHOD;

                        break;
                    case 112:
                        if (length == 5 && data[2] == 97 && data[3] == 116 && data[4] == 104)
                            return Header.PATH;

                        break;
                    case 115:
                        if (length <= 2)
                            return null;
                        switch (data[2]) {
                            case 99:
                                if (length == 7 && data[3] == 104 && data[4] == 101 && data[5] == 109 && data[6] == 101)
                                    return Header.SCHEME;

                                break;
                            case 116:
                                if (length == 7 && data[3] == 97 && data[4] == 116 && data[5] == 117 && data[6] == 115)
                                    return Header.STATUS;

                                break;
                        }
                        break;
                }
                break;
            case 97:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 99:
                        if (length > 4 && data[2] == 99 && data[3] == 101) {
                            if (length <= 4)
                                return null;
                            switch (data[4]) {
                                case 112:
                                    if (length >= 6 && data[5] == 116) {
                                        if (length == 6)
                                            return Header.ACCEPT;
                                        else {

                                            if (length > 7 && data[6] == 45) {
                                                if (length <= 7)
                                                    return null;
                                                switch (data[7]) {
                                                    case 99:
                                                        if (length == 14 && data[8] == 104 && data[9] == 97 && data[10] == 114 && data[11] == 115 && data[12] == 101 && data[13] == 116)
                                                            return Header.ACCEPT_CHARSET;

                                                        break;
                                                    case 101:
                                                        if (length == 15 && data[8] == 110 && data[9] == 99 && data[10] == 111 && data[11] == 100 && data[12] == 105 && data[13] == 110 && data[14] == 103)
                                                            return Header.ACCEPT_ENCODING;

                                                        break;
                                                    case 108:
                                                        if (length == 15 && data[8] == 97 && data[9] == 110 && data[10] == 103 && data[11] == 117 && data[12] == 97 && data[13] == 103 && data[14] == 101)
                                                            return Header.ACCEPT_LANGUAGE;

                                                        break;
                                                    case 114:
                                                        if (length == 13 && data[8] == 97 && data[9] == 110 && data[10] == 103 && data[11] == 101 && data[12] == 115)
                                                            return Header.ACCEPT_RANGES;

                                                        break;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                case 115:
                                    if (length == 27 && data[5] == 115 && data[6] == 45 && data[7] == 99 && data[8] == 111 && data[9] == 110 && data[10] == 116 && data[11] == 114 && data[12] == 111 && data[13] == 108 && data[14] == 45 && data[15] == 97 && data[16] == 108 && data[17] == 108 && data[18] == 111 && data[19] == 119 && data[20] == 45 && data[21] == 111 && data[22] == 114 && data[23] == 105 && data[24] == 103 && data[25] == 105 && data[26] == 110)
                                        return Header.ACCESS_CONTROL_ALLOW_ORIGIN;

                                    break;
                            }
                        }
                        break;
                    case 103:
                        if (length == 3 && data[2] == 101)
                            return Header.AGE;

                        break;
                    case 108:
                        if (length == 5 && data[2] == 108 && data[3] == 111 && data[4] == 119)
                            return Header.ALLOW;

                        break;
                    case 117:
                        if (length == 13 && data[2] == 116 && data[3] == 104 && data[4] == 111 && data[5] == 114 && data[6] == 105 && data[7] == 122 && data[8] == 97 && data[9] == 116 && data[10] == 105 && data[11] == 111 && data[12] == 110)
                            return Header.AUTHORIZATION;

                        break;
                }
                break;
            case 99:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 97:
                        if (length == 13 && data[2] == 99 && data[3] == 104 && data[4] == 101 && data[5] == 45 && data[6] == 99 && data[7] == 111 && data[8] == 110 && data[9] == 116 && data[10] == 114 && data[11] == 111 && data[12] == 108)
                            return Header.CACHE_CONTROL;

                        break;
                    case 111:
                        if (length <= 2)
                            return null;
                        switch (data[2]) {
                            case 110:
                                if (length > 8 && data[3] == 116 && data[4] == 101 && data[5] == 110 && data[6] == 116 && data[7] == 45) {
                                    if (length <= 8)
                                        return null;
                                    switch (data[8]) {
                                        case 100:
                                            if (length == 19 && data[9] == 105 && data[10] == 115 && data[11] == 112 && data[12] == 111 && data[13] == 115 && data[14] == 105 && data[15] == 116 && data[16] == 105 && data[17] == 111 && data[18] == 110)
                                                return Header.CONTENT_DISPOSITION;

                                            break;
                                        case 101:
                                            if (length == 16 && data[9] == 110 && data[10] == 99 && data[11] == 111 && data[12] == 100 && data[13] == 105 && data[14] == 110 && data[15] == 103)
                                                return Header.CONTENT_ENCODING;

                                            break;
                                        case 108:
                                            if (length <= 9)
                                                return null;
                                            switch (data[9]) {
                                                case 97:
                                                    if (length == 16 && data[10] == 110 && data[11] == 103 && data[12] == 117 && data[13] == 97 && data[14] == 103 && data[15] == 101)
                                                        return Header.CONTENT_LANGUAGE;

                                                    break;
                                                case 101:
                                                    if (length == 14 && data[10] == 110 && data[11] == 103 && data[12] == 116 && data[13] == 104)
                                                        return Header.CONTENT_LENGTH;

                                                    break;
                                                case 111:
                                                    if (length == 16 && data[10] == 99 && data[11] == 97 && data[12] == 116 && data[13] == 105 && data[14] == 111 && data[15] == 110)
                                                        return Header.CONTENT_LOCATION;

                                                    break;
                                            }
                                            break;
                                        case 114:
                                            if (length == 13 && data[9] == 97 && data[10] == 110 && data[11] == 103 && data[12] == 101)
                                                return Header.CONTENT_RANGE;

                                            break;
                                        case 116:
                                            if (length == 12 && data[9] == 121 && data[10] == 112 && data[11] == 101)
                                                return Header.CONTENT_TYPE;

                                            break;
                                    }
                                }
                                break;
                            case 111:
                                if (length == 6 && data[3] == 107 && data[4] == 105 && data[5] == 101)
                                    return Header.COOKIE;

                                break;
                        }
                        break;
                }
                break;
            case 100:
                if (length == 4 && data[1] == 97 && data[2] == 116 && data[3] == 101)
                    return Header.DATE;

                break;
            case 101:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 116:
                        if (length == 4 && data[2] == 97 && data[3] == 103)
                            return Header.ETAG;

                        break;
                    case 120:
                        if (length > 3 && data[2] == 112) {
                            if (length <= 3)
                                return null;
                            switch (data[3]) {
                                case 101:
                                    if (length == 6 && data[4] == 99 && data[5] == 116)
                                        return Header.EXPECT;

                                    break;
                                case 105:
                                    if (length == 7 && data[4] == 114 && data[5] == 101 && data[6] == 115)
                                        return Header.EXPIRES;

                                    break;
                            }
                        }
                        break;
                }
                break;
            case 102:
                if (length == 4 && data[1] == 114 && data[2] == 111 && data[3] == 109)
                    return Header.FROM;

                break;
            case 104:
                if (length == 4 && data[1] == 111 && data[2] == 115 && data[3] == 116)
                    return Header.HOST;

                break;
            case 105:
                if (length > 3 && data[1] == 102 && data[2] == 45) {
                    if (length <= 3)
                        return null;
                    switch (data[3]) {
                        case 109:
                            if (length <= 4)
                                return null;
                            switch (data[4]) {
                                case 97:
                                    if (length == 8 && data[5] == 116 && data[6] == 99 && data[7] == 104)
                                        return Header.IF_MATCH;

                                    break;
                                case 111:
                                    if (length == 17 && data[5] == 100 && data[6] == 105 && data[7] == 102 && data[8] == 105 && data[9] == 101 && data[10] == 100 && data[11] == 45 && data[12] == 115 && data[13] == 105 && data[14] == 110 && data[15] == 99 && data[16] == 101)
                                        return Header.IF_MODIFIED_SINCE;

                                    break;
                            }
                            break;
                        case 110:
                            if (length == 13 && data[4] == 111 && data[5] == 110 && data[6] == 101 && data[7] == 45 && data[8] == 109 && data[9] == 97 && data[10] == 116 && data[11] == 99 && data[12] == 104)
                                return Header.IF_NONE_MATCH;

                            break;
                        case 114:
                            if (length == 8 && data[4] == 97 && data[5] == 110 && data[6] == 103 && data[7] == 101)
                                return Header.IF_RANGE;

                            break;
                        case 117:
                            if (length == 19 && data[4] == 110 && data[5] == 109 && data[6] == 111 && data[7] == 100 && data[8] == 105 && data[9] == 102 && data[10] == 105 && data[11] == 101 && data[12] == 100 && data[13] == 45 && data[14] == 115 && data[15] == 105 && data[16] == 110 && data[17] == 99 && data[18] == 101)
                                return Header.IF_UNMODIFIED_SINCE;

                            break;
                    }
                }
                break;
            case 108:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 97:
                        if (length == 13 && data[2] == 115 && data[3] == 116 && data[4] == 45 && data[5] == 109 && data[6] == 111 && data[7] == 100 && data[8] == 105 && data[9] == 102 && data[10] == 105 && data[11] == 101 && data[12] == 100)
                            return Header.LAST_MODIFIED;

                        break;
                    case 105:
                        if (length == 4 && data[2] == 110 && data[3] == 107)
                            return Header.LINK;

                        break;
                    case 111:
                        if (length == 8 && data[2] == 99 && data[3] == 97 && data[4] == 116 && data[5] == 105 && data[6] == 111 && data[7] == 110)
                            return Header.LOCATION;

                        break;
                }
                break;
            case 109:
                if (length == 12 && data[1] == 97 && data[2] == 120 && data[3] == 45 && data[4] == 102 && data[5] == 111 && data[6] == 114 && data[7] == 119 && data[8] == 97 && data[9] == 114 && data[10] == 100 && data[11] == 115)
                    return Header.MAX_FORWARDS;

                break;
            case 112:
                if (length > 10 && data[1] == 114 && data[2] == 111 && data[3] == 120 && data[4] == 121 && data[5] == 45 && data[6] == 97 && data[7] == 117 && data[8] == 116 && data[9] == 104) {
                    if (length <= 10)
                        return null;
                    switch (data[10]) {
                        case 101:
                            if (length == 18 && data[11] == 110 && data[12] == 116 && data[13] == 105 && data[14] == 99 && data[15] == 97 && data[16] == 116 && data[17] == 101)
                                return Header.PROXY_AUTHENTICATE;

                            break;
                        case 111:
                            if (length == 19 && data[11] == 114 && data[12] == 105 && data[13] == 122 && data[14] == 97 && data[15] == 116 && data[16] == 105 && data[17] == 111 && data[18] == 110)
                                return Header.PROXY_AUTHORIZATION;

                            break;
                    }
                }
                break;
            case 114:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 97:
                        if (length == 5 && data[2] == 110 && data[3] == 103 && data[4] == 101)
                            return Header.RANGE;

                        break;
                    case 101:
                        if (length <= 2)
                            return null;
                        switch (data[2]) {
                            case 102:
                                if (length <= 3)
                                    return null;
                                switch (data[3]) {
                                    case 101:
                                        if (length == 7 && data[4] == 114 && data[5] == 101 && data[6] == 114)
                                            return Header.REFERER;

                                        break;
                                    case 114:
                                        if (length == 7 && data[4] == 101 && data[5] == 115 && data[6] == 104)
                                            return Header.REFRESH;

                                        break;
                                }
                                break;
                            case 116:
                                if (length == 11 && data[3] == 114 && data[4] == 121 && data[5] == 45 && data[6] == 97 && data[7] == 102 && data[8] == 116 && data[9] == 101 && data[10] == 114)
                                    return Header.RETRY_AFTER;

                                break;
                        }
                        break;
                }
                break;
            case 115:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 101:
                        if (length <= 2)
                            return null;
                        switch (data[2]) {
                            case 114:
                                if (length == 6 && data[3] == 118 && data[4] == 101 && data[5] == 114)
                                    return Header.SERVER;

                                break;
                            case 116:
                                if (length == 10 && data[3] == 45 && data[4] == 99 && data[5] == 111 && data[6] == 111 && data[7] == 107 && data[8] == 105 && data[9] == 101)
                                    return Header.SET_COOKIE;

                                break;
                        }
                        break;
                    case 116:
                        if (length == 25 && data[2] == 114 && data[3] == 105 && data[4] == 99 && data[5] == 116 && data[6] == 45 && data[7] == 116 && data[8] == 114 && data[9] == 97 && data[10] == 110 && data[11] == 115 && data[12] == 112 && data[13] == 111 && data[14] == 114 && data[15] == 116 && data[16] == 45 && data[17] == 115 && data[18] == 101 && data[19] == 99 && data[20] == 117 && data[21] == 114 && data[22] == 105 && data[23] == 116 && data[24] == 121)
                            return Header.STRICT_TRANSPORT_SECURITY;

                        break;
                }
                break;
            case 116:
                if (length == 17 && data[1] == 114 && data[2] == 97 && data[3] == 110 && data[4] == 115 && data[5] == 102 && data[6] == 101 && data[7] == 114 && data[8] == 45 && data[9] == 101 && data[10] == 110 && data[11] == 99 && data[12] == 111 && data[13] == 100 && data[14] == 105 && data[15] == 110 && data[16] == 103)
                    return Header.TRANSFER_ENCODING;

                break;
            case 117:
                if (length == 10 && data[1] == 115 && data[2] == 101 && data[3] == 114 && data[4] == 45 && data[5] == 97 && data[6] == 103 && data[7] == 101 && data[8] == 110 && data[9] == 116)
                    return Header.USER_AGENT;

                break;
            case 118:
                if (length <= 1)
                    return null;
                switch (data[1]) {
                    case 97:
                        if (length == 4 && data[2] == 114 && data[3] == 121)
                            return Header.VARY;

                        break;
                    case 105:
                        if (length == 3 && data[2] == 97)
                            return Header.VIA;

                        break;
                }
                break;
            case 119:
                if (length == 16 && data[1] == 119 && data[2] == 119 && data[3] == 45 && data[4] == 97 && data[5] == 117 && data[6] == 116 && data[7] == 104 && data[8] == 101 && data[9] == 110 && data[10] == 116 && data[11] == 105 && data[12] == 99 && data[13] == 97 && data[14] == 116 && data[15] == 101)
                    return Header.WWW_AUTHENTICATE;

                break;
        }


        return null;
    }

    static public Header match_value(Header name_header, byte[] data) {
        assert name_header.valued_variants_exist : "if there are no variants there is no need to look for them";
        int length = data.length;
        switch(name_header) {
            case METHOD:
                if (length < 3 || length > 4) {
                    return null;
                }

                switch(data[0]) {
                    case (byte)0x47 :
                        if (length == 3 && data[1] == (byte)0x45 && data[2] == (byte)0x54)
                            return Header.METHOD_GET;

                        return null;
                    case (byte)0x50 :
                        if (length == 4 && data[1] == (byte)0x4f && data[2] == (byte)0x53 && data[3] == (byte)0x54)
                            return Header.METHOD_POST;

                        return null;
                }
            case PATH:
                if (length < 1 || length > 11) {
                    return null;
                }

                if ( length>=1 && data[0] == (byte)0x2f) {
                    if ( length==1 )
                        return Header.PATH_SLASH;
                    else {

                        if (length == 11 && data[1] == (byte)0x69 && data[2] == (byte)0x6e && data[3] == (byte)0x64 && data[4] == (byte)0x65 && data[5] == (byte)0x78 && data[6] == (byte)0x2e && data[7] == (byte)0x68 && data[8] == (byte)0x74 && data[9] == (byte)0x6d && data[10] == (byte)0x6c)
                            return Header.PATH_SLASH_INDEX_HTML;

                        return null;
                    }}
                return null;
            case SCHEME:
                if (length < 4 || length > 5) {
                    return null;
                }

                if ( length>=4 && data[0] == (byte)0x68 && data[1] == (byte)0x74 && data[2] == (byte)0x74 && data[3] == (byte)0x70) {
                    if ( length==4 )
                        return Header.SCHEME_HTTP;
                    else {

                        if (length == 5 && data[4] == (byte)0x73)
                            return Header.SCHEME_HTTPS;

                        return null;
                    }}
                return null;
            case STATUS:
// prefix:List() position:0
// prefix:List() position:0 alphabet:List(50, 51, 52, 53)
                if (length <= 0)
                    return null;
                switch(data[0]) {
                    case 50 :
// prefix:List(50) position:1
// prefix:List(50) position:1 alphabet:List(48)
// !!!!!!!!!!!!!(j=1) 1
// (List(50, 48),List((200,200,List(50, 48, 48)), (204,204,List(50, 48, 52)), (206,206,List(50, 48, 54))))
// (----) List()
// !!!!!!!!!!!!!(j=2) 3
// (List(50, 48, 54),List((206,206,List(50, 48, 54))))
//(List(50, 48, 52),List((204,204,List(50, 48, 52))))
//(List(50, 48, 48),List((200,200,List(50, 48, 48))))
// (----) List()
                        if ( length>2 && data[1] == 48){
//List(50, 48)
// prefix:List(50, 48) position:2
// prefix:List(50, 48) position:2 alphabet:List(48, 52, 54)
                            if (length <= 2)
                                return null;
                            switch(data[2]) {
                                case 48 :
                                    if (length == 3)
                                        return Header.STATUS_200 ;

// prefix:List(50, 48, 48) position:3
// prefix:List(50, 48, 48) position:3 alphabet:List()
                                    break; // 48
                                case 52 :
                                    if (length == 3)
                                        return Header.STATUS_204 ;

// prefix:List(50, 48, 52) position:3
// prefix:List(50, 48, 52) position:3 alphabet:List()
                                    break; // 52
                                case 54 :
                                    if (length == 3)
                                        return Header.STATUS_206 ;

// prefix:List(50, 48, 54) position:3
// prefix:List(50, 48, 54) position:3 alphabet:List()
                                    break; // 54
                            } // data[2]
                        }
                        break; // 50
                    case 51 :
// prefix:List(51) position:1
// prefix:List(51) position:1 alphabet:List(48)
// matching (304,304,List(51, 48, 52))
                        if (length == 3 && data[1] == 48 && data[2] == 52)
                            return Header.STATUS_304;

                        break; // 51
                    case 52 :
// prefix:List(52) position:1
// prefix:List(52) position:1 alphabet:List(48)
// !!!!!!!!!!!!!(j=1) 1
// (List(52, 48),List((400,400,List(52, 48, 48)), (404,404,List(52, 48, 52))))
// (----) List()
// !!!!!!!!!!!!!(j=2) 2
// (List(52, 48, 48),List((400,400,List(52, 48, 48))))
//(List(52, 48, 52),List((404,404,List(52, 48, 52))))
// (----) List()
                        if ( length>2 && data[1] == 48){
//List(52, 48)
// prefix:List(52, 48) position:2
// prefix:List(52, 48) position:2 alphabet:List(48, 52)
                            if (length <= 2)
                                return null;
                            switch(data[2]) {
                                case 48 :
                                    if (length == 3)
                                        return Header.STATUS_400 ;

// prefix:List(52, 48, 48) position:3
// prefix:List(52, 48, 48) position:3 alphabet:List()
                                    break; // 48
                                case 52 :
                                    if (length == 3)
                                        return Header.STATUS_404 ;

// prefix:List(52, 48, 52) position:3
// prefix:List(52, 48, 52) position:3 alphabet:List()
                                    break; // 52
                            } // data[2]
                        }
                        break; // 52
                    case 53 :
// prefix:List(53) position:1
// prefix:List(53) position:1 alphabet:List(48)
// matching (500,500,List(53, 48, 48))
                        if (length == 3 && data[1] == 48 && data[2] == 48)
                            return Header.STATUS_500;

                        break; // 53
                } // data[0]
                break;
            case ACCEPT_ENCODING:
                if (length != 13) {
                    return null;
                }

                if ( data[0] == (byte)0x67 && data[1] == (byte)0x7a && data[2] == (byte)0x69 && data[3] == (byte)0x70 && data[4] == (byte)0x2c && data[5] == (byte)0x20 && data[6] == (byte)0x64 && data[7] == (byte)0x65 && data[8] == (byte)0x66 && data[9] == (byte)0x6c && data[10] == (byte)0x61 && data[11] == (byte)0x74 && data[12] == (byte)0x65)
                    return Header.ACCEPT_ENCODING_GZIP_DEFLATE;

                return null;
        }
        return null;
    }
}