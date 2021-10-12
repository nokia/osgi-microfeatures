// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api;

import com.alcatel.as.http2.client.api.HttpResponse.BodyHandler;
import com.alcatel.as.http2.client.api.HttpResponse.PushPromiseHandler;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URLPermission;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;
import org.osgi.annotation.versioning.ProviderType;

/**
 * An HTTP Client.
 *
 * <p> An {@code HttpClient} can be used to send {@linkplain HttpRequest
 * requests} and retrieve their {@linkplain HttpResponse responses}. An {@code
 * HttpClient} is created through a {@link HttpClient#newBuilder() builder}. The
 * builder can be used to configure per-client state, like: the preferred
 * protocol version ( HTTP/1.1 or HTTP/2 ), whether to follow redirects, a
 * proxy, an authenticator, etc. Once built, an {@code HttpClient} is immutable,
 * and can be used to send multiple requests.
 *
 * <p> An {@code HttpClient} provides configuration information, and resource
 * sharing, for all requests sent through it.
 *
 * <p> A {@link BodyHandler BodyHandler} must be supplied for each {@link
 * HttpRequest} sent. The {@code BodyHandler} determines how to handle the
 * response body, if any. Once an {@link HttpResponse} is received, the
 * headers, response code, and body (typically) are available. Whether the
 * response body bytes have been read or not depends on the type, {@code T}, of
 * the response body.
 *
 * <p> Requests can be sent either synchronously or asynchronously:
 * <ul>
 *     <li>{@link HttpClient#send(HttpRequest, BodyHandler)} blocks
 *     until the request has been sent and the response has been received.</li>
 *
 *     <li>{@link HttpClient#sendAsync(HttpRequest, BodyHandler)} sends the
 *     request and receives the response asynchronously. The {@code sendAsync}
 *     method returns immediately with a {@link CompletableFuture
 *     CompletableFuture}&lt;{@link HttpResponse}&gt;. The {@code
 *     CompletableFuture} completes when the response becomes available. The
 *     returned {@code CompletableFuture} can be combined in different ways to
 *     declare dependencies among several asynchronous tasks.</li>
 * </ul>
 *
 * <p><b>Synchronous Example</b>
 * <pre>{@code    HttpClient client = HttpClient.newBuilder()
 *        .version(Version.HTTP_1_1)
 *        .followRedirects(Redirect.NORMAL)
 *        .connectTimeout(Duration.ofSeconds(20))
 *        .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
 *        .authenticator(Authenticator.getDefault())
 *        .build();
 *   HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
 *   System.out.println(response.statusCode());
 *   System.out.println(response.body());  }</pre>
 *
 * <p><b>Asynchronous Example</b>
 * <pre>{@code    HttpRequest request = HttpRequest.newBuilder()
 *        .uri(URI.create("https://foo.com/"))
 *        .timeout(Duration.ofMinutes(2))
 *        .header("Content-Type", "application/json")
 *        .POST(BodyPublishers.ofFile(Paths.get("file.json")))
 *        .build();
 *   client.sendAsync(request, BodyHandlers.ofString())
 *        .thenApply(HttpResponse::body)
 *        .thenAccept(System.out::println);  }</pre>
 *
 * <p> <a id="securitychecks"></a><b>Security checks</b></a>
 *
 * <p> If a security manager is present then security checks are performed by
 * the HTTP Client's sending methods. An appropriate {@link URLPermission} is
 * required to access the destination server, and proxy server if one has
 * been configured. The form of the {@code URLPermission} required to access a
 * proxy has a {@code method} parameter of {@code "CONNECT"} (for all kinds of
 * proxying) and a {@code URL} string of the form {@code "socket://host:port"}
 * where host and port specify the proxy's address.
 *
 * @implNote If an explicit {@linkplain HttpClient.Builder#executor(Executor)
 * executor} has not been set for an {@code HttpClient}, and a security manager
 * has been installed, then the default executor will execute asynchronous and
 * dependent tasks in a context that is granted no permissions. Custom
 * {@linkplain HttpRequest.BodyPublisher request body publishers}, {@linkplain
 * HttpResponse.BodyHandler response body handlers}, {@linkplain
 * HttpResponse.BodySubscriber response body subscribers}, and {@linkplain
 * WebSocket.Listener WebSocket Listeners}, if executing operations that require
 * privileges, should do so within an appropriate {@linkplain
 * AccessController#doPrivileged(PrivilegedAction) privileged context}.
 *
 */
@ProviderType
public interface HttpClient {

    /**
     * A builder of {@linkplain HttpClient HTTP Clients}.
     *
     * <p> Builders are created by invoking {@link HttpClient#newBuilder()
     * newBuilder}. Each of the setter methods modifies the state of the builder
     * and returns the same instance. Builders are not thread-safe and should not be
     * used concurrently from multiple threads without external synchronization.
     *
     */
    @ProviderType
    public interface Builder {

        /**
         * Sets the connect timeout duration for this client.
         *
         * <p> In the case where a new connection needs to be established, if
         * the connection cannot be established within the given {@code
         * duration}, then {@link HttpClient#send(HttpRequest,BodyHandler)
         * HttpClient::send} throws an {@link HttpConnectTimeoutException}, or
         * {@link HttpClient#sendAsync(HttpRequest,BodyHandler)
         * HttpClient::sendAsync} completes exceptionally with an
         * {@code HttpConnectTimeoutException}. If a new connection does not
         * need to be established, for example if a connection can be reused
         * from a previous request, then this timeout duration has no effect.
         *
         * @param duration the duration to allow the underlying connection to be
         *                 established
         * @return this builder
         * @throws IllegalArgumentException if the duration is non-positive
         */
        public Builder connectTimeout(Duration duration);

        /**
         * Sets the executor to be used for asynchronous and dependent tasks.
         *
         * <p> If this method is not invoked prior to {@linkplain #build()
         * building}, a default executor is created for each newly built {@code
         * HttpClient}.
         *
         * @implNote The default executor uses a thread pool, with a custom
         * thread factory. If a security manager has been installed, the thread
         * factory creates threads that run with an access control context that
         * has no permissions.
         *
         * @param executor the Executor
         * @return this builder
         */
        public Builder executor(Executor executor);

        /**
         * Sets the default priority for any HTTP/2 requests sent from this
         * client. The value provided must be between {@code 1} and {@code 256}
         * (inclusive).
         *
         * @param priority the priority weighting
         * @return this builder
         * @throws IllegalArgumentException if the given priority is out of range
         */
        public Builder priority(int priority);

        Builder proxy(String address, Integer port);

        Builder setSingleProxySocket();

        Builder initDelay(Duration delay);

        Builder secureProtocols(List<String> protocols);

        Builder secureCipher(List<String> ciphers);

        Builder secureKeystoreFile(String path);

        Builder secureKeystorePwd(String pwd);

        Builder secureKeystoreType(String type);

	Builder secureKeystoreAlgo(String algo);

	    Builder secureEndpointIdentificationAlgo(String algo);

        Builder secureProxyProtocols(List<String> protocols);

        Builder secureProxyCipher(List<String> ciphers);

        Builder secureProxyKeystoreFile(String path);

        Builder secureProxyKeystorePwd(String pwd);

        Builder secureProxyKeystoreType(String type);

	Builder secureProxyKeystoreAlgo(String algo);

        Builder secureProxyEndpointIdentificationAlgo(String algo);

        Builder setNoDelay();

        Builder clearNoDelay();

        Builder onAvailableTimeout(Duration duration);

        Builder retryConnection(int maxAttempts);

        /**
         * Instruct this client for each connection to export keying material (RFC 5705).
         * Keying material is made available through {@link HttpResponse#exportKeyingMaterial}
         * @param label
         * @param context : new byte[0] for the empty string
         * @param length
         * @return
         */
        Builder exportKeyingMaterial(String label, byte [] context, int length);

        /**
         * Sets the implementation dependent properties that are subject to change
         * and therefore are not implemented using proper methods.
         * @param key
         * @param value
         * @return this builder
         */
        <T> Builder setProperty(String key, T value);

        /**
         * Returns a new {@link HttpClient} built from the current state of this
         * builder.
         *
         * @return a new {@code HttpClient}
         */
        public HttpClient build();
    }


    /**
     * Returns an {@code Optional} containing the <i>connect timeout duration</i>
     * for this client. If the {@linkplain Builder#connectTimeout(Duration)
     * connect timeout duration} was not set in the client's builder, then the
     * {@code Optional} is empty.
     *
     * @return an {@code Optional} containing this client's connect timeout
     *         duration
     */
     public Optional<Duration> connectTimeout();

    /**
     * Returns an {@code Optional} containing the {@code ProxySelector}
     * supplied to this client. If no proxy selector was set in this client's
     * builder, then the {@code Optional} is empty.
     *
     * <p> Even though this method may return an empty optional, the {@code
     * HttpClient} may still have a non-exposed {@linkplain
     * Builder#proxy(ProxySelector) default proxy selector} that is
     * used for sending HTTP requests.
     *
     * @return an {@code Optional} containing the proxy selector supplied
     *        to this client.
     */
//    public Optional<ProxySelector> proxy();

    /**
     * Returns an {@code Optional} containing this client's {@link
     * Executor}. If no {@code Executor} was set in the client's builder,
     * then the {@code Optional} is empty.
     *
     * <p> Even though this method may return an empty optional, the {@code
     * HttpClient} may still have an non-exposed {@linkplain
     * HttpClient.Builder#executor(Executor) default executor} that is used for
     * executing asynchronous and dependent tasks.
     *
     * @return an {@code Optional} containing this client's {@code Executor}
     */
    public Optional<Executor> executor();

    /**
     * The HTTP protocol version.
     *
     */
    public enum Version {

        /**
         * HTTP version 1.1
         */
        HTTP_1_1,

        /**
         * HTTP version 2
         */
        HTTP_2
    }

    /**
     * Sends the given request using this client, blocking if necessary to get
     * the response. The returned {@link HttpResponse}{@code <T>} contains the
     * response status, headers, and body ( as handled by given response body
     * handler ).
     *
     * @param <T> the response body type
     * @param request the request
     * @param responseBodyHandler the response body handler
     * @return the response
     * @throws IOException if an I/O error occurs when sending or receiving
     * @throws InterruptedException if the operation is interrupted
     * @throws IllegalArgumentException if the {@code request} argument is not
     *         a request that could have been validly built as specified by {@link
     *         HttpRequest.Builder HttpRequest.Builder}.
     * @throws SecurityException If a security manager has been installed
     *          and it denies {@link java.net.URLPermission access} to the
     *          URL in the given request, or proxy if one is configured.
     *          See <a href="#securitychecks">security checks</a> for further
     *          information.
     */
    public <T> HttpResponse<T>
    send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler)
        throws IOException, InterruptedException;

    /**
     * Sends the given request asynchronously using this client with the given
     * response body handler.
     *
     * <p> Equivalent to: {@code sendAsync(request, responseBodyHandler, null)}.
     *
     * @param <T> the response body type
     * @param request the request
     * @param responseBodyHandler the response body handler
     * @return a {@code CompletableFuture<HttpResponse<T>>}
     * @throws IllegalArgumentException if the {@code request} argument is not
     *         a request that could have been validly built as specified by {@link
     *         HttpRequest.Builder HttpRequest.Builder}.
     */
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest request,
              BodyHandler<T> responseBodyHandler);

    /**
     * Sends the given request asynchronously using this client with the given
     * response body handler and push promise handler.
     *
     * <p> The returned completable future, if completed successfully, completes
     * with an {@link HttpResponse}{@code <T>} that contains the response status,
     * headers, and body ( as handled by given response body handler ).
     *
     * <p> {@linkplain PushPromiseHandler Push promises} received, if any, are
     * handled by the given {@code pushPromiseHandler}. A {@code null} valued
     * {@code pushPromiseHandler} rejects any push promises.
     *
     * <p> The returned completable future completes exceptionally with:
     * <ul>
     * <li>{@link IOException} - if an I/O error occurs when sending or receiving</li>
     * <li>{@link SecurityException} - If a security manager has been installed
     *          and it denies {@link java.net.URLPermission access} to the
     *          URL in the given request, or proxy if one is configured.
     *          See <a href="#securitychecks">security checks</a> for further
     *          information.</li>
     * </ul>
     *
     * @param <T> the response body type
     * @param request the request
     * @param responseBodyHandler the response body handler
     * @param pushPromiseHandler push promise handler, may be null
     * @return a {@code CompletableFuture<HttpResponse<T>>}
     * @throws IllegalArgumentException if the {@code request} argument is not
     *         a request that could have been validly built as specified by {@link
     *         HttpRequest.Builder HttpRequest.Builder}.
     */
    public <T> CompletableFuture<HttpResponse<T>>
    sendAsync(HttpRequest request,
              BodyHandler<T> responseBodyHandler,
              PushPromiseHandler<T> pushPromiseHandler);

    /**
     * Close the client.
     * Release all resources help by the client (connections, etc...).
     */
    void close();

    /**
     * Close all connections.
     * gracefully if graceful is set to true
     * forcibly otherwise.
     *
     * When used gracefully it will allow new request to start on fresh connections. This can help refreshing TLS credentials.
     * @param graceful
     */
    void close(boolean graceful);
}
