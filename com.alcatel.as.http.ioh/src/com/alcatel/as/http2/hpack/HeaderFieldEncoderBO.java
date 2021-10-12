// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class HeaderFieldEncoderBO {

    final Logger                    logger;
    final String                    logger_prefix;
    final TrivialHuffmanEncoderBO   huffmanEncoder;
    final EncodingDynamicTable      edt ;
    final HashMap<String,CueBase>   cueMap;
    final CueBase[]                 cueArray;

    protected HeaderFieldEncoderBO(int max_table_size) {
        this(max_table_size,Logger.getLogger("hpack.HeaderFieldEncoder"), System.currentTimeMillis());
    }

    public HeaderFieldEncoderBO(int max_table_size, Logger logger, long connection_id) {
        this.logger         = logger;
        this.logger_prefix  = "HeaderFieldEncoder{"+connection_id+",max_table_size="+max_table_size+"} ";
        this.edt            = new EncodingDynamicTable(max_table_size,logger,connection_id);
        this.huffmanEncoder = new TrivialHuffmanEncoderBO();
        this.cueMap         = new HashMap<String,CueBase>();
        this.cueArray       = new CueBase[Header.ACCEPT_ENCODING.name_code+1];
        put(new StaticLTE14CueBase(":authority",1));
        put(new StaticCueMethod());
        put(new StaticCuePath());
        put(new StaticCueScheme());
        put(new StaticCueStatus());
        put(new StaticGT14CueBase("accept-charset",15));
        put(new StaticCueAcceptEncoding());

        for(int i=17; i <= 61;i++) {
            if (i == 23 ) {
                put(new StaticCueAuthorizationEncoding());
            } else if (i == 32 ) {
                put(new StaticCueCookieEncoding());
            } else {
                StaticEntry entry = StaticTableEntries.get(i);
                put(new StaticGT14CueBase(entry.header, entry.position));
            }
        }

        for(Header h: Header.values()) {
            if (h.name_value_code == Header.DO_NOT_USE) {
                cueArray[h.name_code] = cueMap.get(h.name);
            }
        }

    }

    protected void update(int header_table_size, ByteOutput out) {
        edt.resize(header_table_size);
        UnsignedInteger.encode(5, header_table_size,(byte)0x20,out);
    }

    private void put(CueBase cue) {
        cueMap.put(cue.name, cue);
    }

    private static final class ValueEntry {
        final String value;
        final long   uid;

        public ValueEntry(String value, long uid) {
            this.value = value;
            this.uid = uid;
        }
    }

    private interface Cue {
        void encode(String name, String value, ByteOutput out);
    }

    private abstract class CueBase implements Cue {
        protected CueBase(String name ) {
            this.name=name;
        }
        final String name;
        protected List<ValueEntry> values = new ArrayList<ValueEntry>();

        boolean insert_from_dynamic_table(String value, ByteOutput out) {
            long result = dynamic_table_lookup(value);
            if (result == NOT_FOUND)
                return false;
                UnsignedInteger.encode(7, result,(byte)0x80,out);
                return true;
        }

        void index(String name, String value) {
            long uid = edt.insert(name,value);
            values.add(new ValueEntry(value,uid));
        }

        public static final long NOT_FOUND = -2L;

        private long dynamic_table_lookup(String value) {
            return dynamic_table_lookup(NOT_FOUND, value);
        }

        protected long dynamic_table_lookup(long default_value, String value) {
            long                  result       = default_value;
            ArrayList<ValueEntry> ve_to_remove = null;
            for (ValueEntry entry : values) {
                long index = edt.translate_to_index(entry.uid);
                if (index == edt.NOT_FOUND) {
                    if (ve_to_remove == null)
                        ve_to_remove = new ArrayList<ValueEntry>();
                    ve_to_remove.add(entry);
                } else {
                    if (result == default_value) {
                        result = NOT_FOUND;
                    }
                    if (value.hashCode() == entry.value.hashCode() && value.equals(entry.value)) {
                        result = index;
                        break;
                    }
                }
            }
            if (ve_to_remove != null) {
                values.removeAll(ve_to_remove);
                ve_to_remove.clear();
            }

            return result;
        }
    }

    // Note that static entries will never be encoded as Literal name followed by a Literal value,
    //      meaning it is always an indexed name.

    /**
     * Less Than or Equal to 14
     */
    private class StaticLTE14CueBase extends CueBase {
        final int  index;
        private final byte index_indexing,index_without_indexing,index_never_indexed;
        StaticLTE14CueBase(String name, int index) {
            super(name);
            assert index <=14                : "wrong class use GT14" ;
            assert index <= 61 && index != 0 : "not a static entry" ;
            this.index = index;
            this.index_indexing         = (byte)((index|0x40) & 0xff);
            this.index_without_indexing = (byte)( index       & 0xff);
            this.index_never_indexed    = (byte)((index|0x10) & 0xff);
        }

        void encode_indexed_literal_with_indexing(String value, ByteOutput out) {
            out.put(index_indexing);
            index(name,value);
            encode_string(value,out);
        }
        void encode_indexed_literal_without_indexing(String value, ByteOutput out) {
            out.put(index_without_indexing);
            encode_string(value,out);
        }
        void encode_indexed_literal_never_indexed(String value, ByteOutput out) {
            out.put(index_never_indexed);
            encode_string(value,out);
        }

        public void encode(String name, String value, ByteOutput out) {
            if (insert_from_dynamic_table(value,out))
                return;
            else
                encode_indexed_literal_with_indexing(value, out);
        }
    }

    /**
     * Greater than 14
     */
    private class StaticGT14CueBase extends CueBase {
        final int  index;
        private final byte index_indexing;
        private final byte index_without_indexing_1,index_without_indexing_2;
        private final byte index_never_indexed_1,index_never_indexed_2;
        StaticGT14CueBase(String name, int index) {
            super(name);
            assert index >14                 : "wrong class use LTE14 index:"+ index+" name:"+name ;
            assert index <= 61 && index != 0 : "not a static entry" ;

            this.index = index;
            this.index_indexing           = (byte)((index|0x40) & 0xff);
            this.index_without_indexing_1 = (byte)(0x0f);
            this.index_never_indexed_1    = (byte)(0x1f);

            this.index_without_indexing_2 = (byte)((index-15)&0xff);
            this.index_never_indexed_2    = (byte)((index-15)&0xff);
        }

        void encode_indexed_literal_with_indexing(String value, ByteOutput out) {
            out.put(index_indexing);
            index(name,value);
            encode_string(value,out);
        }
        void encode_indexed_literal_without_indexing(String value, ByteOutput out) {
            out.put(index_without_indexing_1);
            out.put(index_without_indexing_2);
            encode_string(value,out);
        }
        void encode_indexed_literal_never_indexed(String value, ByteOutput out) {
            out.put(index_never_indexed_1);
            out.put(index_never_indexed_2);
            // don't compress using huffman either!
            UnsignedInteger.encode(7, value.length(),(byte)0x00,out);
            byte [] value_as_bytes = value.getBytes();
            out.put(value_as_bytes);

        }
        public void encode(String name, String value, ByteOutput out) {
            if (insert_from_dynamic_table(value,out))
                return;
            else
                encode_indexed_literal_with_indexing(value, out);
        }
    }

    //          | 2     | :method                     | GET           |
    //          | 3     | :method                     | POST          |
    private class StaticCueMethod extends StaticLTE14CueBase {
        public StaticCueMethod() {
            super(":method",2);
        }

        public final void encode(String name, String value, ByteOutput out) {
            int length = value.length();
            if (length == 3 && "GET".hashCode() == value.hashCode() && "GET".equals(value) ) {
                out.put((byte)0x82);
                return;
            } else if (length == 4 && "POST".hashCode() == value.hashCode() && "POST".equals(value) ) {
                out.put((byte) 0x83);
                return;
            }

            super.encode(name,value,out);
        }
    }

    //          | 4     | :path                       | /             |
    //          | 5     | :path                       | /index.html   |
    private class StaticCuePath extends StaticLTE14CueBase {
        public StaticCuePath() {
            super(":path",4);
        }

        public final void encode(String name, String value, ByteOutput out) {
            int length = value.length();
            if (length == 1 && value.charAt(0) == '/' ) {
                out.put((byte)0x84);
                return;
            } else if (length == 11 && "/index.html".hashCode() == value.hashCode() && "/index.html".equals(value) ) {
                out.put((byte) 0x85);
                return;
            }

            super.encode(name,value,out);
        }
    }

    //            | 6     | :scheme                     | http          |
    //            | 7     | :scheme                     | https         |
    private class StaticCueScheme extends StaticLTE14CueBase {
        public StaticCueScheme() {
            super(":scheme",6);
        }

        public final void encode(String name, String value, ByteOutput out) {
            int length = value.length();
            if (length == 5 && "https".hashCode() == value.hashCode() && "https".equals(value) ) {
                out.put((byte)0x87);
                return;
            } else if (length == 4 && "http".hashCode() == value.hashCode() && "http".equals(value) ) {
                out.put((byte) 0x86);
                return;
            }

            super.encode(name,value,out);
        }
    }

    //          | 8     | :status                     | 200           |
    //          | 9     | :status                     | 204           |
    //          | 10    | :status                     | 206           |
    //          | 11    | :status                     | 304           |
    //          | 12    | :status                     | 400           |
    //          | 13    | :status                     | 404           |
    //          | 14    | :status                     | 500           |
    private class StaticCueStatus extends StaticLTE14CueBase {
        public StaticCueStatus() {
            super(":status",8);
        }

        public final void encode(String name, String value, ByteOutput out) {
            if (value.length() == 3 && value.charAt(1) == '0') {
                switch(value.charAt(2)) {
                    case '0':
                        switch(value.charAt(0)) {
                            case '2':
                                out.put( (byte) 0x88) ; //200
                                return;
                            case '4':
                                out.put( (byte) 0x8C) ; //400
                                return;
                            case '5':
                                out.put( (byte) 0x8E) ; //500
                                return;
                        }
                        break;
                    case '4':
                        switch(value.charAt(0)) {
                            case '2':
                                out.put( (byte) 0x89) ; //204
                                return;
                            case '3':
                                out.put( (byte) 0x8B) ; //304
                                return;
                            case '4':
                                out.put( (byte) 0x8D) ; //404
                                return;
                        }
                        break;
                    case '6':
                        if (value.charAt(0) == '2') {
                            out.put((byte) 0x8A); //206
                            return;
                        }
                        break;
                }
            }

            super.encode(name,value,out);
        }
    }

    //          | 16    | accept-encoding             | gzip, deflate |
    private class StaticCueAcceptEncoding extends StaticGT14CueBase {
        public StaticCueAcceptEncoding() {
            super("accept-encoding",16);
        }

        public final void encode(String name, String value, ByteOutput out) {
            if (value.length() == 13 && value.charAt(0)=='g' && value.charAt(6 )=='d' && value.equals("gzip, deflate") ) {
                out.put( (byte) 0x90) ; // 16    | accept-encoding             | gzip, deflate
                return;
            }
            super.encode(name,value,out);
        }
    }

    //          | 23    | authorization               |               |
    private class StaticCueAuthorizationEncoding extends StaticGT14CueBase {
        StaticCueAuthorizationEncoding() {
            super("authorization",23);
        }
        public final void encode(String name, String value, ByteOutput out) {
            if (value.length() < 20 ) {
                encode_indexed_literal_never_indexed(value,out);
                return;
            } else {
                if (insert_from_dynamic_table(value,out))
                    return;
                else
                    encode_indexed_literal_with_indexing(value, out);
            }
        }
    }

    //          | 32    | cookie                      |               |
    private class StaticCueCookieEncoding extends StaticGT14CueBase {
        StaticCueCookieEncoding() {
            super("cookie",32);
        }
        public final void encode(String name, String value, ByteOutput out) {
            if (value.length() < 20 ) {
                encode_indexed_literal_never_indexed(value,out);
                return;
            } else {
                if (insert_from_dynamic_table(value,out))
                    return;
                else
                    encode_indexed_literal_with_indexing(value, out);
            }
        }
    }

    // Dynamic entries
    private class DynamicCue extends CueBase {
        protected DynamicCue(String name ) {
            super(name);
        }

        @Override
        public void encode(String name, String value, ByteOutput out) {
            // if at least one entry is still in the dynamic table
            //    then we can index the name
            // otherwise we need to use literal for the name
            long lookup = dynamic_table_lookup(value);
            if (lookup == NAME_NOT_IN_DYNAMIC_TABLE) {
            // 6.2.1.  Literal Header Field with Incremental Indexing
                out.put((byte)0x40);
                encode_string(name,out);
                encode_string(value,out);
                index(name,value);
            } else if (lookup == NOT_FOUND) {
                long index = dynamic_table_lookup_name();
                UnsignedInteger.encode(6, index,(byte)0x40, out);
                encode_string(value,out);
                index(name,value);
            } else {
                UnsignedInteger.encode(7, lookup,(byte)0x80, out);
            }
        }
        public static final long NAME_NOT_IN_DYNAMIC_TABLE = -3L;

        private long dynamic_table_lookup(String value) {
            return dynamic_table_lookup(NAME_NOT_IN_DYNAMIC_TABLE, value);
        }

        private long dynamic_table_lookup_name() {
            long no_match = NAME_NOT_IN_DYNAMIC_TABLE;
            for (ValueEntry entry : values) {
                long index = edt.translate_to_index(entry.uid);
                if (index != edt.NOT_FOUND)
                    return index;
            }
            return no_match;
        }

    }

    private void encode_string(String value, ByteOutput out) {
        int hsize=TrivialHuffmanEncoder.compute_size(value);
        if (hsize < value.length() ) {
            UnsignedInteger.encode(7, hsize,(byte)0x80,out);
            huffmanEncoder.encode(value,out);
        } else {
            UnsignedInteger.encode(7, value.length(),(byte)0x00,out);
            byte [] value_as_bytes = value.getBytes();
            out.put(value_as_bytes);
        }
    }

    private void purgeCuesAccordingToDynamicTable() {
        List<String> cue_to_remove = new ArrayList<String>();
        List<ValueEntry> ve_to_remove = new ArrayList<ValueEntry>();
        int ves_removed = 0;
        for (CueBase cue:cueMap.values()) {
            if (cue.values.isEmpty() && cue instanceof DynamicCue)
                cue_to_remove.add(cue.name);
            for(ValueEntry ve : cue.values) {
                long position = edt.translate_to_index(ve.uid);
                if (position == edt.NOT_FOUND) {
                    ve_to_remove.add(ve);
                }
            }
            cue.values.removeAll(ve_to_remove);
            ves_removed = ves_removed + ve_to_remove.size();
            ve_to_remove.clear();
        }
        cueMap.keySet().removeAll(cue_to_remove);
//        logger.debug("purge: {} cue(s) evicted {} header(s) forgotten",ves_removed, cue_to_remove.size());
        if (logger.isDebugEnabled()) logger.debug(toString()+"purge: "+ves_removed+" value entrie(s) evicted ");
        if (! cue_to_remove.isEmpty()) {
            if (logger.isDebugEnabled()) logger.debug(toString() + "purge: header(s) evicted:" + cue_to_remove);
            cue_to_remove.clear();
        }
    }

    public void encode(String name, String value, ByteOutput out) {
        CueBase cue = cueMap.get(name);
        if (cue == null) {
            cue = new DynamicCue(name);
            cueMap.put(name,cue);
        }
        cue.encode(name,value,out);

    }

    public void encode(Header header, String value, ByteOutput out) {
        assert header.name_value_code == Header.DO_NOT_USE : "please use "+header.variant_parent+" instead" ;
        CueBase cue = cueArray[header.name_code];
        cue.encode(header.name,value,out);
    }

    public void encode(Header header, ByteOutput out) {
       int code =  header.name_value_code;
       assert code != Header.DO_NOT_USE : "aren't you missing a value?";

       out.put( (byte) (code | 0x80 ) ) ;
    }

    public void last() {
        purgeCuesAccordingToDynamicTable();
    }

    @Override
    public String toString() {
        return logger_prefix;
    }
}
