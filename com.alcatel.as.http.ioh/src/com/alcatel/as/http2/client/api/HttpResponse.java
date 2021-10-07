package com.alcatel.as.http2.client.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentMap;
import com.alcatel.as.http2.client.api.Flow;
import com.alcatel.as.http2.client.api.Flow.Subscriber;
import com.alcatel.as.http2.client.api.Flow.Publisher;
import com.alcatel.as.http2.client.api.Flow.Subscription;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.net.ssl.SSLSession;
import static java.nio.file.StandardOpenOption.*;
import static com.alcatel.as.http2.client.api.impl.common.Utils.charsetFrom;
import org.osgi.annotation.versioning.ProviderType;

/**
 * An HTTP response.
 *
 * <p> An {@code HttpResponse} is not created directly, but rather returned as
 * a result of sending an {@link HttpRequest}. An {@code HttpResponse} is
 * made available when the response status code and headers have been received,
 * and typically after the response body has also been completely received.
 * Whether or not the {@code HttpResponse} is made available before the response
 * body has been completely received depends on the {@link BodyHandler
 * BodyHandler} provided when sending the {@code HttpRequest}.
 *
 * <p> This class provides methods for accessing the response status code,
 * headers, the response body, and the {@code HttpRequest} corresponding
 * to this response.
 *
 * <p> The following is an example of retrieving a response as a String:
 *
 * <pre>{@code    HttpResponse<String> response = client
 *     .send(request, BodyHandlers.ofString()); }</pre>
 *
 * <p> The class {@link BodyHandlers BodyHandlers} provides implementations
 * of many common response handlers. Alternatively, a custom {@code BodyHandler}
 * implementation can be used.
 *
 * @param <T> the response body type
 */
@ProviderType
public interface HttpResponse<T> {


    /**
     * Returns the status code for this response.
     *
     * @return the response code
     */
    public int statusCode();

    /**
     * Returns the {@link HttpRequest} corresponding to this response.
     *
     * <p> The returned {@code HttpRequest} may not be the initiating request
     * provided when {@linkplain HttpClient#send(HttpRequest, BodyHandler)
     * sending}. For example, if the initiating request was redirected, then the
     * request returned by this method will have the redirected URI, which will
     * be different from the initiating request URI.
     *
     * @see #previousResponse()
     *
     * @return the request
     */
    public HttpRequest request();

    /**
     * Returns the received response headers.
     *
     * @return the response headers
     */
    public HttpHeaders headers();

    /**
     * Returns the body. Depending on the type of {@code T}, the returned body
     * may represent the body after it was read (such as {@code byte[]}, or
     * {@code String}, or {@code Path}) or it may represent an object with
     * which the body is read, such as an {@link java.io.InputStream}.
     *
     * <p> If this {@code HttpResponse} was returned from an invocation of
     * {@link #previousResponse()} then this method returns {@code null}
     *
     * @return the body
     */
    public T body();

    /**
     * Returns the {@code URI} that the response was received from. This may be
     * different from the request {@code URI} if redirection occurred.
     *
     * @return the URI of the response
     */
    public URI uri();

    /**
     * When setup was performed during creation of client
     * c.f. {@link HttpClient.Builder#exportKeyingMaterial}, export the TLS keying material.
     * @return null if not setup properly or not in TLS 1.2
     */
    public Map<String, Object> exportKeyingMaterial();

    /**
     * Initial response information supplied to a {@link BodyHandler BodyHandler}
     * when a response is initially received and before the body is processed.
     */
    @ProviderType
    public interface ResponseInfo {
        /**
         * Provides the response status code.
         * @return the response status code
         */
        public int statusCode();

        /**
         * Provides the response headers.
         * @return the response headers
         */
        public HttpHeaders headers();

        /**
         * Provides the response protocol version.
         * @return the response protocol version
         */
        public HttpClient.Version version();

        /**
         * If {@HttpClient} was configured to extract keying material
         * make keying material available through this method.
         * Otherwise returns null.
         * @return
         */
        public Map<String, Object> exportKeyingMaterial();
    }

    /**
     * A handler for response bodies.  The class {@link BodyHandlers BodyHandlers}
     * provides implementations of many common body handlers.
     *
     * <p> The {@code BodyHandler} interface allows inspection of the response
     * code and headers, before the actual response body is received, and is
     * responsible for creating the response {@link BodySubscriber
     * BodySubscriber}. The {@code BodySubscriber} consumes the actual response
     * body bytes and, typically, converts them into a higher-level Java type.
     *
     * <p> A {@code BodyHandler} is a function that takes a {@link ResponseInfo
     * ResponseInfo} object; and which returns a {@code BodySubscriber}. The
     * {@code BodyHandler} is invoked when the response status code and headers
     * are available, but before the response  body bytes are received.
     *
     * <p> The following example uses one of the {@linkplain BodyHandlers
     * predefined body handlers} that always process the response body in the
     * same way ( streams the response body to a file ).
     *
     * <pre>{@code   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("http://www.foo.com/"))
     *        .build();
     *  client.sendAsync(request, BodyHandlers.ofFile(Paths.get("/tmp/f")))
     *        .thenApply(HttpResponse::body)
     *        .thenAccept(System.out::println); }</pre>
     *
     * Note, that even though the pre-defined handlers do not examine the
     * response code, the response code and headers are always retrievable from
     * the {@link HttpResponse}, when it is returned.
     *
     * <p> In the second example, the function returns a different subscriber
     * depending on the status code.
     * <pre>{@code   HttpRequest request = HttpRequest.newBuilder()
     *        .uri(URI.create("http://www.foo.com/"))
     *        .build();
     *  BodyHandler<Path> bodyHandler = (rspInfo) -> rspInfo.statusCode() == 200
     *                      ? BodySubscribers.ofFile(Paths.get("/tmp/f"))
     *                      : BodySubscribers.replacing(Paths.get("/NULL"));
     *  client.sendAsync(request, bodyHandler)
     *        .thenApply(HttpResponse::body)
     *        .thenAccept(System.out::println); }</pre>
     *
     * @param <T> the response body type
     * @see BodyHandlers
     * @since 11
     */
    @FunctionalInterface
    public interface BodyHandler<T> {

        /**
         * Returns a {@link BodySubscriber BodySubscriber} considering the
         * given response status code and headers. This method is invoked before
         * the actual response body bytes are read and its implementation must
         * return a {@link BodySubscriber BodySubscriber} to consume the response
         * body bytes.
         *
         * <p> The response body can be discarded using one of {@link
         * BodyHandlers#discarding() discarding} or {@link
         * BodyHandlers#replacing(Object) replacing}.
         *
         * @param responseInfo the response info
         * @return a body subscriber
         */
        public BodySubscriber<T> apply(ResponseInfo responseInfo);
    }

    /**
     * Implementations of {@link BodyHandler BodyHandler} that implement various
     * useful handlers, such as handling the response body as a String, or
     * streaming the response body to a file.
     *
     * <p> These implementations do not examine the status code, meaning the
     * body is always accepted. They typically return an equivalently named
     * {@code BodySubscriber}. Alternatively, a custom handler can be used to
     * examine the status code and headers, and return a different body
     * subscriber, of the same type, as appropriate.
     *
     * <p>The following are examples of using the predefined body handlers to
     * convert a flow of response body data into common high-level Java objects:
     *
     * <pre>{@code    // Receives the response body as a String
     *   HttpResponse<String> response = client
     *     .send(request, BodyHandlers.ofString());
     *
     *   // Receives the response body as a file
     *   HttpResponse<Path> response = client
     *     .send(request, BodyHandlers.ofFile(Paths.get("example.html")));
     *
     *   // Receives the response body as an InputStream
     *   HttpResponse<InputStream> response = client
     *     .send(request, BodyHandlers.ofInputStream());
     *
     *   // Discards the response body
     *   HttpResponse<Void> response = client
     *     .send(request, BodyHandlers.discarding());  }</pre>
     *
     */
    @ProviderType
    public interface BodyHandlers {
	
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
	    fromSubscriber(Subscriber<? super List<ByteBuffer>> subscriber);

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
	    fromSubscriber(S subscriber, Function<? super S,? extends T> finisher);

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
	    fromLineSubscriber(Subscriber<? super String> subscriber);

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
			       String lineSeparator);

        /**
         * Returns a response body handler that discards the response body.
         *
         * @return a response body handler
         */
        public BodyHandler<Void> discarding();

        /**
         * Returns a response body handler that returns the given replacement
         * value, after discarding the response body.
         *
         * @param <U> the response body type
         * @param value the value of U to return as the body, may be {@code null}
         * @return a response body handler
         */
        public <U> BodyHandler<U> replacing(U value);

        /**
         * Returns a {@code BodyHandler<String>} that returns a
         * {@link BodySubscriber BodySubscriber}{@code <String>} obtained from
         * {@link BodySubscribers#ofString(Charset) BodySubscribers.ofString(Charset)}.
         * The body is decoded using the given character set.
         *
         * @param charset the character set to convert the body with
         * @return a response body handler
         */
        public BodyHandler<String> ofString(Charset charset);

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
        public BodyHandler<Path> ofFile(Path file, OpenOption... openOptions);

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
        public BodyHandler<Path> ofFile(Path file);

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
						OpenOption... openOptions);

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
	    ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer);

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
        public BodyHandler<byte[]> ofByteArray();

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
        public BodyHandler<String> ofString();

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
        public BodyHandler<Publisher<List<ByteBuffer>>> ofPublisher();

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
					    int bufferSize);
    }

    /**
     * A handler for push promises.
     *
     * <p> A <i>push promise</i> is a synthetic request sent by an HTTP/2 server
     * when retrieving an initiating client-sent request. The server has
     * determined, possibly through inspection of the initiating request, that
     * the client will likely need the promised resource, and hence pushes a
     * synthetic push request, in the form of a push promise, to the client. The
     * client can choose to accept or reject the push promise request.
     *
     * <p> A push promise request may be received up to the point where the
     * response body of the initiating client-sent request has been fully
     * received. The delivery of a push promise response, however, is not
     * coordinated with the delivery of the response to the initiating
     * client-sent request.
     *
     * @param <T> the push promise response body type
     */
    public interface PushPromiseHandler<T> {

        /**
         * Notification of an incoming push promise.
         *
         * <p> This method is invoked once for each push promise received, up
         * to the point where the response body of the initiating client-sent
         * request has been fully received.
         *
         * <p> A push promise is accepted by invoking the given {@code acceptor}
         * function. The {@code acceptor} function must be passed a non-null
         * {@code BodyHandler}, that is to be used to handle the promise's
         * response body. The acceptor function will return a {@code
         * CompletableFuture} that completes with the promise's response.
         *
         * <p> If the {@code acceptor} function is not successfully invoked,
         * then the push promise is rejected. The {@code acceptor} function will
         * throw an {@code IllegalStateException} if invoked more than once.
         *
         * @param initiatingRequest the initiating client-send request
         * @param pushPromiseRequest the synthetic push request
         * @param acceptor the acceptor function that must be successfully
         *                 invoked to accept the push promise
         */
        public void applyPushPromise(
				     HttpRequest initiatingRequest,
				     HttpRequest pushPromiseRequest,
				     Function<HttpResponse.BodyHandler<T>,CompletableFuture<HttpResponse<T>>> acceptor
				     );


    }

    /**
     * A {@code BodySubscriber} consumes response body bytes and converts them
     * into a higher-level Java type.  The class {@link BodySubscribers
     * BodySubscriber} provides implementations of many common body subscribers.
     *
     * <p> The object acts as a {@link Flow.Subscriber}&lt;{@link List}&lt;{@link
     * ByteBuffer}&gt;&gt; to the HTTP Client implementation, which publishes
     * lists of ByteBuffers containing the response body. The Flow of data, as
     * well as the order of ByteBuffers in the Flow lists, is a strictly ordered
     * representation of the response body. Both the Lists and the ByteBuffers,
     * once passed to the subscriber, are no longer used by the HTTP Client. The
     * subscriber converts the incoming buffers of data to some higher-level
     * Java type {@code T}.
     *
     * <p> The {@link #getBody()} method returns a
     * {@link CompletionStage}&lt;{@code T}&gt; that provides the response body
     * object. The {@code CompletionStage} must be obtainable at any time. When
     * it completes depends on the nature of type {@code T}. In many cases,
     * when {@code T} represents the entire body after being consumed then
     * the {@code CompletionStage} completes after the body has been consumed.
     * If  {@code T} is a streaming type, such as {@link java.io.InputStream
     * InputStream}, then it completes before the body has been read, because
     * the calling code uses the {@code InputStream} to consume the data.
     *
     * @apiNote To ensure that all resources associated with the corresponding
     * HTTP exchange are properly released, an implementation of {@code
     * BodySubscriber} should ensure to {@link Flow.Subscription#request
     * request} more data until one of {@link #onComplete() onComplete} or
     * {@link #onError(Throwable) onError} are signalled, or {@link
     * Flow.Subscription#request cancel} its {@linkplain
     * #onSubscribe(Flow.Subscription) subscription} if unable or unwilling to
     * do so. Calling {@code cancel} before exhausting the response body data
     * may cause the underlying HTTP connection to be closed and prevent it
     * from being reused for subsequent operations.
     *
     * @implNote The flow of data containing the response body is immutable.
     * Specifically, it is a flow of unmodifiable lists of read-only ByteBuffers.
     *
     * @param <T> the response body type
     * @see BodySubscribers
     */
    public interface BodySubscriber<T>
	extends Flow.Subscriber<List<ByteBuffer>> {

        /**
         * Returns a {@code CompletionStage} which when completed will return
         * the response body object. This method can be called at any time
         * relative to the other {@link Flow.Subscriber} methods and is invoked
         * using the client's {@link HttpClient#executor() executor}.
         *
         * @return a CompletionStage for the response body
         */
        public CompletionStage<T> getBody();
    }

    /**
     * Implementations of {@link BodySubscriber BodySubscriber} that implement
     * various useful subscribers, such as converting the response body bytes
     * into a String, or streaming the bytes to a file.
     *
     * <p>The following are examples of using the predefined body subscribers
     * to convert a flow of response body data into common high-level Java
     * objects:
     *
     * <pre>{@code    // Streams the response body to a File
     *   HttpResponse<byte[]> response = client
     *     .send(request, responseInfo -> BodySubscribers.ofByteArray());
     *
     *   // Accumulates the response body and returns it as a byte[]
     *   HttpResponse<byte[]> response = client
     *     .send(request, responseInfo -> BodySubscribers.ofByteArray());
     *
     *   // Discards the response body
     *   HttpResponse<Void> response = client
     *     .send(request, responseInfo -> BodySubscribers.discarding());
     *
     *   // Accumulates the response body as a String then maps it to its bytes
     *   HttpResponse<byte[]> response = client
     *     .send(request, responseInfo ->
     *        BodySubscribers.mapping(BodySubscribers.ofString(UTF_8), String::getBytes));
     * }</pre>
     *
     */
    @ProviderType
    public interface BodySubscribers {
	
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
	    fromSubscriber(Subscriber<? super List<ByteBuffer>> subscriber);

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
			   Function<? super S,? extends T> finisher);

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
	    fromLineSubscriber(Subscriber<? super String> subscriber);

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
			       String lineSeparator);

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
        public BodySubscriber<String> ofString(Charset charset);

        /**
         * Returns a {@code BodySubscriber} which stores the response body as a
         * byte array.
         *
         * <p> The {@link HttpResponse} using this subscriber is available after
         * the entire response has been read.
         *
         * @return a body subscriber
         */
        public BodySubscriber<byte[]> ofByteArray();

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
        public BodySubscriber<Path> ofFile(Path file, OpenOption... openOptions);

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
        public BodySubscriber<Path> ofFile(Path file);

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
	    ofByteArrayConsumer(Consumer<Optional<byte[]>> consumer);

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
        public BodySubscriber<Publisher<List<ByteBuffer>>> ofPublisher();

        /**
         * Returns a response subscriber which discards the response body. The
         * supplied value is the value that will be returned from
         * {@link HttpResponse#body()}.
         *
         * @param <U> the type of the response body
         * @param value the value to return from HttpResponse.body(), may be {@code null}
         * @return a {@code BodySubscriber}
         */
        public <U> BodySubscriber<U> replacing(U value);

        /**
         * Returns a response subscriber which discards the response body.
         *
         * @return a response body subscriber
         */
        public BodySubscriber<Void> discarding();

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
					       int bufferSize);

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
         * <pre> {@code  public static <W> BodySubscriber<W> asJSON(Class<W> targetType) {
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
					       Function<? super T, ? extends U> mapper);
    }
}
