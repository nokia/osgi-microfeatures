package com.alcatel.as.service.recorder;

import java.time.format.DateTimeFormatter;

/**
 * An interface wrapping the recorder service.
 * 
 */
public interface RecorderService {

    public static final String RECORD_CAPACITY_INIT = "record.capacity.init";
    public static final String RECORD_CAPACITY_MAX = "record.capacity.max";

    public static final String RECORD_PRESERVE_HEAD = "record.preserve.head";
    public static final String RECORD_PRESERVE_TAIL = "record.preserve.tail";

    public Record getRecord (String name);

    public Record newRecord (String name, java.util.Map<String, Object> properties, boolean mustCreate);

    public void iterate (java.util.function.Consumer<Record> f);

    public void iterate (java.util.function.BiConsumer<Record, Event> f, Record... records);
    
    public DateTimeFormatter getDateFormatter ();
}
