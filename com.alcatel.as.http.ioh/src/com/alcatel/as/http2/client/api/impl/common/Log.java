// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl.common;

import java.util.Locale;

import org.apache.log4j.Level;

/**
 * -Djava.net.HttpClient.log=
 *          errors,requests,headers,
 *          frames[:control:data:window:all..],content,ssl,trace,channel
 *
 * Any of errors, requests, headers or content are optional.
 *
 * Other handlers may be added. All logging is at level INFO
 *
 * Logger name is "asr.httpclient.HttpClient"
 */
// implements System.Logger in order to be skipped when printing the caller's
// information
public abstract class Log implements Logger {

    static final String logProp = "asr.httpclient.HttpClient.log";

    public static final int OFF = 0;
    public static final int ERRORS = 0x1;
    public static final int REQUESTS = 0x2;
    public static final int HEADERS = 0x4;
    public static final int CONTENT = 0x8;
    public static final int FRAMES = 0x10;
    public static final int SSL = 0x20;
    public static final int TRACE = 0x40;
    public static final int CHANNEL = 0x80;
    static int logging;

    // Frame types: "control", "data", "window", "all"
    public static final int CONTROL = 1; // all except DATA and WINDOW_UPDATES
    public static final int DATA = 2;
    public static final int WINDOW_UPDATES = 4;
    public static final int ALL = CONTROL| DATA | WINDOW_UPDATES;
    static int frametypes;

    static final org.apache.log4j.Logger logger;

    static {
        String s = Utils.getNetProperty(logProp);
        if (s == null) {
            logging = OFF;
        } else {
            String[] vals = s.split(",");
            for (String val : vals) {
                switch (val.toLowerCase(Locale.US)) {
                    case "errors":
                        logging |= ERRORS;
                        break;
                    case "requests":
                        logging |= REQUESTS;
                        break;
                    case "headers":
                        logging |= HEADERS;
                        break;
                    case "content":
                        logging |= CONTENT;
                        break;
                    case "ssl":
                        logging |= SSL;
                        break;
                    case "channel":
                        logging |= CHANNEL;
                        break;
                    case "trace":
                        logging |= TRACE;
                        break;
                    case "all":
                        logging |= CONTENT|HEADERS|REQUESTS|FRAMES|ERRORS|TRACE|SSL| CHANNEL;
                        frametypes |= ALL;
                        break;
                    default:
                        // ignore bad values
                }
                if (val.startsWith("frames")) {
                    logging |= FRAMES;
                    String[] types = val.split(":");
                    if (types.length == 1) {
                        frametypes = CONTROL | DATA | WINDOW_UPDATES;
                    } else {
                        for (String type : types) {
                            switch (type.toLowerCase(Locale.US)) {
                                case "control":
                                    frametypes |= CONTROL;
                                    break;
                                case "data":
                                    frametypes |= DATA;
                                    break;
                                case "window":
                                    frametypes |= WINDOW_UPDATES;
                                    break;
                                case "all":
                                    frametypes = ALL;
                                    break;
                                default:
                                    // ignore bad values
                            }
                        }
                    }
                }
            }
        }
        if (logging != OFF) {
            logger = org.apache.log4j.Logger.getLogger("com.alcatel.as.http2.client.api.HttpClient");
        } else {
            logger = null;
        }
    }
    public static boolean errors() {
        return (logging & ERRORS) != 0;
    }

    public static boolean headers() {
        return (logging & HEADERS) != 0;
    }

    public static boolean trace() {
        return (logging & TRACE) != 0;
    }

    public static boolean ssl() {
        return (logging & SSL) != 0;
    }

    public static void logError(String s, Object... s1) {
        if (errors()) {
            logger.log(Level.INFO, "ERROR: " + s/*FIXME:, s1*/);
        }
    }

    public static void logError(Throwable t) {
        if (errors()) {
            String s = Utils.stackTrace(t);
            logger.log(Level.INFO, "ERROR: " + s);
        }
    }

    public static void logTrace(String s, Object... s1) {
        if (trace()) {
            String format = "MISC: " + s;
            logger.log(Level.INFO, format/*FIXME:, s1*/);
        }
    }


    // not instantiable
    private Log() {}
}
