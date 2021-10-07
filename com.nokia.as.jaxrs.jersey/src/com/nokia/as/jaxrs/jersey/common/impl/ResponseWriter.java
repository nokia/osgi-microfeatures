package com.nokia.as.jaxrs.jersey.common.impl;

import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.UTF_8;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import com.alcatel.as.http.parser.AccessLog;
import com.alcatel.as.http.parser.CommonLogFormat;
import com.alcatel.as.http.parser.HttpStatuses;
import com.nokia.as.jaxrs.jersey.common.ClientContext;

public final class ResponseWriter extends OutputStream implements ContainerResponseWriter {

	private static final char SPACE = ' ';
	private static final String HTTP_VERSION_0 = "HTTP/1.0";
	private static final String HTTP_VERSION_1 = "HTTP/1.1";
	private static final String HEADER_SEPARATOR = ": ";
	private static final byte[] CHUNKED_END = "0\r\n\r\n".getBytes(UTF_8);
	private static final byte[] CRLF_CHUNKED_END = "\r\n0\r\n\r\n".getBytes(UTF_8);

	private ClientContext _clientCtx;
	private int _version;
	private boolean _keepAlive;
	private boolean _streaming = true;
	private boolean _wroteFirstChunk = false;
	private AccessLog _al;
	private int _responseSize = 0;
	
	public ResponseWriter(ClientContext clientCtx, boolean keepAlive, int version, AccessLog al) {
		_clientCtx = clientCtx;
		_keepAlive = keepAlive;
		_version = version;
		_al = al;
	}

	@Override
	public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse context)
			throws ContainerException {
		final int code = context.getStatusInfo().getStatusCode();

		StringBuilder sb = new StringBuilder();
		statusLine(sb, code);
		headers(sb, contentLength, context);
		String responseHTTP = sb.append("\r\n").toString();

		_clientCtx.send(ByteBuffer.wrap(responseHTTP.getBytes(UTF_8)), false);
		_al.responseStatus(code);
		_clientCtx.getServerContext ().getMeters ().getWriteRespMeter(code).inc(1);
		return this;
	}

	/************************************/
	/** Implementation of OutputStream **/
	/************************************/
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b }, 0, 1);
	}

	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	public void write(byte b[], int from, int len) throws IOException {
		if (len == 0)
			return;
		if (_streaming) {
			String s = Integer.toHexString(len);
			int size = s.length();
			byte[] h = null;
			int off = 0;
			if (_wroteFirstChunk) {
				h = new byte[size + 4];
				h[0] = (byte) '\r';
				h[1] = (byte) '\n';
				off = 2;
			} else {
				h = new byte[size + 2];
			}
			for (int k = 0; k < size; k++)
				h[off++] = (byte) s.charAt(k);
			h[off++] = (byte) '\r';
			h[off++] = (byte) '\n';
			_clientCtx.send(ByteBuffer.wrap(h, 0, h.length), false);
			_responseSize += h.length;
		}
		_clientCtx.send(ByteBuffer.wrap(b, from, len), true);
		_wroteFirstChunk = true;
		_responseSize += len;

	}

	public void flush() {
	}

	/****************************************/
	/** End Implementation of OutputStream **/
	/****************************************/

	private StringBuilder headers(StringBuilder stringBuilder, long contentLen, ContainerResponse context) {
		_streaming = (contentLen == -1); // sometimes jersey does not respect the enableResponseBuffering response
		if (_streaming)
			stringBuilder.append("Transfer-Encoding: chunked\r\n");
		else
			stringBuilder.append("Content-Length: ").append(contentLen).append("\r\n");
		if (_version == 0 && _keepAlive)
			stringBuilder.append("Connection: keep-alive\r\n");
		for (final Map.Entry<String, List<String>> e : context.getStringHeaders().entrySet()) {
			for (final String value : e.getValue()) {
				stringBuilder.append(e.getKey());
				stringBuilder.append(HEADER_SEPARATOR);
				stringBuilder.append(value);
				stringBuilder.append("\r\n");
			}
		}
		return stringBuilder;
	}

	private StringBuilder statusLine(StringBuilder stringBuilder, int code) {
		String reason = HttpStatuses.getReason (code);
		stringBuilder.append(_version == 0 ? HTTP_VERSION_0 : HTTP_VERSION_1);
		stringBuilder.append(SPACE);
		stringBuilder.append(code);
		stringBuilder.append(SPACE);
		stringBuilder.append(reason);
		stringBuilder.append("\r\n");
		return stringBuilder;
	}

	@Override
	public void failure(Throwable error) {
		_clientCtx.getServerContext ().getLogger ().error(_clientCtx + " : jersey processing error", error);

		StringBuilder sb = new StringBuilder();
		statusLine(sb, 500);
		String responseHTTP = sb.append("\r\n").toString();

		_clientCtx.send(ByteBuffer.wrap((responseHTTP).getBytes(UTF_8)), false);
		_clientCtx.close();

		_clientCtx.getServerContext ().getMeters ().getWriteRespMeter(500).inc(1);
		if (!_al.isEmpty()) {
		    _al.responseStatus(500);
		    _al.responseSize(0);
		    CommonLogFormat clf = _clientCtx.getServerContext ().getCommonLogFormat ();
		    clf.log(_al);
		}
		
		// Rethrow the original exception as required by JAX-RS, 3.3.4.
		if (error instanceof RuntimeException) {
			throw (RuntimeException) error;
		} else {
			throw new ContainerException(error);
		}
	}

	@Override
	public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
		return true;
	}

	@Override
	public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) throws IllegalStateException {
		_clientCtx.setSuspendTimeout(timeOut);
	}

	@Override
	public void commit() {
		if (_streaming) {
		    if (_wroteFirstChunk){
			_clientCtx.send(ByteBuffer.wrap(CRLF_CHUNKED_END, 0, CRLF_CHUNKED_END.length), true);
			_responseSize += CRLF_CHUNKED_END.length;
		    } else {
			_clientCtx.send(ByteBuffer.wrap(CHUNKED_END, 0, CHUNKED_END.length), true);
			_responseSize += CHUNKED_END.length;
		    }
		}
		if (!_keepAlive)
			_clientCtx.close();

		if (!_al.isEmpty()) {
			_al.responseSize(_responseSize);
			CommonLogFormat clf = _clientCtx.getServerContext ().getCommonLogFormat ();
			clf.log(_al);
		}
	}

	@Override
	public boolean enableResponseBuffering() {
		return !_streaming;
	}
}
