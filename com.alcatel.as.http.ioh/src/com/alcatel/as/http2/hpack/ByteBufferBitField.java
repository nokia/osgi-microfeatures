// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;

import java.nio.ByteBuffer;

public class ByteBufferBitField implements HuffmanDecoder.In {

    private final ByteBuffer buffer;
    private final int        length ;

    public ByteBufferBitField(ByteBuffer buffer, int length) {
        this.buffer    = buffer;
        this.length  = length;
        current_byte = buffer.get() & 0xff;
        if (length > 1)
            next_byte    = buffer.get() & 0xff;
        else
            next_byte    = 0xff;
    }

    private int     bit_cursor  = 0;
    private int     byte_cursor = 0;
    private boolean last        = true;

    private int current_byte    = -1;
    private int next_byte       = -1;

    byte[] mask_this = new byte[]{ (byte) 0xFF, (byte) 0x7F, (byte) 0x3F, (byte) 0x1F, (byte) 0x0F, (byte) 0x07, (byte) 0x03, (byte) 0x01};

    public int fetchByte() {
/*        int ret = _fetchByte();
        System.err.println(" -> "+ret);
        return ret;
    }
    public int _fetchByte() {*/
        switch (bit_cursor) {
            case 0:
                return (byte_cursor >= length) ? Parser.EOF : current_byte ;
//                break;
            default:
                return (byte_cursor >= length) ? Parser.EOF :
                        (((current_byte & mask_this[bit_cursor]) << bit_cursor) | ((next_byte  >> (8 - bit_cursor))& mask_this[8-bit_cursor])) & 0xFF;
        }
    }

    public void advance(int advance) {
        bit_cursor = bit_cursor + advance;
        while (bit_cursor >= 8) {
            bit_cursor = bit_cursor - 8;
            byte_cursor = byte_cursor + 1;
            current_byte=next_byte;
            next_byte = (byte_cursor + 1 >= this.length) ? 0xff : buffer.get() & 0xff;
        }
    }

    public void rewind(int nb_byte) {
        byte_cursor = byte_cursor - nb_byte;
    }

    public boolean isEos() {return (byte_cursor >= length);}

    public Header early_match() {
        buffer.mark();
        if (length < 2 || length > 19) {
            buffer.reset();
            return null;

        }

        switch(current_byte) {
            case 0x19 :
                if ( length>2 && next_byte == 0x8){
                    if (length <= 2) {
                        buffer.reset();
                        return null;

                    }
                    switch(buffer.get()) {
                        case (byte)0x54 :
                            if (length == 19 && buffer.get() == (byte)0x21 && buffer.get() == (byte)0x62 && buffer.get() == (byte)0x1e && buffer.get() == (byte)0xa4 && buffer.get() == (byte)0xd8 && buffer.get() == (byte)0x7a && buffer.get() == (byte)0x16 && buffer.get() == (byte)0x1d && buffer.get() == (byte)0x14 && buffer.get() == (byte)0x1f && buffer.get() == (byte)0xc2 && buffer.get() == (byte)0xc7 && buffer.get() == (byte)0xb0 && buffer.get() == (byte)0xd3 && buffer.get() == (byte)0x1a && buffer.get() == (byte)0xaf)
                                return Header.ACCESS_CONTROL_ALLOW_ORIGIN;

                            buffer.reset();
                            return null;

                        
                        case (byte)0x5a :
                            if (length <= 3) {
                                buffer.reset();
                                return null;

                            }
                            switch(buffer.get()) {
                                case (byte)0xd2 :
                                    if (length <= 4) {
                                        buffer.reset();
                                        return null;

                                    }
                                    switch(buffer.get()) {
                                        case (byte)0xb1 :
                                            if (length <= 5) {
                                                buffer.reset();
                                                return null;

                                            }
                                            switch(buffer.get()) {
                                                case (byte)0x27 :
                                                    if (length == 10 && buffer.get() == (byte)0x1d && buffer.get() == (byte)0x88 && buffer.get() == (byte)0x2a && buffer.get() == (byte)0x7f)
                                                        return Header.ACCEPT_CHARSET;

                                                    buffer.reset();
                                                    return null;

                                                
                                                case (byte)0x6a :
                                                    if (length == 11 && buffer.get() == (byte)0x21 && buffer.get() == (byte)0xe4 && buffer.get() == (byte)0x35 && buffer.get() == (byte)0x53 && buffer.get() == (byte)0x7f)
                                                        return Header.ACCEPT_ENCODING;

                                                    buffer.reset();
                                                    return null;

                                                
                                            }
                                            buffer.reset();
                                            return null;

                                        
                                        case (byte)0xb5 :
                                            if (length <= 5) {
                                                buffer.reset();
                                                return null;

                                            }
                                            switch(buffer.get()) {
                                                case (byte)0x3 :
                                                    if (length == 11 && buffer.get() == (byte)0xaa && buffer.get() == (byte)0x6b && buffer.get() == (byte)0x47 && buffer.get() == (byte)0x31 && buffer.get() == (byte)0x7f)
                                                        return Header.ACCEPT_LANGUAGE;

                                                    buffer.reset();
                                                    return null;

                                                
                                                case (byte)0x83 :
                                                    if (length == 9 && buffer.get() == (byte)0xaa && buffer.get() == (byte)0x62 && buffer.get() == (byte)0xa3)
                                                        return Header.ACCEPT_RANGES;

                                                    buffer.reset();
                                                    return null;

                                                
                                            }
                                            buffer.reset();
                                            return null;

                                        
                                    }
                                    buffer.reset();
                                    return null;

                                
                                case (byte)0xd3 :
                                    if (length == 4)
                                        return Header.ACCEPT ;

                                    buffer.reset();
                                    return null;

                                
                            }
                            buffer.reset();
                            return null;

                        
                    }
                    buffer.reset();
                    return null;

                }
                buffer.reset();
                return null;

            
            case 0x1c :
                if (length == 2 && next_byte == 0xc5)
                    return Header.AGE;

                buffer.reset();
                return null;

            
            case 0x1d :
                switch(next_byte) {
                    case 0x14 :
                        if (length == 4 && buffer.get() == (byte)0x1f && buffer.get() == (byte)0xc7)
                            return Header.ALLOW;

                        buffer.reset();
                        return null;

                    
                    case 0xa9 :
                        if (length == 9 && buffer.get() == (byte)0x9c && buffer.get() == (byte)0xf6 && buffer.get() == (byte)0x1b && buffer.get() == (byte)0xd8 && buffer.get() == (byte)0xd2 && buffer.get() == (byte)0x63 && buffer.get() == (byte)0xd5)
                            return Header.AUTHORIZATION;

                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0x20 :
                if (length == 9 && next_byte == 0xc9 && buffer.get() == (byte)0x39 && buffer.get() == (byte)0x56 && buffer.get() == (byte)0x21 && buffer.get() == (byte)0xea && buffer.get() == (byte)0x4d && buffer.get() == (byte)0x87 && buffer.get() == (byte)0xa3)
                    return Header.CACHE_CONTROL;

                buffer.reset();
                return null;

            
            case 0x21 :
                switch(next_byte) {
                    case 0xcf :
                        if (length == 4 && buffer.get() == (byte)0xd4 && buffer.get() == (byte)0xc5)
                            return Header.COOKIE;

                        buffer.reset();
                        return null;

                    
                    case 0xea :
                        if ( length>5 && buffer.get() == (byte)0x49 && buffer.get() == (byte)0x6a && buffer.get() == (byte)0x4a){
                            if (length <= 5) {
                                buffer.reset();
                                return null;

                            }
                            switch(buffer.get()) {
                                case (byte)0xc5 :
                                    if (length == 11 && buffer.get() == (byte)0xa8 && buffer.get() == (byte)0x87 && buffer.get() == (byte)0x90 && buffer.get() == (byte)0xd5 && buffer.get() == (byte)0x4d)
                                        return Header.CONTENT_ENCODING;

                                    buffer.reset();
                                    return null;

                                
                                case (byte)0xc9 :
                                    if (length == 9 && buffer.get() == (byte)0xf5 && buffer.get() == (byte)0x59 && buffer.get() == (byte)0x7f)
                                        return Header.CONTENT_TYPE;

                                    buffer.reset();
                                    return null;

                                
                                case (byte)0xd2 :
                                    if (length == 13 && buffer.get() == (byte)0x19 && buffer.get() == (byte)0x15 && buffer.get() == (byte)0x9d && buffer.get() == (byte)0x6 && buffer.get() == (byte)0x49 && buffer.get() == (byte)0x8f && buffer.get() == (byte)0x57)
                                        return Header.CONTENT_DISPOSITION;

                                    buffer.reset();
                                    return null;

                                
                                case (byte)0xd4 :
                                    if (length <= 6) {
                                        buffer.reset();
                                        return null;

                                    }
                                    switch(buffer.get()) {
                                        case (byte)0xe :
                                            if (length == 11 && buffer.get() == (byte)0xa9 && buffer.get() == (byte)0xad && buffer.get() == (byte)0x1c && buffer.get() == (byte)0xc5)
                                                return Header.CONTENT_LANGUAGE;

                                            buffer.reset();
                                            return null;

                                        
                                        case (byte)0x16 :
                                            if (length == 10 && buffer.get() == (byte)0xa9 && buffer.get() == (byte)0x93 && buffer.get() == (byte)0x3f)
                                                return Header.CONTENT_LENGTH;

                                            buffer.reset();
                                            return null;

                                        
                                        case (byte)0x1c :
                                            if (length == 11 && buffer.get() == (byte)0x83 && buffer.get() == (byte)0x49 && buffer.get() == (byte)0x8f && buffer.get() == (byte)0x57)
                                                return Header.CONTENT_LOCATION;

                                            buffer.reset();
                                            return null;

                                        
                                    }
                                    buffer.reset();
                                    return null;

                                
                                case (byte)0xd6 :
                                    if (length == 9 && buffer.get() == (byte)0xe && buffer.get() == (byte)0xa9 && buffer.get() == (byte)0x8b)
                                        return Header.CONTENT_RANGE;

                                    buffer.reset();
                                    return null;

                                
                            }
                            buffer.reset();
                            return null;

                        }
                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0x2a :
                if (length == 3 && next_byte == 0x47 && buffer.get() == (byte)0x37)
                    return Header.ETAG;

                buffer.reset();
                return null;

            
            case 0x2f :
                if ( length>2 && next_byte == 0x9a){
                    if (length <= 2) {
                        buffer.reset();
                        return null;

                    }
                    switch(buffer.get()) {
                        case (byte)0xca :
                            if (length == 5 && buffer.get() == (byte)0x44 && buffer.get() == (byte)0xff)
                                return Header.EXPECT;

                            buffer.reset();
                            return null;

                        
                        case (byte)0xcd :
                            if (length == 5 && buffer.get() == (byte)0x61 && buffer.get() == (byte)0x51)
                                return Header.EXPIRES;

                            buffer.reset();
                            return null;

                        
                    }
                    buffer.reset();
                    return null;

                }
                buffer.reset();
                return null;

            
            case 0x34 :
                if ( length>2 && next_byte == 0xab){
                    if (length <= 2) {
                        buffer.reset();
                        return null;

                    }
                    switch(buffer.get()) {
                        case (byte)0x52 :
                            if (length <= 3) {
                                buffer.reset();
                                return null;

                            }
                            switch(buffer.get()) {
                                case (byte)0x34 :
                                    if (length == 6 && buffer.get() == (byte)0x92 && buffer.get() == (byte)0x7f)
                                        return Header.IF_MATCH;

                                    buffer.reset();
                                    return null;

                                
                                case (byte)0x79 :
                                    if (length == 12 && buffer.get() == (byte)0xd && buffer.get() == (byte)0x29 && buffer.get() == (byte)0x8b && buffer.get() == (byte)0x22 && buffer.get() == (byte)0xc8 && buffer.get() == (byte)0x35 && buffer.get() == (byte)0x44 && buffer.get() == (byte)0x2f)
                                        return Header.IF_MODIFIED_SINCE;

                                    buffer.reset();
                                    return null;

                                
                            }
                            buffer.reset();
                            return null;

                        
                        case (byte)0x54 :
                            if (length == 9 && buffer.get() == (byte)0x7a && buffer.get() == (byte)0x8a && buffer.get() == (byte)0xb5 && buffer.get() == (byte)0x23 && buffer.get() == (byte)0x49 && buffer.get() == (byte)0x27)
                                return Header.IF_NONE_MATCH;

                            buffer.reset();
                            return null;

                        
                        case (byte)0x58 :
                            if (length == 6 && buffer.get() == (byte)0x3a && buffer.get() == (byte)0xa6 && buffer.get() == (byte)0x2f)
                                return Header.IF_RANGE;

                            buffer.reset();
                            return null;

                        
                        case (byte)0x5b :
                            if (length == 14 && buffer.get() == (byte)0x55 && buffer.get() == (byte)0x27 && buffer.get() == (byte)0x90 && buffer.get() == (byte)0xd2 && buffer.get() == (byte)0x98 && buffer.get() == (byte)0xb2 && buffer.get() == (byte)0x2c && buffer.get() == (byte)0x83 && buffer.get() == (byte)0x54 && buffer.get() == (byte)0x42 && buffer.get() == (byte)0xff)
                                return Header.IF_UNMODIFIED_SINCE;

                            buffer.reset();
                            return null;

                        
                    }
                    buffer.reset();
                    return null;

                }
                buffer.reset();
                return null;

            
            case 0x41 :
                switch(next_byte) {
                    case 0x52 :
                        if (length == 7 && buffer.get() == (byte)0xb1 && buffer.get() == (byte)0xe && buffer.get() == (byte)0x7e && buffer.get() == (byte)0xa6 && buffer.get() == (byte)0x2f)
                            return Header.SET_COOKIE;

                        buffer.reset();
                        return null;

                    
                    case 0x6c :
                        if (length == 5 && buffer.get() == (byte)0xee && buffer.get() == (byte)0x5b && buffer.get() == (byte)0x3f)
                            return Header.SERVER;

                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0x42 :
                if (length == 17 && next_byte == 0x6c && buffer.get() == (byte)0x31 && buffer.get() == (byte)0x12 && buffer.get() == (byte)0xb2 && buffer.get() == (byte)0x6c && buffer.get() == (byte)0x1d && buffer.get() == (byte)0x48 && buffer.get() == (byte)0xac && buffer.get() == (byte)0xf6 && buffer.get() == (byte)0x25 && buffer.get() == (byte)0x64 && buffer.get() == (byte)0x14 && buffer.get() == (byte)0x96 && buffer.get() == (byte)0xd8 && buffer.get() == (byte)0x64 && buffer.get() == (byte)0xfa)
                    return Header.STRICT_TRANSPORT_SECURITY;

                buffer.reset();
                return null;

            
            case 0x4d :
                if (length == 12 && next_byte == 0x83 && buffer.get() == (byte)0xa9 && buffer.get() == (byte)0x12 && buffer.get() == (byte)0x96 && buffer.get() == (byte)0xc5 && buffer.get() == (byte)0x8b && buffer.get() == (byte)0x51 && buffer.get() == (byte)0xf && buffer.get() == (byte)0x21 && buffer.get() == (byte)0xaa && buffer.get() == (byte)0x9b)
                    return Header.TRANSFER_ENCODING;

                buffer.reset();
                return null;

            
            case 0x90 :
                if (length == 3 && next_byte == 0x69 && buffer.get() == (byte)0x2f)
                    return Header.DATE;

                buffer.reset();
                return null;

            
            case 0x96 :
                if (length == 3 && next_byte == 0xc3 && buffer.get() == (byte)0xd3)
                    return Header.FROM;

                buffer.reset();
                return null;

            
            case 0x9c :
                if (length == 3 && next_byte == 0xe8 && buffer.get() == (byte)0x4f)
                    return Header.HOST;

                buffer.reset();
                return null;

            
            case 0xa0 :
                switch(next_byte) {
                    case 0x68 :
                        if (length == 9 && buffer.get() == (byte)0x4a && buffer.get() == (byte)0xd4 && buffer.get() == (byte)0x9e && buffer.get() == (byte)0x43 && buffer.get() == (byte)0x4a && buffer.get() == (byte)0x62 && buffer.get() == (byte)0xc9)
                            return Header.LAST_MODIFIED;

                        buffer.reset();
                        return null;

                    
                    case 0xd5 :
                        if (length == 3 && buffer.get() == (byte)0x75)
                            return Header.LINK;

                        buffer.reset();
                        return null;

                    
                    case 0xe4 :
                        if (length == 6 && buffer.get() == (byte)0x1a && buffer.get() == (byte)0x4c && buffer.get() == (byte)0x7a && buffer.get() == (byte)0xbf)
                            return Header.LOCATION;

                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0xa4 :
                if (length == 9 && next_byte == 0x7e && buffer.get() == (byte)0x56 && buffer.get() == (byte)0x94 && buffer.get() == (byte)0xf6 && buffer.get() == (byte)0x78 && buffer.get() == (byte)0x1d && buffer.get() == (byte)0x92 && buffer.get() == (byte)0x23)
                    return Header.MAX_FORWARDS;

                buffer.reset();
                return null;

            
            case 0xae :
                if ( length>7 && next_byte == 0xc3 && buffer.get() == (byte)0xf9 && buffer.get() == (byte)0xf4 && buffer.get() == (byte)0xb0 && buffer.get() == (byte)0xed && buffer.get() == (byte)0x4c){
                    if (length <= 7) {
                        buffer.reset();
                        return null;

                    }
                    switch(buffer.get()) {
                        case (byte)0xe5 :
                            if (length == 13 && buffer.get() == (byte)0xa9 && buffer.get() == (byte)0x26 && buffer.get() == (byte)0x20 && buffer.get() == (byte)0xd2 && buffer.get() == (byte)0x5f)
                                return Header.PROXY_AUTHENTICATE;

                            buffer.reset();
                            return null;

                        
                        case (byte)0xe7 :
                            if (length == 14 && buffer.get() == (byte)0xb0 && buffer.get() == (byte)0xde && buffer.get() == (byte)0xc6 && buffer.get() == (byte)0x93 && buffer.get() == (byte)0x1e && buffer.get() == (byte)0xaf)
                                return Header.PROXY_AUTHORIZATION;

                            buffer.reset();
                            return null;

                        
                    }
                    buffer.reset();
                    return null;

                }
                buffer.reset();
                return null;

            
            case 0xb0 :
                switch(next_byte) {
                    case 0x75 :
                        if (length == 4 && buffer.get() == (byte)0x4c && buffer.get() == (byte)0x5f)
                            return Header.RANGE;

                        buffer.reset();
                        return null;

                    
                    case 0xa9 :
                        if (length == 8 && buffer.get() == (byte)0xb3 && buffer.get() == (byte)0xd2 && buffer.get() == (byte)0xc3 && buffer.get() == (byte)0x95 && buffer.get() == (byte)0x25 && buffer.get() == (byte)0xb3)
                            return Header.RETRY_AFTER;

                        buffer.reset();
                        return null;

                    
                    case 0xb2 :
                        if (length <= 2) {
                            buffer.reset();
                            return null;

                        }
                        switch(buffer.get()) {
                            case (byte)0x96 :
                                if (length == 5 && buffer.get() == (byte)0xc2 && buffer.get() == (byte)0xd9)
                                    return Header.REFERER;

                                buffer.reset();
                                return null;

                            
                            case (byte)0xd8 :
                                if (length == 5 && buffer.get() == (byte)0x54 && buffer.get() == (byte)0x4f)
                                    return Header.REFRESH;

                                buffer.reset();
                                return null;

                            
                        }
                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0xb5 :
                if (length == 7 && next_byte == 0x5 && buffer.get() == (byte)0xb1 && buffer.get() == (byte)0x61 && buffer.get() == (byte)0xcc && buffer.get() == (byte)0x5a && buffer.get() == (byte)0x93)
                    return Header.USER_AGENT;

                buffer.reset();
                return null;

            
            case 0xb8 :
                switch(next_byte) {
                    case 0x3b :
                        if (length == 8 && buffer.get() == (byte)0x53 && buffer.get() == (byte)0x39 && buffer.get() == (byte)0xec && buffer.get() == (byte)0x32 && buffer.get() == (byte)0x7d && buffer.get() == (byte)0x7f)
                            return Header.AUTHORITY;

                        buffer.reset();
                        return null;

                    
                    case 0x82 :
                        if (length == 5 && buffer.get() == (byte)0x4e && buffer.get() == (byte)0x5a && buffer.get() == (byte)0x4b)
                            return Header.SCHEME;

                        buffer.reset();
                        return null;

                    
                    case 0x84 :
                        if (length == 5 && buffer.get() == (byte)0x8d && buffer.get() == (byte)0x36 && buffer.get() == (byte)0xa3)
                            return Header.STATUS;

                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0xb9 :
                switch(next_byte) {
                    case 0x49 :
                        if (length == 5 && buffer.get() == (byte)0x53 && buffer.get() == (byte)0x39 && buffer.get() == (byte)0xe4)
                            return Header.METHOD;

                        buffer.reset();
                        return null;

                    
                    case 0x58 :
                        if (length == 4 && buffer.get() == (byte)0xd3 && buffer.get() == (byte)0x3f)
                            return Header.PATH;

                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0xee :
                switch(next_byte) {
                    case 0x3b :
                        if (length == 4 && buffer.get() == (byte)0x3d && buffer.get() == (byte)0x7f)
                            return Header.VARY;

                        buffer.reset();
                        return null;

                    
                    case 0x61 :
                        if (length == 3 && buffer.get() == (byte)0xff)
                            return Header.VIA;

                        buffer.reset();
                        return null;

                    
                }
                buffer.reset();
                return null;

            
            case 0xf1 :
                if (length == 12 && next_byte == 0xe3 && buffer.get() == (byte)0xc2 && buffer.get() == (byte)0xc3 && buffer.get() == (byte)0xb5 && buffer.get() == (byte)0x33 && buffer.get() == (byte)0x96 && buffer.get() == (byte)0xa4 && buffer.get() == (byte)0x98 && buffer.get() == (byte)0x83 && buffer.get() == (byte)0x49 && buffer.get() == (byte)0x7f)
                    return Header.WWW_AUTHENTICATE;

                buffer.reset();
                return null;

            
        }
        buffer.reset();
        return null;

    }

    // TODO: rename to early_match_value
    public Header early_match_header(Header wellknown_name)  {
        buffer.mark();
        switch (wellknown_name) {

            case METHOD:

                if (length < 3 || length > 4) {
                    buffer.reset();
                    return null;

                }

                switch(current_byte) {
                    case 0xc5 :
                        if (length == 3 && next_byte == 0x83 && buffer.get() == (byte)0x7f)
                            return Header.METHOD_GET;

                        buffer.reset();
                        return null;

                    case 0xd7 :
                        if (length == 4 && next_byte == 0xab && buffer.get() == (byte)0x76 && buffer.get() == (byte)0xff)
                            return Header.METHOD_POST;

                        buffer.reset();
                        return null;

                }
                buffer.reset();
                return null;

            case ACCEPT_ENCODING:

                if (length != 10) {
                    buffer.reset();
                    return null;

                }

                if ( current_byte == 0x9b && next_byte == 0xd9 && buffer.get() == (byte)0xab && buffer.get() == (byte)0xfa && buffer.get() == (byte)0x52 && buffer.get() == (byte)0x42 && buffer.get() == (byte)0xcb && buffer.get() == (byte)0x40 && buffer.get() == (byte)0xd2 && buffer.get() == (byte)0x5f)
                    return Header.ACCEPT_ENCODING_GZIP_DEFLATE;

                buffer.reset();
                return null;

            case STATUS:

                if (length < 2 || length > 3) {
                    buffer.reset();
                    return null;

                }

                switch(current_byte) {
                    case 0x10 :
                        switch(next_byte) {
                            case 0x1 :
                                if (length == 2)
                                    return Header.STATUS_200 ;

                                buffer.reset();
                                return null;

                            case 0x1a :
                                if (length == 2)
                                    return Header.STATUS_204 ;

                                buffer.reset();
                                return null;

                            case 0x1c :
                                if (length == 2)
                                    return Header.STATUS_206 ;

                                buffer.reset();
                                return null;

                        }
                        buffer.reset();
                        return null;

                    case 0x64 :
                        if (length == 3 && next_byte == 0xd && buffer.get() == (byte)0x7f)
                            return Header.STATUS_304;

                        buffer.reset();
                        return null;

                    case 0x68 :
                        switch(next_byte) {
                            case 0x0 :
                                if (length == 2)
                                    return Header.STATUS_400 ;

                                buffer.reset();
                                return null;

                            case 0xd :
                                if (length == 3 && buffer.get() == (byte)0x7f)
                                    return Header.STATUS_404;

                                buffer.reset();
                                return null;

                        }
                        buffer.reset();
                        return null;

                    case 0x6c :
                        if (length == 2 && next_byte == 0x0)
                            return Header.STATUS_500;

                        buffer.reset();
                        return null;

                }
                buffer.reset();
                return null;

            case SCHEME:

                if (length < 3 || length > 4) {
                    buffer.reset();
                    return null;

                }

                if ( length>2 && current_byte == 0x9d && next_byte == 0x29){
                    switch(buffer.get()) {
                        case (byte)0xad :
                            if (length == 4 && buffer.get() == (byte)0x1f)
                                return Header.SCHEME_HTTPS;

                            buffer.reset();
                            return null;

                        case (byte)0xaf :
                            if (length == 3)
                                return Header.SCHEME_HTTP ;

                            buffer.reset();
                            return null;

                    }
                    buffer.reset();
                    return null;

                }
                buffer.reset();
                return null;

            case PATH:

                if (length < 1 || length > 8) {
                    buffer.reset();
                    return null;

                }

                switch(current_byte) {
                    case 0x60 :
                        if (length == 8 && next_byte == 0xd5 && buffer.get() == (byte)0x48 && buffer.get() == (byte)0x5f && buffer.get() == (byte)0x2b && buffer.get() == (byte)0xce && buffer.get() == (byte)0x9a && buffer.get() == (byte)0x68)
                            return Header.PATH_SLASH_INDEX_HTML;

                        buffer.reset();
                        return null;

                    case 0x63 :
                        if (length == 1)
                            return Header.PATH_SLASH ;

                        buffer.reset();
                        return null;

                }
                buffer.reset();
                return null;
        }

        buffer.reset();
        return null;
    }
}

