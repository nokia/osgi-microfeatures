package com.alcatel.as.http2.hpack;

import org.apache.log4j.Logger;

import java.util.concurrent.Executor;

public interface Config {

    long getConnectionId ();
    int getHeaderTableSize ();
    int getMaxHeaderListSize ();
    Logger getLogger ();
    Executor getExecutor ();
    int getMaxFrameSize (); // used for outbound : no need to buffer more

    // FIXME: Handle charset
    String encodingCharset();

    // other conf, ex:
    // get list of headers to cache ???
}

