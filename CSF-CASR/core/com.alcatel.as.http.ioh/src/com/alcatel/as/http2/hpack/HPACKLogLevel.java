package com.alcatel.as.http2.hpack;

import org.apache.log4j.Level;

public class HPACKLogLevel extends Level {
    public static final Level DUMP = new HPACKLogLevel(TRACE_INT/2, "DUMP", 7);

    HPACKLogLevel(int level, String levelString, int syslogEquivalent) {
        super(level,levelString,syslogEquivalent);
    }
}
