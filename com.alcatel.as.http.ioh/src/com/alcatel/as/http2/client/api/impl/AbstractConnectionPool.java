package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.Http2Client;
import com.alcatel.as.http2.client.Http2Connection;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.impl.common.Config;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alcatel.as.http2.client.api.impl.common.Config.*;

abstract public class AbstractConnectionPool<T> implements ConnectionPool {
    protected InetSocketAddress map(HttpRequest userRequest) {
        if (isSingleProxySocket)
            return proxyAddress;
        if (userRequest instanceof ImmutableHttpRequest && ((ImmutableHttpRequest)userRequest).destination.isPresent() ) {
            InetSocketAddress dest = ((ImmutableHttpRequest) userRequest).destination.get();
            if (logger.isInfoEnabled()) logger.info("using forced destination:"+dest);
            return dest;
        }
        int p = userRequest.uri().getPort();
        if (p == -1) {
            if (userRequest.uri().getScheme().equalsIgnoreCase("https")) {
                p = 443;
            } else {
                p = 80;
            }
        }
        final String host = userRequest.uri().getHost();
        final int    port = p;
        return InetSocketAddress.createUnresolved(host, port);
    }

    protected HashSet<Http2Connection> grace_set = new HashSet<>();

    @Override
    public void close(ClosePolicy policy) {
        assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
        Collection<CacheEntry<T>> set          = cache.values();
        boolean                   forced       = (policy==ClosePolicy.CLOSE_FORCIBLY);

        Function<CacheEntry, Boolean> lambda = (x)->true;
        switch(policy){
            case CLOSE_GRACEFULLY_SECURE:
                lambda = x -> x.isSecure;;
                break;
        }

        Function<CacheEntry, Boolean> finalLambda = lambda;
        set = set.stream().filter(x-> finalLambda.apply(x)).collect(Collectors.toCollection(HashSet::new));

        HashSet<Http2Connection> grace_set_tmp = new HashSet<>();
        for ( CacheEntry<T> entry : set ) {
            cache.remove(entry.key,entry);
            entry.close();
            Http2Connection conn=handle_close(policy, entry);
            if (conn!=null && !forced)
                grace_set_tmp.add(conn);
        }
        if (forced) {
            grace_set_tmp.addAll(grace_set);
            for (Http2Connection conn : grace_set_tmp) {
                //  code = 0x08; // RFC7540 : CANCEL (0x8):  Used by the endpoint to indicate that the stream is no longer needed.
                conn.close(0x80,  "closing client", 0L, 0L);
            }
            grace_set.clear();
        } else {
            grace_set.addAll(grace_set_tmp);
            grace_set_tmp.clear();
        }
        // duplicate the hashSet

        // handle isSecure differently

    }

    final protected Function<CacheEntry<T>, CompletableFuture<Http2Connection>> entry_to_cf_conn ;

    protected void close_gracefull( Http2Connection conn) {
        int                       code         = 0x08; // RFC7540 : CANCEL (0x8):  Used by the endpoint to indicate that the stream is no longer needed.
        String msg = "closing client gracefully";
        long delay = (long) props.getOrDefault(CLOSE_GRACEFUL_DELAY_PROPS_KEY, CLOSE_GRACEFUL_DELAY_DEFAULT_VALUE);
        long idle_timeout = (long) props.getOrDefault(CLOSE_GRACEFUL_TIMEOUT_PROPS_KEY, CLOSE_GRACEFUL_TIMEOUT_DEFAULT_VALUE);

        conn.close(code, msg, delay, idle_timeout);
    }

    protected Http2Connection handle_close(ClosePolicy policy, CacheEntry<T> entry ) {
        Http2Connection                    ret     = null;
        CompletableFuture<Http2Connection> cf_conn = entry_to_cf_conn.apply(entry);
        InetSocketAddress                  address = entry.key;

        long         delay        = 0L;
        long         idle_timeout = 0L;
        int          code         = 0x08; // RFC7540 : CANCEL (0x8):  Used by the endpoint to indicate that the stream is no longer needed.
        boolean      warn         = false;
        final String msg;

        switch(policy) {
            case CLOSE_FORCIBLY:
                msg = "closing client";
                break;
            case CLOSE_GRACEFULLY:
                msg = "closing client gracefully";
                delay = (long) props.getOrDefault(CLOSE_GRACEFUL_DELAY_PROPS_KEY, CLOSE_GRACEFUL_DELAY_DEFAULT_VALUE);
                idle_timeout = (long) props.getOrDefault(CLOSE_GRACEFUL_TIMEOUT_PROPS_KEY, CLOSE_GRACEFUL_TIMEOUT_DEFAULT_VALUE);
                break;
            case CLOSE_GRACEFULLY_SECURE:
                msg = "keystore reloading related graceful close";
                delay = (long) props.getOrDefault(CLOSE_GRACEFUL_DELAY_PROPS_KEY, CLOSE_GRACEFUL_DELAY_DEFAULT_VALUE);
                idle_timeout = (long) props.getOrDefault(CLOSE_GRACEFUL_TIMEOUT_PROPS_KEY, CLOSE_GRACEFUL_TIMEOUT_DEFAULT_VALUE);
                code = 0x0c; // INADEQUATE_SECURITY (0xc):  The underlying transport has properties that do not meet minimum security requirements (see Section 9.2).
                warn = true;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + policy);
        }
        long final_delay = delay;
        long final_idle_timeout = idle_timeout;
        Exception close_exception = new Exception(msg);

        if (cf_conn.isCompletedExceptionally()) {
            if (logger.isDebugEnabled()) logger.debug(msg + ": completable future with exception for:" + address.toString());
        } else if (cf_conn.isDone()) {
            Http2Connection conn = cf_conn.join();
            if (logger.isDebugEnabled()) logger.debug(msg + ": closing Http2 Connection: " + address.toString() + " -> " + conn.toString());
            else if (logger.isInfoEnabled()) logger.info(msg + ": closing Http2 Connection: " + address.toString());
            else if (warn) logger.warn(msg + ": closing Http2 Connection: " + address.toString());
            // We are on the write_executor, we can invoke close from here.
            conn.close(code, msg, final_delay, final_idle_timeout);
            ret = conn;
        } else {
            if (logger.isDebugEnabled()) logger.debug(msg + ": aborting completable future for:" + address.toString());
            cf_conn.completeExceptionally(close_exception);
        }
        return ret;
    }

    protected static class CacheEntry<U> {
        CacheEntry(U entry, boolean isSecure, InetSocketAddress key) {
            this.entry    = entry;
            this.isSecure = isSecure;
            this.key      = key;
        }
        U       entry;
        boolean isSecure;
        InetSocketAddress key;
        boolean closed;
        void close() {
            this.closed=true;
        }
        boolean isClosed() {
            return closed;
        }
    }

    protected final PlatformExecutors                                           execs;
    protected final Executor                                                    pool_executor;
    protected final Executor                                                    write_executor;
    protected final HashMap<InetSocketAddress, AsrConnectionPool.CacheEntry<T>> cache = new HashMap<>();
    protected final HttpClientImpl                                              client;
    protected final Map<String, Object>                                         props;
    protected final Map<String, Object>                                         props_not_secure;
    protected final Logger                                                      logger;
    protected final long                                                        onAvailableTimeout;
    protected final int                                                         retryConnection;
    protected final boolean                                                     isSingleProxySocket;
    protected final java.net.InetSocketAddress                                  proxyAddress;

    AbstractConnectionPool(Function<CacheEntry<T>, CompletableFuture<Http2Connection>> entry_to_cf_conn,
            Logger logger, Executor pool_executor, PlatformExecutors execs, Executor write_executor, HttpClientImpl client, Map<String, Object> props) {
        this.entry_to_cf_conn = entry_to_cf_conn;
        this.logger = Objects.requireNonNull(logger);
        this.execs = Objects.requireNonNull(execs);
        this.pool_executor = Objects.requireNonNull(pool_executor);
        this.write_executor = Objects.requireNonNull(write_executor);
        this.client = Objects.requireNonNull(client);
        this.props = Objects.requireNonNull(props);

        onAvailableTimeout = (long) props.getOrDefault(ON_AVAILABLE_TIMEOUT_PROPS_KEY, ON_AVAILABLE_TIMEOUT_DEFAULT_VALUE);
        retryConnection = (int) props.getOrDefault(RETRY_CONNECTION_PROPS_KEY, RETRY_CONNECTION_DEFAULT_VALUE);
        this.isSingleProxySocket = (boolean) props.getOrDefault(SINGLE_PROXY_SOCKET_PROPS_KEY, SINGLE_PROXY_SOCKET_DEFAULT_VALUE);
        proxyAddress = isSingleProxySocket ? proxyAddress(props) : null ;

        if (props.containsKey(Http2Client.PROP_TCP_SECURE)
            // if communicating in "reverse-proxy" then it is a proxy_credential placed in the tcp_credential
            && !isSingleProxySocket// && !props.containsKey(Http2Client.PROP_PROXY_IP)
        ) {
            Map<String,Object> props_not_secure = new HashMap<>(props);

            props_not_secure.remove(Http2Client.PROP_TCP_SECURE);
            this.props_not_secure = Collections.unmodifiableMap(props_not_secure);

            if (props.containsKey(Http2Client.PROP_TCP_SECURE_KEYSTORE_FILE)) {
                KeyStoreWatch ksw =
                        new KeyStoreWatch(logger) {
                            @Override
                            protected void notify_updated() {
                                write_executor.execute(() -> {
                                    close(ClosePolicy.CLOSE_GRACEFULLY_SECURE);
                                });
                            }

                            @Override
                            protected void notify_stopped() {

                            }
                        };
                ksw.startKeyStoreWatch((String)props.get(Http2Client.PROP_TCP_SECURE_KEYSTORE_FILE));
//            } else {
//                logger.warn("Configuration warning, secure is set, but no keystore is defined.");
            }

        } else {
            props_not_secure = null;
        }
    }

    private java.net.InetSocketAddress proxyAddress(Map<String, Object> props) {
        java.net.InetSocketAddress result = new java.net.InetSocketAddress(
                (String) props.get(Config.SINGLE_PROXY_SOCKET_ADDR_PROPS_KEY)
                , (Integer) props.get(Config.SINGLE_PROXY_SOCKET_PORT_PROPS_KEY)
        );
        return result;
    }

}
