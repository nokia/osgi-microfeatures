// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

  package com.alcatel.as.http2.client.api.impl.common;

  import com.alcatel.as.http2.client.api.HttpHeaders;
  import org.apache.log4j.Level;
  import sun.net.NetProperties;
  import sun.net.www.HeaderParser;

  import java.io.*;
  import java.nio.ByteBuffer;
  import java.nio.charset.Charset;
  import java.nio.charset.StandardCharsets;
  import java.security.AccessController;
  import java.security.PrivilegedAction;
  import java.util.*;
  import java.util.concurrent.CompletableFuture;
  import java.util.function.BiPredicate;
  import java.util.function.Function;
  import java.util.function.Predicate;
  import java.util.function.Supplier;

  import static java.lang.String.format;

/**
 * Miscellaneous utilities
 */
public final class Utils {

    public static final boolean ASSERTIONSENABLED;

    static {
        boolean enabled = false;
        assert enabled = true;
        ASSERTIONSENABLED = enabled;
    }

//    public static final boolean TESTING;
//    static {
//        if (ASSERTIONSENABLED) {
//            PrivilegedAction<String> action = () -> System.getProperty("test.src");
//            TESTING = AccessController.doPrivileged(action) != null;
//        } else TESTING = false;
//    }
    // FIXME: false
    public static final boolean DEBUG = false;
//    public static final boolean DEBUG = // Revisit: temporary dev flag.
//            getBooleanProperty(DebugLogger.HTTP_NAME, false);
//    public static final boolean DEBUG_WS = // Revisit: temporary dev flag.
//            getBooleanProperty(DebugLogger.WS_NAME, false);
//    public static final boolean DEBUG_HPACK = // Revisit: temporary dev flag.
//            getBooleanProperty(DebugLogger.HPACK_NAME, false);
//    public static final boolean TESTING = DEBUG;

//    public static final boolean isHostnameVerificationDisabled = // enabled by default
//            hostnameVerificationDisabledValue();
//
//    private static boolean hostnameVerificationDisabledValue() {
//        String prop = getProperty("jdk.internal.httpclient.disableHostnameVerification");
//        if (prop == null)
//            return false;
//        return prop.isEmpty() ? true : Boolean.parseBoolean(prop);
//    }

    /**
     * Allocated buffer size. Must never be higher than 16K. But can be lower
     * if smaller allocation units preferred. HTTP/2 mandates that all
     * implementations support frame payloads of at least 16K.
     */
    private static final int DEFAULT_BUFSIZE = 16 * 1024;

    public static final int BUFSIZE = getIntegerNetProperty(
            "jdk.httpclient.bufsize", DEFAULT_BUFSIZE
    );

    public static final BiPredicate<String,String> ACCEPT_ALL = (x,y) -> true;

    private static final Set<String> DISALLOWED_HEADERS_SET;

    static {
        // A case insensitive TreeSet of strings.
        TreeSet<String> treeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        treeSet.addAll(Arrays.asList(new String [] {"connection", "content-length",
                "date", "expect", "from", "host", "origin",
                "referer", "upgrade",
                "via", "warning"}));
        DISALLOWED_HEADERS_SET = Collections.unmodifiableSet(treeSet);
    }

    public static final BiPredicate<String, String>
            ALLOWED_HEADERS = (header, unused) -> !DISALLOWED_HEADERS_SET.contains(header);

    public static final BiPredicate<String, String> VALIDATE_USER_HEADER =
            (name, value) -> {
                assert name != null : "null header name";
                assert value != null : "null header value";
                if (!isValidName(name)) {
                    throw newIAE("invalid header name: \"%s\"", name);
                }
                if (!Utils.ALLOWED_HEADERS.test(name, null)) {
                    throw newIAE("restricted header name: \"%s\"", name);
                }
                if (!isValidValue(value)) {
                    throw newIAE("invalid header value for %s: \"%s\"", name, value);
                }
                return true;
            };

    private static final Predicate<String> IS_PROXY_HEADER = (k) ->
            k != null && k.length() > 6 && "proxy-".equalsIgnoreCase(k.substring(0,6));
    private static final Predicate<String> NO_PROXY_HEADER =
            IS_PROXY_HEADER.negate();
    private static final Predicate<String> ALL_HEADERS = (s) -> true;

    private static final Set<String> PROXY_AUTH_DISABLED_SCHEMES;
    private static final Set<String> PROXY_AUTH_TUNNEL_DISABLED_SCHEMES;
    static {
//        String proxyAuthDisabled =
//                getNetProperty("jdk.http.auth.proxying.disabledSchemes");
//        String proxyAuthTunnelDisabled =
//                getNetProperty("jdk.http.auth.tunneling.disabledSchemes");
        PROXY_AUTH_DISABLED_SCHEMES = Collections.EMPTY_SET;
                // FIXME:
//                proxyAuthDisabled == null ? Set.of() :
//                        Stream.of(proxyAuthDisabled.split(","))
//                                .map(String::trim)
//                                .filter((s) -> !s.isEmpty())
//                                .collect(Collectors.toUnmodifiableSet());
        PROXY_AUTH_TUNNEL_DISABLED_SCHEMES = Collections.EMPTY_SET;
        // FIXME:
//                proxyAuthTunnelDisabled == null ? Set.of() :
//                        Stream.of(proxyAuthTunnelDisabled.split(","))
//                                .map(String::trim)
//                                .filter((s) -> !s.isEmpty())
//                                .collect(Collectors.toUnmodifiableSet());
    }

    public static <T> CompletableFuture<T> wrapForDebug(Logger logger, String name, CompletableFuture<T> cf) {
        if (logger.on()) {
            return cf.handle((r,t) -> {
                logger.log("%s completed %s", name, t == null ? "successfully" : t );
                return cf;
            }).thenCompose(Function.identity());
        } else {
            return cf;
        }
    }

    private static final String WSPACES = " \t\r\n";
    private static final boolean isAllowedForProxy(String name,
                                                   String value,
                                                   Set<String> disabledSchemes,
                                                   Predicate<String> allowedKeys) {
        if (!allowedKeys.test(name)) return false;
        if (disabledSchemes.isEmpty()) return true;
        if (name.equalsIgnoreCase("proxy-authorization")) {
            if (value.isEmpty()) return false;
            for (String scheme : disabledSchemes) {
                int slen = scheme.length();
                int vlen = value.length();
                if (vlen == slen) {
                    if (value.equalsIgnoreCase(scheme)) {
                        return false;
                    }
                } else if (vlen > slen) {
                    if (value.substring(0,slen).equalsIgnoreCase(scheme)) {
                        int c = value.codePointAt(slen);
                        if (WSPACES.indexOf(c) > -1
                                || Character.isSpaceChar(c)
                                || Character.isWhitespace(c)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public static final BiPredicate<String, String> PROXY_TUNNEL_FILTER =
            (s,v) -> isAllowedForProxy(s, v, PROXY_AUTH_TUNNEL_DISABLED_SCHEMES,
                    IS_PROXY_HEADER);
    public static final BiPredicate<String, String> PROXY_FILTER =
            (s,v) -> isAllowedForProxy(s, v, PROXY_AUTH_DISABLED_SCHEMES,
                    ALL_HEADERS);
    public static final BiPredicate<String, String> NO_PROXY_HEADERS_FILTER =
            (n,v) -> Utils.NO_PROXY_HEADER.test(n);


    public static boolean proxyHasDisabledSchemes(boolean tunnel) {
        return tunnel ? ! PROXY_AUTH_TUNNEL_DISABLED_SCHEMES.isEmpty()
                      : ! PROXY_AUTH_DISABLED_SCHEMES.isEmpty();
    }

    public static IllegalArgumentException newIAE(String message, Object... args) {
        return new IllegalArgumentException(format(message, args));
    }
    public static ByteBuffer getBuffer() {
        return ByteBuffer.allocate(BUFSIZE);
    }


    private Utils() { }


    // ABNF primitives defined in RFC 7230
    private static final boolean[] tchar      = new boolean[256];
    private static final boolean[] fieldvchar = new boolean[256];

    static {
        char[] allowedTokenChars =
                ("!#$%&'*+-.^_`|~0123456789" +
                 "abcdefghijklmnopqrstuvwxyz" +
                 "ABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
        for (char c : allowedTokenChars) {
            tchar[c] = true;
        }
        for (char c = 0x21; c < 0xFF; c++) {
            fieldvchar[c] = true;
        }
        fieldvchar[0x7F] = false; // a little hole (DEL) in the range
    }

    /*
     * Validates a RFC 7230 field-name.
     */
    public static boolean isValidName(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c > 255 || !tchar[c]) {
                return false;
            }
        }
        return !token.isEmpty();
    }

    /*
     * Validates a RFC 7230 field-value.
     *
     * "Obsolete line folding" rule
     *
     *     obs-fold = CRLF 1*( SP / HTAB )
     *
     * is not permitted!
     */
    public static boolean isValidValue(String token) {
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c > 255) {
                return false;
            }
            if (c == ' ' || c == '\t') {
                continue;
            } else if (!fieldvchar[c]) {
                return false; // forbidden byte
            }
        }
        return true;
    }


    public static int getIntegerNetProperty(String name, int defaultValue) {
        return AccessController.doPrivileged((PrivilegedAction<Integer>) () ->
                NetProperties.getInteger(name, defaultValue));
    }

    public static String getNetProperty(String name) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () ->
                NetProperties.get(name));
    }

    public static String stackTrace(Throwable t) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String s = null;
        try {
            PrintStream p = new PrintStream(bos, true, "US-ASCII");
            t.printStackTrace(p);
            s = bos.toString("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new InternalError(ex); // Can't happen
        }
        return s;
    }

    public static boolean hasRemaining(List<ByteBuffer> bufs) {
        synchronized (bufs) {
            for (ByteBuffer buf : bufs) {
                if (buf.hasRemaining())
                    return true;
            }
        }
        return false;
    }

    public static int remaining(List<ByteBuffer> bufs, int max) {
        long remain = 0;
        synchronized (bufs) {
            for (ByteBuffer buf : bufs) {
                remain += buf.remaining();
                if (remain > max) {
                    throw new IllegalArgumentException("too many bytes");
                }
            }
        }
        return (int) remain;
    }

    public static void close(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (IOException ignored) { }
        }
    }

    // Put all these static 'empty' singletons here
    public static final ByteBuffer[] EMPTY_BB_ARRAY = new ByteBuffer[0];

    /**
     * Get the Charset from the Content-encoding header. Defaults to
     * UTF_8
     */
    public static Charset charsetFrom(HttpHeaders headers) {
        String type = headers.firstValue("Content-type")
                .orElse("text/html; charset=utf-8");
        int i = type.indexOf(";");
        if (i >= 0) type = type.substring(i+1);
        try {
            HeaderParser parser = new HeaderParser(type);
            String value = parser.findValue("charset");
            if (value == null) return StandardCharsets.UTF_8;
            return Charset.forName(value);
        } catch (Throwable x) {
            Log.logTrace("Can't find charset in \"{0}\" ({1})", type, x);
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * Get a logger for debug HTTP traces.The logger should only be used
     * with levels whose severity is {@code <= DEBUG}.
     *
     * By default, this logger will forward all messages logged to an internal
     * logger named "jdk.internal.httpclient.debug".
     * In addition, the provided boolean {@code on==true}, it will print the
     * messages on stderr.
     * The logger will add some decoration to the printed message, in the form of
     * {@code <Level>:[<thread-name>] [<elapsed-time>] <dbgTag>: <formatted message>}
     *
     * @apiNote To obtain a logger that will always print things on stderr in
     *          addition to forwarding to the internal logger, use
     *          {@code getDebugLogger(this::dbgTag, true);}.
     *          This is also equivalent to calling
     *          {@code getDebugLogger(this::dbgTag, Level.ALL);}.
     *          To obtain a logger that will only forward to the internal logger,
     *          use {@code getDebugLogger(this::dbgTag, false);}.
     *          This is also equivalent to calling
     *          {@code getDebugLogger(this::dbgTag, Level.OFF);}.
     *
     * @param dbgTag A lambda that returns a string that identifies the caller
     *               (e.g: "SocketTube(3)", or "Http2Connection(SocketTube(3))")
     * @param on  Whether messages should also be printed on
     *               stderr (in addition to be forwarded to the internal logger).
     *
     * @return A logger for HTTP internal debug traces
     */
    public static Logger getDebugLogger(Supplier<String> dbgTag, boolean on) {
        Level errLevel = on ? Level.ALL : Level.OFF;
        // FIXME: null
        return null;
    }



    // -- toAsciiString-like support to encode path and query URI segments

    private static final char[] hexDigits = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

}
