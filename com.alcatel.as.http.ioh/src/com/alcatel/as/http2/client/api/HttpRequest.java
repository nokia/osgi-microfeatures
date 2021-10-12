// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import com.alcatel.as.http2.client.api.Flow;
import java.util.function.Supplier;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.osgi.annotation.versioning.ProviderType;

/**
 * An HTTP request.
 *
 * <p> An {@code HttpRequest} instance is built through an {@code HttpRequest}
 * {@linkplain HttpRequest.Builder builder}. An {@code HttpRequest} builder
 * is obtained from one of the {@link HttpRequest#newBuilder(URI) newBuilder}
 * methods. A request's {@link URI}, headers, and body can be set. Request
 * bodies are provided through a {@link BodyPublisher BodyPublisher} supplied
 * to one of the {@link Builder#POST(BodyPublisher) POST},
 * {@link Builder#PUT(BodyPublisher) PUT} or
 * {@link Builder#method(String,BodyPublisher) method} methods.
 * Once all required parameters have been set in the builder, {@link
 * Builder#build() build} will return the {@code HttpRequest}. Builders can be
 * copied and modified many times in order to build multiple related requests
 * that differ in some parameters.
 *
 * <p> The following is an example of a GET request that prints the response
 * body as a String:
 *
 * <pre>{@code    HttpClient client = HttpClient.newHttpClient();
 *   HttpRequest request = HttpRequest.newBuilder()
 *         .uri(URI.create("http://foo.com/"))
 *         .build();
 *   client.sendAsync(request, BodyHandlers.ofString())
 *         .thenApply(HttpResponse::body)
 *         .thenAccept(System.out::println)
 *         .join(); }</pre>
 *
 * <p>The class {@link BodyPublishers BodyPublishers} provides implementations
 * of many common publishers. Alternatively, a custom {@code BodyPublisher}
 * implementation can be used.
 *
 */
@ProviderType
public interface HttpRequest {

    /**
     * A builder of {@linkplain HttpRequest HTTP requests}.
     *
     * <p> Instances of {@code HttpRequest.Builder} are created by calling {@link
     * HttpRequest#newBuilder(URI)} or {@link HttpRequest#newBuilder()}.
     *
     * <p> The builder can be used to configure per-request state, such as: the
     * request URI, the request method (default is GET unless explicitly set),
     * specific request headers, etc. Each of the setter methods modifies the
     * state of the builder and returns the same instance. The methods are not
     * synchronized and should not be called from multiple threads without
     * external synchronization. The {@link #build() build} method returns a new
     * {@code HttpRequest} each time it is invoked. Once built an {@code
     * HttpRequest} is immutable, and can be sent multiple times.
     *
     * <p> Note, that not all request headers may be set by user code. Some are
     * restricted for security reasons and others such as the headers relating
     * to authentication, redirection and cookie management may be managed by
     * specific APIs rather than through directly user set headers.
     *
     * @since 11
     */
    @ProviderType
    public interface Builder {

        /**
         * Sets this {@code HttpRequest}'s request {@code URI}.
         *
         * @param uri the request URI
         * @return this builder
         * @throws IllegalArgumentException if the {@code URI} scheme is not
         *         supported
         */
        public Builder uri(URI uri);

        /**
         * Requests the server to acknowledge the request before sending the
         * body. This is disabled by default. If enabled, the server is
         * requested to send an error response or a {@code 100 Continue}
         * response before the client sends the request body. This means the
         * request publisher for the request will not be invoked until this
         * interim response is received.
         *
         * @param enable {@code true} if Expect continue to be sent
         * @return this builder
         */
        public Builder expectContinue(boolean enable);

        /**
         * Sets the preferred {@link HttpClient.Version} for this request.
         *
         * <p> The corresponding {@link HttpResponse} should be checked for the
         * version that was actually used. If the version is not set in a
         * request, then the version requested will be that of the sending
         * {@link HttpClient}.
         *
         * @param version the HTTP protocol version requested
         * @return this builder
         */
        public Builder version(HttpClient.Version version);

        /**
         * Adds the given name value pair to the set of headers for this request.
         * The given value is added to the list of values for that name.
         *
         * @implNote An implementation may choose to restrict some header names
         *           or values, as the HTTP Client may determine their value itself.
         *           For example, "Content-Length", which will be determined by
         *           the request Publisher. In such a case, an implementation of
         *           {@code HttpRequest.Builder} may choose to throw an
         *           {@code IllegalArgumentException} if such a header is passed
         *           to the builder.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         * @throws IllegalArgumentException if the header name or value is not
         *         valid, see <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
         *         RFC 7230 section-3.2</a>, or the header name or value is restricted
         *         by the implementation.
         */
        public Builder header(String name, String value);

        /**
         * Adds the given name value pairs to the set of headers for this
         * request. The supplied {@code String} instances must alternate as
         * header names and header values.
         * To add several values to the same name then the same name must
         * be supplied with each new value.
         *
         * @param headers the list of name value pairs
         * @return this builder
         * @throws IllegalArgumentException if there are an odd number of
         *         parameters, or if a header name or value is not valid, see
         *         <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
         *         RFC 7230 section-3.2</a>, or a header name or value is
         *         {@linkplain #header(String, String) restricted} by the
         *         implementation.
         */
        public Builder headers(String... headers);

        /**
         * Sets a timeout for this request. If the response is not received
         * within the specified timeout then an {@link HttpTimeoutException} is
         * thrown from {@link HttpClient#send(com.alcatel.as.http2.client.api.HttpRequest,
         * com.alcatel.as.http2.client.api.HttpResponse.BodyHandler) HttpClient::send} or
         * {@link HttpClient#sendAsync(com.alcatel.as.http2.client.api.HttpRequest,
         * com.alcatel.as.http2.client.api.HttpResponse.BodyHandler) HttpClient::sendAsync}
         * completes exceptionally with an {@code HttpTimeoutException}. The effect
         * of not setting a timeout is the same as setting an infinite Duration, ie.
         * block forever.
         *
         * @param duration the timeout duration
         * @return this builder
         * @throws IllegalArgumentException if the duration is non-positive
         */
        public Builder timeout(Duration duration);

        /**
         * Sets the given name value pair to the set of headers for this
         * request. This overwrites any previously set values for name.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         * @throws IllegalArgumentException if the header name or value is not valid,
         *         see <a href="https://tools.ietf.org/html/rfc7230#section-3.2">
         *         RFC 7230 section-3.2</a>, or the header name or value is
         *         {@linkplain #header(String, String) restricted} by the
         *         implementation.
         */
        public Builder setHeader(String name, String value);

        /**
         * Sets the request method of this builder to GET.
         * This is the default.
         *
         * @return this builder
         */
        public Builder GET();

        /**
         * Sets the request method of this builder to POST and sets its
         * request body publisher to the given value.
         *
         * @param bodyPublisher the body publisher
         *
         * @return this builder
         */
        public Builder POST(BodyPublisher bodyPublisher);

        /**
         * Sets the request method of this builder to PUT and sets its
         * request body publisher to the given value.
         *
         * @param bodyPublisher the body publisher
         *
         * @return this builder
         */
        public Builder PUT(BodyPublisher bodyPublisher);

        /**
         * Sets the request method of this builder to DELETE.
         *
         * @return this builder
         */
        public Builder DELETE();

        /**
         * Sets the request method and request body of this builder to the
         * given values.
         *
         * @apiNote The {@link BodyPublishers#noBody() noBody} request
         * body publisher can be used where no request body is required or
         * appropriate. Whether a method is restricted, or not, is
         * implementation specific. For example, some implementations may choose
         * to restrict the {@code CONNECT} method.
         *
         * @param method the method to use
         * @param bodyPublisher the body publisher
         * @return this builder
         * @throws IllegalArgumentException if the method name is not
         *         valid, see <a href="https://tools.ietf.org/html/rfc7230#section-3.1.1">
         *         RFC 7230 section-3.1.1</a>, or the method is restricted by the
         *         implementation.
         */
        public Builder method(String method, BodyPublisher bodyPublisher);

        /**
         * Sets the destination IP address / Port and shortcuts resolution of host/port part
         * as provided in the URI.
         * Only shortcuts connection establishment, URI is still used as is in HTTP/2.
         *
         * If the port is set to 0 the API will deduct the port from the URI.
         *
         * @param destination use port of 0 if the client has to deduct it from the URI
         * @return this builder
         */
        public Builder destination(InetSocketAddress destination);

        /**
         * Builds and returns an {@link HttpRequest}.
         *
         * @return a new {@code HttpRequest}
         * @throws IllegalStateException if a URI has not been set
         */
        public HttpRequest build();

        /**
         * Returns an exact duplicate copy of this {@code Builder} based on
         * current state. The new builder can then be modified independently of
         * this builder.
         *
         * @return an exact copy of this builder
         */
        public Builder copy();
    }

    /**
     * Returns an {@code Optional} containing the {@link BodyPublisher} set on
     * this request. If no {@code BodyPublisher} was set in the requests's
     * builder, then the {@code Optional} is empty.
     *
     * @return an {@code Optional} containing this request's {@code BodyPublisher}
     */
    public Optional<BodyPublisher> bodyPublisher();

    /**
     * Returns the request method for this request. If not set explicitly,
     * the default method for any request is "GET".
     *
     * @return this request's method
     */
    public String method();

    /**
     * Returns an {@code Optional} containing this request's timeout duration.
     * If the timeout duration was not set in the request's builder, then the
     * {@code Optional} is empty.
     *
     * @return an {@code Optional} containing this request's timeout duration
     */
    public Optional<Duration> timeout();

    /**
     * Returns this request's {@linkplain HttpRequest.Builder#expectContinue(boolean)
     * expect continue} setting.
     *
     * @return this request's expect continue setting
     */
    public boolean expectContinue();

    /**
     * Returns this request's {@code URI}.
     *
     * @return this request's URI
     */
    public URI uri();

    /**
     * Returns an {@code Optional} containing the HTTP protocol version that
     * will be requested for this {@code HttpRequest}. If the version was not
     * set in the request's builder, then the {@code Optional} is empty.
     * In that case, the version requested will be that of the sending
     * {@link HttpClient}. The corresponding {@link HttpResponse} should be
     * queried to determine the version that was actually used.
     *
     * @return HTTP protocol version
     */
    public Optional<HttpClient.Version> version();

    /**
     * The (user-accessible) request headers that this request was (or will be)
     * sent with.
     *
     * @return this request's HttpHeaders
     */
    public HttpHeaders headers();

    /**
     * A {@code BodyPublisher} converts high-level Java objects into a flow of
     * byte buffers suitable for sending as a request body.  The class
     * {@link BodyPublishers BodyPublishers} provides implementations of many
     * common publishers.
     *
     * <p> The {@code BodyPublisher} interface extends {@link Flow.Publisher
     * Flow.Publisher&lt;ByteBuffer&gt;}, which means that a {@code BodyPublisher}
     * acts as a publisher of {@linkplain ByteBuffer byte buffers}.
     *
     * <p> When sending a request that contains a body, the HTTP Client
     * subscribes to the request's {@code BodyPublisher} in order to receive the
     * flow of outgoing request body data. The normal semantics of {@link
     * Flow.Subscriber} and {@link Flow.Publisher} are implemented by the HTTP
     * Client and are expected from {@code BodyPublisher} implementations. Each
     * outgoing request results in one HTTP Client {@code Subscriber}
     * subscribing to the {@code BodyPublisher} in order to provide the sequence
     * of byte buffers containing the request body. Instances of {@code
     * ByteBuffer} published by the publisher must be allocated by the
     * publisher, and must not be accessed after being published to the HTTP
     * Client. These subscriptions complete normally when the request body is
     * fully sent, and can be canceled or terminated early through error. If a
     * request needs to be resent for any reason, then a new subscription is
     * created which is expected to generate the same data as before.
     *
     * <p> A {@code BodyPublisher} that reports a {@linkplain #contentLength()
     * content length} of {@code 0} may not be subscribed to by the HTTP Client,
     * as it has effectively no data to publish.
     *
     * @see BodyPublishers
     * @since 11
     */
    public interface BodyPublisher extends Flow.Publisher<ByteBuffer> {

        /**
         * Returns the content length for this request body. May be zero
         * if no request body being sent, greater than zero for a fixed
         * length content, or less than zero for an unknown content length.
         *
         * <p> This method may be invoked before the publisher is subscribed to.
         * This method may be invoked more than once by the HTTP client
         * implementation, and MUST return the same constant value each time.
         *
         * @return the content length for this request body, if known
         */
        long contentLength();
    }

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
    public interface BodyPublishers {
	/**
	 * Returns a request body publisher whose body is retrieved from the
	 * given {@code Flow.Publisher}. The returned request body publisher
	 * has an unknown content length.
	 *
	 * @apiNote This method can be used as an adapter between {@code
	 * HttpRequest.BodyPublisher} and {@code Flow.Publisher}, where the amount of
	 * request body that the publisher will publish is unknown.
	 *
	 * @param publisher the publisher responsible for publishing the body
	 * @return a HttpRequest.BodyPublisher
	 */
	public HttpRequest.BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher);

	/**
	 * Returns a request body publisher whose body is retrieved from the
	 * given {@code Flow.Publisher}. The returned request body publisher
	 * has the given content length.
	 *
	 * <p> The given {@code contentLength} is a positive number, that
	 * represents the exact amount of bytes the {@code publisher} must
	 * publish.
	 *
	 * @apiNote This method can be used as an adapter between {@code
	 * HttpRequest.BodyPublisher} and {@code Flow.Publisher}, where the amount of
	 * request body that the publisher will publish is known.
	 *
	 * @param publisher the publisher responsible for publishing the body
	 * @param contentLength a positive number representing the exact
	 *                      amount of bytes the publisher will publish
	 * @throws IllegalArgumentException if the content length is
	 *                                  non-positive
	 * @return a HttpRequest.BodyPublisher
	 */
	public HttpRequest.BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher,
						       long contentLength);

	/**
	 * Returns a request body publisher whose body is the given {@code
	 * String}, converted using the {@link StandardCharsets#UTF_8 UTF_8}
	 * character set.
	 *
	 * @param body the String containing the body
	 * @return a HttpRequest.BodyPublisher
	 */
	public HttpRequest.BodyPublisher ofString(String body);

	/**
	 * Returns a request body publisher whose body is the given {@code
	 * String}, converted using the given character set.
	 *
	 * @param s the String containing the body
	 * @param charset the character set to convert the string to bytes
	 * @return a HttpRequest.BodyPublisher
	 */
	public HttpRequest.BodyPublisher ofString(String s, Charset charset);
    
	/**
	 * A request body publisher that reads its data from an {@link
	 * InputStream}. A {@link Supplier} of {@code InputStream} is used in
	 * case the request needs to be repeated, as the content is not buffered.
	 * The {@code Supplier} may return {@code null} on subsequent attempts,
	 * in which case the request fails.
	 *
	 * @param streamSupplier a Supplier of open InputStreams
	 * @return a HttpRequest.BodyPublisher
	 */
	// TODO (spec): specify that the stream will be closed
	public HttpRequest.BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier);

	/**
	 * Returns a request body publisher whose body is the given byte array.
	 *
	 * @param buf the byte array containing the body
	 * @return a HttpRequest.BodyPublisher
	 */
	public HttpRequest.BodyPublisher ofByteArray(byte[] buf);

	/**
	 * Returns a request body publisher whose body is the content of the
	 * given byte array of {@code length} bytes starting from the specified
	 * {@code offset}.
	 *
	 * @param buf the byte array containing the body
	 * @param offset the offset of the first byte
	 * @param length the number of bytes to use
	 * @return a HttpRequest.BodyPublisher
	 * @throws IndexOutOfBoundsException if the sub-range is defined to be
	 *                                   out of bounds
	 */
	public HttpRequest.BodyPublisher ofByteArray(byte[] buf, int offset, int length);

	/**
	 * A request body publisher that takes data from the contents of a File.
	 *
	 * <p> Security manager permission checks are performed in this factory
	 * method, when the {@code HttpRequest.BodyPublisher} is created. Care must be taken
	 * that the {@code HttpRequest.BodyPublisher} is not shared with untrusted code.
	 *
	 * @param path the path to the file containing the body
	 * @return a HttpRequest.BodyPublisher
	 * @throws java.io.FileNotFoundException if the path is not found
	 * @throws SecurityException if a security manager has been installed
	 *          and it denies {@link SecurityManager#checkRead(String)
	 *          read access} to the given file
	 */
	public HttpRequest.BodyPublisher ofFile(Path path) throws FileNotFoundException;

	/**
	 * A request body publisher that takes data from an {@code Iterable}
	 * of byte arrays. An {@link Iterable} is provided which supplies
	 * {@link Iterator} instances. Each attempt to send the request results
	 * in one invocation of the {@code Iterable}.
	 *
	 * @param iter an Iterable of byte arrays
	 * @return a HttpRequest.BodyPublisher
	 */
	public HttpRequest.BodyPublisher ofByteArrays(Iterable<byte[]> iter);

	/**
	 * A request body publisher which sends no request body.
	 *
	 * @return a HttpRequest.BodyPublisher which completes immediately and sends
	 *         no request body.
	 */
	public HttpRequest.BodyPublisher noBody();
    }

}
