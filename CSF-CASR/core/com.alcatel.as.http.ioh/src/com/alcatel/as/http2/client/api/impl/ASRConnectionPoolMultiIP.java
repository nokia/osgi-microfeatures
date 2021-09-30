package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.Http2Connection;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.impl.common.Config;
import com.alcatel.as.http2.client.api.impl.common.MinimalFuture;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ASRConnectionPoolMultiIP extends AbstractConnectionPool<ASRConnectionPoolMultiIP.SingleManager> {

    private final long cycle_trigger_threshold_on_stream_id;
    public ASRConnectionPoolMultiIP(Logger logger, Executor pool_executor, PlatformExecutors execs, Executor write_executor, HttpClientImpl client, Map<String, Object> props) {
        super((e)->e.entry.get() ,logger, pool_executor,  execs, write_executor, client, props);

        this.cycle_trigger_threshold_on_stream_id =
                (long) props.getOrDefault(Config.CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID_PROPS_KEY, Config.CYCLE_TRIGGER_THRESHOLD_ON_STREAM_ID_DEFAULT_VALUE);
    }

    @Override
    public void get(CompletableFuture<Http2Connection> cf_conn, HttpRequest request) {
        Objects.requireNonNull(cf_conn);
        Objects.requireNonNull(request);
        final InetSocketAddress key = map(request);
        write_executor.execute(() -> {
            CacheEntry<SingleManager> entry = cache.get(key);
            if ( entry == null) {
                SingleManager singleManager = new SingleManager(
                        request.uri()
                        , (c) -> { }
                        , (c) -> grace_set.remove(c)
                        , (c) -> { grace_set.add(c) ; close_gracefull(c); }
                );
                entry = new CacheEntry<SingleManager>(singleManager, singleManager.isSecure , key) {
                    @Override
                    void close() {
                        super.close();
                        ((SingleManager)entry).close();
                    }

                };
                cache.put(key, entry);
            }

            CacheEntry<SingleManager> finalEntry = entry;
            entry.entry.get().handleAsync(
                    (c, e) -> {
                        if (e != null)
                            cf_conn.completeExceptionally(e);
                        else if (c != null) {
                            cf_conn.complete(c);
                            // has to be called on the write_executor
                            if (finalEntry.entry.stream_id_exhausted(c) ) {
                                if ( ! finalEntry.entry.cycle_lock.get() ) {
                                    finalEntry.entry.cycle_lock.set(true);
                                    // force scheduling a new task.
                                    write_executor.execute(finalEntry.entry::connect_and_swap);
                                } else {
                                    if (logger.isTraceEnabled()) logger.trace("locked cycle");
                                }
                            }
                        }
                        else
                            cf_conn.completeExceptionally(new IllegalStateException("http2connection is null"));
                        return c;
                    },write_executor);

        });
    }

    class SingleManager {
        private final   AtomicBoolean                  cycle_lock = new AtomicBoolean(false);
        private final   Consumer<Http2Connection>      register;
        private final   Consumer<Http2Connection>      unregister;
        private final   Consumer<Http2Connection>      cycle_closer;
        private final   List<InetAddress>              black_ips = new ArrayList<>();
        private final   URI                            uri;
        private final   boolean                        isSecure;
        // can't be final because of swap
        protected       MinimalFuture<Http2Connection> cache_cf_conn;
        private         InetSocketAddress              current_address;
        private         boolean                        closed = false;

    public SingleManager(
            URI uri
            , Consumer<Http2Connection> register
            , Consumer<Http2Connection> unregister
            , Consumer<Http2Connection> cycle_closer
    ) {
        this.uri              = uri;
        this.register         = register;
        this.unregister       = unregister;
        this.cycle_closer     = cycle_closer;

        this.isSecure = uri.getScheme().equalsIgnoreCase("https");
    }

    private void close() {
        closed = true;
    }

    private boolean stream_id_exhausted(Http2Connection c) {
        int remainingRequests = c.remainingRequests();
        if (logger.isTraceEnabled()) logger.trace("c.remainingRequests: "+remainingRequests + " / trigger :"+cycle_trigger_threshold_on_stream_id);
        if ( remainingRequests <= cycle_trigger_threshold_on_stream_id)
            return true;
        return false;
    }

    /**
     * Called anticipate because swapping is not performed right away.
     * If "re"connection fails then no more actions are taken until the connections dies
     * from stream exhaustion.
     * May be called indefinitely, will reconnect to the same IP (no DNS lookup or round-robin).
     */
    public void anticipate() {
        if (cache_cf_conn.isDone() && !cache_cf_conn.isCompletedExceptionally()) {
            connect_and_swap();
        } else {
            logger.warn("can't cycle if not connected! for:" + uri.getHost());
        }
    }

    /**
     * Errors are vehiculated through the CompletableFuture by the means of an exception.
     */
    public CompletableFuture<Http2Connection> get() {
        if (cache_cf_conn == null) {
            cache_cf_conn = new MinimalFuture<>();

            attempt();
        }

        return cache_cf_conn;
    }

    /**
     * enables to iterate through the IPs resolved by the name service.
     */
    protected void attempt() {
        MinimalFuture<InetAddress[]> cf_resolver = new MinimalFuture<>();
        cf_resolver
                .completeAsync(() -> getAllByName(uri), pool_executor)
                .handleAsync((ips, e) -> {
                    InetAddress ip = null;
                    if (e != null) {
                        error(new UnknownHostException("unknown host:" + uri.getHost()));
                    } else if (ips != null && ips.length != 0) {
                        if (logger.isTraceEnabled()) logger.trace("IPs[]:" + listToString(ips));
                        ip = elect(ips);
                        if (ip == null) {
                            error(new java.net.ConnectException("all attempts to reach for: " + uri.getHost() + " through" + listToString(black_ips)));
                        } else {
                            InetSocketAddress socket_address = make_address(ip, uri);
                            connect(socket_address);
                        }
                    } else {
                        error(new UnknownHostException("can't resolve, resolution returned null or empty list for:" + uri.getHost()));
                    }
                    return ip;
                }, write_executor)
                .handleAsync((x, e) -> {
                    if (e != null)
                        error(new IllegalStateException("caught unexpected exception", e));
                    return x;
                }, write_executor)
        ;

    }

    protected InetSocketAddress make_address(InetAddress ip, URI uri) {
        int p = uri.getPort();
        if (p == -1) {
            if (uri.getScheme().equalsIgnoreCase("https")) {
                p = 443;
            } else {
                p = 80;
            }
        }

        return new InetSocketAddress(ip.getHostAddress(), p);
    }

    /**
     * Choose an address we have not yet contacted.
     *
     * @param ips
     * @return null if it can't elect an ip all are black listed
     */
    protected InetAddress elect(InetAddress[] ips) {
        Objects.requireNonNull(ips);
        InetAddress elected = null;
        ip_loop:
        for (InetAddress ip : ips) {
            elected = ip;
            for (InetAddress black : black_ips) {
                if (black.equals(ip)) {
                    elected = null;
                    continue ip_loop;
                }
            }
            if (elected != null)
                break ip_loop;
        }
        if (elected != null)
            black_ips.add(elected);

        return elected;
    }

    protected void connect(InetSocketAddress socket_address_resolved) {
        final AtomicReference<Http2Connection> ar_c = new AtomicReference();
        client.client.newHttp2Connection(socket_address_resolved
                , (Http2Connection c) -> {
                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                    ar_c.set(c);
                    current_address = socket_address_resolved;
                    cache_cf_conn.complete(c);
                    register.accept(c);
//RFU://                    connected_state();
                    if (logger.isInfoEnabled()) logger.info("connected to " + uri.getHost() + "/" + socket_address_resolved);
                }
                , () -> {
                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                    write_executor.execute(
                            () -> {
                                if (logger.isInfoEnabled()) logger.info("can't connect to " + uri.getHost() + "/" + socket_address_resolved);
                                attempt();
                            }
                    );
                }
                , () -> {
                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                    Http2Connection c = ar_c.get();
                    unregister.accept(c);
//RFU://                    if (cache_cf_conn.join() == c)
//RFU://                        closed_state();
                }
                , props_not_secure != null && !isSecure ? props_not_secure : props
        );
    }

    protected void connect_and_swap() {
        if (logger.isTraceEnabled()) logger.trace("cycle entry "+cache_cf_conn.join());
        final AtomicReference<Http2Connection> ar_c = new AtomicReference();
        client.client.newHttp2Connection(current_address
                , (Http2Connection c) -> {
                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                    if (closed) {
                        c.close(0x08, "entry was clause while cycling", 0,0);
                        return;
                    }
                    ar_c.set(c);
                    if (logger.isInfoEnabled())
                        logger.info("cycling connection to:" + uri.getHost() + "(" + current_address + ") by anticipation.");
                    // swap
                    cycle_closer.accept(cache_cf_conn.join());
                    MinimalFuture<Http2Connection> cf = new MinimalFuture<>();
                    cf.complete(c);
                    cache_cf_conn = cf;
                    register.accept(c);
                    cycle_lock.set(false);
                    //connected_state();
                }
                , () -> {
                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                    logger.warn("attempt to anticipate connection to:" + uri.getHost() + "(" + current_address + ") failed");
                    cycle_lock.set(false);
                }
                , () -> {
                    assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                    Http2Connection c = ar_c.get();
                    unregister.accept(c);
//RFU://                    if (cache_cf_conn.join() == c)
//RFU://                        closed_state();
                }
                , props_not_secure != null && !isSecure ? props_not_secure : props
        );
    }

    // ---------------------------------------------------------------------------------------------------- STATE MANAGEMENT

//RFU://    protected void connected_state() {}
//RFU://    protected void closed_state() {}

    protected void error(Exception e) {
        cache_cf_conn.completeExceptionally(e);
    }

}
    // ---------------------------------------------------------------------------------------------------- UTILITIES
    static InetAddress[] getAllByName(URI uri) {
        String host = uri.getHost();
        try {
            return InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new RuntimeException("UnknownHost can't resolve" + host, e);
        }
    }

    static <T> String listToString(T[] array) {
        return listToString(java.util.Arrays.asList(array));
    }

    static <T> String listToString(List<T> list) {
        return list
                .stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
    }

}