// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.demux;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Level;

import com.alcatel.as.http2.client.api.Flow.Subscriber;
import com.alcatel.as.http2.client.api.Flow.Subscription;
import com.alcatel.as.http2.client.api.HttpClient;
import com.alcatel.as.http2.client.api.HttpRequest.BodyPublisher;
import com.alcatel.as.http2.client.api.HttpResponse;
import com.alcatel.as.http2.client.api.HttpResponse.BodyHandler;
import com.alcatel.as.http2.client.api.HttpResponse.BodySubscriber;
import com.alcatel.as.http2.client.api.HttpResponse.ResponseInfo;
import com.alcatel.as.http2.client.api.HttpTimeoutException;
import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel_lucent.as.service.dns.DNSHelper;
import com.alcatel_lucent.as.service.dns.DNSHelper.Listener;
import com.alcatel_lucent.as.service.dns.RecordAddress;
import com.nextenso.http.agent.HttpChannel;
import com.nextenso.http.agent.ReentrantSerialExecutor;
import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.parser.HttpParserException;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpUtils;

import alcatel.tess.hometop.gateways.utils.ObjectPool;
import alcatel.tess.hometop.gateways.utils.Recyclable;

@SuppressWarnings({ "deprecation" })
public class HttpPipeline extends HttpChannel implements DemuxSocket, Recyclable {

	static int SOCKET_TIMEOUT = 60;
	private long _lastAccessTime;
	private long _cnxId;
	private Future<?> _timeoutTask;
	private volatile DemuxClientSocket _demuxSocket;
	private volatile boolean _retry;
	private InetSocketAddress _outgoingAddress;

	/**
	 * this flag is an attempt to avoid handling multiple times a cascade of h2
	 * client errors (see proxySocketError(Throwable ex) method).
	 */
	private volatile boolean _errorHandled;

	// Bufferize chunks received during the DNS query
	private List<ByteBuffer> _preDNSBuffer = Collections.synchronizedList(new LinkedList<>());
	// Is a DNS query in progress?
	private DnsListener _dnsListener;
	// Publisher for streamed HTTP2 Request
	private MyPublisher _publisher = null;
	private H2ResponseHandler _activeH2Handler = null;
	private final static ObjectPool _pool = new ObjectPool();

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void recycled() {
		super.recycled();
		_demuxSocket = null;
		_retry = false;
		_outgoingAddress = null;
		_errorHandled = false;
		_preDNSBuffer.clear();
		_dnsListener = null;
		_publisher = null;
	}

	public static HttpPipeline acquire(Utils utils) {
		HttpPipeline pipeline = (HttpPipeline) _pool.acquire(() -> new HttpPipeline(utils));
		return pipeline;
	}

	public HttpPipeline(Utils utils) {
		super(utils);
	}

	public void init(int clientSockId, MuxConnection connection, long cnxId, String remoteIp, boolean secure) {
		super.init(clientSockId, connection, remoteIp);
		this._cnxId = cnxId;
		this._lastAccessTime = System.currentTimeMillis();
		if (HttpPipeline.SOCKET_TIMEOUT > 0)
			_timeoutTask = scheduleTimeout(getSockId(), new ServerTimeout(), SOCKET_TIMEOUT + 1);
	}

	private void schedule(int sockId, ExecutorPolicy policy, Executor serial, Runnable task) {
		PlatformExecutor channelQueue = Utils.getPlatformExecutors().getProcessingThreadPoolExecutor(sockId);
		channelQueue.execute(() -> serial.execute(task), policy);
	}

	private Future<?> scheduleTimeout(int sockId, Runnable task, int sec) {
		PlatformExecutor channelQueue = Utils.getPlatformExecutors().getProcessingThreadPoolExecutor(sockId);
		return _utils.getTimerService().schedule(channelQueue, () -> _serial.execute(task), sec, TimeUnit.SECONDS);
	}

	@Override // DemuxSocket
	public void socketData(byte[] data, int off, int len) {
		_serial.execute(data, off, len, (bytes, offset, length) -> {
			try {
				access();
				handleData(bytes, offset, length, true);
			} catch (HttpParserException e) {
				_logger.warn("Got exception while data parsing", e);
				connection.sendTcpSocketClose(clientSockId);
			}
		});
	}

	@Override // DemuxSocket
	public void socketClosed() {
		_serial.execute(() -> {
			if (_timeoutTask != null)
				_timeoutTask.cancel(false);
			MuxHeaderV0 header = new MuxHeaderV0();
			int flags = Utils.PRE_FILTER_FLAGS | Utils.CLOSED;
			if (isServerClosed())
				flags = Utils.ACK_MASK | Utils.POST_FILTER_FLAGS | Utils.CLOSED;
			header.set(getSessionId(), clientSockId, flags);
			
			// there is a bug here: the muxData won't find the socket id, which has already been removed by our caller method.
			_utils.getAgent().muxData(connection, header, null, 0, 0);
			
			// here, the client socket is closed, and we need to abort any pending request.
			// Normally, it's the above muxData method which calls HttpChannel.handleClientClose -> abortRequst etc ...
			// But the muxData has no effet because the Agent.tcpSocketClosed who is calling us has already removed the channel from 
			// connection socket manager. 
			// so, as a temporary work around: we call abortRequest directly.
			if (! isClosed()) {
				abortRequest();
			}
			
			if (_demuxSocket != null && _demuxSocket.isTunneling()) {
				connection.sendTcpSocketClose(_demuxSocket.getSockId());
			}
			if (_activeH2Handler != null) {
				_activeH2Handler.cancel();
			}
			if (_dnsListener != null) {
				_dnsListener.cancel();
			}
			_pool.release(this);
		});
	}
	
	protected void requestDone() {
		if (_activeH2Handler != null) {
			_activeH2Handler.cancel();
		}
		if (_timeoutTask != null) {
			_timeoutTask.cancel(false);
		}
		super.requestDone();		
	}

	private void handleData(byte[] data, int off, int len, boolean last) throws HttpParserException {
		if (websocket) {
			MuxHeaderV0 header = new MuxHeaderV0();
			header.set(0, clientSockId, Utils.PRE_FILTER_FLAGS | Utils.WEBSOCKET);
			_utils.getAgent().muxData(connection, header, data, off, len);
		} else if (_demuxSocket != null && _demuxSocket.isTunneling()) {
			if (_logger.isDebugEnabled())
				_logger.debug(this + " tunnel proxying cleint data len=" + len);
			connection.sendTcpSocketData(_demuxSocket.getSockId(), data, off, len, true);
		} else {
			MuxHeaderV0 header = new MuxHeaderV0();
			header.set(0, clientSockId, Utils.PRE_FILTER_FLAGS | Utils.DATA);
			_utils.getAgent().muxData(connection, header, data, off, len);
		}
	}

	protected void prepareRequest() throws IOException {
		_retry = false;

		if (request.getProtocol().endsWith("2.0") && _utils.isH2Enabled()) {
			isH2 = true;
		} else {
			isH2 = false;
		}

		String host = request.getProlog().getURL().getHost();
		int port = request.getProlog().getURL().getPort();
		String proxy = null;
		// Proxying ?
		String nextHop = request.getNextHop();
		if (nextHop == HttpRequest.NEXT_HOP_DEFAULT) {
			proxy = _utils.getAgent().getNextHopEvaluator().getNextProxy(host);
		} else {
			if (nextHop != HttpRequest.NEXT_HOP_DIRECT) {
				proxy = nextHop.trim();
			}
		}

		if (request.getProxyMode()) {
			// Via header
			String viaContent = _utils.getAgent().getViaPseudonym();
			if (viaContent == null) {
				viaContent = _utils.getAgent().getConnectionPool().getViaContent(connection, _cnxId);
			}
			if (viaContent != null) {
				// -- Via RFC-2616 14.45: The Via general-header field MUST be used by gateways
				// and proxies ... etc ...
				// Via: 1.0 fred, 1.1 example.com (Apache/1.1)
				String via = request.getHeader(HttpUtils.VIA);
				if (via == null) {
					request.setHeader(HttpUtils.VIA, viaContent.substring(3));
				} else {
					request.setHeader(HttpUtils.VIA, via + viaContent);
				}
			}
			// Proxy-Connection header
			String pxCnx = request.getHeader(HttpUtils.PROXY_CONNECTION);
			if (pxCnx != null) {
				String cnx = request.getHeader(HttpUtils.CONNECTION);
				if (cnx == null) {
					request.setHeader(HttpUtils.CONNECTION, pxCnx);
				} else {
					request.removeHeader(HttpUtils.PROXY_CONNECTION);
				}
			}
		} else {
			if (request.getNextServer().isPresent()) {
				InetSocketAddress addr = request.getNextServer().get();
				request.setProxyMode(false);
				if (!isH2) { // not necessary for http2
					String hostFromReq = new StringBuilder(addr.getHostString()).append(':').append(addr.getPort())
							.toString();
					request.setHttpRequestUrlAuthority(hostFromReq);
					host = request.getProlog().getURL().getHost();
					port = request.getProlog().getURL().getPort();
				} else {
					host = addr.getHostString();
					port = addr.getPort();
				}
				if (_logger.isDebugEnabled())
					_logger.debug("Next server is " + host + ":" + port);
			} else {
				String server = _utils.getAgent().getNextHopEvaluator().getNextServer(host, request.getURL().getPath());
				if (server != null) {
					if (server.startsWith(NextHopEvaluator.SERVER_PREFIX)) {
						request.setProxyMode(false);
						request.setHttpRequestUrlAuthority(server.substring(NextHopEvaluator.SERVER_PREFIX.length()));
						host = request.getProlog().getURL().getHost();
						port = request.getProlog().getURL().getPort();
						if (_logger.isDebugEnabled())
							_logger.debug(
									"Next server is " + (server.substring(NextHopEvaluator.SERVER_PREFIX.length())));
					} else if (server.startsWith(NextHopEvaluator.PROXY_PREFIX)) {
						proxy = server.substring(NextHopEvaluator.PROXY_PREFIX.length());
					}
				}
			}
		}
		if (request.getNextProxy().isPresent()) {
			if (_logger.isDebugEnabled())
				_logger.debug("Proxying to " + request.getNextProxy().get());
			request.setProxyMode(true);
		} else if (proxy != null) {
			int p = proxy.lastIndexOf(':');
			if (p > 1) {
				host = proxy.substring(0, p);
				try {
					port = Integer.valueOf(proxy.substring(p + 1));
				} catch (NumberFormatException e) {
					port = 3128;
				}
			}

			if (_logger.isDebugEnabled())
				_logger.debug("Proxying to " + host + ":" + port);
			request.setProxyMode(true);
		} else {
			request.setProxyMode(false);
		}

		response.removeAttribute(com.nextenso.proxylet.http.HttpResponse.ERROR_REASON_ATTR);

		if (request.getNextProxy().isPresent()) {
			_outgoingAddress = request.getNextProxy().get();
		} else {
			_outgoingAddress = new InetSocketAddress(host, port);
		}
		serverSocket.setRequestMethod(request.getMethod());
	}
	
	private HttpClient getNextH2Client() {
		return _utils.getH2ClientPool().getClient();
	}

	private HttpClient getNextH2ClientProxied(InetSocketAddress addr) {
		return _utils.getH2ClientPool().getClientProxied(addr);
	}

	private void sendHTTP2Request(MyPublisher pub) {
		// Reset the flag used to check if an h2 response is fully parsed.
		super.isH2ResponseFull = false;

		com.alcatel.as.http2.client.api.HttpRequest.Builder http2ReqBuilder;
		isH2 = true;

		// Detect self connection
		ConnectionPool pool = _utils.getAgent().getConnectionPool();
		if (pool.isSameConnection(connection, _cnxId, _outgoingAddress) || pool.isSelfConnection(_outgoingAddress)) {
			_logger.warn("refusing an attempt to connect to ourselves on " + _outgoingAddress);
			proxySocketError(BAD_GW, "Self-connection refused by this proxylet agent");
			return;
		}

		if (pub != null) {
			http2ReqBuilder = _utils.toHttp2Request(request, pub);
		} else {
			http2ReqBuilder = _utils.toHttp2Request(request);
		}
		if (_activeH2Handler != null) {
			_activeH2Handler.cancel();
			_errorHandled = false;
		}

		if (request.getProxyMode()) {
			_outgoingAddress = request.getNextProxy().orElse(_outgoingAddress);
		} else if (request.getNextServer().isPresent()) {
			_outgoingAddress = request.getNextServer().get();
			http2ReqBuilder.destination(_outgoingAddress);
		}

		com.alcatel.as.http2.client.api.HttpRequest http2Req = http2ReqBuilder.build();
		_activeH2Handler = new H2ResponseHandler(); // set the _h2BodyResponseBuffer attributes

		CompletableFuture<HttpResponse<byte[]>> future;

		if (request.getProxyMode()) {
			HttpClient proxyClient = getNextH2ClientProxied(_outgoingAddress);
			future = proxyClient.sendAsync(http2Req, _activeH2Handler);
		} else {
			future = getNextH2Client().sendAsync(http2Req, _activeH2Handler);
		}

		H2ResponseHandler h2bodyHandler = _activeH2Handler;
		future.whenComplete((http2Response, ex) -> {
			if (ex != null) {
				_serial.execute(() -> {
					if (h2bodyHandler.isCancelled()) {
						return;
					}
					proxySocketError(ex);
				});
			}
		});
	}

	@Override
	protected void sendRedirectedRequest() throws Throwable {
		if (isH2) {
			sendHTTP2Request(null);
		} else {
			// HTTP 1.1 handled in HttpSocket
			super.sendRedirectedRequest();
		}
	}

	@Override
	protected void sendOutgoingRequest() throws IOException {
		prepareRequest();
		if (isH2) {
			sendHTTP2Request(null);
		} else {
			request.writeTo(clientSocket.getOutputStream(), request.getProxyMode());
		}

	}

	@Override
	protected void sendOutgoingHeaders() throws IOException {
		prepareRequest();
		if (isH2) {
			_publisher = new MyPublisher();
			sendHTTP2Request(_publisher);
		} else {
			request.writeHeadersTo(clientSocket.getOutputStream(), request.getProxyMode());
		}

	}

	@Override
	public void sendMuxData(MuxHeader hdr, boolean copy, ByteBuffer... buffers) {
		// for now, copy is always false, so we can safely schedule without copying data
		_serial.execute(() -> doSendMuxData(hdr, copy, buffers));
	}

	private void doSendMuxData(MuxHeader hdr, boolean copy, ByteBuffer... buffers) {
		if (_logger.isDebugEnabled())
			_logger.debug(this + " sendMuxData hdr=" + hdr);
		if (_timeoutTask != null) // TODO check
			access();
		int flags = hdr.getFlags();
		switch (flags) {
		case Utils.PRE_FILTER_FLAGS | Utils.CLOSED:
		case Utils.POST_FILTER_FLAGS | Utils.CLOSED:
			connection.sendTcpSocketClose(hdr.getChannelId());
			return;

		case Utils.POST_FILTER_FLAGS | Utils.DATA: // HTTP Response
			connection.sendTcpSocketData(hdr.getChannelId(), copy, buffers);
			return;

		case Utils.PRE_FILTER_FLAGS | Utils.DATA: // HTTP Request (proxy)
			sendMuxDataPre(buffers);
			break;

		case Utils.POST_FILTER_FLAGS | Utils.WEBSOCKET:
			connection.sendTcpSocketData(hdr.getChannelId(), copy, buffers);
			return;

		default:
			if (_logger.isInfoEnabled())
				_logger.info("sendMuxData - dropping data: flags=" + flags, new Throwable());
			break;
		}
	}

	private void sendMuxDataPre(ByteBuffer... buffers) {
		if (connection.isOpened()) {
			if (_demuxSocket == null) {
				// First chunk or more chunk but demuxSocket is not created yet
				sendMuxDataCreateDemuxAndSend(buffers);
			} else {
				// more chunks arrived, demuxSocket was created but we need to check
				// if the socket is ready to be used
				sendMuxDataDemuxMore(buffers);
			}
		}
	}

	private void sendMuxDataDemuxMore(ByteBuffer... buffers) {
		_demuxSocket.access();
		request.clearContent();
		ProxySocketUser proxySocketUser = (ProxySocketUser) _demuxSocket.getUser();
		if (proxySocketUser.isReady()) {
			sendProxyRequestData(_demuxSocket.getSockId(), buffers); // "false" means "error while
																		// writing data _ socket closed"
			return;
		} else {
			// bufferize until socket is ready
			proxySocketUser.putData(buffers);
			return;
		}
	}

	private void sendMuxDataCreateDemuxAndSend(ByteBuffer... buffers) {
		// Detect self connection
		ConnectionPool pool = _utils.getAgent().getConnectionPool();
		if (pool.isSameConnection(connection, _cnxId, _outgoingAddress)
				|| pool.isSelfConnection(_outgoingAddress)) {
			_logger.warn("refusing an attempt to connect to ourselves on " + _outgoingAddress);
			proxySocketError(BAD_GW, MuxUtils.getErrorMessage(MuxUtils.ERROR_CONNECTION_REFUSED));
			// request.clearContent();
			return;
		}
		int port = _outgoingAddress.getPort();
		String host = _outgoingAddress.getHostString();
		if (pool.isHostName(host)) {
			if (_logger.isDebugEnabled())
				_logger.debug("Trying to resolve host=" + host);
			for (ByteBuffer b : buffers) {
				_preDNSBuffer.add(b);
			}
			if (_dnsListener == null) {
				_dnsListener = new DnsListener(_preDNSBuffer);
				DNSHelper.getHostByName(host, _dnsListener);
			}
			return;
		} else {
			sendProxyRequest(host, port, buffers);
			return;
		}
	}

	private boolean sendProxyRequest(String host, int port, ByteBuffer[] buffers) {
		_demuxSocket = _utils.getAgent().getConnectionPool().getProxySocket(connection, _cnxId, host, port, request,
				this, buffers, _retry);
		if (_demuxSocket == null) {
			// MUX was disconnected while handling initial request
			proxySocketError(BAD_GW, MuxUtils.getErrorMessage(MuxUtils.ERROR_CONNECTION_REFUSED));
			// request.clearContent();
			return false;
		}
		return (_demuxSocket != null) ? true : false;
	}

	private void createTunnel(String host, int port) {
		_demuxSocket = _utils.getAgent().getConnectionPool().getTunnelSocket(connection, host, port, this, request);

		if (_demuxSocket == null) {
			StringBuilder error = buildError(BAD_GW, "Bad Gateway");
			connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
		}
	}

	void tunnelSocketConnected() {
		MuxHeaderV0 tunnelHdr = new MuxHeaderV0();
		if (_logger.isDebugEnabled())
			_logger.debug(this + " tunneling from/to " + _demuxSocket);
		tunnelHdr.set(clientSockId, _demuxSocket.getSockId(), Utils.POST_FILTER_FLAGS | Utils.TUNNEL);
		connection.sendMuxData(tunnelHdr, true);

		StringBuilder buf = new StringBuilder(64);
		buf.append(request.getProtocol());
		buf.append(" 200 OK\r\n\r\n");
		connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(buf.toString().getBytes()));
	}

	void tunnelData(byte[] data, int off, int len) {
		if (_logger.isDebugEnabled())
			_logger.debug(this + " tunnel proxying server data len=" + len);
		connection.sendTcpSocketData(clientSockId, data, off, len, true);
	}

	void tunnelSocketClosed() {
		connection.sendTcpSocketClose(clientSockId);
	}

	void tunnelSocketError(int errno) {
		StringBuilder error = buildError(errno, MuxUtils.getErrorMessage(errno));
		connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
	}

	@Override
	protected void sendClose(boolean pre, boolean close) {
		if (_timeoutTask != null)
			_timeoutTask.cancel(false);
		if (_demuxSocket != null) {
			connection.sendTcpSocketClose(_demuxSocket.getSockId()); // close pending proxy socket
		}
		super.sendClose(pre, close);
	}

	private void access() {
		this._lastAccessTime = System.currentTimeMillis();
	}

	public boolean sendProxyRequestData(int sockId, ByteBuffer... buffers) {
		return connection.sendTcpSocketData(sockId, false, buffers);
	}

	private void proxySocketTimeout(DemuxClientSocket socket) {
		if (_demuxSocket != null) { // Pending request
			proxySocketError(MuxUtils.ERROR_TIMEOUT);
		}
		connection.sendTcpSocketClose(socket.getSockId());
	}

	private void proxyResponseData(byte[] data, int off, int len) {
		MuxHeaderV0 header = new MuxHeaderV0();
		header.set(0, clientSockId, Utils.POST_FILTER_FLAGS | Utils.DATA);
		_utils.getAgent().muxData(connection, header, data, off, len);
		if (_requestDone) {
			// request.clearContent();
			_utils.getAgent().getConnectionPool().releaseClientSocket(connection, _cnxId, _demuxSocket);
			_demuxSocket = null;
		}
	}

	private void proxySocketClosed(DemuxClientSocket socket, boolean noResponse) {
		if (_demuxSocket != null) { // Pending request
			if (noResponse)
				proxySocketError(BAD_GW, "Connection Closed");
		}
		_utils.getAgent().getConnectionPool().clientSocketClosed(connection, _cnxId, socket);
	}

	private void proxySocketAborted(DemuxClientSocket socket) {
		if (_logger.isInfoEnabled())
			_logger.info(this + ".proxySocketAborted: retry");
		_retry = true;
		try {
			_demuxSocket = null;
			request.writeTo(clientSocket.getOutputStream(), request.getProxyMode());
		} catch (IOException e) {
			proxySocketError(BAD_GW, "Connection Closed");
		}
	}

	private void proxySocketError(int errno) {
		proxySocketError(errno, MuxUtils.getErrorMessage(errno));
	}

	private void proxySocketError(Throwable ex) { // called in http agent queue
		if (ex instanceof HttpTimeoutException) {
			_utils.getAgent().getAgentMeters().incHttpClientTimeouts();
		} else if (ex instanceof ProtocolException) {
			_utils.getAgent().getAgentMeters().incHttpClientProtocolErrors();
		} else {
			_utils.getAgent().getAgentMeters().incHttpClientExceptions();
		}
		Level logLevel = getErrorLevel(ex);
		if (_logger.isEnabledFor(logLevel)) {
			_logger.log(logLevel, this + " exception raised while handling http2 request", ex);
		}

		if (isH2) {
			if (_errorHandled) {
				_logger.debug(this + " error already handled");
				return;
			}
			_errorHandled = true;

			if (ex != null) {
				response.setAttribute(com.nextenso.proxylet.http.HttpResponse.ERROR_REASON_ATTR, ex);
			}
			if (!_requestDone) {
				response.clearContent();
				response.setHttpResponseStatus(502);
				response.setContent("502 Bad Gateway");
				response.appendContent("\n");

				if (ex != null) {
					response.appendContent(ex.getClass().getName() + ": " + ex.getMessage());
				}
				isH2ResponseFull = true;

				try {
					busy = false;
					handleResponse();
				} catch (Throwable t) {
					handleException(t, false);
				}
			}
		} else {
			// TODO: not possible, this method is always called when using http2 client.
			if (_errorHandled) {
				_logger.debug(this + " error already handled");
				return;
			}
			_errorHandled = true;

			if (ex != null) {
				response.setAttribute(com.nextenso.proxylet.http.HttpResponse.ERROR_REASON_ATTR, ex);
			}
			MuxHeaderV0 header = new MuxHeaderV0();
			header.set(0, clientSockId, Utils.POST_FILTER_FLAGS | Utils.DATA);
			byte[] data = buildError(502, "502 Bad Gateway").toString().getBytes();
			_utils.getAgent().muxData(connection, header, data, 0, data.length);
			_demuxSocket = null;
		}
	}

	private Level getErrorLevel(Throwable err) {
		if (err instanceof HttpTimeoutException || err instanceof ProtocolException
				|| err instanceof PortUnreachableException || err instanceof UnknownHostException
				|| err instanceof ConnectException) {
			return Level.DEBUG;
		} else {
			return Level.WARN;
		}
	}

	private void proxySocketError(int errno, String reason) {
		if (_logger.isDebugEnabled())
			_logger.debug(this + " proxySocketError " + errno);
		response.setAttribute(com.nextenso.proxylet.http.HttpResponse.ERROR_REASON_ATTR, new RuntimeException(reason));
		if (isH2) {
			response.clearContent();
			switch (errno) {
			case MuxUtils.ERROR_TIMEOUT:
				response.setHttpResponseStatus(504);
				response.setContent("504 Gateway Timeout");
				break;

			case BAD_REQUEST:
				response.setHttpResponseStatus(400);
				response.setContent("400 Bad Request");
				break;

			case FORBIDDEN:
				response.setHttpResponseStatus(403);
				response.setContent("403 Forbidden");
				break;

			case NOT_FOUND:
				response.setHttpResponseStatus(404);
				response.setContent("404 Not Found");
				break;

			case INTERNAL_SERVER_ERROR:
				response.setHttpResponseStatus(404);
				response.setContent("500 Internal Server Error");
				break;

			default:
				response.setHttpResponseStatus(502);
				response.setContent("502 Bad Gateway");
				break;
			}
			response.appendContent("\n");
			if (reason != null) {
				response.appendContent(reason);
			}
			response.setProtocol(HttpUtils.HTTP_11);
			response.setContentLength();
			response.setStreaming(false);
			isH2ResponseFull = true;

			try {
				busy = false;
				handleResponse();
			} catch (Throwable t) {
				handleException(t, false);
			}
		} else {
			MuxHeaderV0 header = new MuxHeaderV0();
			header.set(0, clientSockId, Utils.POST_FILTER_FLAGS | Utils.DATA);
			byte[] data = buildError(errno, reason).toString().getBytes();
			_utils.getAgent().muxData(connection, header, data, 0, data.length);
			_demuxSocket = null;
		}
	}

	private StringBuilder buildError(int errno, String reason) {
		StringBuilder buf = new StringBuilder(512);
		buf.append(request.getProtocol());
		switch (errno) {
		case MuxUtils.ERROR_TIMEOUT:
			buf.append(" 504 Gateway Time-out");
			break;

		case BAD_REQUEST:
			buf.append(" 400 Bad Request");
			break;

		case FORBIDDEN:
			buf.append(" 403 Forbidden");
			break;

		case NOT_FOUND:
			buf.append(" 404 Not Found");
			break;

		case INTERNAL_SERVER_ERROR:
			buf.append(" 500 Internal Server Error");
			break;

		default:
			buf.append(" 502 Bad Gateway");
			break;
		}
		buf.append("\r\nContent-Length:     ");
		int clenPos = buf.length() - 4;
		buf.append("\r\nConnection: close\r\nContent-Type: text/html; charset=utf-8\r\n\r\n");
		int start = buf.length();
		buf.append(
				"\r\n<html><body><div style='width: 90%;margin-left: auto;margin-right: auto;'>\r\n<font face='Verdana, Arial, Helvetica, sans-serif'>\r\n<br><hr color='#6950A1' size='4'/><div style='background-color:lightgrey;'><br><br>\r\n<h2 style='text-align:center'>Error: ");
		buf.append(reason);
		buf.append(
				"</h2>\r\n<br><br></div><hr color='#6950A1' size='4'/>\r\n<font size=\"-1\"><p style='text-align:right'><em>Generated by Nokia ASR</em></p></font>\r\n</font>\r\n</div></body></html>\r\n");
		int clen = buf.length() - start;
		String clenTxt = String.valueOf(clen);
		buf.replace(clenPos, clenPos + clenTxt.length(), clenTxt);
		return buf;
	}

	public static void setSocketTimeout(int timeout) {
		SOCKET_TIMEOUT = timeout;
	}

	
	
	@Override
	protected void pushHttp2Body(HttpRequestFacade request, boolean lastPart) {
		if (_logger.isDebugEnabled())
			_logger.debug("Pushing HTTP2 data... lastPart : " + lastPart);

		if (_publisher == null) {
			throw new IllegalStateException("received body chunks for HTTP2 query but the publisher is null");
		}
		byte[] body = request.getBody().getContent(); // TODO: can we avoid to copy content ?
		int size = request.getBody().getSize();
		_publisher.publish(body, 0, size, lastPart);
		request.clearContent();
	}

	protected void handleConnect() {
		if (request.getURL() == null) {
			StringBuilder error = buildError(BAD_REQUEST, "Bad Request");
			connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
		} else {
			String host = request.getURL().getHost();
			int port = request.getURL().getPort();
			boolean allowed = _utils.getAgent().getNextHopEvaluator().isConnectTunneling(host, port);
			if (allowed) {
				if (_logger.isDebugEnabled())
					_logger.debug(this + "CONNECT request");
				_outgoingAddress = new InetSocketAddress(host, port);
				// Detect self connection
				ConnectionPool pool = _utils.getAgent().getConnectionPool();
				if (pool.isSameConnection(connection, getId(), _outgoingAddress)
						|| pool.isSelfConnection(_outgoingAddress)) {
					_logger.warn("refusing an attempt to connect to ourselves on " + _outgoingAddress);
					StringBuilder error = buildError(FORBIDDEN, "Forbidden");
					connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
				} else {
					host = _outgoingAddress.getHostString();
					if (pool.isHostName(host)) {
						if (_logger.isDebugEnabled())
							_logger.debug("Trying to resolve host=" + host);
						DNSHelper.getHostByName(host, new DnsTunnelListener());
					} else {
						createTunnel(host, port);
					}
				}
			} else {
				StringBuilder error = buildError(FORBIDDEN, "Forbidden");
				connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
			}
		}
	}		

	/*****************************************
	 * Inner class that checks Socket Timeout
	 *****************************************/
	private class ServerTimeout implements Runnable {

		@Override
		public void run() {
			if (_timeoutTask.isCancelled() || isClosed() || isH2)
				return;

			long elapsed = (System.currentTimeMillis() - _lastAccessTime) / 1000;
			long remaining = SOCKET_TIMEOUT - elapsed + 1;
			if (elapsed > SOCKET_TIMEOUT || remaining <= 1) {
				if (_logger.isDebugEnabled())
					_logger.debug(HttpPipeline.this + ":timeout response generated");
				proxySocketError(MuxUtils.ERROR_TIMEOUT);
				// MuxHeaderV0 header = new MuxHeaderV0();
				// header.set(sessionId, clientSockId, Utils.POST_FILTER_FLAGS | Utils.CLOSED);
				// // Server Close
				// _utils.getAgent().muxData(connection, header, null, 0, 0);
			} else {
				if (_logger.isDebugEnabled())
					_logger.debug(HttpPipeline.this + ": Re-arming socket timer for " + remaining + " sec");
				_timeoutTask = scheduleTimeout(getSockId(), new ServerTimeout(), (int) remaining);
			}
		}

	}

	private class DnsListener implements Listener<RecordAddress> {
		private final List<ByteBuffer> _buffers;
		private boolean _cancelled;
		private final ReentrantSerialExecutor _queue;

		public DnsListener(List<ByteBuffer> buffers) {
			this._buffers = buffers;
			_queue = _serial; // we store the channel queue in case it's closed and reused while the dns request is running
		}
		
		public void cancel() {
			_cancelled = true;
		}
		
		@Override
		public void requestCompleted(String query, List<RecordAddress> records) {
			_queue.execute(() -> {
				try {
					if (_cancelled) {
						return;
					}
					if (records.isEmpty()) {
						request.clearContent();
						proxySocketError(BAD_GW, _outgoingAddress.getHostString() + " cannot be resolved");
					} else {
						sendProxyRequest(records.get(0).getAddress(), _outgoingAddress.getPort(),
								_buffers.toArray(new ByteBuffer[0]));
					}
				} finally {
					_buffers.clear();
					_dnsListener = null;
				}
			});
		}

	}

	private class H2ResponseHandler implements BodyHandler<byte[]>, BodySubscriber<byte[]> { 
		private boolean _cancelled;
		private final int _sockId;
		private final ReentrantSerialExecutor _queue;
		private Subscription _subscription;

		H2ResponseHandler() {
			_h2BodyResponseBuffer = new LinkedList<ByteBuffer>();
			_sockId = getSockId(); // keep sockid in our instance in case http channel is reused with a new sockid
			_queue = _serial; // keep serial executor in our instance in case http channel is reused with a new sockid
		}
		
		public boolean isCancelled() {
			return _cancelled;
		}

		void cancel() {
			// called from http channel serial queue.
			_cancelled = true;
			Subscription sub = _subscription;
			if (sub != null) {
				sub.cancel();
			}
		}

		@Override
		public BodySubscriber<byte[]> apply(ResponseInfo responseInfo) {
			// called in http client impl thread, we need to redispatch in channel queue
			schedule(_sockId, ExecutorPolicy.SCHEDULE_HIGH, _queue, () -> {
				if (_cancelled) {
					return;
				}

				_utils.getAgent().getAgentMeters().incHttpClientResponses();
				response.setHttpResponseStatus(responseInfo.statusCode());
				response.setStatus(responseInfo.statusCode());
				response.setProtocol(HttpUtils.HTTP_11);
				_logger.debug("HTTP2 Response received");
				if (!buffResp) {
					response.setStreaming(true);
				}

				for (String key : responseInfo.headers().map().keySet()) {
					for (String value : responseInfo.headers().allValues(key)) {
						response.addHeader(key, value);
					}
				}

				state = ST_WAITING_HEADERS_RESP;
				isH2ResponseFull = false;

				try {
					handleResponse();
				} catch (Throwable t) {
					proxySocketError(t);
				}
			});
			return this;
		}

		// BodySubscriber impl

		@Override
		public void onSubscribe(Subscription subscription) {
			// TODO: this method is never called ?
			// called in http client impl thread, we need to redispatch in channel queue
			schedule(_sockId, ExecutorPolicy.SCHEDULE_HIGH, _queue, () -> _subscription = subscription);
		}

		@Override
		public void onNext(List<ByteBuffer> item) {
			// called in http client impl thread, we need to redispatch in channel queue
			schedule(_sockId, ExecutorPolicy.SCHEDULE_HIGH, _queue, () -> {
				try {
					if (!_requestDone && !_cancelled) {
						_h2BodyResponseBuffer.addAll(item);
						handleResponse();
					}
				} catch (Throwable e) {
					proxySocketError(e);
				}
			});
		}

		@Override
		public void onError(Throwable e) {
			// called in http client impl thread, we need to redispatch in channel queue
			schedule(_sockId, ExecutorPolicy.SCHEDULE_HIGH, _queue, () -> {
				if (!_cancelled) {
					proxySocketError(e);
				}
			});
		}

		@Override
		public void onComplete() {
			// called in http client impl thread, we need to redispatch in channel queue
			schedule(_sockId, ExecutorPolicy.SCHEDULE_HIGH, _queue, () -> {
				try {
					if (!_requestDone && !_cancelled) {
						_h2BodyResponseBuffer.add(H2_BUFFER_FINISHED);
						handleResponse();
					}
				} catch (Exception e) {
					proxySocketError(e);
				}
			});
		}

		@Override
		public CompletionStage<byte[]> getBody() {
			return CompletableFuture.completedFuture(new byte[0]);
		}
	}

	private class MyPublisher implements BodyPublisher, Subscription {
		private Subscriber<? super ByteBuffer> sub;
		private final List<ByteBuffer> buffer = new ArrayList<>();
		private boolean over = false;

		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			_serial.execute(() -> {
				if (sub != null) {
					subscriber.onError(new IllegalStateException("Publisher support only a single sub"));
					return;
				}
				sub = subscriber;
				subscriber.onSubscribe(this);
				buffer.forEach((i) -> sub.onNext(i));
				buffer.clear();

				if (over) {
					sub.onComplete();
				}
			});
		}

		/**
		 * Sends data
		 * @param buf a buf which is "given" to us, we can keep it and send later
		 * @param lastChunk
		 */
		public void publish(byte[] data, int off, int len, boolean lastChunk) {
			_serial.execute(() -> {
				if (sub == null) {
					if (!over) {
						buffer.add(ByteBuffer.wrap(data, off, len));
						over = lastChunk;
					}
				} else {
					if (buffer.size() > 0) {
						buffer.forEach((i) -> sub.onNext(i));
						buffer.clear();
					}

					sub.onNext(ByteBuffer.wrap(data, off, len));

					if (over || lastChunk) {
						sub.onComplete();
					}
				}
			});
		}

		@Override
		public long contentLength() {
			return -1;
		}

		@Override
		public void request(long n) {

		}

		@Override
		public void cancel() {
			// TODO ? who may call this method ?
		}
	}

	private class DnsTunnelListener implements Listener<RecordAddress> {
		@Override
		public void requestCompleted(String query, List<RecordAddress> records) {
			if (records.isEmpty()) {
				StringBuilder error = buildError(BAD_GW, "Bad Gateway");
				connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
			} else {
				createTunnel(records.get(0).getAddress(), _outgoingAddress.getPort());
			}
		}
	}

	public ProxySocketUser createProxySocketUser(String requestMethod) {
		return new ProxySocketUser();
	}

	public class ProxySocketUser implements SocketUser {
		private final List<ByteBuffer> data;
		private boolean ready = false;
		private boolean _responseDataReceived;

		public ProxySocketUser() {
			this.data = new ArrayList<>();
		}

		public boolean isReady() {
			return ready;
		}

		// called from http pipeline (protected by serial executor):
		public void putData(ByteBuffer... buffers) {
			for (ByteBuffer b : buffers) {
				data.add(b);
			}
		}

		// called by Agent.handleTcpSocketConnected -> connectionPool.socketConnected
		@Override
		public void connected(int sockId) {
			_serial.execute(() -> {
				sendProxyRequestData(sockId, data.toArray(new ByteBuffer[0])); // TODO false = error while writing data
				data.clear();
				ready = true;
			});
		}

		// called by Agent.muxClosed or Agent.tcpSocketClosed
		@Override
		public void closed(DemuxClientSocket socket, boolean aborted) {
			_serial.execute(() -> {
				if (aborted)
					proxySocketAborted(socket);
				else
					proxySocketClosed(socket, ! _responseDataReceived);
				ready = false;
			});
		}

		@Override
		public void error(int errno) {
			_serial.execute(() -> {
				proxySocketError(errno);
				ready = false;
			});
		}

		@Override
		public void timeout(DemuxClientSocket socket) {
			_serial.execute(() -> proxySocketTimeout(socket));
		}

		/**
		 * Called by Agent.tcpSocketData
		 */
		@Override
		public void dataReceived(byte[] data, int off, int len) {
			_serial.execute(data, off, len, (bytes, offset, length) -> {
				_responseDataReceived = true;
				proxyResponseData(bytes, offset, length);
			});
		}

		@Override
		public boolean isTunneling() {
			return false;
		}
	}

}
