package com.alcatel.as.http2;

import org.apache.log4j.Logger;
import java.util.*;

public class Settings {

    public static final String PROP_SETTINGS_HEADER_TABLE_SIZE = "http2.settings.header.table.size";
    public static final String PROP_SETTINGS_ENABLE_PUSH = "http2.settings.enable.push";
    public static final String PROP_SETTINGS_MAX_CONCURRENT_STREAMS = "http2.settings.max.concurrent.streams";
    public static final String PROP_SETTINGS_INITIAL_WINDOW_SIZE = "http2.settings.initial.window.size";
    public static final String PROP_SETTINGS_MAX_FRAME_SIZE = "http2.settings.max.frame.size";
    public static final String PROP_SETTINGS_MAX_HEADER_LIST_SIZE = "http2.settings.header.list.size";


    public static final Settings DEF_INSTANCE = new Settings ();

    public static final long DEF_HEADER_TABLE_SIZE = Long.getLong(PROP_SETTINGS_HEADER_TABLE_SIZE, 4096);
    public static final long DEF_ENABLE_PUSH = Long.getLong(PROP_SETTINGS_ENABLE_PUSH, 0);
    public static final long DEF_MAX_CONCURRENT_STREAMS = Long.getLong(PROP_SETTINGS_MAX_CONCURRENT_STREAMS, 65535);
    public static final long DEF_INITIAL_WINDOW_SIZE = Long.getLong(PROP_SETTINGS_INITIAL_WINDOW_SIZE, 65535);
    public static final long DEF_MAX_FRAME_SIZE = Long.getLong (PROP_SETTINGS_MAX_FRAME_SIZE, 16384);
    public static final long DEF_MAX_HEADER_LIST_SIZE = Long.getLong(PROP_SETTINGS_MAX_HEADER_LIST_SIZE, 65535);
    
    public long HEADER_TABLE_SIZE = DEF_HEADER_TABLE_SIZE;
    public long ENABLE_PUSH = DEF_ENABLE_PUSH;
    public long MAX_CONCURRENT_STREAMS = DEF_MAX_CONCURRENT_STREAMS;
    public long INITIAL_WINDOW_SIZE = DEF_INITIAL_WINDOW_SIZE;
    public long MAX_FRAME_SIZE = DEF_MAX_FRAME_SIZE;
    public long MAX_HEADER_LIST_SIZE = DEF_MAX_HEADER_LIST_SIZE;

    public String toString (){ return new StringBuilder ()
	    .append ("Settings[")
	    .append ("HEADER_TABLE_SIZE=").append (HEADER_TABLE_SIZE)
	    .append (", ENABLE_PUSH=").append (ENABLE_PUSH)
	    .append (", MAX_CONCURRENT_STREAMS=").append (MAX_CONCURRENT_STREAMS)
	    .append (", INITIAL_WINDOW_SIZE=").append (INITIAL_WINDOW_SIZE)
	    .append (", MAX_FRAME_SIZE=").append (MAX_FRAME_SIZE)
	    .append (", MAX_HEADER_LIST_SIZE=").append (MAX_HEADER_LIST_SIZE)
	    .append (']')
	    .toString ();
    }
    
    public void check () throws ConnectionError {
	if (ENABLE_PUSH != 0 && ENABLE_PUSH != 1)
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
				       "Invalid Settings : ENABLE_PUSH : "+ENABLE_PUSH);
	if (MAX_FRAME_SIZE < 16384 || MAX_FRAME_SIZE > 16777215)
	    throw new ConnectionError (Http2Error.Code.PROTOCOL_ERROR,
				       "Invalid Settings : MAX_FRAME_SIZE : "+MAX_FRAME_SIZE);
    }

    public Settings load (Map props){
	return load (props, null);
    }

    public Settings load (Map props, String prefix){
	if (props == null) return this;
	boolean noprefix = prefix == null || prefix.length () == 0;
	String value = (String) props.get (noprefix ? PROP_SETTINGS_HEADER_TABLE_SIZE : prefix + PROP_SETTINGS_HEADER_TABLE_SIZE);
	if (value != null) HEADER_TABLE_SIZE = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_SETTINGS_ENABLE_PUSH : prefix + PROP_SETTINGS_ENABLE_PUSH);
	if (value != null) ENABLE_PUSH = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_SETTINGS_MAX_CONCURRENT_STREAMS : prefix + PROP_SETTINGS_MAX_CONCURRENT_STREAMS);
	if (value != null) MAX_CONCURRENT_STREAMS = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_SETTINGS_INITIAL_WINDOW_SIZE : prefix + PROP_SETTINGS_INITIAL_WINDOW_SIZE);
	if (value != null) INITIAL_WINDOW_SIZE = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_SETTINGS_MAX_FRAME_SIZE : prefix + PROP_SETTINGS_MAX_FRAME_SIZE);
	if (value != null) MAX_FRAME_SIZE = Long.parseLong (value);
	value = (String) props.get (noprefix ? PROP_SETTINGS_MAX_HEADER_LIST_SIZE : prefix + PROP_SETTINGS_MAX_HEADER_LIST_SIZE);
	if (value != null) MAX_HEADER_LIST_SIZE = Long.parseLong (value);
	return this;
    }
    
}
