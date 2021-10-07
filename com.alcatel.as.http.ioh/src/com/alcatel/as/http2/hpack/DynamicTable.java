package com.alcatel.as.http2.hpack;

import com.alcatel.as.http2.headers.Header;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import java.util.Arrays;

public class DynamicTable {

    static final  long    OFFSET = 62L;
    private       int     size_limit;
    private final Logger  logger;
    private final long    connection_id;
    private       int     size;
    private       int     first;
    private       int     quantity;
    private       Entry[] index;

    DynamicTable(int size_limit) {
        this(size_limit, Logger.getLogger("hpack.DynamicTable"),System.currentTimeMillis());
    }

    public DynamicTable(int size_limit, Logger logger, long connection_id) {
        this.size_limit = size_limit;
        this.logger = logger;
        this.connection_id = connection_id;
        this.index = new Entry[(size_limit >> 5) + 1]; // entries are bigger than 32 (2^5)

        clear();

        if (logger.isTraceEnabled()) logger.trace(toString("instanciated"));
    }

    public static int computeSize(String name, String value) {
        assert name != null : "name can't be null" ;
        assert value != null : "value can't be null" ;
        return 32+name.length()+value.length();
    }

    private void clear() {
        java.util.Arrays.fill(index,null);
        this.first = index.length-1;
        this.quantity = 0;
        this.size = 0;
    }

    public void insert(Entry from_entry, String value, byte attribute) {
        Entry entry = from_entry.duplicate(value, attribute);
        if (logger.isTraceEnabled()) logger.trace(toString("insert(" +entry.toString()+ ")"));
        insert_step_1(entry);
    }

    public void insert(Header header, byte attribute) {
        Entry entry = new EntryHeader(header, attribute);
        if (logger.isTraceEnabled()) logger.trace(toString("insert(" +entry.toString()+ ")"));
        insert_step_1(entry);
    }

    public void insert(Header header, String value, byte attribute) {
        Entry entry = new EntryHeaderValue(header, value, attribute);
        if (logger.isTraceEnabled()) logger.trace(toString("insert(" +entry.toString()+ ")"));
        insert_step_1(entry);
    }

    public void insert(String name, String value, byte attribute) {
        Entry entry = new EntryNameValue(name, value, attribute);
        if (logger.isTraceEnabled()) logger.trace(toString("insert(" +entry.toString()+ ")"));
        insert_step_1(entry);
    }

    public void insert_step_1(Entry entry) {
        if (entry.size > size_limit) {
            clear();
        } else if (entry.size == size_limit) {
            clear();
            insert_step_2(entry);
        } else if (entry.size + size > size_limit) {
            while (entry.size + size > size_limit) {
                evict();
            }
            insert_step_2(entry);
        } else {
            insert_step_2(entry);
        }
        if (logger.isTraceEnabled()) logger.trace(toString("/insert"));
        if (logger.isTraceEnabled() && logger.isEnabledFor(HPACKLogLevel.DUMP)) logger.log(HPACKLogLevel.DUMP,dump());
    }

    private void insert_step_2(Entry entry) {
        if (quantity == 0) {
            // nothing to do
        } else if (quantity > index.length) {
            assert quantity > index.length : "should not happen, table size exceeded; "+dump() ;
        } else {
            first = first + 1;
            if (first >= index.length)
                first -= index.length;
        }
        assert index[first]==null : "entry should be empty : found this instead: "+index[first];
        index[first] = entry;
        size = size + entry.size;
        quantity = quantity + 1;

    }

    private void evict() {
        if (logger.isTraceEnabled()) logger.trace(toString("evict"));
        if (quantity > 1) {
            int last = first -( quantity-1 );
            if (last < 0)
                last+=index.length;
            size = size - index[last].size;
            if (logger.isTraceEnabled() && index[last]!=null) logger.trace(toString("evicted("+index[last].toString()+")"));
            index[last] = null;
            quantity = quantity - 1;
        } else if (quantity == 1) {
            clear();
        } else {
            assert quantity>0 : "should not happen, evicting on an empty table; "+dump() ;
        }
        if (logger.isTraceEnabled()) logger.trace(toString("/evict"));
    }

    public void resize(long size_limit) {
        if (logger.isTraceEnabled()) logger.trace(toString("resize"));
        if ((((int)size_limit >> 5) + 1) > this.index.length) {
//        if (size_limit > this.size_limit) {
            Entry[] new_index = new Entry[((int)size_limit >> 5) + 1];
            for(int i=0;i<quantity;i++) {
                new_index[quantity-i] = get(OFFSET+i);
            }
            this.index = new_index;
            if (quantity > 0) {
                this.first = quantity;
            } else {
                clear();
            }
        } else if (size_limit == this.size_limit ) {
            return;
        } else { // size_limit < this.size_limit
            while(size > size_limit) evict();
        }
        this.size_limit = (int)size_limit;
        if (logger.isTraceEnabled()) logger.trace(toString("/resize"));
        if (logger.isTraceEnabled() && logger.isEnabledFor(HPACKLogLevel.DUMP)) logger.log(HPACKLogLevel.DUMP,dump());
    }

    public boolean isValid(long position) {
        if ((position - OFFSET) >=  quantity)
            return false;
        return true;
    }

    public Entry get(long position) {
        //System.err.println("get "+uid);
        //System.err.println(dump());
        assert (position >= OFFSET) : "this should not happen, uid is static or negative:"+position;
        assert (position - OFFSET) < quantity : "use isValid for check! Beyond dynamic table: "+position+" dump:"+dump();

        position-=OFFSET;
        position=first - position;
        if (position < 0 ) position+=index.length;
        Entry ret=index[(int)position];
        assert ret != null : "this should not happen, referencing an entry which is null; "+dump();
        if (logger.isTraceEnabled()) logger.trace(toString("get("+position+")="+ret.toString()));
        return ret;
    }

    static public abstract class Entry {
        protected final int    size;
        private Entry(int size) {
            this.size = size;
        }
        public abstract void notif(HeaderField.HeaderOnTheFly hotf);

        /**
         * The name of entry can be reference and be reused.
         * @param hotf
         * @param value
         */
        public abstract void notif_reuse_name(HeaderField.HeaderOnTheFly hotf, String value);

        protected abstract Entry duplicate(String value, byte attribute);

        public abstract boolean has_wellknown_value();

        public abstract Header get_wellknown_name();
    }

    static public class EntryHeader extends Entry {
        public final Header header;
        private final byte   attribute;

        private EntryHeader(Header header, byte attribute) {
            super(computeSize(header.name,header.value));
            assert header.getNameValueCode() != Header.DO_NOT_USE ;
            this.header = header;
            this.attribute = attribute;
        }

        @Override
        public void notif(HeaderField.HeaderOnTheFly hotf) {
            hotf.add_header(header);
        }

        @Override
        public void notif_reuse_name(HeaderField.HeaderOnTheFly hotf,String value) {
            hotf.add_header(header, value);
        }

        @Override
        public Entry duplicate(String value, byte attribute) {
            return new EntryHeaderValue(header.variant_parent, value, attribute);
        }

        @Override
        public boolean has_wellknown_value() {
            return header.valued_variants_exist;
        }

        @Override
        public Header get_wellknown_name() {
            assert header.valued_variants_exist;
            return header.variant_parent;
        }

        @Override
        public String toString() {
            return "EntryHeader{" +
                    "size=" + size +
                    ", header=" + header +
                    '}';
        }
    }

    static public class EntryHeaderValue extends Entry {
        public final Header  header;
        private final byte   attribute;
        private final String value;

        private EntryHeaderValue(Header header, String value, byte attribute) {
            super(computeSize(header.name,value));
            assert header.name_value_code == Header.DO_NOT_USE ;
            this.header = header;
            this.value  = value;
            this.attribute = attribute;
        }

        @Override
        public void notif(HeaderField.HeaderOnTheFly hotf) {
            hotf.add_header(header, this.value);
        }

        @Override
        public void notif_reuse_name(HeaderField.HeaderOnTheFly hotf, String value) {
            hotf.add_header(header, value);
        }

        @Override
        public Entry duplicate(String value, byte attribute) {
            return new EntryHeaderValue(header, value, attribute);
        }

        @Override
        public boolean has_wellknown_value() {
            return header.valued_variants_exist;
        }

        @Override
        public Header get_wellknown_name() {
            return header;
        }

        @Override
        public String toString() {
            return "EntryHeaderValue{" +
                    "header=" + header +
                    ", attribute=" + attribute +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    static public class EntryNameValue extends Entry {
        public final  String name;
        public final  String value;
        private final byte   attribute;

        private EntryNameValue(String name, String value, byte attribute) {
            super(computeSize(name,value));
            assert name != null : "name can't be null";
            assert value != null : "value can't be null";
            this.name = name;
            this.value = value;
            this.attribute = attribute;
        }

        @Override
        public void notif(HeaderField.HeaderOnTheFly hotf) {
            hotf.add_header(name,value);
        }

        @Override
        public void notif_reuse_name(HeaderField.HeaderOnTheFly hotf, String value) {
            // This is not a typo! we reused this entry 's name
            hotf.add_header(name, value);
        }

        @Override
        public Entry duplicate(String other_value, byte attribute) {
            return new EntryNameValue(name, other_value, attribute);
        }

        @Override
        public boolean has_wellknown_value() {
            return false;
        }

        @Override
        public Header get_wellknown_name() {
            assert false : "should not be invoked!";
            throw new IllegalStateException("should not be invoked");
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", size=" + size +
                    '}';
        }
    }

    @Override
    public String toString() {
        return toString("");
    }

    private String toString(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        if (message.length() != 0)
            sb.append('#');
        sb.append(message)
                .append("{cid=").append(connection_id).append(", ");
        sb.append(" quantity:");
        sb.append(quantity);
        sb.append(" / space:");
        sb.append(index.length);
        sb.append(" size:");
        sb.append(size);
        sb.append(" byte(s) / max:");
        sb.append(size_limit);
        sb.append(" byte(s) first:");
        sb.append(first);
        sb.append("}");
        return sb.toString();
    }

    private String dump() {
        StringBuilder sb = new StringBuilder(toString());
        sb.append(System.lineSeparator());
        for(int i=0;i<quantity;i++) {
            int position = first - i;
            if (position < 0)
                position +=index.length;
            sb.append("[");
            sb.append(i+OFFSET);
            sb.append(" / ");
            sb.append(position);
            sb.append("]");
            if (index[position] != null) {
                sb.append(index[position].toString());
            } else {
                sb.append(" null entry !");
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

}
