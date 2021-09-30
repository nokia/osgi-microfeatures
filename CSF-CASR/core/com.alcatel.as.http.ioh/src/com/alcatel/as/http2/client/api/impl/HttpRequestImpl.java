package com.alcatel.as.http2.client.api.impl;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpHeaders;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.impl.common.HttpHeadersBuilder;

import static com.alcatel.as.http2.client.api.impl.common.Utils.ALLOWED_HEADERS;

public class HttpRequestImpl implements HttpRequest {//implements WebSocketRequest {

    private final HttpHeaders userHeaders;
    private final URI uri;
    private final InetSocketAddress authority; // only used when URI not specified
    private final String method;
    final BodyPublisher requestPublisher;
    final boolean secure;
    final boolean expectContinue;
    private final Duration timeout;  // may be null
    private final Optional<HttpClient.Version> version;

    /**
     * Creates an HttpRequestImpl from the given builder.
     */
    public HttpRequestImpl(HttpRequestBuilderImpl builder) {
        String method = builder.method();
        this.method = method == null ? "GET" : method;
        this.userHeaders = HttpHeaders.of(builder.headersBuilder().map(), ALLOWED_HEADERS);
        this.uri = builder.uri();
        assert uri != null;
        this.expectContinue = builder.expectContinue();
        this.secure = uri.getScheme().toLowerCase(Locale.US).equals("https");
        this.requestPublisher = builder.bodyPublisher();  // may be null
        this.timeout = builder.timeout();
        this.version = builder.version();
        this.authority = null;
    }

    @Override
    public String toString() {
        return (uri == null ? "" : uri.toString()) + " " + method;
    }

    @Override
    public HttpHeaders headers() {
        return userHeaders;
    }

    @Override
    public boolean expectContinue() { return expectContinue; }

    @Override
    public Optional<BodyPublisher> bodyPublisher() {
        return requestPublisher == null ? Optional.empty()
                                        : Optional.of(requestPublisher);
    }

    /**
     * Returns the request method for this request. If not set explicitly,
     * the default method for any request is "GET".
     */
    @Override
    public String method() { return method; }

    @Override
    public URI uri() { return uri; }

    @Override
    public Optional<Duration> timeout() {
        return timeout == null ? Optional.empty() : Optional.of(timeout);
    }

    @Override
    public Optional<HttpClient.Version> version() { return version; }
}
