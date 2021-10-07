package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.Http2Client;
import com.alcatel.as.http2.client.Http2Connection;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpTimeoutException;
import com.alcatel.as.http2.client.api.impl.common.Config;
import com.alcatel.as.http2.client.api.impl.common.MinimalFuture;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This will execute on the write executor, because we need to poll
 * for the status of the connection (which in turn requires us to
 * be in the write_executor).
 */
class AsrConnectionPool extends AbstractConnectionPool<MinimalFuture<Http2Connection>> {

    AsrConnectionPool(Logger logger, Executor pool_executor, PlatformExecutors execs, Executor write_executor, HttpClientImpl client, Map<String, Object> props) {
        super( (e)->e.entry , logger, pool_executor,  execs, write_executor, client, props);
    }

    static Optional<Proxy> hasProxy(Map<String, Object> props) {
        if (props.containsKey(Http2Client.PROP_PROXY_IP) && props.containsKey(Http2Client.PROP_PROXY_PORT)) {
            Proxy proxy = new Proxy(
                    Proxy.Type.HTTP
                    , new java.net.InetSocketAddress((String) props.get(Http2Client.PROP_PROXY_IP)
                    , (Integer) props.get(Http2Client.PROP_PROXY_PORT))
            );
            return Optional.of(proxy);
        } else {
            return Optional.empty();
        }
    }

    protected void delegate_connect(CompletableFuture<Http2Connection> cf_conn, final InetSocketAddress key,
                                    java.net.InetSocketAddress socket_address, boolean isSecure) {
        Objects.requireNonNull(cf_conn);
        Objects.requireNonNull(key);
        Objects.requireNonNull(socket_address);
        assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);

        if (cache.containsKey(key)) {
            CacheEntry<MinimalFuture<Http2Connection>> cacheEntry = cache.get(key);
            //MinimalFuture<Http2Connection> cache_cf_conn = cache.get(key);
            if (cacheEntry.entry.isCompletedExceptionally()) {
                try {
                    cacheEntry.entry.join();
                } catch (Throwable t) {
                    logger.debug("cache_cf_conn exception",t);
                }
            }
            if (cacheEntry.entry.isDone() && !cacheEntry.entry.isCompletedExceptionally()) {
                Http2Connection        conn   = cacheEntry.entry.join();
                Http2Connection.Status status = conn.status();
                if (status == Http2Connection.Status.AVAILABLE) {
                    cf_conn.complete(conn);
                } else if (status.retriable()) {
                    conn.onAvailable(
                            () -> cf_conn.complete(conn)
                            , () -> {
                                // by contract, callback is in the write_executor!
                                if (conn.status().retriable()) {
                                    cf_conn.completeExceptionally(new HttpTimeoutException("no stream available."));
                                } else {
                                    delegate_connect(cf_conn, key, socket_address, isSecure);
                                }
                            }
                            , onAvailableTimeout
                    );
                } else {
                    cache.remove(key);
                    new_connection(cf_conn, key, socket_address, isSecure);
                    conn.close(0, "graceful shutdown", -1);
                }
            } else {
                cacheEntry.entry.handleAsync((conn, e) -> {
                    if (e!= null)
                        cf_conn.completeExceptionally(e);
                    else
                        cf_conn.complete(conn);
                    return conn;
                }, write_executor);
            }
        } else {

            new_connection(cf_conn, key, socket_address, isSecure);
        }

    }

    private void new_connection(CompletableFuture<Http2Connection> cf_conn, final InetSocketAddress key, java.net.InetSocketAddress socket_address,
                                boolean isSecure) {
        Objects.requireNonNull(cf_conn);
        Objects.requireNonNull(key);
        assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);

        final MinimalFuture<Http2Connection> cache_cf_conn = new MinimalFuture<>();
        CacheEntry<MinimalFuture<Http2Connection>> cache_entry = new CacheEntry(cache_cf_conn,isSecure,key);
        cache.put(key, cache_entry);

        cache_cf_conn.handleAsync((c, e) -> {
            if (e != null) {
                cf_conn.completeExceptionally(e);
                cache.remove(key);
            }
            else
                cf_conn.complete(c);
            return c;
        }, write_executor);

        new_connection_(key, socket_address, 0, isSecure, cache_cf_conn);
    }

    private void new_connection_(final InetSocketAddress key, java.net.InetSocketAddress socket_address,
                                int attempt, boolean isSecure, final MinimalFuture<Http2Connection> cache_cf_conn) {
        MinimalFuture<java.net.InetSocketAddress> cf_socket_address_resolved = new MinimalFuture<>();
        cf_socket_address_resolved
                .completeAsync(
                        () -> {
                            if (socket_address.isUnresolved()) {
                                if (logger.isDebugEnabled()) logger.debug("asynchronously resolving address for:" + socket_address.getHostString());
                                return new java.net.InetSocketAddress(socket_address.getHostName(), socket_address.getPort());
                            } else
                                return socket_address;
                        }
                        , pool_executor
                );

        cf_socket_address_resolved
                .thenAcceptAsync((InetSocketAddress socket_address_resolved) -> {
                    if (socket_address_resolved.isUnresolved()) {
                        if (logger.isInfoEnabled()) logger.info("Could not resolve : " + key.toString());
                        cache_cf_conn.completeExceptionally(new UnknownHostException(socket_address.getHostString()));
                    } else
                        if (logger.isTraceEnabled()) logger.trace("resolved address:"+key.toString());
                    try {
                        final AtomicReference<Http2Connection> ar_c = new AtomicReference();
                        client.client.newHttp2Connection(socket_address_resolved
                                , (Http2Connection c) -> {
                                    ar_c.set(c);
                                    write_executor.execute(() -> {
                                        cache_cf_conn.complete(c);
                                    });
                                }
                                , () -> {
                                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                                    //cache_cf_conn.cancel(false);
                                    if ((attempt + 1) < retryConnection) {
                                        if (logger.isDebugEnabled()) logger.debug("re-attempting to connect to : " + key.toString());
                                        new_connection_(key, socket_address, attempt + 1, isSecure, cache_cf_conn);
                                    } else {
                                        if (logger.isDebugEnabled()) logger.debug("maximum number of attempts to reached for : " + key.toString());
                                        cache_cf_conn.completeExceptionally(new PortUnreachableException(socket_address_resolved.toString() + " " +
                                                "after " + (attempt + 1) + " attempt(s)"));
                                        cache.remove(key);
                                    }
                                }
                                , () -> {
//                    write_executor.execute(() -> {
                                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                                    if (cache_cf_conn.isDone()) {
                                        if (logger.isDebugEnabled()) logger.debug("connection was closed : " + key.toString());
                                        cache.remove(key, cache_cf_conn);
                                    } else {
                                        if (logger.isDebugEnabled()) logger.debug("connection was closed but cf_conn was not completed: " + key.toString());
                                        cache_cf_conn.completeExceptionally(new IllegalStateException("should not happen, cf_conn was not " +
                                                "completed"));
                                    }
                                    Http2Connection c = ar_c.get();
                                    grace_set.remove(c);
//                    });
                                }
                                , props_not_secure!=null && !isSecure ? props_not_secure : props
                        );
                    } catch (RuntimeException re) {
                        if (logger.isInfoEnabled()) logger.info("failed to connect to:"+key.toString(), re);
                        cache_cf_conn.completeExceptionally(re);
                    } finally {
                        if (logger.isTraceEnabled()) logger.trace("finally resolved address:"+key.toString());
                    }

                }, write_executor)
                .handleAsync((aVoid, throwable) -> {
                    if (throwable != null) {
                        if (logger.isDebugEnabled()) logger.debug("caught a throwable will complete exceptionally", throwable);
                        cache_cf_conn.completeExceptionally(throwable);
                    }
                    return null;
                }, write_executor)
        ;
    }

    @Override
    public void get(CompletableFuture<Http2Connection> cf_conn, HttpRequest request) {
        Objects.requireNonNull(cf_conn);
        Objects.requireNonNull(request);

        write_executor.execute(() -> {
            delegate_connect(cf_conn, map(request), map(request), request.uri().getScheme().equalsIgnoreCase("https"));
        });
    }

}
