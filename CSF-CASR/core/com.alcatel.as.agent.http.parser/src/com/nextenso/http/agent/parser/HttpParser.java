package com.nextenso.http.agent.parser;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.CharBuffer;
import alcatel.tess.hometop.gateways.utils.Recyclable;
import alcatel.tess.hometop.gateways.utils.Splitter;
import alcatel.tess.hometop.gateways.utils.Trie;
import alcatel.tess.hometop.gateways.utils.Utils;

import com.nextenso.proxylet.http.HttpUtils;

/**
 * This class parses http messages in a non-blocking manner. It parses
 * request/response and callback handlers which are meant to really construct
 * the message.
 * <p>
 *
 * This class uses InputStream when parsing, and call the available method in
 * order to avoid blocking on the read method.
 * <p>
 * 
 * The InputStream given in parseRequest/parseResponse method's arguments must
 * throw the EOFException exception when its "available" method is called and
 * the InputStream has been closed.
 */
public class HttpParser implements Recyclable {
	// The parser is reading http headers.
	public static final int READING_HEADERS = 1;

	// The parser is reading http body.
	public static final int READING_BODY = 2;

	// The parser has fully read an http message.
	public static final int PARSED = 3;

	/**
	 * Creates a new parser.
	 */
	public HttpParser() {
		this(false);
	}

	/**
	 * Creates a new parser.
	 * 
	 * @param handleClose true if the socket close must be expected while reading
	 *                    bodies without content-length. Returning false means that
	 *                    the socket close won't be expected while reading body
	 *                    without a content-length header.
	 * @param strict      true for strict http parser, false if not.
	 */
	public HttpParser(boolean strict) {
		this(strict, false);
	}

	/**
	 * Creates a new parser.
	 * 
	 * @param handleClose            true if the socket close must be expected while
	 *                               reading bodies without content-length.
	 *                               Returning false means that the socket close
	 *                               won't be expected while reading body without a
	 *                               content-length header.
	 * @param strict                 true for strict http parser, false if not.
	 * @param keepParsedHeadersNames true if http parser should keep parsed headers
	 *                               names.
	 */
	protected HttpParser(boolean strict, boolean keepParsedHeadersNames) {
		this.strictParser = strict;
		this.keepParsedHeadersNames = keepParsedHeadersNames;
		this.chunked = new HttpChunkedInputStream();
		this.splitter = new Splitter();
		init();
	}

	/**
	 * Add an header descriptor.
	 */
	public void addHeaderDescriptor(HttpHeaderDescriptor hdrDescriptor) {
		if (wellKnownHeaders == null) {
			wellKnownHeaders = new Trie();
		}

		if (hdrDescriptor.abbreviatedName() != null) {
			wellKnownHeaders.put(hdrDescriptor.abbreviatedName(), hdrDescriptor);
		}
		wellKnownHeaders.put(hdrDescriptor.name(), hdrDescriptor);
	}

	/**
	 * Prepare this parser before starting to parse a new http message.
	 */
	public void init() {
		clear(ST_READY);
	}

	/**
	 * Reads the http request from a given InputStream.
	 *
	 */
	public int parseRequest(InputStream in, HttpRequestHandler reqHandler) throws HttpParserException {
		if (state == ST_CLOSED) {
			throw new HttpParserException(new EOFException("Stream closed while parsing request"));
		}

		try {
			int s = readRequestHeaders(reqHandler, in);

			if (s != READING_BODY) {
				return (s);
			}
			// Support https
			// Set a false content, to stimulate a long request
			if (httpMethod != null && httpMethod.equalsIgnoreCase(HttpUtils.METHOD_CONNECT)) {
				if (logger.isDebugEnabled())
					logger.debug("Setting false content length for https");
				this.clen = 100000;
				httpMethod = null;
				return READING_BODY;
			}
			// end hack for https
			return (readRequestBody(reqHandler, in));
		} catch (HttpParserException e) {
			throw e;
		} catch (Throwable t) {
			throw new HttpParserException(t);
		}
	}

	/**
	 * Read the HTTP headers
	 * 
	 * @param in         the input stream
	 * @param reqHandler the request where to store decoded headers
	 * @return the status of the HTTP parser
	 * @throws HttpParserException
	 */
	public int parseRequestHeaders(InputStream in, HttpRequestHandler reqHandler) throws HttpParserException {
		if (state == ST_CLOSED) {
			throw new HttpParserException(new EOFException("Stream closed while parsing request"));
		}
		try {
			// Parse HTTP headers
			return readRequestHeaders(reqHandler, in);
		} catch (HttpParserException e) {
			throw e;
		} catch (Throwable t) {
			throw new HttpParserException(t);
		}
	}

	/**
	 * Reads the http response from a given InputStream.
	 *
	 */
	public int parseResponse(String requestMethod, InputStream in, HttpResponseHandler rspHandler)
			throws HttpParserException {
		if (state == ST_CLOSED) {
			throw new HttpParserException(new EOFException("Stream closed while parsing response"));
		}

		try {
			int s = readResponseHeaders(requestMethod, rspHandler, in);

			if (s != READING_BODY) {
				return (s);
			}

			return (readResponseBody(rspHandler, in));
		} catch (HttpParserException e) {
			throw e;
		} catch (Throwable t) {
			throw new HttpParserException(t);
		}
	}

	/**
	 * Specifies if the stream is waiting for more data.
	 */
	public boolean needsInput() {
		return (state > ST_READY);
	}

	/**
	 * Specifies the number of bytes to come
	 */
	public int getRemainingBytes() {
		if (isChunked || clen == -1)
			return -1;
		return clen;
	}

	/**
	 * Flushes the body and specifies if the whole body has arrived
	 */
	public boolean flushRequestBody(InputStream in) throws HttpParserException {
		try {
			int n;

			if (this.isChunked) {
				this.chunked.setInputStream(in);
				n = in.available();
				if (n == 0) {
					if (this.chunked.needsInput())
						return false;
					clear(ST_READY);
					return true;
				}
				byte[] tmp = new byte[n];
				int bytesRead = 0;
				while (bytesRead < n) {
					int k = this.chunked.read(tmp, 0, n - bytesRead);
					if (k == -1)
						break;
					bytesRead += k;
				}
				if (this.chunked.needsInput())
					return false;
				clear(ST_READY);
				return true;
			} else if (this.clen != -1) {
				//
				// we are reading a body request with a known length body part
				//
				n = Math.min(in.available(), clen);
				for (int k = 0; k < n; k++)
					in.read();
				clen -= n;
				if (clen != 0)
					return false;
				clear(ST_READY);
				return true;
			}

			throw new HttpParserException("Invalid request body");
		} catch (IOException ie) {
			throw new HttpParserException(ie);
		}
	}

	/**
	 * Flushes the body and specifies is the whole body has arrived
	 */
	public boolean flushResponseBody(InputStream in) throws HttpParserException {
		try {
			int n;

			if (this.isChunked) {
				this.chunked.setInputStream(in);
				n = in.available();
				if (n == 0) {
					if (this.chunked.needsInput())
						return false;
					clear(ST_READY);
					return true;
				}
				byte[] tmp = new byte[n];
				int bytesRead = 0;
				while (bytesRead < n) {
					int k = this.chunked.read(tmp, 0, n - bytesRead);
					if (k == -1)
						break;
					bytesRead += k;
				}
				if (this.chunked.needsInput())
					return false;
				clear(ST_READY);
				return true;
			} else if (this.clen != -1) {
				//
				// we are reading a body response with a known length body part
				//
				n = Math.min(in.available(), clen);
				for (int k = 0; k < n; k++)
					in.read();
				clen -= n;
				if (clen != 0)
					return false;
				clear(ST_READY);
				return true;
			} else {
				//
				// we are reading a body response with an unknown length body part
				//
				try {
					n = in.available();
				}

				catch (EOFException e) {
					// Received a close: all message has been consumed.
					return true;
				}

				for (int k = 0; k < n; k++)
					in.read();
				return false;
			}
		} catch (IOException ie) {
			throw new HttpParserException(ie);
		}
	}

	/****************** Recyclable interface. ************************************/

	/**
	 * Checks if this recycled object still available for future reallocation. This
	 * method is called by the ObjectPool.acquire () method in order to avoid
	 * keeping forever this avail Pxsocket in cache.
	 */
	public boolean isValid() {
		return (System.currentTimeMillis() - recycledTime < MAX_RECYCLE_TIME);
	}

	/**
	 * Recycle this object into the object pool.
	 */
	public void recycled() {
		init();
		recycledTime = System.currentTimeMillis();
	}

	/****************** Protected statements **************************************/

	/**
	 * Reset our attributes and prepare for a next http message read.
	 */
	protected void clear(int nextState) {
		buf.reset();
		chunked.init();
		hdrName = hdrValue = null;
		hdrNameHasSpace = relativeURL = isChunked = false;
		responseStatus = clen = hdrSeparator = -1;
		state = nextState;
	}

	/**
	 * Reads a string from the underlying input stream.
	 *
	 * @param delim   The character specifying the end of string
	 * @param toLower true if the string must be lowercase, false if not
	 * @throws IOException on any the underlying error
	 */
	protected String readString(char delim, boolean toLower, InputStream in) throws IOException {
		return readString(delim, toLower, in, true);
	}

	/**
	 * Reads a string from the underlying input stream.
	 *
	 * @param delim   The character specifying the end of string
	 * @param toLower true if the string must be lowercase, false if not
	 * @throws IOException on any the underlying error
	 */
	protected String readString(char delim, boolean toLower, InputStream in, boolean trim) throws IOException {
		int c = -1;
		int avail = in.available();
		isEol = false;

		loop: while (true) {
			if (--avail < 0)
				return (null);

			if ((c = in.read()) == delim) {
				break;
			}

			switch (c) {
			case -1:
				break loop;

			case '\n':
				isEol = true;
				break loop;

			case '\r':
				continue loop;

			default:
				buf.append(c);
				break;
			}
		}

		String s = buf.toASCIIString(true, trim, toLower);
		buf.reset();
		return (s);
	}

	/**
	 * Reads the http request method. this method is not strict and will ignores
	 * any. supplementary spaces.
	 */
	protected boolean readHttpRequestMethod(HttpRequestHandler reqHandler, InputStream in)
			throws IOException, HttpParserException {
		String s;

		if (strictParser) {
			if ((s = readString(' ', false, in)) == null)
				return (false);

			if (s.length() == 0) {
				// We expect an http method, but instead of that, we got a space, or an empty
				// line.
				throw new HttpParserException("Failed to parse invalid request method.");
			}
		} else {
			do {
				if ((s = readString(' ', false, in)) == null)
					return (false);

				// Leave unexpected ' ' before request method
			} while (s.length() == 0);
		}

		reqHandler.setHttpRequestMethod(s);
		// Support https
		httpMethod = s;
		return true;
	}

	/**
	 * Reads the http request Uri. this method is not strict and will ignores any.
	 * supplementary spaces.
	 */
	protected boolean readHttpRequestUri(HttpRequestHandler reqHandler, InputStream in)
			throws IOException, HttpParserException {
		String s;

		if (strictParser) {
			if ((s = readString(' ', false, in)) == null) {
				return false;
			}

			if (s.length() == 0) {
				// Got unexpected space between request method and Uri
				throw new HttpParserException("Unexpected space found between request method and Uri");
			}
		} else {
			do {
				if ((s = readString(' ', false, in)) == null) {
					return false;
				}
				// we leave spaces for unexpected ' ' between method and URI
			} while (s.length() == 0);
		}

		relativeURL = (s.charAt(0) == '/'); // relative URL = reverse proxy mode

		try {
			reqHandler.setHttpRequestUri(s, relativeURL);
		}

		catch (MalformedURLException ue) {
			throw new HttpParserException("Invalid URL <" + s + ">", ue);
		}

		return true;
	}

	/**
	 * Reads the http request protocol. this method is not strict and will ignores
	 * any. supplementary spaces.
	 */
	protected boolean readHttpRequestProtocol(HttpRequestHandler reqHandler, InputStream in)
			throws IOException, HttpParserException {
		String s;

		if (strictParser) {
			if ((s = readString('\n', false, in, false)) == null) {
				return false;
			}

			// Check if the protocol does not contains any extra spaces.
			if (s.length() == 0 || s.indexOf(" ") != -1) {
				throw new HttpParserException(
						"Invalid request protocol: empty protocol or extra space(s) found in protocol.");
			}
		} else {
			if ((s = readString('\n', false, in)) == null) {
				return false;
			}
		}

		reqHandler.setHttpProtocol(s);
		return true;
	}

	/**
	 * Check for the header syntax.
	 * 
	 * @return true if the header name is well formed, false if contains a syntax
	 *         error. If this method consider that a bad formed header name may be
	 *         ignored, then it will return false. Otherwise, this method throws an
	 *         HttpParserException in order to stop the parser.
	 */
	protected boolean checkHeaderSyntax(String hdrName, String hdrVal, boolean hdrNameHasSpace)
			throws HttpParserException {
		if (strictParser) {
			if (hdrName.length() == 0) {
				throw new HttpParserException("Empty http header name");
			}

			if (hdrNameHasSpace) {
				throw new HttpParserException("http header name contains space");
			}
		} else {
			if (hdrName.length() == 0) {
				// Ignores empty header names !
				return false;
			}
		}

		return true;
	}

	/****************** Private statements ****************************************/

	/**
	 * Reads all request headers.
	 *
	 * @param rsp The http message where to store decoded headers.
	 */
	private int readRequestHeaders(HttpRequestHandler reqHandler, InputStream in)
			throws HttpParserException, IOException, EOFException {
		switch (state) {
		case ST_READY:
			if (!readHttpRequestMethod(reqHandler, in)) {
				return (READING_HEADERS);
			}
			state = ST_READING_REQ_URI;

		case ST_READING_REQ_URI:
			if (!readHttpRequestUri(reqHandler, in)) {
				return READING_HEADERS;
			}
			state = ST_READING_REQ_PROTOCOL;

		case ST_READING_REQ_PROTOCOL:
			if (!readHttpRequestProtocol(reqHandler, in)) {
				return READING_HEADERS;
			}
			state = ST_READING_HEADERS;

		case ST_READING_HEADERS:
			return (readHeaders(reqHandler, in));

		case ST_CLOSED:
			throw new EOFException("Stream closed while parsing request headers");

		case ST_READING_BODY:
			return (READING_BODY);
		}

		throw new HttpParserException("invalid state while reading request headers: " + state);
	}

	/**
	 * Reads the http request body part from a given InputStream.
	 *
	 * @param msg The http message where to store body part.
	 */
	protected int readRequestBody(HttpRequestHandler reqHandler, InputStream in) throws IOException {
		int n = clen;

		if (this.isChunked) {
			this.chunked.setInputStream(in);
			reqHandler.addHttpBody(this.chunked, in.available());

			if (chunked.needsInput()) {
				return (READING_BODY);
			}
		} else if (this.clen != -1) {
			//
			// we are reading a body request with a known length body part
			//
			if (clen > 0) {
				n = in.available();
				reqHandler.addHttpBody(in, n);
				clen -= n;
			}

			if (clen > 0) {
				return (READING_BODY);
			}
		} else if (!readRequestBodyWithoutContentLength(reqHandler, in)) {
			return (READING_BODY);
		}

		clear(ST_READY);
		return (PARSED);
	}

	protected boolean readRequestBodyWithoutContentLength(HttpRequestHandler reqHandler, InputStream in)
			throws IOException {
		// In HTTP, there is no request body without a content-length header.
		return true;
	}

	protected int readResponseBodyWithoutContentLength(HttpResponseHandler rspHandler, InputStream in)
			throws IOException {
		try {
			rspHandler.addHttpBody(in, in.available());
			return (READING_BODY);
		}

		catch (EOFException e) {
			state = ST_CLOSED;
			return (PARSED);
		}
	}

	/**
	 * Reads all response headers.
	 *
	 * @param rsp The http message where to store decoded headers.
	 */
	private int readResponseHeaders(String requestMethod, HttpResponseHandler rspHandler, InputStream in)
			throws HttpParserException, IOException, EOFException {
		String s;

		switch (state) {
		case ST_READY:
			do {
				if ((s = readString(' ', false, in)) == null)
					return (READING_HEADERS);

				// Leave unexpected ' ' before response protocol
			} while (s.length() == 0);

			rspHandler.setHttpProtocol(s);
			state = ST_READING_RSP_STATUS;

		case ST_READING_RSP_STATUS:
			do {
				if ((s = readString(' ', false, in)) == null)
					return (READING_HEADERS);
				// we leave spaces for unexpected ' ' between protocol and status
			} while (s.length() == 0);

			rspHandler.setHttpResponseStatus(responseStatus = Integer.parseInt(s));
			state = ST_READING_RSP_REASON;

		case ST_READING_RSP_REASON:
			if (isEol) {
				rspHandler.setHttpResponseReason(Utils.getHttpReason(responseStatus));
			} else {
				if ((s = readString('\n', false, in)) == null)
					return (READING_HEADERS);

				rspHandler.setHttpResponseReason(s);
			}
			state = ST_READING_HEADERS;

		case ST_READING_HEADERS:
			int status = readHeaders(rspHandler, in);
			if (status == READING_BODY) {
				// we handle the HEAD response separately
				if (requestMethod.equalsIgnoreCase(METHOD_HEAD)) {
					this.isChunked = false;
					this.clen = 0;
				}
			}
			return status;

		case ST_READING_BODY:
			return (READING_BODY);

		case ST_CLOSED:
			throw new EOFException("Stream closed while parsing response headers");
		}

		throw new HttpParserException("invalid state while reading response headers: " + state);
	}

	/**
	 * Reads the http response body part from a given InputStream.
	 *
	 * @param msg The http message where to store body part.
	 */
	private int readResponseBody(HttpResponseHandler rspHandler, InputStream in) throws IOException {
		if (responseStatus == 100 || responseStatus == 101 || responseStatus == 204 || responseStatus == 304) {
			//
			// We have fully read the response: reinitialize our attributes
			// before further read Message call.
			//
			clear(ST_READY);
			return (PARSED);
		}

		if (this.isChunked) {
			return (readChunkedResponseBody(rspHandler, in));
		} else if (this.clen == -1) {
			return (readResponseBodyWithoutContentLength(rspHandler, in));
		} else {
			return (readResponseBodyWithContentLength(rspHandler, in));
		}
	}

	private int readChunkedResponseBody(HttpResponseHandler rspHandler, InputStream in) throws IOException {
		this.chunked.setInputStream(in);
		rspHandler.addHttpBody(this.chunked, in.available());

		if (this.chunked.needsInput()) {
			return (READING_BODY);
		}

		clear(ST_READY);
		return (PARSED);
	}

	private int readResponseBodyWithContentLength(HttpResponseHandler rspHandler, InputStream in) throws IOException {
		if (clen > 0) {
			int n = Math.min(clen, in.available());
			rspHandler.addHttpBody(in, n);
			clen -= n;

			if (clen != 0) {
				return (READING_BODY);
			}
		}

		clear(ST_READY);
		return (PARSED);
	}

	/**
	 * Read all http message headers.
	 *
	 * @param HttpMessage msg the message where to store decoded headers.
	 * @throws IOException on any the underlying error
	 */
	private int readHeaders(HttpHandler handler, InputStream in) throws HttpParserException, IOException, EOFException {
		while (true) {
			if (!readLine(in)) {
				return (READING_HEADERS);
			}

			if (buf.size() == 0) {
				// We got a blank line.
				readBlankLine(handler);
				// Stop parsing the headers because we have a blank line.
				break;
			} else {
				switch (buf.charAt(0)) {
				case ' ':
				case '\t':

					// LWS (Header value continuation) - not frequent
					readContinuation();
					break;

				default:

					// We are parsing a new header line.
					readNewHeaderLine(handler);
					break;
				}

				buf.reset();
				hdrSeparator = -1;
			}
		}

		state = ST_READING_BODY;
		return (READING_BODY);
	}

	private void readNewHeaderLine(HttpHandler handler) throws HttpParserException {
		if (hdrName != null) {
			// We already have parsed the hdr name in previous lines.
			if (hdrValue != null) {
				// We already have parsed the hdr value in previous lines: notify our handler.
				newHttpHeader(handler, hdrName, hdrValue, hdrNameHasSpace);
				this.hdrName = this.hdrValue = null;
				this.hdrDescriptor = null;
			} else {
				// We already have parsed the hdr name, but the current line does not start with
				// a LWS continuation.
				throw new HttpParserException("Invalid header: " + hdrName);
			}
		}

		if (hdrSeparator == -1) {
			// Header sep not found. Probably followed by a LWS in the next line.
			hdrName = getHeaderName(buf, 0, buf.size());
		} else {
			hdrName = getHeaderName(buf, 0, hdrSeparator);
			hdrValue = buf.toASCIIString(hdrSeparator + 1, true, true, false);
		}
		// Keep in mind if we got a space in the header name.
		hdrNameHasSpace = foundSpaceBeforeSep;
	}

	private void readContinuation() throws HttpParserException {
		if (hdrName == null) {
			throw new HttpParserException("Invalid header line: " + buf.toString());
		} else if (hdrValue == null) {
			// We have parsed the header name in the previous line, but we did not find hdr
			// sep.
			// So we must be currently reading the header value in the form "LWS:<header
			// value>"

			int sep;
			for (sep = 1; sep < buf.size(); sep++) {
				if (buf.charAt(sep) == ' ' || buf.charAt(sep) == '\t') {
					continue;
				} else if (buf.charAt(sep) == ':') {
					break;
				} else {
					throw new HttpParserException("Invalid header line: " + buf.toString());
				}
			}
			hdrValue = buf.toASCIIString(sep + 1, true, true, false);
		} else {
			// We have parsed the header value: Replace the LWS by a space (see rfc2616:
			// chap 4.2)
			hdrValue += " " + buf.toASCIIString(true, true, false);
			hdrValue = hdrValue.trim();
		}
	}

	private void readBlankLine(HttpHandler handler) throws HttpParserException {
		if (hdrName != null && hdrValue != null) {
			// Notify our handler about the last parsed header.
			newHttpHeader(handler, hdrName, hdrValue, hdrNameHasSpace);
			this.hdrName = this.hdrValue = null;
			this.hdrDescriptor = null;
		}
	}

	/**
	 * Get the http header name, avoiding (if possible) the allocation of a string
	 * object.
	 */
	private String getHeaderName(CharBuffer buf, int off, int len) {
		// return (buf.toASCIIString(off, len, true, true, true));

		// trim from left and right side.
		char[] chars = buf.toCharArray(false);
		int max = off + len;

		while ((off < max) && (chars[off] <= ' ')) {
			off++;
			len--;
		}

		while ((len > 0) && (chars[off + len - 1]) <= ' ') {
			len--;
		}

		// Lookup the header from our list of well known header names.
		// if original header's name must be kept, header descriptor is useless
		if (!keepParsedHeadersNames && wellKnownHeaders != null) {
			this.hdrDescriptor = (HttpHeaderDescriptor) wellKnownHeaders.get(chars, off, len);
			if (this.hdrDescriptor != null) {
				return hdrDescriptor.name();
			}
		}

		// Not found. We have to allocate a string for that header.
		String hdrName = buf.toASCIIString(off, len, true, false, !keepParsedHeadersNames);
		return (hdrName);
	}

	/**
	 * Notify our parser handler about a new header.
	 */
	private void newHttpHeader(HttpHandler handler, String name, String value, boolean hdrNameHasSpace)
			throws HttpParserException {
		if (!checkHeaderSyntax(hdrName, hdrValue, hdrNameHasSpace)) {
			return;
		}

		if (name.equalsIgnoreCase(SET_COOKIE)) {
			newSetCookie(handler, value);
			return;
		}
		if (name.equalsIgnoreCase(SET_COOKIE2)) {
			// we ignore it - we make sure we do not put it in the headers
			return;
		}
		if (name.equalsIgnoreCase(COOKIE)) {
			// cookie contains 1+ cookies per line separated by ';'
			newCookie(handler, value);
			return;
		}

		if (name.equalsIgnoreCase(CONTENT_LENGTH)) {
			newContentLength(value);
		} else if (name.equalsIgnoreCase(TRANSFER_ENCODING)) {
			if (! newTransferEncoding(value)) {
				return;
			}
		} else if (relativeURL && name.equalsIgnoreCase(HOST)) {
			newHost(handler, value);
		}

		// finally...
		addHttpHeader(handler, name, value);
	}

	private void addHttpHeader(HttpHandler handler, String name, String value) {
		if (hdrDescriptor != null) {
			handler.addHttpHeader(hdrDescriptor, value);
		} else {
			handler.addHttpHeader(name, value);
		}
	}

	private void newHost(HttpHandler handler, String value) throws HttpParserException {
		HttpRequestHandler reqHandler = (HttpRequestHandler) handler;
		try {
			reqHandler.setHttpRequestUrlAuthority(value);
		} catch (MalformedURLException ue) {
			throw new HttpParserException("Invalid URL Authority <" + value + ">", ue);
		}
	}

	private boolean newTransferEncoding(String value) {
		String[] values = splitter.splitrim(value, ",");
		if (values.length == 0) {
			// empty header value
			return false;
		} else {
			// "chunked" must be the last value
			this.isChunked = (values[values.length - 1].equalsIgnoreCase(PARAM_CHUNKED));
		}
		return true;
	}

	private void newContentLength(String value) throws HttpParserException, NumberFormatException {
		if (this.clen != -1) {
			throw new HttpParserException("Duplicate Content-Length");
		}
		this.clen = Integer.parseInt(value);
	}

	private void newCookie(HttpHandler handler, String value) {
		String[] values = splitter.splitrim(value, ";");
		for (int i = 0; i < values.length; i++) {
			if (values[i].length() > 0) {
				try {
					handler.addHttpCookie(CookieParser.parse(values[i]));
				} catch (IllegalArgumentException e) {
					// the cookie is invalid
					// ignore it for now
					logger.warn("Ignoring invalid cookie header:" + values[i]);
				}
			}
		}
	}

	private void newSetCookie(HttpHandler handler, String value) {
		// set-cookie contains 1 cookie per line
		if (value.length() > 0) {
			try {
				// we do not apply defaults values for domain and path
				handler.addHttpCookie(CookieParser.parse(value));
			} catch (IllegalArgumentException e) {
				// the cookie is invalid
				// ignore it for now
				logger.warn("Ignoring invalid set-cookie header:" + value);
			}
		} else
			logger.warn("Ignoring blank set-cookie header");
	}

	/**
	 * Read a header line. If the header separator is found, then its position is
	 * kept in the "hdrSeparator" attribute.
	 * 
	 * @return true if a line has been parsed, false if not enough data is
	 *         available.
	 */
	private boolean readLine(InputStream in) throws IOException {
		int c = 0;
		int avail = in.available();
		isEol = false;
		foundSpaceBeforeSep = false;

		loop: while (true) {
			if (--avail < 0)
				return false;

			c = in.read();

			switch (c) {
			case '\n':
				isEol = true;
				break loop;

			case -1:
				throw new EOFException("Stream closed while reading header line: " + buf.toString());

			case '\r':
				continue loop;

			case ':':
				if (hdrSeparator == -1)
					hdrSeparator = buf.size();
				buf.append(c);
				break;

			case ' ':
				if (hdrSeparator == -1) {
					// We got a space before the hdr separator: probably an uri ..
					foundSpaceBeforeSep = true;
				}
				buf.append(c);
				break;

			default:
				buf.append(c);
				continue loop;
			}
		}

		return true;
	}

//  private boolean isDigit(char c) {
//    int ascii = c & 0xffff;
//    return (((ascii - 0x30) | (0x39 - ascii)) >= 0 || ((ascii - 0x41) | (0x46 - ascii)) >= 0 || ((ascii - 0x61) | (0x66 - ascii)) >= 0);
//  }

	public int getState() {
		return state;
	}

	@Override
	public String toString() {
		return "HttpParser [strictParser=" + strictParser + ", state=" + state + ", httpMethod=" + httpMethod + "]";
	}

	/** The response status code **/
	protected int responseStatus;

	/**
	 * Specifies if the URL is relative (reverse proxy mode) or absolute (common
	 * case)
	 */
	protected boolean relativeURL;

	/** The current http header name that we are reading */
	protected String hdrName;

	/** The current http header value that we are reading */
	protected String hdrValue;

	/** The current http header descriptor that we are reading */
	protected HttpHeaderDescriptor hdrDescriptor;

	/** The location of the ':' separating the header name and value */
	protected int hdrSeparator = -1;

	/** Tells if the header name contains a space. */
	protected boolean hdrNameHasSpace;

	/**
	 * Tells if there is a space in a header line before the separator char (":")
	 */
	protected boolean foundSpaceBeforeSep;

	/** Internal buffer used to store current headers */
	protected CharBuffer buf = new CharBuffer();

	/** The stream used to decode http chunked body messages */
	protected HttpChunkedInputStream chunked;

	/**
	 * The integer found in the content-length message header, or -1 if not found
	 */
	protected int clen;

	/** Are we decoding an http chunked message body */
	protected boolean isChunked;

	/** Have we reached the end of header line */
	protected boolean isEol;

	/** Last time we were recycled to an object pool */
	protected long recycledTime;

	/** Splitter used to parse headers */
	protected Splitter splitter;

	/** Tells if this parser is strict or not. */
	protected boolean strictParser;

	/** Tells if this parser should keep parsed headers names. */
	protected boolean keepParsedHeadersNames;

	/** The internal parser state */
	protected int state = ST_READY;

	/** State indicating that we are ready to read a new http message */
	protected final static int ST_READY = 0;

	/** State indicating that we are reading a response status */
	protected final static int ST_READING_RSP_STATUS = 1;

	/** State indicating that we are reading a response reason */
	protected final static int ST_READING_RSP_REASON = 2;

	/** State indicating that we are reading a request uri */
	protected final static int ST_READING_REQ_URI = 3;

	/** State indicating that we are reading a request protocol */
	protected final static int ST_READING_REQ_PROTOCOL = 4;

	/** State indicating that we are reading some message headers */
	protected final static int ST_READING_HEADERS = 5;

	/** State indicating that we are reading the message body */
	protected final static int ST_READING_BODY = 6;

	/** Our stream is closed */
	protected final static int ST_CLOSED = -1;

	/** Characters allower in a URI */
	private static boolean[] allowedUriChars;

	/** Hashtable for well known headers. */
	private Trie wellKnownHeaders;

	/** Our Logger **/
	private final static Logger logger = Logger.getLogger("agent.http");

	/** Misc constants */
	protected final static String CONTENT_LENGTH = "Content-Length";
	protected final static String METHOD_HEAD = "HEAD";
	protected final static String TRANSFER_ENCODING = "Transfer-Encoding";
	protected final static String HOST = "Host";
	protected final static String PARAM_CHUNKED = "chunked";
	protected final static String SET_COOKIE = "Set-Cookie";
	protected final static String SET_COOKIE2 = "Set-Cookie2";
	protected final static String COOKIE = "Cookie";
	protected static long MAX_RECYCLE_TIME = 10 * 1000;
	private String httpMethod = null;

	static {
		// Init forbiden uri chars.
		allowedUriChars = new boolean[256];
		for (int i = 0; i < 255; i++) {
			allowedUriChars[i] = true;
		}
		// exclude ASCII control chars.
		for (int i = 0; i < 0x1F; i++) {
			allowedUriChars[i] = false;
		}
		allowedUriChars[0x7f] = false;

		// exclude space.
		allowedUriChars[0x20] = false;

		// exclude delimiters. (the % char is checked outside this array).
		allowedUriChars['<'] = false;
		allowedUriChars['>'] = false;
		allowedUriChars['#'] = false;
		allowedUriChars['<'] = false;

		// exclude unwise.
		allowedUriChars['{'] = false;
		allowedUriChars['}'] = false;
		allowedUriChars['|'] = false;
		allowedUriChars['\\'] = false;
		allowedUriChars['^'] = false;
		allowedUriChars['['] = false;
		allowedUriChars[']'] = false;
		allowedUriChars['`'] = false;
	}
}
