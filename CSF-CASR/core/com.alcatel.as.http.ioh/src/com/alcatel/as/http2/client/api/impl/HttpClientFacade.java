package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpResponse;
import com.alcatel.as.http2.client.api.HttpResponse.BodyHandler;
import com.alcatel.as.http2.client.api.HttpResponse.PushPromiseHandler;
import com.alcatel.as.http2.client.api.impl.common.OperationTrackers.Trackable;
import com.alcatel.as.http2.client.api.impl.common.OperationTrackers.Tracker;

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * An HttpClientFacade is a simple class that wraps an HttpClient implementation
 * and delegates everything to its implementation delegate.
 */
final class HttpClientFacade implements HttpClient, Trackable {

    final HttpClientImpl impl;

    /**
     * Creates an HttpClientFacade.
     */
    HttpClientFacade(HttpClientImpl impl) {
        this.impl = impl;
    }

    @Override // for tests
    public Tracker getOperationsTracker() {
        return impl.getOperationsTracker();
    }

    @Override
    public Optional<Duration> connectTimeout() {
        return impl.connectTimeout();
    }

//    @Override
//    public Optional<ProxySelector> proxy() {
//        return impl.proxy();
//    }

    @Override
    public Optional<Executor> executor() {
        return impl.executor();
    }

    @Override
    public <T> HttpResponse<T>
    send(HttpRequest req, HttpResponse.BodyHandler<T> responseBodyHandler)
        throws IOException, InterruptedException
    {
        try {
            return impl.send(req, responseBodyHandler);
        } finally {
            // FIXME: Reference.reachabilityFence
//            Reference.reachabilityFence(this);
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest req, HttpResponse.BodyHandler<T> responseBodyHandler) {
        try {
            return impl.sendAsync(req, responseBodyHandler);
        } finally {
            // FIXME: Reference.reachabilityFence
//            Reference.reachabilityFence(this);
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest req,
              BodyHandler<T> responseBodyHandler,
              PushPromiseHandler<T> pushPromiseHandler){
        try {
            return impl.sendAsync(req, responseBodyHandler, pushPromiseHandler);
        } finally {
            // FIXME: Reference.reachabilityFence
//            Reference.reachabilityFence(this);
        }
    }

    /**
     * Close the client.
     * Release all resources help by the client (connections, etc...).
     */
    @Override
    public void close() {
        try {
            impl.close(false);
        } finally {
            // FIXME: Reference.reachabilityFence
//            Reference.reachabilityFence(this);
        }
    }

    /**
     * Close the client.
     * Release all resources help by the client (connections, etc...).
     */
    @Override
    public void close(boolean graceful) {
        try {
            impl.close(graceful);
        } finally {
            // FIXME: Reference.reachabilityFence
//            Reference.reachabilityFence(this);
        }
    }

    @Override
    public String toString() {
        // Used by tests to get the client's id.
        return impl.toString();
    }
}
