// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl;

import java.net.InetSocketAddress;
import java.net.URI;
import com.alcatel.as.http2.client.api.HttpHeaders;
import com.alcatel.as.http2.client.api.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import com.alcatel.as.http2.client.api.HttpClient.Version;
import static com.alcatel.as.http2.client.api.impl.common.Utils.ALLOWED_HEADERS;
import static com.alcatel.as.http2.client.api.impl.common.Utils.ACCEPT_ALL;

final class ImmutableHttpRequest implements HttpRequest {

    private final String method;
    private final URI uri;
    private final HttpHeaders headers;
    private final Optional<BodyPublisher> requestPublisher;
    private final boolean expectContinue;
    private final Optional<Duration> timeout;
    private final   Optional<Version>           version;
    protected final Optional<InetSocketAddress> destination;

    /** Creates an ImmutableHttpRequest from the given builder. */
    ImmutableHttpRequest(HttpRequestBuilderImpl builder) {
        this.method = Objects.requireNonNull(builder.method());
        this.uri = Objects.requireNonNull(builder.uri());
	if (builder.isDirect)
          this.headers = HttpHeaders.of(builder.headersBuilder().map(), ACCEPT_ALL);
	else
        this.headers = HttpHeaders.of(builder.headersBuilder().map(), ALLOWED_HEADERS);
        this.requestPublisher = Optional.ofNullable(builder.bodyPublisher());
        this.expectContinue = builder.expectContinue();
        this.timeout = Optional.ofNullable(builder.timeout());
        this.version = Objects.requireNonNull(builder.version());
        this.destination = builder.destination;
    }

    @Override
    public String method() { return method; }

    @Override
    public URI uri() { return uri; }

    @Override
    public HttpHeaders headers() {
        return headers;
    }

    @Override
    public Optional<BodyPublisher> bodyPublisher() { return requestPublisher; }

    @Override
    public boolean expectContinue() { return expectContinue; }

    @Override
    public Optional<Duration> timeout() { return timeout; }

    @Override
    public Optional<Version> version() { return version; }

    @Override
    public String toString() {
        return uri.toString() + " " + method;
    }
}
