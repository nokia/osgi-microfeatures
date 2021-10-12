// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;
import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class HeaderField {
    private final DynamicTable dt;
    private final Logger  logger;
    private final long    connection_id;
    public HeaderField(DynamicTable dt, Logger logger, long connection_id) {
        this.dt=dt;
        this.logger = logger;
        this.connection_id = connection_id;
    }

    HeaderField() {
        this(new DynamicTable(256), Logger.getLogger("hpack.HeaderField"),System.currentTimeMillis());
    }

    HeaderField(DynamicTable dt) {
        this(dt, Logger.getLogger("hpack.HeaderField"),System.currentTimeMillis());
    }

    public interface HeaderOnTheFly {
      void add_header(String name, String value);
      void add_header(Header well_known, String value);
      void add_header(Header well_known);
    }

    private int name_value(boolean indexing, ByteBuffer bb, HeaderOnTheFly hotf) {
      Header wellknown_name   = null;
      String name             = null;
      int    length_info_name = decode_len_huffman_indicator(bb);
      if (length_info_name == ERROR) {
        return ERROR;
      } else if (length_info_name == UNDERFLOW) {
        return UNDERFLOW;
      } else if (length_info_name == HUFFMAN_INDICATOR || length_info_name ==0) {
        // FIXME : empty name should probably considered an error
        name = "";
        return ERROR;
      } else if ((length_info_name & HUFFMAN_INDICATOR) == HUFFMAN_INDICATOR) {
        int length_name = length_info_name & (MASK_HUFFMAN_INDICATOR) ;
        ByteBufferBitField bf = new ByteBufferBitField(bb, length_name);
        wellknown_name = bf.early_match();
        if (wellknown_name == null) {
          ByteBuffer out = ByteBuffer.allocate(length_name * 2 + 1); // compression can't be better than 5/8
          HuffmanDecoder.parse(bf, out);
          out.flip();
          byte[] tmp_out = new byte[out.remaining()];
          out.get(tmp_out);
          name = new String(tmp_out, StandardCharsets.ISO_8859_1);
        }
      } else {
        byte[] tmp = new byte[length_info_name];
        bb.get(tmp);
        wellknown_name = WellKnownString.match_name(tmp);
        if (wellknown_name == null)
          name = new String(tmp, StandardCharsets.ISO_8859_1);
      }
      assert (name != null || wellknown_name != null) ;

      // value
      Header wellknown_name_value = null;
      String value                = null;
      int    length_info_value    = decode_len_huffman_indicator(bb);
      if (length_info_value == ERROR) {
        return ERROR;
      } else if (length_info_value == UNDERFLOW) {
        return UNDERFLOW;
      }

      if (length_info_value == HUFFMAN_INDICATOR || length_info_value == 0) {
        value = "";
      } else if ((length_info_value & HUFFMAN_INDICATOR) == HUFFMAN_INDICATOR) {
        int length_value = length_info_value & (MASK_HUFFMAN_INDICATOR) ;
        assert length_value > 0 : "length is :"+length_value;
        ByteBufferBitField bf  = new ByteBufferBitField(bb, length_value);
        if (wellknown_name != null && wellknown_name.valued_variants_exist) {
          wellknown_name_value = bf.early_match_header(wellknown_name);
        }
        if (wellknown_name_value == null ) {
          ByteBuffer out = ByteBuffer.allocate(length_value * 2 + 1); // compression can't be better than 5/8
          HuffmanDecoder.parse(bf, out);
          out.flip();
          byte[] tmp_out = new byte[out.remaining()];
          out.get(tmp_out);
          value = new String(tmp_out, StandardCharsets.ISO_8859_1);

        }
      } else {
        byte[] tmp = new byte[length_info_value];
        bb.get(tmp);
        if (wellknown_name != null && wellknown_name.valued_variants_exist)
          wellknown_name_value = WellKnownString.match_value(wellknown_name,tmp);
        if (wellknown_name_value == null) {
          value = new String(tmp, StandardCharsets.ISO_8859_1);
        }
      }

      assert value != null || wellknown_name_value != null ;

      if (wellknown_name_value != null) {
        hotf.add_header(wellknown_name_value);
        insert_dynamic_table(wellknown_name_value);
      } else {
        if (wellknown_name != null) {
          hotf.add_header(wellknown_name, value);
          if (indexing)
            insert_dynamic_table(wellknown_name, value);
        } else {
          hotf.add_header(name, value);
          if (indexing)
            insert_dynamic_table(name, value);
        }
      }
      return OK;
    }

    private int value_from_dynamic_table(DynamicTable.Entry entry, boolean indexing, ByteBuffer bb, HeaderOnTheFly hotf) {
      Header wellknown_name = entry.has_wellknown_value() ? entry.get_wellknown_name() : null;
      // value
      Header wellknown_name_value = null;
      String value                = null;
      int    length_info_value    = decode_len_huffman_indicator(bb);
      if (length_info_value == ERROR) {
        return ERROR;
      } else if (length_info_value == UNDERFLOW) {
        return UNDERFLOW;
      }

      if (length_info_value == HUFFMAN_INDICATOR || length_info_value == 0) {
        value = "";
      } else if ((length_info_value & HUFFMAN_INDICATOR) == HUFFMAN_INDICATOR) {
        int length_value = length_info_value & (MASK_HUFFMAN_INDICATOR) ;
        ByteBufferBitField bf  = new ByteBufferBitField(bb, length_value);
        if (wellknown_name !=null && wellknown_name.valued_variants_exist)
          wellknown_name_value = bf.early_match_header(wellknown_name);
        if (wellknown_name_value == null ) {
          ByteBuffer out = ByteBuffer.allocate(length_value * 2 + 1); // compression can't be better than 5/8
          HuffmanDecoder.parse(bf, out);
          out.flip();
          byte[] tmp_out = new byte[out.remaining()];
          out.get(tmp_out);
          value = new String(tmp_out, StandardCharsets.ISO_8859_1);

        }
      } else {
        byte[] tmp = new byte[length_info_value];
        bb.get(tmp);
        if (wellknown_name !=null && wellknown_name.valued_variants_exist)
          wellknown_name_value = WellKnownString.match_value(wellknown_name,tmp);
        if (wellknown_name_value == null) {
          value = new String(tmp, StandardCharsets.ISO_8859_1);
        }
      }

      assert value != null || wellknown_name_value != null ;

      if (wellknown_name_value != null) {
        hotf.add_header(wellknown_name_value);
        if (indexing)
          insert_dynamic_table(wellknown_name_value);
      } else {
        entry.notif_reuse_name(hotf, value);
        if (indexing)
          insert_dynamic_table(entry, value);
      }

      return OK;
    }

    private int value_from_wellknown_name(Header wellknown_name, boolean indexing, ByteBuffer bb, HeaderOnTheFly hotf) {
      Header wellknown_name_value = null;
      String value                = null;
      int    length_info_value    = decode_len_huffman_indicator(bb);
      if (length_info_value == ERROR) {
        return ERROR;
      } else if (length_info_value == UNDERFLOW) {
        return UNDERFLOW;
      }

      if (length_info_value == HUFFMAN_INDICATOR || length_info_value == 0) {
        value = "";
      } else if ((length_info_value & HUFFMAN_INDICATOR) == HUFFMAN_INDICATOR) {
        int length_value = length_info_value & (MASK_HUFFMAN_INDICATOR) ;
        ByteBufferBitField bf  = new ByteBufferBitField(bb, length_value);
        if (wellknown_name!=null && wellknown_name.valued_variants_exist == true)
          wellknown_name_value = bf.early_match_header(wellknown_name);
        if (wellknown_name_value == null ) {
          ByteBuffer out = ByteBuffer.allocate(length_value * 2 + 1); // compression can't be better than 5/8
          HuffmanDecoder.parse(bf, out);
          out.flip();
          byte[] tmp_out = new byte[out.remaining()];
          out.get(tmp_out);
          value = new String(tmp_out, StandardCharsets.ISO_8859_1);

        }
      } else {
        byte[] tmp = new byte[length_info_value];
        bb.get(tmp);
        if (wellknown_name!=null && wellknown_name.valued_variants_exist == true)
          wellknown_name_value = WellKnownString.match_value(wellknown_name,tmp);
        if (wellknown_name_value == null) {
          value = new String(tmp, StandardCharsets.ISO_8859_1);
        }
      }

      assert value != null || wellknown_name_value != null ;

      if (wellknown_name_value != null) {
        hotf.add_header(wellknown_name_value);
        if (indexing)
          insert_dynamic_table(wellknown_name_value);
      } else {
        hotf.add_header(wellknown_name, value);
        if (indexing)
          insert_dynamic_table(wellknown_name, value);
      }
      return OK;
    }

    /*public void add_header(String name, String value) {
        System.out.println(" !!! added : "+name+" -> "+value);
    }*/

    public void insert_dynamic_table(Header header) {
        dt.insert(header,(byte)0);
    }

    public void insert_dynamic_table(Header header, String value) {
        dt.insert(header, value, (byte)0);
    }

    public void insert_dynamic_table(DynamicTable.Entry entry, String value) {
        dt.insert(entry,value,(byte)0);
    }

    public void insert_dynamic_table(String name, String value) {
        dt.insert(name,value,(byte)0);
    }

    public int add_header_from_dynamic_table(HeaderOnTheFly hotf,long position) {
        if ( ! dt.isValid(position) )
          return ERROR;
        DynamicTable.Entry entry=dt.get(position);
        entry.notif(hotf);
        return OK;
    }

    public DynamicTable.Entry get_from_dynamic_table(long position) {
        return dt.get(position);
    }

    /**
     *  In terms of ByteBuffer this is a guard function, it will check
     *  the bytes composing the string are all there or return UNDERFLOW.
     */
    public int decode_len_huffman_indicator(ByteBuffer bb ) {
        if (!bb.hasRemaining())
          return UNDERFLOW;
        byte b = bb.get();
        switch(b) {
            case (byte)0xFF: {
              long long_length = UnsignedInteger.decode(7, b, bb);
              if (long_length == UnsignedInteger.UNDERFLOW)
                return UNDERFLOW;
              else if (long_length == UnsignedInteger.ERROR)
                return ERROR;
              int length = (int) long_length;
              if (bb.remaining()<length)
                return UNDERFLOW;
              return length | HUFFMAN_INDICATOR;
            }

            case (byte)0x7F: {
              long long_length = UnsignedInteger.decode(7, b, bb);
              if (long_length == UnsignedInteger.UNDERFLOW)
                return UNDERFLOW;
              else if (long_length == UnsignedInteger.ERROR)
                return ERROR;
              int length = (int) long_length;
              if (bb.remaining()<length)
                return UNDERFLOW;
              return length;
            }
            default:
                if ((b &0x80) == 0) {
                    int length = (int)b;
                    if (bb.remaining()<length)
                      return UNDERFLOW;
                    return length;
                } else {
                    int length = b & 0x7F;
                    if (bb.remaining()<length)
                      return UNDERFLOW;
                    return length | HUFFMAN_INDICATOR;
                }
        }
    }

    public long decode(int n, byte b, ByteBuffer bb) {
        return UnsignedInteger.decode(n,b,bb);
    }

    public static final int ERROR                  = 0xFFFF;
    public static final int UNDERFLOW              = 0xFFFE;
    public static final int OK                     = 0x0000;
    public static final int HUFFMAN_INDICATOR      = 0x8000;
    public static final int MASK_HUFFMAN_INDICATOR = 0x7FFF;

    public int bb_position = -1;

    public final int parser(ByteBuffer bb, HeaderOnTheFly hotf) {
        while (bb.remaining() != 0) {
            bb_position = bb.position();
            byte b = bb.get();
            //System.err.println("  -----------------  Pocessing : 0x"+Integer.toHexString(b&0xFF));
            switch (b) {


              case (byte)0x80:
                logger.warn("cid:"+connection_id+" received 0x80 which is forbidden");
                return ERROR;

              case (byte)0x81:
                hotf.add_header(Header.AUTHORITY ,"" );
                break;

              case (byte)0x82:
                hotf.add_header(Header.METHOD_GET);
                break;

              case (byte)0x83:
                hotf.add_header(Header.METHOD_POST);
                break;

              case (byte)0x84:
                hotf.add_header(Header.PATH_SLASH);
                break;

              case (byte)0x85:
                hotf.add_header(Header.PATH_SLASH_INDEX_HTML);
                break;

              case (byte)0x86:
                hotf.add_header(Header.SCHEME_HTTP);
                break;

              case (byte)0x87:
                hotf.add_header(Header.SCHEME_HTTPS);
                break;

              case (byte)0x88:
                hotf.add_header(Header.STATUS_200);
                break;

              case (byte)0x89:
                hotf.add_header(Header.STATUS_204);
                break;

              case (byte)0x8a:
                hotf.add_header(Header.STATUS_206);
                break;

              case (byte)0x8b:
                hotf.add_header(Header.STATUS_304);
                break;

              case (byte)0x8c:
                hotf.add_header(Header.STATUS_400);
                break;

              case (byte)0x8d:
                hotf.add_header(Header.STATUS_404);
                break;

              case (byte)0x8e:
                hotf.add_header(Header.STATUS_500);
                break;

              case (byte)0x8f:
                hotf.add_header(Header.ACCEPT_CHARSET ,"" );
                break;

              case (byte)0x90:
                hotf.add_header(Header.ACCEPT_ENCODING_GZIP_DEFLATE);
                break;

              case (byte)0x91:
                hotf.add_header(Header.ACCEPT_LANGUAGE ,"" );
                break;

              case (byte)0x92:
                hotf.add_header(Header.ACCEPT_RANGES ,"" );
                break;

              case (byte)0x93:
                hotf.add_header(Header.ACCEPT ,"" );
                break;

              case (byte)0x94:
                hotf.add_header(Header.ACCESS_CONTROL_ALLOW_ORIGIN ,"" );
                break;

              case (byte)0x95:
                hotf.add_header(Header.AGE ,"" );
                break;

              case (byte)0x96:
                hotf.add_header(Header.ALLOW ,"" );
                break;

              case (byte)0x97:
                hotf.add_header(Header.AUTHORIZATION ,"" );
                break;

              case (byte)0x98:
                hotf.add_header(Header.CACHE_CONTROL ,"" );
                break;

              case (byte)0x99:
                hotf.add_header(Header.CONTENT_DISPOSITION ,"" );
                break;

              case (byte)0x9a:
                hotf.add_header(Header.CONTENT_ENCODING ,"" );
                break;

              case (byte)0x9b:
                hotf.add_header(Header.CONTENT_LANGUAGE ,"" );
                break;

              case (byte)0x9c:
                hotf.add_header(Header.CONTENT_LENGTH ,"" );
                break;

              case (byte)0x9d:
                hotf.add_header(Header.CONTENT_LOCATION ,"" );
                break;

              case (byte)0x9e:
                hotf.add_header(Header.CONTENT_RANGE ,"" );
                break;

              case (byte)0x9f:
                hotf.add_header(Header.CONTENT_TYPE ,"" );
                break;

              case (byte)0xa0:
                hotf.add_header(Header.COOKIE ,"" );
                break;

              case (byte)0xa1:
                hotf.add_header(Header.DATE ,"" );
                break;

              case (byte)0xa2:
                hotf.add_header(Header.ETAG ,"" );
                break;

              case (byte)0xa3:
                hotf.add_header(Header.EXPECT ,"" );
                break;

              case (byte)0xa4:
                hotf.add_header(Header.EXPIRES ,"" );
                break;

              case (byte)0xa5:
                hotf.add_header(Header.FROM ,"" );
                break;

              case (byte)0xa6:
                hotf.add_header(Header.HOST ,"" );
                break;

              case (byte)0xa7:
                hotf.add_header(Header.IF_MATCH ,"" );
                break;

              case (byte)0xa8:
                hotf.add_header(Header.IF_MODIFIED_SINCE ,"" );
                break;

              case (byte)0xa9:
                hotf.add_header(Header.IF_NONE_MATCH ,"" );
                break;

              case (byte)0xaa:
                hotf.add_header(Header.IF_RANGE ,"" );
                break;

              case (byte)0xab:
                hotf.add_header(Header.IF_UNMODIFIED_SINCE ,"" );
                break;

              case (byte)0xac:
                hotf.add_header(Header.LAST_MODIFIED ,"" );
                break;

              case (byte)0xad:
                hotf.add_header(Header.LINK ,"" );
                break;

              case (byte)0xae:
                hotf.add_header(Header.LOCATION ,"" );
                break;

              case (byte)0xaf:
                hotf.add_header(Header.MAX_FORWARDS ,"" );
                break;

              case (byte)0xb0:
                hotf.add_header(Header.PROXY_AUTHENTICATE ,"" );
                break;

              case (byte)0xb1:
                hotf.add_header(Header.PROXY_AUTHORIZATION ,"" );
                break;

              case (byte)0xb2:
                hotf.add_header(Header.RANGE ,"" );
                break;

              case (byte)0xb3:
                hotf.add_header(Header.REFERER ,"" );
                break;

              case (byte)0xb4:
                hotf.add_header(Header.REFRESH ,"" );
                break;

              case (byte)0xb5:
                hotf.add_header(Header.RETRY_AFTER ,"" );
                break;

              case (byte)0xb6:
                hotf.add_header(Header.SERVER ,"" );
                break;

              case (byte)0xb7:
                hotf.add_header(Header.SET_COOKIE ,"" );
                break;

              case (byte)0xb8:
                hotf.add_header(Header.STRICT_TRANSPORT_SECURITY ,"" );
                break;

              case (byte)0xb9:
                hotf.add_header(Header.TRANSFER_ENCODING ,"" );
                break;

              case (byte)0xba:
                hotf.add_header(Header.USER_AGENT ,"" );
                break;

              case (byte)0xbb:
                hotf.add_header(Header.VARY ,"" );
                break;

              case (byte)0xbc:
                hotf.add_header(Header.VIA ,"" );
                break;

              case (byte)0xbd:
                hotf.add_header(Header.WWW_AUTHENTICATE ,"" );
                break;

              case (byte)0xbe: {
                int ret = add_header_from_dynamic_table(hotf, 62);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xbf: {
                int ret = add_header_from_dynamic_table(hotf, 63);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc0: {
                int ret = add_header_from_dynamic_table(hotf, 64);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc1: {
                int ret = add_header_from_dynamic_table(hotf, 65);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc2: {
                int ret = add_header_from_dynamic_table(hotf, 66);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc3: {
                int ret = add_header_from_dynamic_table(hotf, 67);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc4: {
                int ret = add_header_from_dynamic_table(hotf, 68);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc5: {
                int ret = add_header_from_dynamic_table(hotf, 69);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc6: {
                int ret = add_header_from_dynamic_table(hotf, 70);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc7: {
                int ret = add_header_from_dynamic_table(hotf, 71);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc8: {
                int ret = add_header_from_dynamic_table(hotf, 72);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xc9: {
                int ret = add_header_from_dynamic_table(hotf, 73);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xca: {
                int ret = add_header_from_dynamic_table(hotf, 74);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xcb: {
                int ret = add_header_from_dynamic_table(hotf, 75);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xcc: {
                int ret = add_header_from_dynamic_table(hotf, 76);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xcd: {
                int ret = add_header_from_dynamic_table(hotf, 77);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xce: {
                int ret = add_header_from_dynamic_table(hotf, 78);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xcf: {
                int ret = add_header_from_dynamic_table(hotf, 79);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd0: {
                int ret = add_header_from_dynamic_table(hotf, 80);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd1: {
                int ret = add_header_from_dynamic_table(hotf, 81);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd2: {
                int ret = add_header_from_dynamic_table(hotf, 82);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd3: {
                int ret = add_header_from_dynamic_table(hotf, 83);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd4: {
                int ret = add_header_from_dynamic_table(hotf, 84);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd5: {
                int ret = add_header_from_dynamic_table(hotf, 85);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd6: {
                int ret = add_header_from_dynamic_table(hotf, 86);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd7: {
                int ret = add_header_from_dynamic_table(hotf, 87);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd8: {
                int ret = add_header_from_dynamic_table(hotf, 88);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xd9: {
                int ret = add_header_from_dynamic_table(hotf, 89);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xda: {
                int ret = add_header_from_dynamic_table(hotf, 90);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xdb: {
                int ret = add_header_from_dynamic_table(hotf, 91);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xdc: {
                int ret = add_header_from_dynamic_table(hotf, 92);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xdd: {
                int ret = add_header_from_dynamic_table(hotf, 93);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xde: {
                int ret = add_header_from_dynamic_table(hotf, 94);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xdf: {
                int ret = add_header_from_dynamic_table(hotf, 95);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe0: {
                int ret = add_header_from_dynamic_table(hotf, 96);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe1: {
                int ret = add_header_from_dynamic_table(hotf, 97);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe2: {
                int ret = add_header_from_dynamic_table(hotf, 98);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe3: {
                int ret = add_header_from_dynamic_table(hotf, 99);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe4: {
                int ret = add_header_from_dynamic_table(hotf, 100);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe5: {
                int ret = add_header_from_dynamic_table(hotf, 101);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe6: {
                int ret = add_header_from_dynamic_table(hotf, 102);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe7: {
                int ret = add_header_from_dynamic_table(hotf, 103);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe8: {
                int ret = add_header_from_dynamic_table(hotf, 104);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xe9: {
                int ret = add_header_from_dynamic_table(hotf, 105);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xea: {
                int ret = add_header_from_dynamic_table(hotf, 106);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xeb: {
                int ret = add_header_from_dynamic_table(hotf, 107);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xec: {
                int ret = add_header_from_dynamic_table(hotf, 108);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xed: {
                int ret = add_header_from_dynamic_table(hotf, 109);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xee: {
                int ret = add_header_from_dynamic_table(hotf, 110);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xef: {
                int ret = add_header_from_dynamic_table(hotf, 111);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf0: {
                int ret = add_header_from_dynamic_table(hotf, 112);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf1: {
                int ret = add_header_from_dynamic_table(hotf, 113);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf2: {
                int ret = add_header_from_dynamic_table(hotf, 114);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf3: {
                int ret = add_header_from_dynamic_table(hotf, 115);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf4: {
                int ret = add_header_from_dynamic_table(hotf, 116);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf5: {
                int ret = add_header_from_dynamic_table(hotf, 117);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf6: {
                int ret = add_header_from_dynamic_table(hotf, 118);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf7: {
                int ret = add_header_from_dynamic_table(hotf, 119);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf8: {
                int ret = add_header_from_dynamic_table(hotf, 120);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xf9: {
                int ret = add_header_from_dynamic_table(hotf, 121);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xfa: {
                int ret = add_header_from_dynamic_table(hotf, 122);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xfb: {
                int ret = add_header_from_dynamic_table(hotf, 123);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xfc: {
                int ret = add_header_from_dynamic_table(hotf, 124);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xfd: {
                int ret = add_header_from_dynamic_table(hotf, 125);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xfe: {
                int ret = add_header_from_dynamic_table(hotf, 126);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0xFF:
              {
                long position = decode(7,b,bb);
                if (position == UnsignedInteger.UNDERFLOW)
                  return UNDERFLOW;
                else if (position == UnsignedInteger.ERROR)
                  return ERROR;
                int ret = add_header_from_dynamic_table(hotf,position);
                if (ret == ERROR)
                  return ERROR;
              }
              break;

              case (byte)0x40:
              {
                int name_value_result = name_value(true, bb, hotf);
                if (name_value_result == ERROR)
                  return ERROR;
                else if (name_value_result == UNDERFLOW)
                  return UNDERFLOW;
              }
              break;


              case (byte)0x41:
              { int vfwn=value_from_wellknown_name(Header.AUTHORITY,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x42:
              { int vfwn=value_from_wellknown_name(Header.METHOD,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x43:
              { int vfwn=value_from_wellknown_name(Header.METHOD,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x44:
              { int vfwn=value_from_wellknown_name(Header.PATH,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x45:
              { int vfwn=value_from_wellknown_name(Header.PATH,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x46:
              { int vfwn=value_from_wellknown_name(Header.SCHEME,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x47:
              { int vfwn=value_from_wellknown_name(Header.SCHEME,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x48:
              { int vfwn=value_from_wellknown_name(Header.STATUS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x49:
              { int vfwn=value_from_wellknown_name(Header.STATUS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x4a:
              { int vfwn=value_from_wellknown_name(Header.STATUS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x4b:
              { int vfwn=value_from_wellknown_name(Header.STATUS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x4c:
              { int vfwn=value_from_wellknown_name(Header.STATUS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x4d:
              { int vfwn=value_from_wellknown_name(Header.STATUS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x4e:
              { int vfwn=value_from_wellknown_name(Header.STATUS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x4f:
              { int vfwn=value_from_wellknown_name(Header.ACCEPT_CHARSET,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x50:
              { int vfwn=value_from_wellknown_name(Header.ACCEPT_ENCODING,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x51:
              { int vfwn=value_from_wellknown_name(Header.ACCEPT_LANGUAGE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x52:
              { int vfwn=value_from_wellknown_name(Header.ACCEPT_RANGES,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x53:
              { int vfwn=value_from_wellknown_name(Header.ACCEPT,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x54:
              { int vfwn=value_from_wellknown_name(Header.ACCESS_CONTROL_ALLOW_ORIGIN,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x55:
              { int vfwn=value_from_wellknown_name(Header.AGE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x56:
              { int vfwn=value_from_wellknown_name(Header.ALLOW,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x57:
              { int vfwn=value_from_wellknown_name(Header.AUTHORIZATION,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x58:
              { int vfwn=value_from_wellknown_name(Header.CACHE_CONTROL,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x59:
              { int vfwn=value_from_wellknown_name(Header.CONTENT_DISPOSITION,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x5a:
              { int vfwn=value_from_wellknown_name(Header.CONTENT_ENCODING,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x5b:
              { int vfwn=value_from_wellknown_name(Header.CONTENT_LANGUAGE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x5c:
              { int vfwn=value_from_wellknown_name(Header.CONTENT_LENGTH,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x5d:
              { int vfwn=value_from_wellknown_name(Header.CONTENT_LOCATION,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x5e:
              { int vfwn=value_from_wellknown_name(Header.CONTENT_RANGE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x5f:
              { int vfwn=value_from_wellknown_name(Header.CONTENT_TYPE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x60:
              { int vfwn=value_from_wellknown_name(Header.COOKIE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x61:
              { int vfwn=value_from_wellknown_name(Header.DATE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x62:
              { int vfwn=value_from_wellknown_name(Header.ETAG,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x63:
              { int vfwn=value_from_wellknown_name(Header.EXPECT,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x64:
              { int vfwn=value_from_wellknown_name(Header.EXPIRES,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x65:
              { int vfwn=value_from_wellknown_name(Header.FROM,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x66:
              { int vfwn=value_from_wellknown_name(Header.HOST,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x67:
              { int vfwn=value_from_wellknown_name(Header.IF_MATCH,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x68:
              { int vfwn=value_from_wellknown_name(Header.IF_MODIFIED_SINCE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x69:
              { int vfwn=value_from_wellknown_name(Header.IF_NONE_MATCH,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x6a:
              { int vfwn=value_from_wellknown_name(Header.IF_RANGE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x6b:
              { int vfwn=value_from_wellknown_name(Header.IF_UNMODIFIED_SINCE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x6c:
              { int vfwn=value_from_wellknown_name(Header.LAST_MODIFIED,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x6d:
              { int vfwn=value_from_wellknown_name(Header.LINK,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x6e:
              { int vfwn=value_from_wellknown_name(Header.LOCATION,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x6f:
              { int vfwn=value_from_wellknown_name(Header.MAX_FORWARDS,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x70:
              { int vfwn=value_from_wellknown_name(Header.PROXY_AUTHENTICATE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x71:
              { int vfwn=value_from_wellknown_name(Header.PROXY_AUTHORIZATION,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x72:
              { int vfwn=value_from_wellknown_name(Header.RANGE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x73:
              { int vfwn=value_from_wellknown_name(Header.REFERER,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x74:
              { int vfwn=value_from_wellknown_name(Header.REFRESH,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x75:
              { int vfwn=value_from_wellknown_name(Header.RETRY_AFTER,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x76:
              { int vfwn=value_from_wellknown_name(Header.SERVER,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x77:
              { int vfwn=value_from_wellknown_name(Header.SET_COOKIE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x78:
              { int vfwn=value_from_wellknown_name(Header.STRICT_TRANSPORT_SECURITY,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x79:
              { int vfwn=value_from_wellknown_name(Header.TRANSFER_ENCODING,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x7a:
              { int vfwn=value_from_wellknown_name(Header.USER_AGENT,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x7b:
              { int vfwn=value_from_wellknown_name(Header.VARY,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x7c:
              { int vfwn=value_from_wellknown_name(Header.VIA,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x7d:
              { int vfwn=value_from_wellknown_name(Header.WWW_AUTHENTICATE,true,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;



              case (byte)0x7E:{
                if (! dt.isValid(62))
                  return ERROR;
                DynamicTable.Entry entry = get_from_dynamic_table(62);

                int value_result = value_from_dynamic_table(entry,true, bb, hotf);
                if (value_result == ERROR)
                  return ERROR;
                else if (value_result == UNDERFLOW)
                  return UNDERFLOW;
              }

              break;
              case (byte)0x7f:{
                long position = decode(6,b,bb);
                if (position == UnsignedInteger.UNDERFLOW)
                  return UNDERFLOW;
                else if (position == UnsignedInteger.ERROR)
                  return ERROR;
                if (! dt.isValid(position))
                  return ERROR;

                DynamicTable.Entry entry = get_from_dynamic_table(position);

                int value_result = value_from_dynamic_table(entry,true, bb, hotf);
                if (value_result == ERROR)
                  return ERROR;
                else if (value_result == UNDERFLOW)
                  return UNDERFLOW;
              }

              break;



              case (byte)0x00:
              case (byte)0x10:
              {
                int name_value_result = name_value(false, bb, hotf);
                if (name_value_result == ERROR)
                  return ERROR;
                else if (name_value_result == UNDERFLOW)
                  return UNDERFLOW;


              }
              break;

              case (byte)0x11:
              { int vfwn=value_from_wellknown_name(Header.AUTHORITY,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x12:
              { int vfwn=value_from_wellknown_name(Header.METHOD,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x13:
              { int vfwn=value_from_wellknown_name(Header.METHOD,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x14:
              { int vfwn=value_from_wellknown_name(Header.PATH,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x15:
              { int vfwn=value_from_wellknown_name(Header.PATH,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x16:
              { int vfwn=value_from_wellknown_name(Header.SCHEME,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x17:
              { int vfwn=value_from_wellknown_name(Header.SCHEME,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x18:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x19:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x1a:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x1b:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x1c:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x1d:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x1e:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;


              case (byte)0x1:
              { int vfwn=value_from_wellknown_name(Header.AUTHORITY,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x2:
              { int vfwn=value_from_wellknown_name(Header.METHOD,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x3:
              { int vfwn=value_from_wellknown_name(Header.METHOD,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x4:
              { int vfwn=value_from_wellknown_name(Header.PATH,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x5:
              { int vfwn=value_from_wellknown_name(Header.PATH,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x6:
              { int vfwn=value_from_wellknown_name(Header.SCHEME,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x7:
              { int vfwn=value_from_wellknown_name(Header.SCHEME,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x8:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0x9:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0xa:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0xb:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0xc:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0xd:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;
              case (byte)0xe:
              { int vfwn=value_from_wellknown_name(Header.STATUS,false,bb,hotf);
if (vfwn==ERROR||vfwn==UNDERFLOW) return vfwn;}

              break;





              case (byte)0x0f:
              case (byte)0x1f:{
                long position = decode(4,b,bb);
                if (position == UnsignedInteger.UNDERFLOW)
                  return UNDERFLOW;
                else if (position == UnsignedInteger.ERROR)
                  return ERROR;

                if (position <= 61) {
                  int vfwn=OK;switch((int)position) {case 0xf:{vfwn=value_from_wellknown_name(Header.ACCEPT_CHARSET,false,bb,hotf);}

                  break;
                    case 0x10:{vfwn=value_from_wellknown_name(Header.ACCEPT_ENCODING,false,bb,hotf);}

                    break;
                    case 0x11:{vfwn=value_from_wellknown_name(Header.ACCEPT_LANGUAGE,false,bb,hotf);}

                    break;
                    case 0x12:{vfwn=value_from_wellknown_name(Header.ACCEPT_RANGES,false,bb,hotf);}

                    break;
                    case 0x13:{vfwn=value_from_wellknown_name(Header.ACCEPT,false,bb,hotf);}

                    break;
                    case 0x14:{vfwn=value_from_wellknown_name(Header.ACCESS_CONTROL_ALLOW_ORIGIN,false,bb,hotf);}

                    break;
                    case 0x15:{vfwn=value_from_wellknown_name(Header.AGE,false,bb,hotf);}

                    break;
                    case 0x16:{vfwn=value_from_wellknown_name(Header.ALLOW,false,bb,hotf);}

                    break;
                    case 0x17:{vfwn=value_from_wellknown_name(Header.AUTHORIZATION,false,bb,hotf);}

                    break;
                    case 0x18:{vfwn=value_from_wellknown_name(Header.CACHE_CONTROL,false,bb,hotf);}

                    break;
                    case 0x19:{vfwn=value_from_wellknown_name(Header.CONTENT_DISPOSITION,false,bb,hotf);}

                    break;
                    case 0x1a:{vfwn=value_from_wellknown_name(Header.CONTENT_ENCODING,false,bb,hotf);}

                    break;
                    case 0x1b:{vfwn=value_from_wellknown_name(Header.CONTENT_LANGUAGE,false,bb,hotf);}

                    break;
                    case 0x1c:{vfwn=value_from_wellknown_name(Header.CONTENT_LENGTH,false,bb,hotf);}

                    break;
                    case 0x1d:{vfwn=value_from_wellknown_name(Header.CONTENT_LOCATION,false,bb,hotf);}

                    break;
                    case 0x1e:{vfwn=value_from_wellknown_name(Header.CONTENT_RANGE,false,bb,hotf);}

                    break;
                    case 0x1f:{vfwn=value_from_wellknown_name(Header.CONTENT_TYPE,false,bb,hotf);}

                    break;
                    case 0x20:{vfwn=value_from_wellknown_name(Header.COOKIE,false,bb,hotf);}

                    break;
                    case 0x21:{vfwn=value_from_wellknown_name(Header.DATE,false,bb,hotf);}

                    break;
                    case 0x22:{vfwn=value_from_wellknown_name(Header.ETAG,false,bb,hotf);}

                    break;
                    case 0x23:{vfwn=value_from_wellknown_name(Header.EXPECT,false,bb,hotf);}

                    break;
                    case 0x24:{vfwn=value_from_wellknown_name(Header.EXPIRES,false,bb,hotf);}

                    break;
                    case 0x25:{vfwn=value_from_wellknown_name(Header.FROM,false,bb,hotf);}

                    break;
                    case 0x26:{vfwn=value_from_wellknown_name(Header.HOST,false,bb,hotf);}

                    break;
                    case 0x27:{vfwn=value_from_wellknown_name(Header.IF_MATCH,false,bb,hotf);}

                    break;
                    case 0x28:{vfwn=value_from_wellknown_name(Header.IF_MODIFIED_SINCE,false,bb,hotf);}

                    break;
                    case 0x29:{vfwn=value_from_wellknown_name(Header.IF_NONE_MATCH,false,bb,hotf);}

                    break;
                    case 0x2a:{vfwn=value_from_wellknown_name(Header.IF_RANGE,false,bb,hotf);}

                    break;
                    case 0x2b:{vfwn=value_from_wellknown_name(Header.IF_UNMODIFIED_SINCE,false,bb,hotf);}

                    break;
                    case 0x2c:{vfwn=value_from_wellknown_name(Header.LAST_MODIFIED,false,bb,hotf);}

                    break;
                    case 0x2d:{vfwn=value_from_wellknown_name(Header.LINK,false,bb,hotf);}

                    break;
                    case 0x2e:{vfwn=value_from_wellknown_name(Header.LOCATION,false,bb,hotf);}

                    break;
                    case 0x2f:{vfwn=value_from_wellknown_name(Header.MAX_FORWARDS,false,bb,hotf);}

                    break;
                    case 0x30:{vfwn=value_from_wellknown_name(Header.PROXY_AUTHENTICATE,false,bb,hotf);}

                    break;
                    case 0x31:{vfwn=value_from_wellknown_name(Header.PROXY_AUTHORIZATION,false,bb,hotf);}

                    break;
                    case 0x32:{vfwn=value_from_wellknown_name(Header.RANGE,false,bb,hotf);}

                    break;
                    case 0x33:{vfwn=value_from_wellknown_name(Header.REFERER,false,bb,hotf);}

                    break;
                    case 0x34:{vfwn=value_from_wellknown_name(Header.REFRESH,false,bb,hotf);}

                    break;
                    case 0x35:{vfwn=value_from_wellknown_name(Header.RETRY_AFTER,false,bb,hotf);}

                    break;
                    case 0x36:{vfwn=value_from_wellknown_name(Header.SERVER,false,bb,hotf);}

                    break;
                    case 0x37:{vfwn=value_from_wellknown_name(Header.SET_COOKIE,false,bb,hotf);}

                    break;
                    case 0x38:{vfwn=value_from_wellknown_name(Header.STRICT_TRANSPORT_SECURITY,false,bb,hotf);}

                    break;
                    case 0x39:{vfwn=value_from_wellknown_name(Header.TRANSFER_ENCODING,false,bb,hotf);}

                    break;
                    case 0x3a:{vfwn=value_from_wellknown_name(Header.USER_AGENT,false,bb,hotf);}

                    break;
                    case 0x3b:{vfwn=value_from_wellknown_name(Header.VARY,false,bb,hotf);}

                    break;
                    case 0x3c:{vfwn=value_from_wellknown_name(Header.VIA,false,bb,hotf);}

                    break;
                    case 0x3d:{vfwn=value_from_wellknown_name(Header.WWW_AUTHENTICATE,false,bb,hotf);}
                  };
if (vfwn == ERROR||vfwn==UNDERFLOW) return vfwn;
                } else {
                  if (! dt.isValid(position))
                    return ERROR;
                  DynamicTable.Entry entry = get_from_dynamic_table(position);

                  int value_result = value_from_dynamic_table(entry,false, bb, hotf);
                  if (value_result == ERROR)
                    return ERROR;
                  else if (value_result == UNDERFLOW)
                    return UNDERFLOW;
                }
              }

              break;




case (byte)0x20:
              case (byte)0x21:
              case (byte)0x22:
              case (byte)0x23:
              case (byte)0x24:
              case (byte)0x25:
              case (byte)0x26:
              case (byte)0x27:
              case (byte)0x28:
              case (byte)0x29:
              case (byte)0x2a:
              case (byte)0x2b:
              case (byte)0x2c:
              case (byte)0x2d:
              case (byte)0x2e:
              case (byte)0x2f:
              case (byte)0x30:
              case (byte)0x31:
              case (byte)0x32:
              case (byte)0x33:
              case (byte)0x34:
              case (byte)0x35:
              case (byte)0x36:
              case (byte)0x37:
              case (byte)0x38:
              case (byte)0x39:
              case (byte)0x3a:
              case (byte)0x3b:
              case (byte)0x3c:
              case (byte)0x3d:
              case (byte)0x3e:
              case (byte)0x3f:
                long length = UnsignedInteger.decode(5, b, bb);
                if (length == UnsignedInteger.ERROR)
                  return ERROR;
                else if (length == UnsignedInteger.UNDERFLOW)
                  return UNDERFLOW;
                else
                  dt.resize(length);
                break;
      }
    }
    bb_position = -1;
    return OK;
  }
}
