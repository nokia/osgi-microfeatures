package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpRequest;
import com.alcatel.as.http2.client.api.HttpRequest.BodyPublisher;
import com.alcatel.as.http2.client.api.impl.common.HttpHeadersBuilder;
import com.alcatel.as.http2.client.api.impl.common.Utils;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

import static com.alcatel.as.http2.client.api.impl.common.Utils.*;
import static java.util.Objects.requireNonNull;

public class HttpRequestBuilderImpl implements HttpRequest.Builder {

    private HttpHeadersBuilder headersBuilder;
    private URI uri;
    private String method;
    private boolean expectContinue;
    private BodyPublisher bodyPublisher;
    private volatile Optional<HttpClient.Version> version;
    private Duration duration;
    protected Optional<InetSocketAddress> destination=Optional.empty();
    final boolean isDirect;

    public HttpRequestBuilderImpl(URI uri) {
        requireNonNull(uri, "uri must be non-null");
        checkURI(uri);
        this.uri = uri;
        this.headersBuilder = new HttpHeadersBuilder();
        this.method = "GET"; // default, as per spec
        this.version = Optional.empty();
        this.isDirect = false;
        this.destination= Optional.empty();
    }

    public HttpRequestBuilderImpl(boolean direct) {
        assert direct : "do not use this API, internal use only!";
        this.isDirect = direct;
        this.headersBuilder = new HttpHeadersBuilder();
        this.version = Optional.empty();
    }

    public HttpRequestBuilderImpl() {
        this.headersBuilder = new HttpHeadersBuilder();
        this.method = "GET"; // default, as per spec
        this.version = Optional.empty();
        this.isDirect = false;
    }

    @Override
    public HttpRequestBuilderImpl uri(URI uri) {
        requireNonNull(uri, "uri must be non-null");
        checkURI(uri);
        this.uri = uri;
        return this;
    }

    static void checkURI(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null)
            throw newIAE("URI with undefined scheme");
        scheme = scheme.toLowerCase(Locale.US);
        if (!(scheme.equals("https") || scheme.equals("http"))) {
            throw newIAE("invalid URI scheme %s", scheme);
        }
        if (uri.getHost() == null) {
            throw newIAE("unsupported URI %s", uri);
        }
    }

    @Override
    public HttpRequestBuilderImpl copy() {
        HttpRequestBuilderImpl b = new HttpRequestBuilderImpl();
        b.uri = this.uri;
        b.headersBuilder = this.headersBuilder.structuralCopy();
        b.method = this.method;
        b.expectContinue = this.expectContinue;
        b.bodyPublisher = bodyPublisher;
        b.uri = uri;
        b.duration = duration;
        b.version = version;
        return b;
    }

    private void checkNameAndValue(String name, String value) {
        requireNonNull(name, "name");
        requireNonNull(value, "value");
        if (isDirect) return;
        if (!isValidName(name)) {
            throw newIAE("invalid header name: \"%s\"", name);
        }
        if (!Utils.ALLOWED_HEADERS.test(name, null)) {
            throw newIAE("restricted header name: \"%s\"", name);
        }
        if (!isValidValue(value)) {
            throw newIAE("invalid header value: \"%s\"", value);
        }
    }

    @Override
    public HttpRequestBuilderImpl setHeader(String name, String value) {
        requireNonNull(name, "name");
        name=name.toLowerCase();
        checkNameAndValue(name, value);
        headersBuilder.setHeader(name, value);
        return this;
    }

    @Override
    public HttpRequestBuilderImpl header(String name, String value) {
        requireNonNull(name, "name");
        name=name.toLowerCase();
        checkNameAndValue(name, value);
        headersBuilder.addHeader(name, value);
        return this;
    }

    @Override
    public HttpRequestBuilderImpl headers(String... params) {
        requireNonNull(params);
        if (params.length == 0 || params.length % 2 != 0) {
            throw newIAE("wrong number, %d, of parameters", params.length);
        }
        for (int i = 0; i < params.length; i += 2) {
            String name  = params[i];
            String value = params[i + 1];
            header(name, value);
        }
        return this;
    }

    @Override
    public HttpRequestBuilderImpl expectContinue(boolean enable) {
        expectContinue = enable;
        return this;
    }

    @Override
    public HttpRequestBuilderImpl version(HttpClient.Version version) {
        requireNonNull(version);
        this.version = Optional.of(version);
        return this;
    }

    HttpHeadersBuilder headersBuilder() {  return headersBuilder; }

    URI uri() { return uri; }

    String method() { return method; }

    boolean expectContinue() { return expectContinue; }

    BodyPublisher bodyPublisher() { return bodyPublisher; }

    Optional<HttpClient.Version> version() { return version; }

    @Override
    public HttpRequest.Builder GET() {
        return method0("GET", null);
    }

    @Override
    public HttpRequest.Builder POST(BodyPublisher body) {
        return method0("POST", requireNonNull(body));
    }

    @Override
    public HttpRequest.Builder DELETE() {
        return method0("DELETE", null);
    }

    @Override
    public HttpRequest.Builder PUT(BodyPublisher body) {
        return method0("PUT", requireNonNull(body));
    }

    @Override
    public HttpRequest.Builder method(String method, BodyPublisher body) {
        requireNonNull(method);
        if (method.equals(""))
            throw newIAE("illegal method <empty string>");
        if (method.equals("CONNECT"))
            throw newIAE("method CONNECT is not supported");
        if (!Utils.isValidName(method))
            throw newIAE("illegal method \""
                    + method.replace("\n","\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t")
                    + "\"");
        return method0(method, requireNonNull(body));
    }

    private HttpRequest.Builder method0(String method, BodyPublisher body) {
        assert method != null;
        assert !method.equals("");
        this.method = method;
        this.bodyPublisher = body;
        return this;
    }

    @Override
    public HttpRequest.Builder destination(InetSocketAddress destination) {
        requireNonNull(destination);
        this.destination=Optional.of(destination);
        return this;
    }

    @Override
    public HttpRequest build() {
        if (uri == null)
            throw new IllegalStateException("uri is null");
        assert method != null;
        String scheme = uri.getScheme().toLowerCase();
        if (scheme == null) {
            throw new IllegalArgumentException("URI scheme is null");
        }
        if (destination.isPresent()) {
            int port = destination.get().getPort();
            if (port == 0) {
                port = uri.getPort();
            }
            if (port == -1) {
                if (scheme.equals("http")) {
                    port = 80;
                } else if (scheme.equals("https")) {
                    port = 443;
                } else {
                    throw new IllegalArgumentException("unknown URI scheme " + scheme);
                }
            }
            if (destination.get().isUnresolved() || destination.get().getPort() == 0)
            destination=Optional.of(createResolved(uri.getHost(),destination.get().getHostString(),port).orElse(
                                    InetSocketAddress.createUnresolved(destination.get().getHostString(),port) ));
        }
        return new ImmutableHttpRequest(this);
    }

    static final private Field ia_holder;
    static final private Field ia_hostname;
    static {
        Field tmp_holder = null;
        Field tmp_hostname = null;
        try {
            tmp_holder = InetAddress.class.getDeclaredField("holder");
            tmp_holder.setAccessible(true);

            Object holder_value = tmp_holder.get(InetAddress.getLocalHost());

            tmp_hostname = holder_value.getClass().getDeclaredField("hostName");
            tmp_hostname.setAccessible(true);

        } catch (NoSuchFieldException | UnknownHostException | IllegalAccessException | NullPointerException e) {
        }
        ia_holder = tmp_holder;
        ia_hostname = tmp_hostname;

    }

    static public Optional<InetSocketAddress> createResolved(String host, String ip, int port) {
        try {
            InetAddress ia = InetAddress.getByName(ip);
            Object ia_holder_value = ia_holder.get(ia);

            ia_hostname.set(ia_holder_value,host);

            InetSocketAddress isa = new InetSocketAddress(ia, port);

            return Optional.of(isa);

        } catch (UnknownHostException | IllegalAccessException | NullPointerException e) {
        }
        return Optional.empty();
    }


    @Override
    public HttpRequest.Builder timeout(Duration duration) {
        requireNonNull(duration);
        if (duration.isNegative() || Duration.ZERO.equals(duration))
            throw new IllegalArgumentException("Invalid duration: " + duration);
        this.duration = duration;
        return this;
    }

    Duration timeout() { return duration; }

}
