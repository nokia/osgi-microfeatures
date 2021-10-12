// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl;

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
import org.osgi.service.component.annotations.*;
import java.util.Objects;
import java.util.Optional;
import java.nio.file.OpenOption;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Consumer;


import static java.nio.file.StandardOpenOption.*;
import static com.alcatel.as.http2.client.api.impl.common.Utils.charsetFrom;

import com.alcatel.as.http2.client.Http2Client;

import com.alcatel.as.http2.client.api.*;
import com.alcatel.as.http2.client.api.HttpRequest.BodyPublisher;
import com.alcatel.as.http2.client.api.HttpResponse.BodyHandler;
import com.alcatel.as.http2.client.api.HttpResponse.BodySubscriber;
import com.alcatel.as.http2.client.api.HttpResponse.PushPromiseHandler;
import com.alcatel.as.http2.client.api.Flow.*;
import com.alcatel.as.http2.client.api.impl.ResponseBodyHandlers.FileDownloadBodyHandler;
import com.alcatel.as.http2.client.api.impl.ResponseBodyHandlers.PushPromisesHandlerWithMap;
import com.alcatel.as.http2.client.api.impl.ResponseBodyHandlers.PathBodyHandler;
import com.alcatel.as.http2.client.api.impl.ResponseSubscribers.PathSubscriber;
import com.alcatel.as.service.concurrent.SerialExecutor;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class HttpClientFactoryImpl implements HttpClientFactory {

    private Http2Client _http2Client;


    private com.alcatel.as.service.concurrent.PlatformExecutors _execs;
    @Reference
    public void setPlatformExecutors (com.alcatel.as.service.concurrent.PlatformExecutors execs){_execs = execs;}
    @Reference
    public void set (Http2Client client){
	_http2Client = client;
    }

    public HttpClient newHttpClient() {
        return newHttpClientBuilder().build();
    }
    
    public HttpClient.Builder newHttpClientBuilder (){
        Objects.requireNonNull(_execs);
        return new HttpClientBuilderImpl(
                _http2Client
                , _execs
        );
    }

    public HttpRequest.Builder newHttpRequestBuilder (){
	return new HttpRequestBuilderImpl();
    }

    public HttpRequest.Builder newHttpRequestBuilder (java.net.URI uri){
	return new HttpRequestBuilderImpl(uri);
    }

    @Override
    public HttpRequest.Builder newHttpRequestBuilder (boolean direct){
	return new HttpRequestBuilderImpl(direct);
    }

    public HttpRequest.BodyPublishers bodyPublishers (){
	return BodyPublishersImpl;
    }

    public HttpResponse.BodyHandlers bodyHandlers (){
	return BodyHandlersImpl;
    }

    public HttpResponse.BodySubscribers bodySubscribers () {
	    return BodySubscribersImpl;
    }
    
    public <T> HttpResponse.PushPromiseHandler<T>
	newPushPromiseHandler(Function<HttpRequest,HttpResponse.BodyHandler<T>> pushPromiseHandler,
			      ConcurrentMap<HttpRequest,CompletableFuture<HttpResponse<T>>> pushPromisesMap){
	return new PushPromisesHandlerWithMap<>(pushPromiseHandler, pushPromisesMap);
    }


    public static HttpRequest.BodyPublishers BodyPublishersImpl = new HttpRequest.BodyPublishers() {

	    /**
	     * Returns a request body publisher whose body is retrieved from the
	     * given {@code Flow.Publisher}. The returned request body publisher
	     * has an unknown content length.
	     *
	     * @apiNote This method can be used as an adapter between {@code
	     * BodyPublisher} and {@code Flow.Publisher}, where the amount of
	     * request body that the publisher will publish is unknown.
	     *
	     * @param publisher the publisher responsible for publishing the body
	     * @return a BodyPublisher
	     */
	    public BodyPublisher
		fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher) {
		return new RequestPublishers.PublisherAdapter(publisher, -1L);
	    }

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
	     * BodyPublisher} and {@code Flow.Publisher}, where the amount of
	     * request body that the publisher will publish is known.
	     *
	     * @param publisher the publisher responsible for publishing the body
	     * @param contentLength a positive number representing the exact
	     *                      amount of bytes the publisher will publish
	     * @throws IllegalArgumentException if the content length is
	     *                                  non-positive
	     * @return a BodyPublisher
	     */
	    public BodyPublisher
		fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher,
			      long contentLength) {
		if (contentLength < 1)
		    throw new IllegalArgumentException("non-positive contentLength: "
						       + contentLength);
		return new RequestPublishers.PublisherAdapter(publisher, contentLength);
	    }

	    /**
	     * Returns a request body publisher whose body is the given {@code
	     * String}, converted using the {@link StandardCharsets#UTF_8 UTF_8}
	     * character set.
	     *
	     * @param body the String containing the body
	     * @return a BodyPublisher
	     */
	    public BodyPublisher ofString(String body) {
		return ofString(body, UTF_8);
	    }

	    /**
	     * Returns a request body publisher whose body is the given {@code
	     * String}, converted using the given character set.
	     *
	     * @param s the String containing the body
	     * @param charset the character set to convert the string to bytes
	     * @return a BodyPublisher
	     */
	    public BodyPublisher ofString(String s, Charset charset) {
		return new RequestPublishers.StringPublisher(s, charset);
	    }

	    /**
	     * A request body publisher that reads its data from an {@link
	     * InputStream}. A {@link Supplier} of {@code InputStream} is used in
	     * case the request needs to be repeated, as the content is not buffered.
	     * The {@code Supplier} may return {@code null} on subsequent attempts,
	     * in which case the request fails.
	     *
	     * @param streamSupplier a Supplier of open InputStreams
	     * @return a BodyPublisher
	     */
	    // TODO (spec): specify that the stream will be closed
	    public BodyPublisher ofInputStream(Supplier<? extends InputStream> streamSupplier) {
		return new RequestPublishers.InputStreamPublisher(streamSupplier);
	    }

	    /**
	     * Returns a request body publisher whose body is the given byte array.
	     *
	     * @param buf the byte array containing the body
	     * @return a BodyPublisher
	     */
	    public BodyPublisher ofByteArray(byte[] buf) {
		return new RequestPublishers.ByteArrayPublisher(buf);
	    }

	    /**
	     * Returns a request body publisher whose body is the content of the
	     * given byte array of {@code length} bytes starting from the specified
	     * {@code offset}.
	     *
	     * @param buf the byte array containing the body
	     * @param offset the offset of the first byte
	     * @param length the number of bytes to use
	     * @return a BodyPublisher
	     * @throws IndexOutOfBoundsException if the sub-range is defined to be
	     *                                   out of bounds
	     */
	    public BodyPublisher ofByteArray(byte[] buf, int offset, int length) {
		//            Objects.checkFromIndexSize(offset, length, buf.length);
		return new RequestPublishers.ByteArrayPublisher(buf, offset, length);
	    }

	    /**
	     * A request body publisher that takes data from the contents of a File.
	     *
	     * <p> Security manager permission checks are performed in this factory
	     * method, when the {@code BodyPublisher} is created. Care must be taken
	     * that the {@code BodyPublisher} is not shared with untrusted code.
	     *
	     * @param path the path to the file containing the body
	     * @return a BodyPublisher
	     * @throws java.io.FileNotFoundException if the path is not found
	     * @throws SecurityException if a security manager has been installed
	     *          and it denies {@link SecurityManager#checkRead(String)
	     *          read access} to the given file
	     */
	    public BodyPublisher ofFile(Path path) throws FileNotFoundException {
		Objects.requireNonNull(path);
		return RequestPublishers.FilePublisher.create(path);
	    }

	    /**
	     * A request body publisher that takes data from an {@code Iterable}
	     * of byte arrays. An {@link Iterable} is provided which supplies
	     * {@link Iterator} instances. Each attempt to send the request results
	     * in one invocation of the {@code Iterable}.
	     *
	     * @param iter an Iterable of byte arrays
	     * @return a BodyPublisher
	     */
	    public BodyPublisher ofByteArrays(Iterable<byte[]> iter) {
		return new RequestPublishers.IterablePublisher(iter);
	    }

	    /**
	     * A request body publisher which sends no request body.
	     *
	     * @return a BodyPublisher which completes immediately and sends
	     *         no request body.
	     */
	    public BodyPublisher noBody() {
		return new RequestPublishers.EmptyPublisher();
	    }
	};

    public static HttpResponse.BodyHandlers BodyHandlersImpl = new HttpResponse.BodyHandlers() {
	    /**
	     * Returns a response body handler that returns a {@link BodySubscriber
	     * BodySubscriber}{@code <Void>} obtained from {@link
	     * BodySubscribers#fromSubscriber(Subscriber)}, with the given
	     * {@code subscriber}.
	     *
	     * <p> The response body is not available through this, or the {@code
	     * HttpResponse} API, but instead all response body is forwarded to the
	     * given {@code subscriber}, which should make it available, if
	     * appropriate, through some other mechanism, e.g. an entry in a
	     * database, etc.
	     *
	     * @apiNote This method can be used as an adapter between {@code
	     * BodySubscriber} and {@code Flow.Subscriber}.
	     *
	     * <p> For example:
	     * <pre> {@code  TextSubscriber subscriber = new TextSubscriber();
	     *  HttpResponse<Void> response = client.sendAsync(request,
	     *      BodyHandlers.fromSubscriber(subscriber)).join();
	     *  System.out.println(response.statusCode()); }</pre>
	     *
	     * @param subscriber the subscriber
	     * @return a response body handler
	     */
	    public BodyHandler<Void>
		fromSubscriber(Subscriber<? super List<ByteBuffer>> subscriber) {
		Objects.requireNonNull(subscriber);
		return (responseInfo) -> BodySubscribersImpl.fromSubscriber(subscriber,
							s -> null);
	    }

	    /**
	     * Returns a response body handler that returns a {@link BodySubscriber
	     * BodySubscriber}{@code <T>} obtained from {@link
	     * BodySubscribers#fromSubscriber(Subscriber, Function)}, with the
	     * given {@code subscriber} and {@code finisher} function.
	     *
	     * <p> The given {@code finisher} function is applied after the given
	     * subscriber's {@code onComplete} has been invoked. The {@code finisher}
	     * function is invoked with the given subscriber, and returns a value
	     * that is set as the response's body.
	     *
	     * @apiNote This method can be used as an adapter between {@code
	     * BodySubscriber} and {@code Flow.Subscriber}.
	     *
	     * <p> For example:
	     * <pre> {@code  TextSubscriber subscriber = ...;  // accumulates bytes and transforms them into a String
	     *  HttpResponse<String> response = client.sendAsync(request,
	     *      BodyHandlers.fromSubscriber(subscriber, TextSubscriber::getTextResult)).join();
	     *  String text = response.body(); }</pre>
	     *
	     * @param <S> the type of the Subscriber
	     * @param <T> the type of the response body
	     * @param subscriber the subscriber
	     * @param finisher a function to be applied after the subscriber has completed
	     * @return a response body handler
	     */
	    public <S extends Subscriber<? super List<ByteBuffer>>,T> BodyHandler<T>
		fromSubscriber(S subscriber, Function<? super S,? extends T> finisher) {
		Objects.requireNonNull(subscriber);
		Objects.requireNonNull(finisher);
		return (responseInfo) -> BodySubscribersImpl.fromSubscriber(subscriber,
									    finisher);
	    }

	    /**
	     * Returns a response body handler that returns a {@link BodySubscriber
	     * BodySubscriber}{@code <Void>} obtained from {@link
	     * BodySubscribers#fromLineSubscriber(Subscriber, Function, Charset, String)
	     * BodySubscribers.fromLineSubscriber(subscriber, s -> null, charset, null)},
	     * with the given {@code subscriber}.
	     * The {@link Charset charset} used to decode the response body bytes is
	     * obtained from the HTTP response headers as specified by {@link #ofString()},
	     * and lines are delimited in the manner of {@link BufferedReader#readLine()}.
	     *
	     * <p> The response body is not available through this, or the {@code
	     * HttpResponse} API, but instead all response body is forwarded to the
	     * given {@code subscriber}, which should make it available, if
	     * appropriate, through some other mechanism, e.g. an entry in a
	     * database, etc.
	     *
	     * @apiNote This method can be used as an adapter between a {@code
	     * BodySubscriber} and a text based {@code Flow.Subscriber} that parses
	     * text line by line.
	     *
	     * <p> For example:
	     * <pre> {@code  // A PrintSubscriber that implements Flow.Subscriber<String>
	     *  // and print lines received by onNext() on System.out
	     *  PrintSubscriber subscriber = new PrintSubscriber(System.out);
	     *  client.sendAsync(request, BodyHandlers.fromLineSubscriber(subscriber))
	     *      .thenApply(HttpResponse::statusCode)
	     *      .thenAccept((status) -> {
	     *          if (status != 200) {
	     *              System.err.printf("ERROR: %d status received%n", status);
	     *          }
	     *      }); }</pre>
	     *
	     * @param subscriber the subscriber
	     * @return a response body handler
	     */
	    public BodyHandler<Void>
		fromLineSubscriber(Subscriber<? super String> subscriber) {
		Objects.requireNonNull(subscriber);
		return (responseInfo) ->
		    BodySubscribersImpl.fromLineSubscriber(subscriber,
							   s -> null,
							   charsetFrom(responseInfo.headers()),
							   null);
	    }

	    /**
	     * Returns a response body handler that returns a {@link BodySubscriber
	     * BodySubscriber}{@code <T>} obtained from {@link
	     * BodySubscribers#fromLineSubscriber(Subscriber, Function, Charset, String)
	     * BodySubscribers.fromLineSubscriber(subscriber, finisher, charset, lineSeparator)},
	     * with the given {@code subscriber}, {@code finisher} function, and line separator.
	     * The {@link Charset charset} used to decode the response body bytes is
	     * obtained from the HTTP response headers as specified by {@link #ofString()}.
	     *
	     * <p> The given {@code finisher} function is applied after the given
	     * subscriber's {@code onComplete} has been invoked. The {@code finisher}
	     * function is invoked with the given subscriber, and returns a value
	     * that is set as the response's body.
	     *
	     * @apiNote This method can be used as an adapter between a {@code
	     * BodySubscriber} and a text based {@code Flow.Subscriber} that parses
	     * text line by line.
	     *
	     * <p> For example:
	     * <pre> {@code  // A LineParserSubscriber that implements Flow.Subscriber<String>
	     *  // and accumulates lines that match a particular pattern
	     *  Pattern pattern = ...;
	     *  LineParserSubscriber subscriber = new LineParserSubscriber(pattern);
	     *  HttpResponse<List<String>> response = client.send(request,
	     *      BodyHandlers.fromLineSubscriber(subscriber, s -> s.getMatchingLines(), "\n"));
	     *  if (response.statusCode() != 200) {
	     *      System.err.printf("ERROR: %d status received%n", response.statusCode());
	     *  } }</pre>
	     *
	     *
	     * @param <S> the type of the Subscriber
	     * @param <T> the type of the response body
	     * @param subscriber the subscriber
	     * @param finisher a function to be applied after the subscriber has completed
	     * @param lineSeparator an optional line separator: can be {@code null},
	     *                      in which case lines will be delimited in the manner of
	     *                      {@link BufferedReader#readLine()}.
	     * @return a response body handler
	     * @throws IllegalArgumentException if the supplied {@code lineSeparator}
	     *         is the empty string
	     */
	    public <S extends Subscriber<? super String>,T> BodyHandler<T>
		fromLineSubscriber(S subscriber,
				   Function<? super S,? extends T> finisher,
				   String lineSeparator) {
		Objects.requireNonNull(subscriber);
		Objects.requireNonNull(finisher);
		// implicit null check
		if (lineSeparator != null && lineSeparator.isEmpty())
		    throw new IllegalArgumentException("empty line separator");
		return (responseInfo) ->
		    BodySubscribersImpl.fromLineSubscriber(subscriber,
							   finisher,
							   charsetFrom(responseInfo.headers()),
							   lineSeparator);
	    }

	    /**
	     * Returns a response body handler that discards the response body.
	     *
	     * @return a response body handler
	     */
	    public BodyHandler<Void> discarding() {
		return (responseInfo) -> BodySubscribersImpl.discarding();
	    }

	    /**
	     * Returns a response body handler that returns the given replacement
	     * value, after discarding the response body.
	     *
	     * @param <U> the response body type
	     * @param value the value of U to return as the body, may be {@code null}
	     * @return a response body handler
	     */
	    public <U> BodyHandler<U> replacing(U value) {
		return (responseInfo) -> BodySubscribersImpl.replacing(value);
	    }

	    /**
	     * Returns a {@code BodyHandler<String>} that returns a
	     * {@link BodySubscriber BodySubscriber}{@code <String>} obtained from
	     * {@link BodySubscribers#ofString(Charset) BodySubscribers.ofString(Charset)}.
	     * The body is decoded using the given character set.
	     *
	     * @param charset the character set to convert the body with
	     * @return a response body handler
	     */
	    public BodyHandler<String> ofString(Charset charset) {
		Objects.requireNonNull(charset);
		return (responseInfo) -> BodySubscribersImpl.ofString(charset);
	    }

	    /**
	     * Returns a {@code BodyHandler<Path>} that returns a
	     * {@link BodySubscriber BodySubscriber}{@code <Path>} obtained from
	     * {@link BodySubscribers#ofFile(Path, OpenOption...)
	     * BodySubscribers.ofFile(Path,OpenOption...)}.
	     *
	     * <p> When the {@code HttpResponse} object is returned, the body has
	     * been completely written to the file, and {@link #body()} returns a
	     * reference to its {@link Path}.
	     *
	     * <p> Security manager permission checks are performed in this factory
	     * method, when the {@code BodyHandler} is created. Care must be taken
	     * that the {@code BodyHandler} is not shared with untrusted code.
	     *
	     * @param file the file to store the body in
	     * @param openOptions any options to use when opening/creating the file
	     * @return a response body handler
	     * @throws IllegalArgumentException if an invalid set of open options
	     *          are specified
	     * @throws SecurityException If a security manager has been installed
	     *          and it denies {@link SecurityManager#checkWrite(String)
	     *          write access} to the file.
	     */
	    public BodyHandler<Path> ofFile(Path file, OpenOption... openOptions) {
		Objects.requireNonNull(file);
		List<OpenOption> opts = java.util.Collections.unmodifiableList(Arrays.asList(openOptions));
		if (opts.contains(DELETE_ON_CLOSE) || opts.contains(READ)) {
		    // these options make no sense, since the FileChannel is not exposed
		    throw new IllegalArgumentException("invalid openOptions: " + opts);
		}
		return PathBodyHandler.create(file, opts);
	    }

	    /**
	     * Returns a {@code BodyHandler<Path>} that returns a
	     * {@link BodySubscriber BodySubscriber}{@code <Path>}.
	     *
	     * <p> Equivalent to: {@code ofFile(file, CREATE, WRITE)}
	     *
	     * <p> Security manager permission checks are performed in this factory
	     * method, when the {@code BodyHandler} is created. Care must be taken
	     * that the {@code BodyHandler} is not shared with untrusted code.
	     *
	     * @param file the file to store the body in
	     * @return a response body handler
	     * @throws SecurityException If a security manager has been installed
	     *          and it denies {@link SecurityManager#checkWrite(String)
	     *          write access} to the file.
	     */
	    public BodyHandler<Path> ofFile(Path file) {
		return ofFile(file, CREATE, WRITE);
	    }

	    /**
	     * Returns a {@code BodyHandler<Path>} that returns a
	     * {@link BodySubscriber BodySubscriber}&lt;{@link Path}&gt;
	     * where the download directory is specified, but the filename is
	     * obtained from the {@code Content-Disposition} response header. The
	     * {@code Content-Disposition} header must specify the <i>attachment</i>
	     * type and must also contain a <i>filename</i> parameter. If the
	     * filename specifies multiple path components only the final component
	     * is used as the filename (with the given directory name).
	     *
	     * <p> When the {@code HttpResponse} object is returned, the body has
	     * been completely written to the file and {@link #body()} returns a
	     * {@code Path} object for the file. The returned {@code Path} is the
	     * combination of the supplied directory name and the file name supplied
	     * by the server. If the destination directory does not exist or cannot
	     * be written to, then the response will fail with an {@link IOException}.
	     *
	     * <p> Security manager permission checks are performed in this factory
	     * method, when the {@code BodyHandler} is created. Care must be taken
	     * that the {@code BodyHandler} is not shared with untrusted code.
	     *
	     * @param directory the directory to store the file in
	     * @param openOptions open options used when opening the file
	     * @return a response body handler
	     * @throws IllegalArgumentException if the given path does not exist,
	     *          is not a directory, is not writable, or if an invalid set
	     *          of open options are specified
	     * @throws SecurityException If a security manager has been installed
	     *          and it denies
	     *          {@linkplain SecurityManager#checkRead(String) read access}
	     *          to the directory, or it denies
	     *          {@linkplain SecurityManager#checkWrite(String) write access}
	     *          to the directory, or it denies
	     *          {@linkplain SecurityManager#checkWrite(String) write access}
	     *          to the files within the directory.
	     */
	    public BodyHandler<Path> ofFileDownload(Path directory,
						    OpenOption... openOptions) {
		Objects.requireNonNull(directory);
		List<OpenOption> opts = java.util.Collections.unmodifiableList(Arrays.asList(openOptions));
		if (opts.contains(DELETE_ON_CLOSE)) {
		    throw new IllegalArgumentException("invalid option: " + DELETE_ON_CLOSE);
		}
		return FileDownloadBodyHandler.create(directory, opts);
	    }

	    /**
	     * Returns a {@code BodyHandler<Void>} that returns a
	     * {@link BodySubscriber BodySubscriber}{@code <Void>} obtained from
	     * {@link BodySubscribers#ofByteArrayConsumer(Consumer)
	     * BodySubscribers.ofByteArrayConsumer(Consumer)}.
	     *
	     * <p> When the {@code HttpResponse} object is returned, the body has
	     * been completely written to the consumer.
	     *
	     * @apiNote
	     * The subscriber returned by this handler is not flow controlled.
	     * Therefore, the supplied consumer must be able to process whatever
	     * amount of data is delivered in a timely fashion.
	     *
	     * @param consumer a Consumer to accept the response body
	     * @return a response body handler
	     */
	    public BodyHandler<Void>
		ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer) {
		Objects.requireNonNull(consumer);
		return (responseInfo) -> BodySubscribersImpl.ofByteArrayConsumer(consumer);
	    }

	    /**
	     * Returns a {@code BodyHandler<byte[]>} that returns a
	     * {@link BodySubscriber BodySubscriber}&lt;{@code byte[]}&gt; obtained
	     * from {@link BodySubscribers#ofByteArray() BodySubscribers.ofByteArray()}.
	     *
	     * <p> When the {@code HttpResponse} object is returned, the body has
	     * been completely written to the byte array.
	     *
	     * @return a response body handler
	     */
	    public BodyHandler<byte[]> ofByteArray() {
		return (responseInfo) -> BodySubscribersImpl.ofByteArray();
	    }

	    /**
	     * Returns a {@code BodyHandler<String>} that returns a
	     * {@link BodySubscriber BodySubscriber}{@code <String>} obtained from
	     * {@link BodySubscribers#ofString(Charset) BodySubscribers.ofString(Charset)}.
	     * The body is decoded using the character set specified in
	     * the {@code Content-Type} response header. If there is no such
	     * header, or the character set is not supported, then
	     * {@link StandardCharsets#UTF_8 UTF_8} is used.
	     *
	     * <p> When the {@code HttpResponse} object is returned, the body has
	     * been completely written to the string.
	     *
	     * @return a response body handler
	     */
	    public BodyHandler<String> ofString() {
		return (responseInfo) -> BodySubscribersImpl.ofString(charsetFrom(responseInfo.headers()));
	    }

	    /**
	     * Returns a {@code BodyHandler<Publisher<List<ByteBuffer>>>} that creates a
	     * {@link BodySubscriber BodySubscriber}{@code <Publisher<List<ByteBuffer>>>}
	     * obtained from {@link BodySubscribers#ofPublisher()
	     * BodySubscribers.ofPublisher()}.
	     *
	     * <p> When the {@code HttpResponse} object is returned, the response
	     * headers will have been completely read, but the body may not have
	     * been fully received yet. The {@link #body()} method returns a
	     * {@link Publisher Publisher<List<ByteBuffer>>} from which the body
	     * response bytes can be obtained as they are received. The publisher
	     * can and must be subscribed to only once.
	     *
	     * @apiNote See {@link BodySubscribers#ofPublisher()} for more
	     * information.
	     *
	     * @return a response body handler
	     */
	    public BodyHandler<Publisher<List<ByteBuffer>>> ofPublisher() {
		return (responseInfo) -> BodySubscribersImpl.ofPublisher();
	    }

	    /**
	     * Returns a {@code BodyHandler} which, when invoked, returns a {@linkplain
	     * BodySubscribers#buffering(BodySubscriber,int) buffering BodySubscriber}
	     * that buffers data before delivering it to the downstream subscriber.
	     * These {@code BodySubscriber} instances are created by calling
	     * {@link BodySubscribers#buffering(BodySubscriber,int)
	     * BodySubscribers.buffering} with a subscriber obtained from the given
	     * downstream handler and the {@code bufferSize} parameter.
	     *
	     * @param <T> the response body type
	     * @param downstreamHandler the downstream handler
	     * @param bufferSize the buffer size parameter passed to {@link
	     *        BodySubscribers#buffering(BodySubscriber,int) BodySubscribers.buffering}
	     * @return a body handler
	     * @throws IllegalArgumentException if {@code bufferSize <= 0}
	     */
	    public <T> BodyHandler<T> buffering(BodyHandler<T> downstreamHandler,
						int bufferSize) {
		Objects.requireNonNull(downstreamHandler);
		if (bufferSize <= 0)
		    throw new IllegalArgumentException("must be greater than 0");
		return (responseInfo) -> BodySubscribersImpl
		    .buffering(downstreamHandler.apply(responseInfo),
			       bufferSize);
	    }
	};
    
    public static HttpResponse.BodySubscribers BodySubscribersImpl = new HttpResponse.BodySubscribers() {
	    /**
	     * Returns a body subscriber that forwards all response body to the
	     * given {@code Flow.Subscriber}. The {@linkplain BodySubscriber#getBody()
	     * completion stage} of the returned body subscriber completes after one
	     * of the given subscribers {@code onComplete} or {@code onError} has
	     * been invoked.
	     *
	     * @apiNote This method can be used as an adapter between {@code
	     * BodySubscriber} and {@code Flow.Subscriber}.
	     *
	     * @param subscriber the subscriber
	     * @return a body subscriber
	     */
	    public BodySubscriber<Void>
		fromSubscriber(Subscriber<? super List<ByteBuffer>> subscriber) {
		return new ResponseSubscribers.SubscriberAdapter<>(subscriber, s -> null);
	    }

	    /**
	     * Returns a body subscriber that forwards all response body to the
	     * given {@code Flow.Subscriber}. The {@linkplain BodySubscriber#getBody()
	     * completion stage} of the returned body subscriber completes after one
	     * of the given subscribers {@code onComplete} or {@code onError} has
	     * been invoked.
	     *
	     * <p> The given {@code finisher} function is applied after the given
	     * subscriber's {@code onComplete} has been invoked. The {@code finisher}
	     * function is invoked with the given subscriber, and returns a value
	     * that is set as the response's body.
	     *
	     * @apiNote This method can be used as an adapter between {@code
	     * BodySubscriber} and {@code Flow.Subscriber}.
	     *
	     * @param <S> the type of the Subscriber
	     * @param <T> the type of the response body
	     * @param subscriber the subscriber
	     * @param finisher a function to be applied after the subscriber has
	     *                 completed
	     * @return a body subscriber
	     */
	    public <S extends Subscriber<? super List<ByteBuffer>>,T> BodySubscriber<T>
		fromSubscriber(S subscriber,
			       Function<? super S,? extends T> finisher) {
		return new ResponseSubscribers.SubscriberAdapter<>(subscriber, finisher);
	    }

	    /**
	     * Returns a body subscriber that forwards all response body to the
	     * given {@code Flow.Subscriber}, line by line.
	     * The {@linkplain BodySubscriber#getBody() completion
	     * stage} of the returned body subscriber completes after one of the
	     * given subscribers {@code onComplete} or {@code onError} has been
	     * invoked.
	     * Bytes are decoded using the {@link StandardCharsets#UTF_8
	     * UTF-8} charset, and lines are delimited in the manner of
	     * {@link BufferedReader#readLine()}.
	     *
	     * @apiNote This method can be used as an adapter between {@code
	     * BodySubscriber} and {@code Flow.Subscriber}.
	     *
	     * @implNote This is equivalent to calling <pre>{@code
	     *      fromLineSubscriber(subscriber, s -> null, StandardCharsets.UTF_8, null)
	     * }</pre>
	     *
	     * @param subscriber the subscriber
	     * @return a body subscriber
	     */
	    public BodySubscriber<Void>
		fromLineSubscriber(Subscriber<? super String> subscriber) {
		return fromLineSubscriber(subscriber,  s -> null,
					  StandardCharsets.UTF_8, null);
	    }

	    /**
	     * Returns a body subscriber that forwards all response body to the
	     * given {@code Flow.Subscriber}, line by line. The {@linkplain
	     * BodySubscriber#getBody() completion stage} of the returned body
	     * subscriber completes after one of the given subscribers
	     * {@code onComplete} or {@code onError} has been invoked.
	     *
	     * <p> The given {@code finisher} function is applied after the given
	     * subscriber's {@code onComplete} has been invoked. The {@code finisher}
	     * function is invoked with the given subscriber, and returns a value
	     * that is set as the response's body.
	     *
	     * @apiNote This method can be used as an adapter between {@code
	     * BodySubscriber} and {@code Flow.Subscriber}.
	     *
	     * @param <S> the type of the Subscriber
	     * @param <T> the type of the response body
	     * @param subscriber the subscriber
	     * @param finisher a function to be applied after the subscriber has
	     *                 completed
	     * @param charset a {@link Charset} to decode the bytes
	     * @param lineSeparator an optional line separator: can be {@code null},
	     *                      in which case lines will be delimited in the manner of
	     *                      {@link BufferedReader#readLine()}.
	     * @return a body subscriber
	     * @throws IllegalArgumentException if the supplied {@code lineSeparator}
	     *         is the empty string
	     */
	    public <S extends Subscriber<? super String>,T> BodySubscriber<T>
		fromLineSubscriber(S subscriber,
				   Function<? super S,? extends T> finisher,
				   Charset charset,
				   String lineSeparator) {
		return LineSubscriberAdapter.create(subscriber,
						    finisher, charset, lineSeparator);
	    }

	    /**
	     * Returns a body subscriber which stores the response body as a {@code
	     * String} converted using the given {@code Charset}.
	     *
	     * <p> The {@link HttpResponse} using this subscriber is available after
	     * the entire response has been read.
	     *
	     * @param charset the character set to convert the String with
	     * @return a body subscriber
	     */
	    public BodySubscriber<String> ofString(Charset charset) {
		Objects.requireNonNull(charset);
		return new ResponseSubscribers.ByteArraySubscriber<>(
								     bytes -> new String(bytes, charset)
								     );
	    }

	    /**
	     * Returns a {@code BodySubscriber} which stores the response body as a
	     * byte array.
	     *
	     * <p> The {@link HttpResponse} using this subscriber is available after
	     * the entire response has been read.
	     *
	     * @return a body subscriber
	     */
	    public BodySubscriber<byte[]> ofByteArray() {
		return new ResponseSubscribers.ByteArraySubscriber<>(
								     Function.identity() // no conversion
								     );
	    }

	    /**
	     * Returns a {@code BodySubscriber} which stores the response body in a
	     * file opened with the given options and name. The file will be opened
	     * with the given options using {@link FileChannel#open(Path,OpenOption...)
	     * FileChannel.open} just before the body is read. Any exception thrown
	     * will be returned or thrown from {@link HttpClient#send(HttpRequest,
	     * BodyHandler) HttpClient::send} or {@link HttpClient#sendAsync(HttpRequest,
	     * BodyHandler) HttpClient::sendAsync} as appropriate.
	     *
	     * <p> The {@link HttpResponse} using this subscriber is available after
	     * the entire response has been read.
	     *
	     * <p> Security manager permission checks are performed in this factory
	     * method, when the {@code BodySubscriber} is created. Care must be taken
	     * that the {@code BodyHandler} is not shared with untrusted code.
	     *
	     * @param file the file to store the body in
	     * @param openOptions the list of options to open the file with
	     * @return a body subscriber
	     * @throws IllegalArgumentException if an invalid set of open options
	     *          are specified
	     * @throws SecurityException if a security manager has been installed
	     *          and it denies {@link SecurityManager#checkWrite(String)
	     *          write access} to the file
	     */
	    public BodySubscriber<Path> ofFile(Path file, OpenOption... openOptions) {
		Objects.requireNonNull(file);
		List<OpenOption> opts = java.util.Collections.unmodifiableList(Arrays.asList(openOptions));
		if (opts.contains(DELETE_ON_CLOSE) || opts.contains(READ)) {
		    // these options make no sense, since the FileChannel is not exposed
		    throw new IllegalArgumentException("invalid openOptions: " + opts);
		}
		return PathSubscriber.create(file, opts);
	    }

	    /**
	     * Returns a {@code BodySubscriber} which stores the response body in a
	     * file opened with the given name.
	     *
	     * <p> Equivalent to: {@code ofFile(file, CREATE, WRITE)}
	     *
	     * <p> Security manager permission checks are performed in this factory
	     * method, when the {@code BodySubscriber} is created. Care must be taken
	     * that the {@code BodyHandler} is not shared with untrusted code.
	     *
	     * @param file the file to store the body in
	     * @return a body subscriber
	     * @throws SecurityException if a security manager has been installed
	     *          and it denies {@link SecurityManager#checkWrite(String)
	     *          write access} to the file
	     */
	    public BodySubscriber<Path> ofFile(Path file) {
		return ofFile(file, CREATE, WRITE);
	    }

	    /**
	     * Returns a {@code BodySubscriber} which provides the incoming body
	     * data to the provided Consumer of {@code Optional<byte[]>}. Each
	     * call to {@link Consumer#accept(java.lang.Object) Consumer.accept()}
	     * will contain a non empty {@code Optional}, except for the final
	     * invocation after all body data has been read, when the {@code
	     * Optional} will be empty.
	     *
	     * <p> The {@link HttpResponse} using this subscriber is available after
	     * the entire response has been read.
	     *
	     * @apiNote
	     * This subscriber is not flow controlled.
	     * Therefore, the supplied consumer must be able to process whatever
	     * amount of data is delivered in a timely fashion.
	     *
	     * @param consumer a Consumer of byte arrays
	     * @return a BodySubscriber
	     */
	    public BodySubscriber<Void>
		ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer) {
		return new ResponseSubscribers.ConsumerSubscriber(consumer);
	    }

	    /**
	     * Returns a response subscriber which publishes the response body
	     * through a {@code Publisher<List<ByteBuffer>>}.
	     *
	     * <p> The {@link HttpResponse} using this subscriber is available
	     * immediately after the response headers have been read, without
	     * requiring to wait for the entire body to be processed. The response
	     * body bytes can then be obtained by subscribing to the publisher
	     * returned by the {@code HttpResponse} {@link HttpResponse#body() body}
	     * method.
	     *
	     * <p>The publisher returned by the {@link HttpResponse#body() body}
	     * method can be subscribed to only once. The first subscriber will
	     * receive the body response bytes if successfully subscribed, or will
	     * cause the subscription to be cancelled otherwise.
	     * If more subscriptions are attempted, the subsequent subscribers will
	     * be immediately subscribed with an empty subscription and their
	     * {@link Subscriber#onError(Throwable) onError} method
	     * will be invoked with an {@code IllegalStateException}.
	     *
	     * @apiNote To ensure that all resources associated with the
	     * corresponding exchange are properly released the caller must
	     * ensure that the provided publisher is subscribed once, and either
	     * {@linkplain Subscription#request(long) requests} all bytes
	     * until {@link Subscriber#onComplete() onComplete} or
	     * {@link Subscriber#onError(Throwable) onError} are invoked, or
	     * cancel the provided {@linkplain Subscriber#onSubscribe(Subscription)
	     * subscription} if it is unable or unwilling to do so.
	     * Note that depending on the actual HTTP protocol {@linkplain
	     * HttpClient.Version version} used for the exchange, cancelling the
	     * subscription instead of exhausting the flow may cause the underlying
	     * HTTP connection to be closed and prevent it from being reused for
	     * subsequent operations.
	     *
	     * @return A {@code BodySubscriber} which publishes the response body
	     *         through a {@code Publisher<List<ByteBuffer>>}.
	     */
	    public BodySubscriber<Publisher<List<ByteBuffer>>> ofPublisher() {
		return ResponseSubscribers.createPublisher();
	    }

	    /**
	     * Returns a response subscriber which discards the response body. The
	     * supplied value is the value that will be returned from
	     * {@link HttpResponse#body()}.
	     *
	     * @param <U> the type of the response body
	     * @param value the value to return from HttpResponse.body(), may be {@code null}
	     * @return a {@code BodySubscriber}
	     */
	    public <U> BodySubscriber<U> replacing(U value) {
		return new ResponseSubscribers.NullSubscriber<>(Optional.ofNullable(value));
	    }

	    /**
	     * Returns a response subscriber which discards the response body.
	     *
	     * @return a response body subscriber
	     */
	    public BodySubscriber<Void> discarding() {
		return new ResponseSubscribers.NullSubscriber<>(Optional.ofNullable(null));
	    }

	    /**
	     * Returns a {@code BodySubscriber} which buffers data before delivering
	     * it to the given downstream subscriber. The subscriber guarantees to
	     * deliver {@code buffersize} bytes of data to each invocation of the
	     * downstream's {@link BodySubscriber#onNext(Object) onNext} method,
	     * except for the final invocation, just before
	     * {@link BodySubscriber#onComplete() onComplete} is invoked. The final
	     * invocation of {@code onNext} may contain fewer than {@code bufferSize}
	     * bytes.
	     *
	     * <p> The returned subscriber delegates its {@link BodySubscriber#getBody()
	     * getBody()} method to the downstream subscriber.
	     *
	     * @param <T> the type of the response body
	     * @param downstream the downstream subscriber
	     * @param bufferSize the buffer size
	     * @return a buffering body subscriber
	     * @throws IllegalArgumentException if {@code bufferSize <= 0}
	     */
	    public <T> BodySubscriber<T> buffering(BodySubscriber<T> downstream,
						   int bufferSize) {
		if (bufferSize <= 0)
		    throw new IllegalArgumentException("must be greater than 0");
		return new BufferingSubscriber<>(downstream, bufferSize);
	    }

	    /**
	     * Returns a {@code BodySubscriber} whose response body value is that of
	     * the result of applying the given function to the body object of the
	     * given {@code upstream} {@code BodySubscriber}.
	     *
	     * <p> The mapping function is executed using the client's {@linkplain
	     * HttpClient#executor() executor}, and can therefore be used to map any
	     * response body type, including blocking {@link InputStream}, as shown
	     * in the following example which uses a well-known JSON parser to
	     * convert an {@code InputStream} into any annotated Java type.
	     *
	     * <p>For example:
	     * <pre> {@code  public <W> BodySubscriber<W> asJSON(Class<W> targetType) {
	     *     BodySubscriber<InputStream> upstream = BodySubscribers.ofInputStream();
	     *
	     *     BodySubscriber<W> downstream = BodySubscribers.mapping(
	     *           upstream,
	     *           (InputStream is) -> {
	     *               try (InputStream stream = is) {
	     *                   ObjectMapper objectMapper = new ObjectMapper();
	     *                   return objectMapper.readValue(stream, targetType);
	     *               } catch (IOException e) {
	     *                   throw new UncheckedIOException(e);
	     *               }
	     *           });
	     *    return downstream;
	     * } }</pre>
	     *
	     * @param <T> the upstream body type
	     * @param <U> the type of the body subscriber returned
	     * @param upstream the body subscriber to be mapped
	     * @param mapper the mapping function
	     * @return a mapping body subscriber
	     */
	    public <T,U> BodySubscriber<U> mapping(BodySubscriber<T> upstream,
						   Function<? super T, ? extends U> mapper)
	    {
		return new ResponseSubscribers.MappingSubscriber<>(upstream, mapper);
	    }
	    
	};
    
}
