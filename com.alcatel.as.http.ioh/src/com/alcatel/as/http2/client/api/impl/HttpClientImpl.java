// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.Http2Client;
import com.alcatel.as.http2.client.Http2Request;
import com.alcatel.as.http2.client.Http2ResponseListener;
import com.alcatel.as.http2.client.SendReqBuffer;
import com.alcatel.as.http2.client.api.*;
import com.alcatel.as.http2.client.api.HttpResponse.BodyHandler;
import com.alcatel.as.http2.client.api.HttpResponse.PushPromiseHandler;
import com.alcatel.as.http2.client.api.impl.common.Config;
import com.alcatel.as.http2.client.api.impl.common.HttpHeadersBuilder;
import com.alcatel.as.http2.client.api.impl.common.MinimalFuture;
import com.alcatel.as.http2.client.api.impl.common.OperationTrackers.Trackable;
import com.alcatel.as.http2.client.api.impl.common.OperationTrackers.Tracker;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.alcatel.as.http2.client.api.impl.common.Config.CONNECTION_VERSION_DEFAULT_VALUE;
import static com.alcatel.as.http2.client.api.impl.common.Config.CONNECTION_VERSION_PROPS_KEY;


/**
 * Client implementation. Contains all configuration information and also
 * the selector manager thread which allows async events to be registered
 * and delivered when they occur. See AsyncEvent.
 */
final class HttpClientImpl implements HttpClient, Trackable {

    static final  AtomicLong                               CLIENT_IDS              = new AtomicLong();
    final         Logger                                   logger;
    final         Http2Client                              client;
    final         Executor                                 executor;//= PostMockImpl.executor;
    final         Executor                                 pool_executor;//= PostMockImpl.executor;
    final         Executor                                 write_executor;
    final         Executor                                 read_executor;
    final         PlatformExecutors                        execs;
    private final Duration                                 connectTimeout;
    private final boolean                                  isDefaultExecutor;
    private final long                                     id;
    private final String                                   dbgTag;
    private final Map<String, Object>                      props;
    private final ConnectionPool                           connection_pool;
    // This reference is used to keep track of the facade HttpClient
    // that was returned to the application code.
    // It makes it possible to know when the application no longer
    // holds any reference to the HttpClient.
    // Unfortunately, this information is not enough to know when
    // to exit the SelectorManager thread. Because of the asynchronous
    // nature of the API, we also need to wait until all pending operations
    // have completed.
    private final WeakReference<HttpClientFacade>          facadeRef;
    // This counter keeps track of the number of operations pending
    // on the HttpClient. The SelectorManager thread will wait
    // until there are no longer any pending operations and the
    // facadeRef is cleared before exiting.
    //
    // The pendingOperationCount is incremented every time a send/sendAsync
    // operation is invoked on the HttpClient, and is decremented when
    // the HttpResponse<T> object is returned to the user.
    // However, at this point, the body may not have been fully read yet.
    // This is the case when the response T is implemented as a streaming
    // subscriber (such as an InputStream).
    //
    // To take care of this issue the pendingOperationCount will additionally
    // be incremented/decremented in the following cases:
    //
    // 1. For HTTP/2  it is incremented when a stream is added to the
    //    Http2Connection streams map, and decreased when the stream is removed
    //    from the map. This should also take care of push promises.
    // 2. For WebSocket the count is increased when creating a
    //    DetachedConnectionChannel for the socket, and decreased
    //    when the the channel is closed.
    //    In addition, the HttpClient facade is passed to the WebSocket builder,
    //    (instead of the client implementation delegate).
    // 3. For HTTP/1.1 the count is incremented before starting to parse the body
    //    response, and decremented when the parser has reached the end of the
    //    response body flow.
    //
    // This should ensure that the selector manager thread remains alive until
    // the response has been fully received or the web socket is closed.
    private final AtomicLong                               pendingOperationCount   = new AtomicLong();
    private final AtomicLong                               pendingWebSocketCount   = new AtomicLong();
    private final AtomicLong                               pendingHttpRequestCount = new AtomicLong();
    private final AtomicLong                               pendingHttp2StreamCount = new AtomicLong();

    // FIXME: make it a configuration
    private final int                                      BUFFERED_MAX_SIZE       = Integer.MAX_VALUE;
    
    //Pseudo header that can be set in the request to pass a raw HTTP URL as a string without the processing
    //done by the URI class
    private static final String RAW_PATH_PSEUDOHEADER = "__raw_path";

    private HttpClientImpl(HttpClientBuilderImpl builder,
                           SingleFacadeFactory facadeFactory,
                           Http2Client client) {
        executor = builder.executor;
        pool_executor = builder.pool_executor;
        execs = builder.execs;

        this.client = client;
        if (builder.read_executor == null)
            read_executor = executor;
        else
            read_executor = builder.read_executor;
        if (builder.write_executor == null)
            write_executor = executor;
        else
            write_executor = builder.write_executor;
        builder.props.put(Http2Client.PROP_EXECUTOR_READ, read_executor);
        builder.props.put(Http2Client.PROP_EXECUTOR_WRITE, write_executor);
        props = Collections.unmodifiableMap(builder.props);
        id = CLIENT_IDS.incrementAndGet();
        logger = builder.props.containsKey(Http2Client.PROP_CLIENT_LOGGER) ?
                (Logger)builder.props.get(Http2Client.PROP_CLIENT_LOGGER) :
                Config.loggerFromProps( builder.props, ()-> Config.getLogger(id));
        dbgTag = "HttpClientImpl(" + id + ")";

        if (executor == null) {
            isDefaultExecutor = true;
            throw new IllegalStateException("executor must be set with the builder.");
        } else {
            isDefaultExecutor = false;
        }
        facadeRef = new WeakReference<>(facadeFactory.createFacade(this));
        connectTimeout = builder.connectTimeout;

        int connection_version = (int) props.getOrDefault(CONNECTION_VERSION_PROPS_KEY, CONNECTION_VERSION_DEFAULT_VALUE);
        switch (connection_version) {
            case 1:
                connection_pool= new AsrConnectionPool(logger, pool_executor, builder.execs, write_executor, this, props);
                break;
            case 2:
                if (direct_connection(props))
                    connection_pool= new ASRConnectionPoolMultiIP(logger, pool_executor, builder.execs, write_executor, this, props);
                else
                    connection_pool = new AsrConnectionPool(logger, pool_executor, builder.execs, write_executor, this, props);
                break;
            default:
                throw new IllegalArgumentException("Property \"" + CONNECTION_VERSION_PROPS_KEY + "\" has illegal value:" + connection_version);
        }

        assert facadeRef.get() != null;
    }

    static boolean direct_connection(Map<String, Object> props) {
        if (props.containsKey(Config.SINGLE_PROXY_SOCKET_PROPS_KEY) && (boolean)props.get(Config.SINGLE_PROXY_SOCKET_PROPS_KEY)) return false;
        if (props.containsKey(Http2Client.PROP_PROXY_IP)) return false;
        return true;
    }

    static HttpClientFacade create(HttpClientBuilderImpl builder) {
        SingleFacadeFactory facadeFactory = new SingleFacadeFactory();
        HttpClientImpl      impl          = new HttpClientImpl(builder, facadeFactory, builder.client);
        assert facadeFactory.facade != null;
        assert impl.facadeRef.get() == facadeFactory.facade;
        return facadeFactory.facade;
    }

    public static ByteBuffer cloneBB(ByteBuffer original) {
        ByteBuffer clone = ByteBuffer.allocate(original.remaining());
        clone.put(original);
        clone.flip();
        return clone;
    }

    // Returns the facade that was returned to the application code.
    // May be null if that facade is no longer referenced.
    final HttpClientFacade facade() {
        return facadeRef.get();
    }

    // Increments the pendingOperationCount.
    final long reference() {
        pendingHttpRequestCount.incrementAndGet();
        return pendingOperationCount.incrementAndGet();
    }

    // Decrements the pendingOperationCount.
    final long unreference() {
        final long count          = pendingOperationCount.decrementAndGet();
        final long httpCount      = pendingHttpRequestCount.decrementAndGet();
        final long http2Count     = pendingHttp2StreamCount.get();
        final long webSocketCount = pendingWebSocketCount.get();
        if (count == 0 && facade() == null) {
            // FIXME:
//            selmgr.wakeupSelector();
        }
        assert httpCount >= 0 : "count of HTTP/1.1 operations < 0";
        assert http2Count >= 0 : "count of HTTP/2 operations < 0";
        assert webSocketCount >= 0 : "count of WS operations < 0";
        assert count >= 0 : "count of pending operations < 0";
        return count;
    }

    public Tracker getOperationsTracker() {
        return new HttpClientTracker(pendingHttpRequestCount,
                pendingHttp2StreamCount,
                pendingWebSocketCount,
                pendingOperationCount,
                facadeRef,
                dbgTag);
    }

    @Override
    public <T> HttpResponse<T>
    send(HttpRequest req, BodyHandler<T> responseHandler)
            throws IOException, InterruptedException {
        CompletableFuture<HttpResponse<T>> cf = null;
        try {
            cf = sendAsync(req, responseHandler, null);
            return cf.get();
        } catch (InterruptedException ie) {
            if (cf != null)
                cf.cancel(true);
            throw ie;
        } catch (ExecutionException e) {
            final Throwable throwable = e.getCause();
            final String    msg       = throwable.getMessage();

            if (throwable instanceof IllegalArgumentException) {
                throw new IllegalArgumentException(msg, throwable);
            } else if (throwable instanceof SecurityException) {
                throw new SecurityException(msg, throwable);
            } else if (throwable instanceof HttpConnectTimeoutException) {
                HttpConnectTimeoutException hcte = new HttpConnectTimeoutException(msg);
                hcte.initCause(throwable);
                throw hcte;
            } else if (throwable instanceof HttpTimeoutException) {
                throw new HttpTimeoutException(msg);
            } else if (throwable instanceof ConnectException) {
                ConnectException ce = new ConnectException(msg);
                ce.initCause(throwable);
                throw ce;
            } else if (throwable instanceof IOException) {
                throw new IOException(msg, throwable);
            } else {
                throw new IOException(msg, throwable);
            }
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest, BodyHandler<T> responseHandler) {
        return sendAsync(userRequest, responseHandler, null);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest userRequest,
              BodyHandler<T> responseHandler,
              PushPromiseHandler<T> pushPromiseHandler) {
        Objects.requireNonNull(userRequest);
        Objects.requireNonNull(responseHandler);


        MinimalFuture<HttpResponse<T>> cf_result = new MinimalFuture<>();

        // we run on the read_executor so that the listener is created on the read_executor (where it lives), other parts are agnostic or properly scheduled
        read_executor.execute(() -> {

            Http2ResponseListenerImpl listener = new Http2ResponseListenerImpl(cf_result, userRequest, responseHandler);


            final MinimalFuture<com.alcatel.as.http2.client.Http2Connection> cf_conn = new MinimalFuture<>();
            cf_conn.thenAcceptAsync((com.alcatel.as.http2.client.Http2Connection conn) ->
                    {
                        assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.WRITE_EXECUTOR_NAME);
                        Http2Request request = conn.newRequest(listener);
                        read_executor.execute( ()->{
                        listener.h2request = request;
                        });
                        userRequest.timeout().ifPresent(
                                duration -> listener.setTimeout(duration)
                        );

                        request
                                .setReqAuth(userRequest.uri().getRawAuthority())
                                .setReqMethod(userRequest.method())
				.setReqScheme(userRequest.uri().getScheme())
                        ;
                        
                        String rawPath = userRequest.headers().firstValue(RAW_PATH_PSEUDOHEADER).orElse(null);     
                        
                        if(rawPath == null) {
							final String query=userRequest.uri().getRawQuery();
							final String fragment=userRequest.uri().getRawFragment();
				                        if (query == null && fragment == null)
				                                request.setReqPath(userRequest.uri().getRawPath());
				                        else if (fragment == null)
				                                request.setReqPath(userRequest.uri().getRawPath()+"?"+query);
				                        else if (query == null)
				                                request.setReqPath(userRequest.uri().getRawPath()+"#"+fragment);
							else
				                                request.setReqPath(userRequest.uri().getRawPath()+"?"+query+"#"+fragment);

                        } else {
                        	if(logger.isDebugEnabled()) {
                        		logger.debug("Used raw query in HTTP2 Client " + rawPath);
                        	}
                        	request.setReqPath(rawPath);
                        }
                        
                        userRequest.headers().map().forEach((k, vs) -> {
                        	if(RAW_PATH_PSEUDOHEADER.equals(k)) return;
                            vs.forEach(v -> {
                                request.setReqHeader(k, v);
                            });
                        })
                        ;
                        
                        if (userRequest.bodyPublisher().isPresent()) {
                            HttpRequest.BodyPublisher body_publisher = userRequest.bodyPublisher().get();

                            long contentLength = body_publisher.contentLength();

                            if (contentLength > 0){
                                request.setReqHeader("content-length", Long.toString(contentLength));
                            }

                            if (contentLength == 0) {
                                request.sendReqHeaders(true);
                            } else {
                            request.sendReqHeaders(false);
                            SendReqBuffer sendBuffer = request.newSendReqBuffer(BUFFERED_MAX_SIZE);
                            body_publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
                                volatile Flow.Subscription subscription;

                                @Override
                                public void onSubscribe(Flow.Subscription subscription) {
                                    Objects.requireNonNull(subscription);
                                    this.subscription = subscription;
                                    subscription.request(1);
                                }

                                @Override
                                public void onNext(ByteBuffer item) {
                                    // XXX: It is already a copy so we should not need to copy again!
                                    write_executor.execute( () -> sendBuffer.send(item, true, false));
                                    Objects.requireNonNull(subscription, "onSubscribe was not invoked");
                                    subscription.request(1);
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    write_executor.execute( () -> {
                                    // RFC7540 0x2 	INTERNAL_ERROR
                                    request.abort(2);
                                    sendBuffer.clear();
                                    });
                                    Objects.requireNonNull(subscription, "onSubscribe was not invoked");
                                    subscription.cancel();

                                    cf_conn.completeExceptionally(throwable);
                                }

                                @Override
                                public void onComplete() {
                                    write_executor.execute( () -> {
                                    sendBuffer.send(null, false, true);
                                    });
                                }
                            });
                          }
                        } else {
                            request.sendReqHeaders(true);
                        }

                    }, write_executor
            );

            // this is executor agnostic as it schedules into the write_executor
            connection_pool.get(cf_conn, userRequest);

            // this is executor agnostic
            cf_conn.handle((conn, exception) -> {
                if (exception != null) cf_result.completeExceptionally(exception);
                return null;
            });

        });

        return cf_result;
    }

    /**
     * Close the client.
     * Release all resources help by the client (connections, etc...).
     */
    @Override
    public void close() {
        close(false);
    }

    /**
     * Close all connections.
     * gracefully if graceful is set to true
     * forcibly otherwise.
     *
     * When used gracefully it will allow new request to start on fresh connections. This can help refreshing TLS credentials.
     * @param graceful
     */
    @Override
    public void close(boolean graceful) {
        if (connection_pool!=null) {
            write_executor.execute( () -> connection_pool.close(graceful?
                    ConnectionPool.ClosePolicy.CLOSE_GRACEFULLY:
                    ConnectionPool.ClosePolicy.CLOSE_FORCIBLY) );
        }
    }

    @Override
    public final Optional<Executor> executor() {
        return isDefaultExecutor
                ? Optional.empty()
                : Optional.of(executor);
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return Optional.ofNullable(connectTimeout);
    }

    @Override
    public String toString() {
        // Used by tests to get the client's id and compute the
        // name of the SelectorManager thread.
        return super.toString() + ("(" + id + ")");
    }

    /**
     * This is a bit tricky:
     * 1. an HttpClientFacade has a final HttpClientImpl field.
     * 2. an HttpClientImpl has a final WeakReference<HttpClientFacade> field,
     * where the referent is the facade created for that instance.
     * 3. We cannot just create the HttpClientFacade in the HttpClientImpl
     * constructor, because it would be only weakly referenced and could
     * be GC'ed before we can return it.
     * The solution is to use an instance of SingleFacadeFactory which will
     * allow the caller of new HttpClientImpl(...) to retrieve the facade
     * after the HttpClientImpl has been created.
     */
    private static final class SingleFacadeFactory {
        HttpClientFacade facade;

        HttpClientFacade createFacade(HttpClientImpl impl) {
            assert facade == null;
            return (facade = new HttpClientFacade(impl));
        }
    }

    final static class HttpClientTracker implements Tracker {
        final AtomicLong   httpCount;
        final AtomicLong   http2Count;
        final AtomicLong   websocketCount;
        final AtomicLong   operationsCount;
        final Reference<?> reference;
        final String       name;

        HttpClientTracker(AtomicLong http,
                          AtomicLong http2,
                          AtomicLong ws,
                          AtomicLong ops,
                          Reference<?> ref,
                          String name) {
            this.httpCount = http;
            this.http2Count = http2;
            this.websocketCount = ws;
            this.operationsCount = ops;
            this.reference = ref;
            this.name = name;
        }

        @Override
        public long getOutstandingOperations() {
            return operationsCount.get();
        }

        @Override
        public long getOutstandingHttpOperations() {
            return httpCount.get();
        }

        @Override
        public long getOutstandingHttp2Streams() {
            return http2Count.get();
        }

        @Override
        public long getOutstandingWebSocketOperations() {
            return websocketCount.get();
        }

        @Override
        public boolean isFacadeReferenced() {
            return reference.get() != null;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    final class Http2ResponseListenerImpl<T> implements Http2ResponseListener {

        private       Http2Request                   h2request;
        private       ScheduledFuture                timeout_future;
        final private AtomicBoolean                  aborted = new AtomicBoolean(false);
        final private MinimalFuture<HttpResponse<T>> cf_response;
        final private HttpRequest                    request;
        final private BodyHandler<T>                 responseHandler;
        final private HttpHeadersBuilder             headers_builder         = new HttpHeadersBuilder();
        final private MinimalFuture<HttpResponse.BodySubscriber<T>>     cf_body_subscriber      =
                new MinimalFuture<>();
        private       CompletableFuture<HttpResponse.BodySubscriber<T>> last_cf_body_subscriber = cf_body_subscriber;
        private       int                                               status;
        private       ResponseInfoImpl                                  info;


        public Http2ResponseListenerImpl(
                MinimalFuture<HttpResponse<T>> cf_response
                , HttpRequest request
                , BodyHandler<T> responseHandler
        ) {
            this.cf_response = cf_response;
            this.request = request;
            this.responseHandler = responseHandler;
            cf_response.handleAsync((conn, exception) -> {
                if (exception != null) abort(exception);
                return null;
            }, read_executor);
        }

        @Override
        public void recvRespStatus(Http2Request req, int status) {
            assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.READ_EXECUTOR_NAME) : execs.getCurrentThreadContext().getCurrentExecutor().getId();
            this.status = status;
        }

        @Override
        public void recvRespHeader(Http2Request req, String name, String value) {
            assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.READ_EXECUTOR_NAME) : execs.getCurrentThreadContext().getCurrentExecutor().getId();
            headers_builder.addHeader(name, value);
        }

        @Override
        public void recvRespHeaders(Http2Request req, boolean done) {
            assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.READ_EXECUTOR_NAME) : execs.getCurrentThreadContext().getCurrentExecutor().getId();
            info = new ResponseInfoImpl(
                    status, headers_builder.build(), Version.HTTP_2,
                    req.getConnection().exportTlsKey()
            );
            executor.execute(()->{
                if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":recvRespHeaders");
            HttpResponse.BodySubscriber<T> body_subscriber = responseHandler.apply(info);

            cf_body_subscriber.complete( body_subscriber );
            });

        }

        @Override
        public void recvRespData(Http2Request req, ByteBuffer data, boolean done) {
            assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.READ_EXECUTOR_NAME) : execs.getCurrentThreadContext().getCurrentExecutor().getId();
            if (data.hasRemaining()) {
            ByteBuffer data_clone = cloneBB(data);

            last_cf_body_subscriber = last_cf_body_subscriber
                    .thenApplyAsync(subscriber -> {
                        if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":recvRespData");
                        subscriber.onNext(Collections.singletonList(data_clone));
                        return subscriber;
                    },executor);
            } else {
                if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":recvRespData:skippedEmptyData");
            }
        }

        @Override
        public void recvRespTrailer(Http2Request req, String name, String value) {
            // FIXME: implement
        }

        @Override
        public void endResponse(Http2Request req) {
            assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.READ_EXECUTOR_NAME) : execs.getCurrentThreadContext().getCurrentExecutor().getId();
            if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":endResponse");
            last_cf_body_subscriber = last_cf_body_subscriber
                    .handleAsync((subscriber,exception) -> {
                        if (exception != null) abort(exception);
                        if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":endResponse:onComplete:"+subscriber+":"+exception,exception);
                        subscriber.onComplete();
                        return subscriber;
                    },executor);
            last_cf_body_subscriber
                    .thenComposeAsync(subscriber -> subscriber.getBody(),executor)
                    .handleAsync((body,exception) -> {
                        if (exception != null) abort(exception);
                        if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":endResponse:response:"+cf_response+":"+body+":"+exception,exception);
                        HttpResponse<T> response = new HttpResponseSimpleImpl<>(
                                info, request, body
                        );
                        //
                        cf_response.complete(response);
                        if (timeout_future != null)
                            timeout_future.cancel(false);
                        // We set it so it can't be aborted anymore
                        aborted.set(true);
                        return null;
                    },executor)
            ;
        }

        @Override
        public void abortRequest(Http2Request req) {
            assert execs.getCurrentThreadContext().getCurrentExecutor().getId().equalsIgnoreCase(Config.READ_EXECUTOR_NAME) : execs.getCurrentThreadContext().getCurrentExecutor().getId();
            if (aborted.get()) {
                if (logger.isTraceEnabled()) logger.trace(dbgTag + h2request + ":abortRequest");
            } else {
                if (logger.isDebugEnabled()) logger.debug(dbgTag+h2request+":abortRequest");
                Exception protocolError = new java.net.ProtocolException();
                if (cf_body_subscriber.isDone() && !cf_body_subscriber.isCompletedExceptionally()) {
                    final Flow.Subscriber subscriber = cf_body_subscriber.join();
                    executor.execute(() -> subscriber.onError(protocolError));
                }
                if (timeout_future != null)
                    timeout_future.cancel(false);
                aborted.set(true);
                // After setting the guard(aborted to true) because we are in read_executor and cf is on executor
                cf_response.completeExceptionally(protocolError);
            }
        }

        public void abort(Throwable reason) {
            // invoked on read_executor explicitly
            if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":abort", reason);
            if (!aborted.get()) {
                if (logger.isDebugEnabled()) logger.debug(dbgTag+h2request+":abort", reason);
                if (cf_body_subscriber.isDone() && !cf_body_subscriber.isCompletedExceptionally()) {
                    final Flow.Subscriber subscriber = cf_body_subscriber.join();
                    executor.execute(() -> subscriber.onError(reason));
                }
                aborted.set(true);
                // h2request may be null
                if (h2request != null) write_executor.execute(()->{
                // RFC7540 0x8 	CANCEL
                h2request.abort(0x08);
                });
                if (timeout_future != null)
                    timeout_future.cancel(false);
                // After setting the guard(aborted to true) because we are in read_executor and cf is on executor
                cf_response.completeExceptionally(reason);
            }
        }

        public void setTimeout(Duration duration) {
            // invoked from: executor => reschedule into read_executor
            if (logger.isTraceEnabled()) logger.trace(dbgTag+h2request+":setting timer "+duration.toString() + " "+ duration.toMillis()+" "+TimeUnit.MILLISECONDS);
            timeout_future =
            ((PlatformExecutor)read_executor)
                    .schedule( () -> abort(new HttpTimeoutException(duration.toString())) , duration.toMillis(), TimeUnit.MILLISECONDS );
        }
    }

    final static class HttpResponseSimpleImpl<T> implements HttpResponse<T> {
        private final ResponseInfoImpl info;
        private final HttpRequest      request;
        private final T                body;

        public HttpResponseSimpleImpl(ResponseInfoImpl info, HttpRequest request, T body) {
            this.info = info;
            this.request = request;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return info.statusCode();
        }

        @Override
        public HttpRequest request() {
            return request;
        }

        @Override
        public HttpHeaders headers() {
            return info.headers();
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public Map<String, Object> exportKeyingMaterial() {
            return info.exportKeyingMaterial();
        }
    }


}
