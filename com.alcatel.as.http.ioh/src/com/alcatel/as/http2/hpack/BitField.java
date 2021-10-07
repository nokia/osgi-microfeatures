package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;

public class BitField implements HuffmanDecoder.In {

    private final byte[] data;
    private final int length ;

    public BitField(byte[] data) {
        this.data    = data;
        this.length  = data.length;
        current_byte = data[0] & 0xff;
        if (length > 1)
            next_byte    = data[1] & 0xff;
        else
            next_byte    = 0xff;
    }

    private int     bit_cursor  = 0;
    private int     byte_cursor = 0;
    private boolean last        = true;

    private int current_byte    = -1;
    private int next_byte       = -1;

    private int eof_or_suspend() {
        return (last) ? Parser.EOF : Parser.SUSPEND;
    }

    byte[] mask_this = new byte[]{ (byte) 0xFF, (byte) 0x7F, (byte) 0x3F, (byte) 0x1F, (byte) 0x0F, (byte) 0x07, (byte) 0x03, (byte) 0x01};

    public int fetchByte() {
/*        int ret = _fetchByte();
        System.err.println(" -> "+ret);
        return ret;
    }
    public int _fetchByte() {*/
        switch (bit_cursor) {
            case 0:
                return (byte_cursor >= length) ? eof_or_suspend() : current_byte ;
//                break;
            default:
                return (byte_cursor >= length) ? eof_or_suspend() :
//                        (((data[byte_cursor] & mask_this[bit_cursor]) << bit_cursor) | ((data_ahead & mask_next[bit_cursor]) >> (8 - bit_cursor))) & 0xFF;
                        (((current_byte & mask_this[bit_cursor]) << bit_cursor) | ((next_byte  >> (8 - bit_cursor))& mask_this[8-bit_cursor])) & 0xFF;
        }
    }

    public void advance(int advance) {
        switch (bit_cursor) {
            case 8:
                byte_cursor = byte_cursor + 1;
                current_byte=next_byte;
                next_byte = (byte_cursor + 1 >= advance) ? 0xff : data[byte_cursor+1] & 0xff;
                break;
            default:
                bit_cursor = bit_cursor + advance;
                while (bit_cursor >= 8) {
                    bit_cursor = bit_cursor - 8;
                    byte_cursor = byte_cursor + 1;
                    current_byte=next_byte;
                    next_byte = (byte_cursor + 1 >= this.length) ? 0xff : data[byte_cursor + 1] & 0xff;
                }
        }
    }

    public void rewind(int nb_byte) {
        byte_cursor = byte_cursor - nb_byte;
    }

    public boolean isEos() {return (byte_cursor >= length);}

    public Header early_match() {
        int length = data.length;
        if (length < 2 || length > 19)
            return null;

        switch(current_byte) {
            case 0x19 :
                if ( length>2 && next_byte == 0x8){
                    if (length <= 2)
                        return null;
                    switch(data[2]) {
                        case (byte)0x54 :
                            if (length == 19 && data[3] == (byte)0x21 && data[4] == (byte)0x62 && data[5] == (byte)0x1e && data[6] == (byte)0xa4 && data[7] == (byte)0xd8 && data[8] == (byte)0x7a && data[9] == (byte)0x16 && data[10] == (byte)0x1d && data[11] == (byte)0x14 && data[12] == (byte)0x1f && data[13] == (byte)0xc2 && data[14] == (byte)0xc7 && data[15] == (byte)0xb0 && data[16] == (byte)0xd3 && data[17] == (byte)0x1a && data[18] == (byte)0xaf)
                                return Header.ACCESS_CONTROL_ALLOW_ORIGIN;

                            break;
                        case (byte)0x5a :
                            if (length <= 3)
                                return null;
                            switch(data[3]) {
                                case (byte)0xd2 :
                                    if (length <= 4)
                                        return null;
                                    switch(data[4]) {
                                        case (byte)0xb1 :
                                            if (length <= 5)
                                                return null;
                                            switch(data[5]) {
                                                case (byte)0x27 :
                                                    if (length == 10 && data[6] == (byte)0x1d && data[7] == (byte)0x88 && data[8] == (byte)0x2a && data[9] == (byte)0x7f)
                                                        return Header.ACCEPT_CHARSET;

                                                    break;
                                                case (byte)0x6a :
                                                    if (length == 11 && data[6] == (byte)0x21 && data[7] == (byte)0xe4 && data[8] == (byte)0x35 && data[9] == (byte)0x53 && data[10] == (byte)0x7f)
                                                        return Header.ACCEPT_ENCODING;

                                                    break;
                                            }
                                            break;
                                        case (byte)0xb5 :
                                            if (length <= 5)
                                                return null;
                                            switch(data[5]) {
                                                case (byte)0x3 :
                                                    if (length == 11 && data[6] == (byte)0xaa && data[7] == (byte)0x6b && data[8] == (byte)0x47 && data[9] == (byte)0x31 && data[10] == (byte)0x7f)
                                                        return Header.ACCEPT_LANGUAGE;

                                                    break;
                                                case (byte)0x83 :
                                                    if (length == 9 && data[6] == (byte)0xaa && data[7] == (byte)0x62 && data[8] == (byte)0xa3)
                                                        return Header.ACCEPT_RANGES;

                                                    break;
                                            }
                                            break;
                                    }
                                    break;
                                case (byte)0xd3 :
                                    if (length == 4)
                                        return Header.ACCEPT ;

                                    break;
                            }
                            break;
                    }
                }
                break;
            case 0x1c :
                if (length == 2 && next_byte == 0xc5)
                    return Header.AGE;

                break;
            case 0x1d :
                switch(next_byte) {
                    case 0x14 :
                        if (length == 4 && data[2] == (byte)0x1f && data[3] == (byte)0xc7)
                            return Header.ALLOW;

                        break;
                    case 0xa9 :
                        if (length == 9 && data[2] == (byte)0x9c && data[3] == (byte)0xf6 && data[4] == (byte)0x1b && data[5] == (byte)0xd8 && data[6] == (byte)0xd2 && data[7] == (byte)0x63 && data[8] == (byte)0xd5)
                            return Header.AUTHORIZATION;

                        break;
                }
                break;
            case 0x20 :
                if (length == 9 && next_byte == 0xc9 && data[2] == (byte)0x39 && data[3] == (byte)0x56 && data[4] == (byte)0x21 && data[5] == (byte)0xea && data[6] == (byte)0x4d && data[7] == (byte)0x87 && data[8] == (byte)0xa3)
                    return Header.CACHE_CONTROL;

                break;
            case 0x21 :
                switch(next_byte) {
                    case 0xcf :
                        if (length == 4 && data[2] == (byte)0xd4 && data[3] == (byte)0xc5)
                            return Header.COOKIE;

                        break;
                    case 0xea :
                        if ( length>5 && data[2] == (byte)0x49 && data[3] == (byte)0x6a && data[4] == (byte)0x4a){
                            if (length <= 5)
                                return null;
                            switch(data[5]) {
                                case (byte)0xc5 :
                                    if (length == 11 && data[6] == (byte)0xa8 && data[7] == (byte)0x87 && data[8] == (byte)0x90 && data[9] == (byte)0xd5 && data[10] == (byte)0x4d)
                                        return Header.CONTENT_ENCODING;

                                    break;
                                case (byte)0xc9 :
                                    if (length == 9 && data[6] == (byte)0xf5 && data[7] == (byte)0x59 && data[8] == (byte)0x7f)
                                        return Header.CONTENT_TYPE;

                                    break;
                                case (byte)0xd2 :
                                    if (length == 13 && data[6] == (byte)0x19 && data[7] == (byte)0x15 && data[8] == (byte)0x9d && data[9] == (byte)0x6 && data[10] == (byte)0x49 && data[11] == (byte)0x8f && data[12] == (byte)0x57)
                                        return Header.CONTENT_DISPOSITION;

                                    break;
                                case (byte)0xd4 :
                                    if (length <= 6)
                                        return null;
                                    switch(data[6]) {
                                        case (byte)0xe :
                                            if (length == 11 && data[7] == (byte)0xa9 && data[8] == (byte)0xad && data[9] == (byte)0x1c && data[10] == (byte)0xc5)
                                                return Header.CONTENT_LANGUAGE;

                                            break;
                                        case (byte)0x16 :
                                            if (length == 10 && data[7] == (byte)0xa9 && data[8] == (byte)0x93 && data[9] == (byte)0x3f)
                                                return Header.CONTENT_LENGTH;

                                            break;
                                        case (byte)0x1c :
                                            if (length == 11 && data[7] == (byte)0x83 && data[8] == (byte)0x49 && data[9] == (byte)0x8f && data[10] == (byte)0x57)
                                                return Header.CONTENT_LOCATION;

                                            break;
                                    }
                                    break;
                                case (byte)0xd6 :
                                    if (length == 9 && data[6] == (byte)0xe && data[7] == (byte)0xa9 && data[8] == (byte)0x8b)
                                        return Header.CONTENT_RANGE;

                                    break;
                            }
                        }
                        break;
                }
                break;
            case 0x2a :
                if (length == 3 && next_byte == 0x47 && data[2] == (byte)0x37)
                    return Header.ETAG;

                break;
            case 0x2f :
                if ( length>2 && next_byte == 0x9a){
                    if (length <= 2)
                        return null;
                    switch(data[2]) {
                        case (byte)0xca :
                            if (length == 5 && data[3] == (byte)0x44 && data[4] == (byte)0xff)
                                return Header.EXPECT;

                            break;
                        case (byte)0xcd :
                            if (length == 5 && data[3] == (byte)0x61 && data[4] == (byte)0x51)
                                return Header.EXPIRES;

                            break;
                    }
                }
                break;
            case 0x34 :
                if ( length>2 && next_byte == 0xab){
                    if (length <= 2)
                        return null;
                    switch(data[2]) {
                        case (byte)0x52 :
                            if (length <= 3)
                                return null;
                            switch(data[3]) {
                                case (byte)0x34 :
                                    if (length == 6 && data[4] == (byte)0x92 && data[5] == (byte)0x7f)
                                        return Header.IF_MATCH;

                                    break;
                                case (byte)0x79 :
                                    if (length == 12 && data[4] == (byte)0xd && data[5] == (byte)0x29 && data[6] == (byte)0x8b && data[7] == (byte)0x22 && data[8] == (byte)0xc8 && data[9] == (byte)0x35 && data[10] == (byte)0x44 && data[11] == (byte)0x2f)
                                        return Header.IF_MODIFIED_SINCE;

                                    break;
                            }
                            break;
                        case (byte)0x54 :
                            if (length == 9 && data[3] == (byte)0x7a && data[4] == (byte)0x8a && data[5] == (byte)0xb5 && data[6] == (byte)0x23 && data[7] == (byte)0x49 && data[8] == (byte)0x27)
                                return Header.IF_NONE_MATCH;

                            break;
                        case (byte)0x58 :
                            if (length == 6 && data[3] == (byte)0x3a && data[4] == (byte)0xa6 && data[5] == (byte)0x2f)
                                return Header.IF_RANGE;

                            break;
                        case (byte)0x5b :
                            if (length == 14 && data[3] == (byte)0x55 && data[4] == (byte)0x27 && data[5] == (byte)0x90 && data[6] == (byte)0xd2 && data[7] == (byte)0x98 && data[8] == (byte)0xb2 && data[9] == (byte)0x2c && data[10] == (byte)0x83 && data[11] == (byte)0x54 && data[12] == (byte)0x42 && data[13] == (byte)0xff)
                                return Header.IF_UNMODIFIED_SINCE;

                            break;
                    }
                }
                break;
            case 0x41 :
                switch(next_byte) {
                    case 0x52 :
                        if (length == 7 && data[2] == (byte)0xb1 && data[3] == (byte)0xe && data[4] == (byte)0x7e && data[5] == (byte)0xa6 && data[6] == (byte)0x2f)
                            return Header.SET_COOKIE;

                        break;
                    case 0x6c :
                        if (length == 5 && data[2] == (byte)0xee && data[3] == (byte)0x5b && data[4] == (byte)0x3f)
                            return Header.SERVER;

                        break;
                }
                break;
            case 0x42 :
                if (length == 17 && next_byte == 0x6c && data[2] == (byte)0x31 && data[3] == (byte)0x12 && data[4] == (byte)0xb2 && data[5] == (byte)0x6c && data[6] == (byte)0x1d && data[7] == (byte)0x48 && data[8] == (byte)0xac && data[9] == (byte)0xf6 && data[10] == (byte)0x25 && data[11] == (byte)0x64 && data[12] == (byte)0x14 && data[13] == (byte)0x96 && data[14] == (byte)0xd8 && data[15] == (byte)0x64 && data[16] == (byte)0xfa)
                    return Header.STRICT_TRANSPORT_SECURITY;

                break;
            case 0x4d :
                if (length == 12 && next_byte == 0x83 && data[2] == (byte)0xa9 && data[3] == (byte)0x12 && data[4] == (byte)0x96 && data[5] == (byte)0xc5 && data[6] == (byte)0x8b && data[7] == (byte)0x51 && data[8] == (byte)0xf && data[9] == (byte)0x21 && data[10] == (byte)0xaa && data[11] == (byte)0x9b)
                    return Header.TRANSFER_ENCODING;

                break;
            case 0x90 :
                if (length == 3 && next_byte == 0x69 && data[2] == (byte)0x2f)
                    return Header.DATE;

                break;
            case 0x96 :
                if (length == 3 && next_byte == 0xc3 && data[2] == (byte)0xd3)
                    return Header.FROM;

                break;
            case 0x9c :
                if (length == 3 && next_byte == 0xe8 && data[2] == (byte)0x4f)
                    return Header.HOST;

                break;
            case 0xa0 :
                switch(next_byte) {
                    case 0x68 :
                        if (length == 9 && data[2] == (byte)0x4a && data[3] == (byte)0xd4 && data[4] == (byte)0x9e && data[5] == (byte)0x43 && data[6] == (byte)0x4a && data[7] == (byte)0x62 && data[8] == (byte)0xc9)
                            return Header.LAST_MODIFIED;

                        break;
                    case 0xd5 :
                        if (length == 3 && data[2] == (byte)0x75)
                            return Header.LINK;

                        break;
                    case 0xe4 :
                        if (length == 6 && data[2] == (byte)0x1a && data[3] == (byte)0x4c && data[4] == (byte)0x7a && data[5] == (byte)0xbf)
                            return Header.LOCATION;

                        break;
                }
                break;
            case 0xa4 :
                if (length == 9 && next_byte == 0x7e && data[2] == (byte)0x56 && data[3] == (byte)0x94 && data[4] == (byte)0xf6 && data[5] == (byte)0x78 && data[6] == (byte)0x1d && data[7] == (byte)0x92 && data[8] == (byte)0x23)
                    return Header.MAX_FORWARDS;

                break;
            case 0xae :
                if ( length>7 && next_byte == 0xc3 && data[2] == (byte)0xf9 && data[3] == (byte)0xf4 && data[4] == (byte)0xb0 && data[5] == (byte)0xed && data[6] == (byte)0x4c){
                    if (length <= 7)
                        return null;
                    switch(data[7]) {
                        case (byte)0xe5 :
                            if (length == 13 && data[8] == (byte)0xa9 && data[9] == (byte)0x26 && data[10] == (byte)0x20 && data[11] == (byte)0xd2 && data[12] == (byte)0x5f)
                                return Header.PROXY_AUTHENTICATE;

                            break;
                        case (byte)0xe7 :
                            if (length == 14 && data[8] == (byte)0xb0 && data[9] == (byte)0xde && data[10] == (byte)0xc6 && data[11] == (byte)0x93 && data[12] == (byte)0x1e && data[13] == (byte)0xaf)
                                return Header.PROXY_AUTHORIZATION;

                            break;
                    }
                }
                break;
            case 0xb0 :
                switch(next_byte) {
                    case 0x75 :
                        if (length == 4 && data[2] == (byte)0x4c && data[3] == (byte)0x5f)
                            return Header.RANGE;

                        break;
                    case 0xa9 :
                        if (length == 8 && data[2] == (byte)0xb3 && data[3] == (byte)0xd2 && data[4] == (byte)0xc3 && data[5] == (byte)0x95 && data[6] == (byte)0x25 && data[7] == (byte)0xb3)
                            return Header.RETRY_AFTER;

                        break;
                    case 0xb2 :
                        if (length <= 2)
                            return null;
                        switch(data[2]) {
                            case (byte)0x96 :
                                if (length == 5 && data[3] == (byte)0xc2 && data[4] == (byte)0xd9)
                                    return Header.REFERER;

                                break;
                            case (byte)0xd8 :
                                if (length == 5 && data[3] == (byte)0x54 && data[4] == (byte)0x4f)
                                    return Header.REFRESH;

                                break;
                        }
                        break;
                }
                break;
            case 0xb5 :
                if (length == 7 && next_byte == 0x5 && data[2] == (byte)0xb1 && data[3] == (byte)0x61 && data[4] == (byte)0xcc && data[5] == (byte)0x5a && data[6] == (byte)0x93)
                    return Header.USER_AGENT;

                break;
            case 0xb8 :
                switch(next_byte) {
                    case 0x3b :
                        if (length == 8 && data[2] == (byte)0x53 && data[3] == (byte)0x39 && data[4] == (byte)0xec && data[5] == (byte)0x32 && data[6] == (byte)0x7d && data[7] == (byte)0x7f)
                            return Header.AUTHORITY;

                        break;
                    case 0x82 :
                        if (length == 5 && data[2] == (byte)0x4e && data[3] == (byte)0x5a && data[4] == (byte)0x4b)
                            return Header.SCHEME;

                        break;
                    case 0x84 :
                        if (length == 5 && data[2] == (byte)0x8d && data[3] == (byte)0x36 && data[4] == (byte)0xa3)
                            return Header.STATUS;

                        break;
                }
                break;
            case 0xb9 :
                switch(next_byte) {
                    case 0x49 :
                        if (length == 5 && data[2] == (byte)0x53 && data[3] == (byte)0x39 && data[4] == (byte)0xe4)
                            return Header.METHOD;

                        break;
                    case 0x58 :
                        if (length == 4 && data[2] == (byte)0xd3 && data[3] == (byte)0x3f)
                            return Header.PATH;

                        break;
                }
                break;
            case 0xee :
                switch(next_byte) {
                    case 0x3b :
                        if (length == 4 && data[2] == (byte)0x3d && data[3] == (byte)0x7f)
                            return Header.VARY;

                        break;
                    case 0x61 :
                        if (length == 3 && data[2] == (byte)0xff)
                            return Header.VIA;

                        break;
                }
                break;
            case 0xf1 :
                if (length == 12 && next_byte == 0xe3 && data[2] == (byte)0xc2 && data[3] == (byte)0xc3 && data[4] == (byte)0xb5 && data[5] == (byte)0x33 && data[6] == (byte)0x96 && data[7] == (byte)0xa4 && data[8] == (byte)0x98 && data[9] == (byte)0x83 && data[10] == (byte)0x49 && data[11] == (byte)0x7f)
                    return Header.WWW_AUTHENTICATE;

                break;
        }


        return null;
    }

    public Header match(Header h, int start) {
        return null;
    }
}

