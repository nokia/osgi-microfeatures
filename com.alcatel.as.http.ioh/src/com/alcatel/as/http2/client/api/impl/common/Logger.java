// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl.common;

import java.util.function.Supplier;

import org.apache.log4j.Level;

/**
 * An internal {@code System.Logger} that is used for internal
 * debugging purposes in the {@link java.net.http} module.
 * <p>
 * Though not enforced, this interface is designed for emitting
 * debug messages with Level.DEBUG.
 * <p>
 * It defines {@code log} methods that default to {@code Level.DEBUG},
 * so that they can be called in statements like:
 * <pre>{@code debug.log("some %s with %d %s", message(), one(), params());}</pre>
 *
 * @implSpec
 * This interface is implemented by loggers returned by
 * {@link Utils#getDebugLogger(Supplier, boolean)},
 * {@link Utils#getWebSocketLogger(Supplier, boolean)}and
 * {@link Utils#getHpackLogger(Supplier, boolean)}.
 * It is not designed to be implemented by any other
 * loggers. Do not use outside of this module.
 */
public interface Logger {//extends System.Logger {

    boolean isLoggable(Level level);
    void log(Level level, String msg);
    void log(Level level, String msg, Throwable ex);

    /**
     * Tells whether this logger is on.
     * @implSpec The default implementation for this method calls
     * {@code this.isLoggable(Level.DEBUG);}
     */
    public default boolean on() {
        return isLoggable(Level.DEBUG);
    }


    /**
     * Logs a message with an optional list of parameters.
     *
     * @implSpec The default implementation for this method calls
     * {@code this.log(Level.DEBUG, format, params);}
     *
     * @param format the string message format in
     * {@link String#format(String, Object...)} or {@link
     * java.text.MessageFormat} format, (or a key in the message
     * catalog, if this logger is a {@link
     * System.LoggerFinder#getLocalizedLogger(java.lang.String,
     * java.util.ResourceBundle, java.lang.Module) localized logger});
     * can be {@code null}.
     * @param params an optional list of parameters to the message (may be
     * none).
     */
    public default void log(String format, Object... params) {
        // FIXME:
        log(Level.DEBUG, "FIXME"/*format, params*/);
    }
}
