package com.alcatel.as.http2.hpack;

import org.apache.log4j.Logger;

public class EncodingDynamicTable {

    static final long    OFFSET = 62L;
    private      int     size_limit;
    private final Logger logger;
    private final long   connection_id;
    private      int     size;
    private      int     first;
    private      int     quantity;
    private      int[]   index;    // we index sizes
    private      long    uid = 0L;

    public EncodingDynamicTable(int size_limit, Logger logger, long connection_id) {
        assert size_limit > 0 : "size_limit should be positive or null : "+size_limit;
        this.size_limit = size_limit;
        this.logger = logger;
        this.connection_id = connection_id;
        this.index = new int[(size_limit >> 5) + 1]; // entries are bigger than 32 (2^5)
        clear();
    }

    private void clear() {
        java.util.Arrays.fill(index,0);
        this.first = index.length-1;
        this.quantity = 0;
        this.size = 0;
    }

    public static final long NOT_FOUND = -2L;

    public long translate_to_index(long uid) {
        if (this.uid-uid > (quantity-1))
            return NOT_FOUND;

        long ret=OFFSET + ( this.uid-uid );

        assert get(ret) != 0L : "entry should be valid > 32 in size; "+dump();

        return ret;
    }

    public long insert(String name, String value) {
        int entry_size = DynamicTable.computeSize(name,value);
        if (logger.isTraceEnabled()) logger.trace(toString("insert("+"name="+name+", value="+value+")"));
        if (entry_size > size_limit) {
            clear();
        } else if (entry_size == size_limit) {
            clear();
            _insert(entry_size);
        } else if (entry_size + size > size_limit) {
            while (entry_size + size > size_limit) {
                evict();
            }
            _insert(entry_size);
        } else {
            _insert(entry_size);
        }
        uid = uid + 1L;
        if (logger.isTraceEnabled()) logger.trace(toString("/insert=("+uid+")"));
        if (logger.isTraceEnabled() && logger.isEnabledFor(HPACKLogLevel.DUMP)) logger.log(HPACKLogLevel.DUMP,dump());
        return uid;
    }

    private void _insert(int entry_size) {
        if (quantity == 0) {
            // nothing to do
        } else if (quantity > index.length) {
            assert quantity > index.length : "should not happen, table size exceeded; "+dump() ;
        } else {
            first = first + 1;
            if (first >= index.length)
                first -= index.length;
        }
        assert index[first]==0 : "entry should be empty : found this instead: "+index[first] + " entry_size"+ entry_size + " dump:"  + dump();
        index[first] = entry_size;
        size = size + entry_size;
        quantity = quantity + 1;

    }

    private void evict() {
        if (logger.isTraceEnabled()) logger.trace(toString("evict"));
        if (quantity > 1) {
            int last = first -( quantity-1 );
            if (last < 0)
                last+=index.length;
            size = size - index[last];
            if (logger.isTraceEnabled()) logger.trace(toString("evicted(size="+index[last]+")"));
            index[last] = 0;
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
            int[] new_index = new int[((int)size_limit >> 5) + 1];
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

    }

    public int get(long position) {
        //System.err.println("get "+uid);
        //System.err.println(dump());
        assert (position >= OFFSET) : "this should not happen, uid is static or negative:"+position;
        assert (position - OFFSET) < quantity : "beyond dynamic table: "+position + " dump:"+ dump();

        position-=OFFSET;
        position=first - position;
        if (position < 0 ) position+=index.length;
        int ret=index[(int)position];
        assert ret != 0 : "this should not happen, referencing an entry which is null; "+dump();
        if (logger.isTraceEnabled()) logger.trace(toString("get("+position+")=("+ret+")"));
        return ret;
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
        sb.append(" uid:");
        sb.append(uid);
        sb.append("}");
        return sb.toString();
    }

    private String dump() {
        StringBuilder sb = new StringBuilder(toString());
        sb.append(System.lineSeparator());
        for(int i=-1;i<quantity+1;i++) {
            int position = first - i;
            if (position < 0)
                position +=index.length;

            if (i==-1) sb.append("[before]");
            else if (i==quantity) sb.append("[after]");
            else {
                sb.append("[");
                sb.append(i + OFFSET);
                sb.append(" / ");
                sb.append(position);
                sb.append("]");
            }
            sb.append(" size:");
            if (position >= 0 && position < index.length)
              sb.append(index[position]);
            else
              sb.append("N/A");
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}
