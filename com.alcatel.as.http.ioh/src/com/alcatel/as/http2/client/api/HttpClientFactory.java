// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api;

import org.osgi.annotation.versioning.ProviderType;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.function.Supplier;
import java.nio.file.Path;
import java.io.FileNotFoundException;
import java.util.function.Function;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;

/**
     * Implementations of {@link BodyPublisher BodyPublisher} that implement
     * various useful publishers, such as publishing the request body from a
     * String, or from a file.
     *
     * <p> The following are examples of using the predefined body publishers to
     * convert common high-level Java objects into a flow of data suitable for
     * sending as a request body:
     *
     *  <pre>{@code    // Request body from a String
     *   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("https://foo.com/"))
     *        .header("Content-Type", "text/plain; charset=UTF-8")
     *        .POST(BodyPublishers.ofString("some body text"))
     *        .build();
     *
     *   // Request body from a File
     *   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("https://foo.com/"))
     *        .header("Content-Type", "application/json")
     *        .POST(BodyPublishers.ofFile(Paths.get("file.json")))
     *        .build();
     *
     *   // Request body from a byte array
     *   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("https://foo.com/"))
     *        .POST(BodyPublishers.ofByteArray(new byte[] { ... }))
     *        .build(); }</pre>
     *
     */
@ProviderType
public interface HttpClientFactory {

    public HttpClient newHttpClient();

    public HttpClient.Builder newHttpClientBuilder ();

    public HttpRequest.Builder newHttpRequestBuilder ();

    public HttpRequest.Builder newHttpRequestBuilder (boolean internal);

    public HttpRequest.Builder newHttpRequestBuilder (java.net.URI uri);

    public HttpRequest.BodyPublishers bodyPublishers ();

    public HttpResponse.BodyHandlers bodyHandlers ();

    public HttpResponse.BodySubscribers bodySubscribers ();

    /**
         * Returns a push promise handler that accumulates push promises, and
         * their responses, into the given map.
         *
         * <p> Entries are added to the given map for each push promise accepted.
         * The entry's key is the push request, and the entry's value is a
         * {@code CompletableFuture} that completes with the response
         * corresponding to the key's push request. A push request is rejected /
         * cancelled if there is already an entry in the map whose key is
         * {@link HttpRequest#equals equal} to it. A push request is
         * rejected / cancelled if it  does not have the same origin as its
         * initiating request.
         *
         * <p> Entries are added to the given map as soon as practically
         * possible when a push promise is received and accepted. That way code,
         * using such a map like a cache, can determine if a push promise has
         * been issued by the server and avoid making, possibly, unnecessary
         * requests.
         *
         * <p> The delivery of a push promise response is not coordinated with
         * the delivery of the response to the initiating client-sent request.
         * However, when the response body for the initiating client-sent
         * request has been fully received, the map is guaranteed to be fully
         * populated, that is, no more entries will be added. The individual
         * {@code CompletableFutures} contained in the map may or may not
         * already be completed at this point.
         *
         * @param <T> the push promise response body type
         * @param pushPromiseHandler t he body handler to use for push promises
         * @param pushPromisesMap a map to accumulate push promises into
         * @return a push promise handler
         */
    public <T> HttpResponse.PushPromiseHandler<T>
	newPushPromiseHandler(Function<HttpRequest,HttpResponse.BodyHandler<T>> pushPromiseHandler,
			      ConcurrentMap<HttpRequest,CompletableFuture<HttpResponse<T>>> pushPromisesMap);
}
