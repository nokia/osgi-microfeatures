package com.nextenso.http.agent.demux;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PortUnreachableException;
import java.net.ProtocolException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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
import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel_lucent.as.service.dns.DNSHelper;
import com.alcatel_lucent.as.service.dns.DNSHelper.Listener;
import com.alcatel_lucent.as.service.dns.RecordAddress;
import com.nextenso.http.agent.Client;
import com.nextenso.http.agent.HttpChannel;
import com.nextenso.http.agent.SessionPolicy;
import com.nextenso.http.agent.SessionPolicy.Policy;
import com.nextenso.http.agent.Utils;
import com.nextenso.http.agent.engine.PushletOutputStream;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.impl.HttpResponseFacade;
import com.nextenso.http.agent.parser.HttpHeaderDescriptor;
import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.http.agent.parser.HttpParserException;
import com.nextenso.http.agent.parser.HttpRequestHandler;
import com.nextenso.http.agent.parser.HttpResponseHandler;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.http.HttpCookie;
import com.nextenso.proxylet.http.HttpRequest;
import com.nextenso.proxylet.http.HttpURL;
import com.nextenso.proxylet.http.HttpUtils;

public class HttpPipeline extends HttpChannel implements DemuxSocket, HttpResponseHandler {

	static int SOCKET_TIMEOUT = 60;

	private long lastAccessTime;
	private long cnxId;
	private Future<?> timeoutTask;
	private HttpRequestParser requestParser;
	private RequestParserHandler requestParserHandler;
	private HttpParser responseParser;
	private int requestParserState;
	private final ReentrantLock lock = new ReentrantLock();
	private final LinkedList<byte[]> queue = new LinkedList<byte[]>();
	private volatile DemuxClientSocket demuxSocket;
	private volatile boolean retry;
	private InetSocketAddress outgoingAddress;
	private int wsConditions; // Upgrade WebSocket?
	private HttpClient http2Client;
	private static ConcurrentMap<InetSocketAddress, HttpClient> proxiedH2Client = new ConcurrentHashMap<>();
	
	/**
	 *  this flag is an attempt to avoid handling multiple times a cascade of h2 client errors
	 *  (see proxySocketError(Throwable ex) method). 
	 */
	private volatile boolean _errorHandled;

	// Bufferize chunks received during the DNS query
	private List<ByteBuffer> preDNSBuffer = Collections.synchronizedList(new LinkedList<>());
	// Is a DNS query in progress?
	private volatile boolean dnsInProgress = false;
	// Publisher for streamed HTTP2 Request
	private volatile MyPublisher publisher = null;
	private volatile H2BodySubscriber activeH2BodyListener = null;

	public HttpPipeline(int clientSockId, MuxConnection connection, long cnxId, Utils utils, String remoteIp,
			boolean secure) {
		super(clientSockId, -1, connection, utils, remoteIp);
		this.cnxId = cnxId;
		this.lastAccessTime = System.currentTimeMillis();
		if (HttpPipeline.SOCKET_TIMEOUT > 0)
			timeoutTask = scheduleTimeout(new ServerTimeout(), SOCKET_TIMEOUT + 1);
		this.requestParser = new HttpRequestParser();
		this.requestParserHandler = new RequestParserHandler(secure);
		this.requestParserState = HttpRequestParser.PARSED;

		http2Client = _utils.getHttp2Client();
	}

	Future<?> scheduleTimeout(Runnable task, int value) {
		return _utils.getTimerService().schedule(_utils.getHttpExecutor(), task, value, TimeUnit.SECONDS);
	}

	@Override // DemuxSocket
	public void socketData(byte[] data, int off, int len) {
		lock.lock();
		try {
			access();
			if (queue.size() > 0) {
				enqueue(data, off, len, true);
			} else {
				handleData(data, off, len, true);
			}
		} catch (HttpParserException e) {
			_logger.warn("Got exception while data parsing", e);
			connection.sendTcpSocketClose(clientSockId);
		} finally {
			lock.unlock();
		}
	}

	@Override // DemuxSocket
	public void socketClosed() {
		if (timeoutTask != null)
			timeoutTask.cancel(false);
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
		
		if (demuxSocket != null && demuxSocket.isTunneling()) {
			connection.sendTcpSocketClose(demuxSocket.getSockId());
		}
	}

	private void handleData(byte[] data, int off, int len, boolean last) throws HttpParserException {
		if (websocket) {
			MuxHeaderV0 header = new MuxHeaderV0();
			header.set(sessionId, clientSockId, Utils.PRE_FILTER_FLAGS | Utils.WEBSOCKET);
			_utils.getAgent().muxData(connection, header, data, off, len);
		} else if (demuxSocket != null && demuxSocket.isTunneling()) {
			if (_logger.isDebugEnabled())
				_logger.debug(this + " tunnel proxying cleint data len=" + len);
			connection.sendTcpSocketData(demuxSocket.getSockId(), data, off, len, true);
		} else {
			boolean parse = true;
			if ((requestParserState == HttpRequestParser.PARSED) && (state != ST_READY_REQ)) {
				parse = false;
			}
			if (parse) {
				requestParserHandler.connectRequest = null;
				ByteArrayInputStream in = new ByteArrayInputStream(data, off, len);
				requestParserState = requestParser.parseRequest(in, requestParserHandler);
				if (requestParserState == HttpRequestParser.PARSED) {
					// Enqueue remaining data
					int available = in.available();
					if (available > 0) {
						enqueue(data, off + len - available, available, last);
						len -= available;
					}
				} else {
					if (requestParserState == HttpRequestParser.READING_BODY) {
						if (requestParserHandler.connectRequest != null) {
							request = requestParserHandler.connectRequest;
							if (request.getURL() == null) {
								StringBuilder error = buildError(BAD_REQUEST, "Bad Request");
								connection.sendTcpSocketData(clientSockId, false,
										ByteBuffer.wrap(error.toString().getBytes()));
							} else {
								String host = requestParserHandler.connectRequest.getURL().getHost();
								int port = requestParserHandler.connectRequest.getURL().getPort();
								boolean allowed = _utils.getAgent().getNextHopEvaluator().isConnectTunneling(host,
										port);
								if (allowed) {
									if (_logger.isDebugEnabled())
										_logger.debug(this + "CONNECT request");
									outgoingAddress = new InetSocketAddress(host, port);
									// Detect self connection
									ConnectionPool pool = _utils.getAgent().getConnectionPool();
									if (pool.isSameConnection(connection, cnxId, outgoingAddress)
											|| pool.isSelfConnection(outgoingAddress)) {
										_logger.warn(
												"refusing an attempt to connect to ourselves on " + outgoingAddress);
										StringBuilder error = buildError(FORBIDDEN, "Forbidden");
										connection.sendTcpSocketData(clientSockId, false,
												ByteBuffer.wrap(error.toString().getBytes()));
									} else {
										host = outgoingAddress.getHostString();
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
									connection.sendTcpSocketData(clientSockId, false,
											ByteBuffer.wrap(error.toString().getBytes()));
								}
							}
							return; // CONNECT method
						}
					}
					if (queue.size() > 0) {
						// read more data
						schedulePoll();
					}
				}

				MuxHeaderV0 header = new MuxHeaderV0();
				header.set(sessionId, clientSockId, Utils.PRE_FILTER_FLAGS | Utils.DATA);
				_utils.getAgent().muxData(connection, header, data, off, len);
			} else {
				enqueue(data, off, len, last);
			}
		}
	}

	private void enqueue(byte[] data, int off, int len, boolean last) {
		if (_logger.isDebugEnabled())
			_logger.debug(this + " enqueue len=" + len);
		byte[] copy = new byte[len];
		System.arraycopy(data, off, copy, 0, len);
		if (last)
			queue.addLast(copy);
		else
			queue.addFirst(copy);
	}

	@Override
	protected void requestDone() {
		lock.lock();
		try {
			super.requestDone();
			if (_logger.isDebugEnabled())
				_logger.debug(this + " requestDone queue=" + queue.size());
			if (queue.size() > 0) {
				schedulePoll();
			}
		} finally {
			lock.unlock();
		}
	}

	private void schedulePoll() {
		_utils.getHttpExecutor().execute(new Runnable() {

			@Override
			public void run() {
				if (!isClosed()) {
					lock.lock();
					try {
						byte[] data = queue.poll();
						if (_logger.isDebugEnabled())
							_logger.debug(this + " dequeue len=" + data.length);
						handleData(data, 0, data.length, false);
					} catch (HttpParserException e) {
						_logger.warn("Got exception while data parsing", e);
						connection.sendTcpSocketClose(clientSockId);
					} finally {
						lock.unlock();
					}
				}
			}
		}, ExecutorPolicy.SCHEDULE);
	}

	protected void prepareRequest() throws IOException {
		retry = false;

		if (request.getProtocol().endsWith("2.0") && http2Client != null) {
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
				viaContent = _utils.getAgent().getConnectionPool().getViaContent(connection, cnxId);
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
			outgoingAddress = request.getNextProxy().get();
		} else {
			outgoingAddress = new InetSocketAddress(host, port);
		}
		serverSocket.setRequestMethod(request.getMethod());
	}

	private void sendHTTP2Request(MyPublisher pub) {
		// Reset the flag used to check if an h2 response is fully parsed.
		super.isH2ResponseFull = false;
		
		com.alcatel.as.http2.client.api.HttpRequest.Builder http2ReqBuilder;
		isH2 = true;

		// Detect self connection
		ConnectionPool pool = _utils.getAgent().getConnectionPool();
		if (pool.isSameConnection(connection, cnxId, outgoingAddress) || pool.isSelfConnection(outgoingAddress)) {
			_logger.warn("refusing an attempt to connect to ourselves on " + outgoingAddress);
			proxySocketError(BAD_GW, "Self-connection refused by this proxylet agent");
			return;
		}

		if (pub != null) {
			http2ReqBuilder = _utils.toHttp2Request(request, pub);
		} else {
			http2ReqBuilder = _utils.toHttp2Request(request);
		}
		if (activeH2BodyListener != null) {
			activeH2BodyListener.cancel();
			_errorHandled = false;
		}

		if (request.getProxyMode()) {
			outgoingAddress = request.getNextProxy().orElse(outgoingAddress);
		} else if (request.getNextServer().isPresent()) {
			outgoingAddress = request.getNextServer().get();
			http2ReqBuilder.destination(outgoingAddress);
		}

		com.alcatel.as.http2.client.api.HttpRequest http2Req = http2ReqBuilder.build();
		BodyHandler<byte[]> handler = new H2BodyHandler(); // set the h2BodyResponseBuffer and activeH2BodyListener attributes

		CompletableFuture<HttpResponse<byte[]>> future;

		if (request.getProxyMode()) {
			HttpClient proxyClient = proxiedH2Client.computeIfAbsent(outgoingAddress, (addr) -> {
				return _utils.newProxiedHttp2Client(addr.getHostString(), addr.getPort());
			});
			future = proxyClient.sendAsync(http2Req, handler);
		} else {
			future = http2Client.sendAsync(http2Req, handler);
		}

		future.whenComplete((http2Response, ex) -> {
			if (ex != null) {
				_utils.getHttpExecutor().execute(() -> proxySocketError(ex), ExecutorPolicy.SCHEDULE_HIGH);
				return;
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
			publisher = new MyPublisher();
			sendHTTP2Request(publisher);
		} else {
			request.writeHeadersTo(clientSocket.getOutputStream(), request.getProxyMode());
		}

	}

	@Override
	public boolean sendMuxData(MuxHeader hdr, boolean copy, ByteBuffer... buffers) {
		lock.lock();
		try {
			return doSendMuxData(hdr, copy, buffers);
		} finally {
			lock.unlock();
		}
	}

	private boolean doSendMuxData(MuxHeader hdr, boolean copy, ByteBuffer... buffers) {
		if (_logger.isDebugEnabled())
			_logger.debug(this + " sendMuxData hdr=" + hdr);
		if (timeoutTask != null)
			access();
		int flags = hdr.getFlags();
		switch (flags) {
		case Utils.PRE_FILTER_FLAGS | Utils.CLOSED:
		case Utils.POST_FILTER_FLAGS | Utils.CLOSED:
			return connection.sendTcpSocketClose(hdr.getChannelId());

		case Utils.POST_FILTER_FLAGS | Utils.DATA: // HTTP Response
			if (responseParser != null) {
				try {
					wsConditions = 0;
					for (ByteBuffer buf : buffers) {
						int res = responseParser.parseResponse(request.getMethod(),
								new ByteArrayInputStream(buf.array(), buf.position(), buf.remaining()), this);
						if (res == HttpParser.PARSED) {
							if (client.isTempClid())
								client.removeClient();
							PushletOutputStream pushletOS = (PushletOutputStream) response
									.getAttribute(HttpResponseFacade.ATTR_PUSHLET_OS);
							if (pushletOS != null && pushletOS.isDirect()) {
								updateRequestCountersAndLog(true);
								client.decPendingRequests();
							}
						}
					}
					if (wsConditions == 3) {
						if (_logger.isDebugEnabled())
							_logger.debug(this + ": switching to websocket");
						switchToWebSocket();
						MuxHeaderV0 wsHdr = new MuxHeaderV0();
						wsHdr.set(hdr.getSessionId(), hdr.getChannelId(), Utils.POST_FILTER_FLAGS | Utils.WEBSOCKET);
						connection.sendMuxData(wsHdr, true);
					}
				} catch (HttpParserException e) {
					_logger.warn(this + " response parsing error", e);
				}
			}
			return connection.sendTcpSocketData(hdr.getChannelId(), copy, buffers);

		case Utils.PRE_FILTER_FLAGS | Utils.DATA: // HTTP Request (proxy)
			if (connection.isOpened()) {
				if (demuxSocket == null) {
					// First chunk or more chunk but demuxSocket is not created yet

					// Detect self connection
					ConnectionPool pool = _utils.getAgent().getConnectionPool();
					if (pool.isSameConnection(connection, cnxId, outgoingAddress)
							|| pool.isSelfConnection(outgoingAddress)) {
						_logger.warn("refusing an attempt to connect to ourselves on " + outgoingAddress);
						proxySocketError(BAD_GW, MuxUtils.getErrorMessage(MuxUtils.ERROR_CONNECTION_REFUSED));
						// request.clearContent();
						return false;
					}
					int port = outgoingAddress.getPort();
					String host = outgoingAddress.getHostString();
					if (pool.isHostName(host)) {
						if (_logger.isDebugEnabled())
							_logger.debug("Trying to resolve host=" + host);
						for (ByteBuffer b : buffers) {
							preDNSBuffer.add(b);
						}
						if (!dnsInProgress) {
							dnsInProgress = true;
							DNSHelper.getHostByName(host, new DnsListener(preDNSBuffer));
						}
						return true;
					} else {
						return sendProxyRequest(host, port, buffers);
					}
				} else {
					// more chunks arrived, demuxSocket was created but we need to check
					// if the socket is ready to be used
					demuxSocket.access();
					request.clearContent();
					ProxySocketUser proxySocketUser = (ProxySocketUser) demuxSocket.getUser();
					if (demuxSocket.isReady()) {
						return sendProxyRequestData(demuxSocket.getSockId(), buffers); // "false" means "error while
																						// writing data _ socket closed"
					} else {
						// bufferize until socket is ready
						proxySocketUser.putData(buffers);
						return true;
					}
				}
			}
			break;

		case Utils.POST_FILTER_FLAGS | Utils.WEBSOCKET:
			return connection.sendTcpSocketData(hdr.getChannelId(), copy, buffers);

		default:
			if (_logger.isInfoEnabled())
				_logger.info("sendMuxData - dropping data: flags=" + flags, new Throwable());
			break;
		}
		return true;
	}

	private boolean sendProxyRequest(String host, int port, ByteBuffer[] buffers) {
		demuxSocket = _utils.getAgent().getConnectionPool().getProxySocket(connection, cnxId, host, port, request, this,
				buffers, retry);
		if (demuxSocket == null) {
			// MUX was disconnected while handling initial request
			proxySocketError(BAD_GW, MuxUtils.getErrorMessage(MuxUtils.ERROR_CONNECTION_REFUSED));
			// request.clearContent();
			return false;
		}
		return (demuxSocket != null) ? true : false;
	}

	private void createTunnel(String host, int port) {
		demuxSocket = _utils.getAgent().getConnectionPool().getTunnelSocket(connection, host, port, this, request);

		if (demuxSocket == null) {
			StringBuilder error = buildError(BAD_GW, "Bad Gateway");
			connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
		}
	}

	void tunnelSocketConnected() {
		MuxHeaderV0 tunnelHdr = new MuxHeaderV0();
		if (_logger.isDebugEnabled())
			_logger.debug(this + " tunneling from/to " + demuxSocket);
		tunnelHdr.set(clientSockId, demuxSocket.getSockId(), Utils.POST_FILTER_FLAGS | Utils.TUNNEL);
		connection.sendMuxData(tunnelHdr, true);

		StringBuilder buf = new StringBuilder(64);
		buf.append(request.getProtocol());
		buf.append(" 200 OK\r\n");
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
	public void sendClose(boolean pre, boolean close) {
		if (timeoutTask != null)
			timeoutTask.cancel(false);
		if (demuxSocket != null) {
			connection.sendTcpSocketClose(demuxSocket.getSockId()); // close pending proxy socket
		}
		super.sendClose(pre, close);
	}

	private void access() {
		this.lastAccessTime = System.currentTimeMillis();
	}

	boolean sendProxyRequestData(int sockId, ByteBuffer... buffers) {
		return connection.sendTcpSocketData(sockId, false, buffers);
	}

	ReentrantLock getLock() {
		return lock;
	}

	void proxySocketTimeout(DemuxClientSocket socket) {
		if (demuxSocket != null) { // Pending request
			proxySocketError(MuxUtils.ERROR_TIMEOUT);
		}
		connection.sendTcpSocketClose(socket.getSockId());
	}

	void proxyResponseData(byte[] data, int off, int len, boolean last) {
		MuxHeaderV0 header = new MuxHeaderV0();
		header.set(sessionId, clientSockId, Utils.POST_FILTER_FLAGS | Utils.DATA);
		_utils.getAgent().muxData(connection, header, data, off, len);
		if (last) {
			// request.clearContent();
			_utils.getAgent().getConnectionPool().releaseClientSocket(connection, cnxId, demuxSocket);
			demuxSocket = null;
		}
	}

	void proxySocketClosed(DemuxClientSocket socket, boolean noResponse) {
		if (demuxSocket != null) { // Pending request
			if (noResponse)
				proxySocketError(BAD_GW, "Connection Closed");
		}
		_utils.getAgent().getConnectionPool().clientSocketClosed(connection, cnxId, socket);
	}

	void proxySocketAborted(DemuxClientSocket socket) {
		if (_logger.isInfoEnabled())
			_logger.info(this + ".proxySocketAborted: retry");
		retry = true;
		try {
			demuxSocket = null;
			request.writeTo(clientSocket.getOutputStream(), request.getProxyMode());
		} catch (IOException e) {
			proxySocketError(BAD_GW, "Connection Closed");
		}
	}

	void proxySocketError(int errno) {
		proxySocketError(errno, MuxUtils.getErrorMessage(errno));
	}

	void proxySocketError(Throwable ex) { // called in http agent queue
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
			synchronized (this) {
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
			}
		} else {
			// TODO: not possible, this method is always called when using http2 client.
			synchronized (this) {
				if (_errorHandled) {
					_logger.debug(this + " error already handled");
					return;
				}
				_errorHandled = true;
			}
			
			if (ex != null) {
				response.setAttribute(com.nextenso.proxylet.http.HttpResponse.ERROR_REASON_ATTR, ex);
			}
			MuxHeaderV0 header = new MuxHeaderV0();
			header.set(sessionId, clientSockId, Utils.POST_FILTER_FLAGS | Utils.DATA);
			byte[] data = buildError(502, "502 Bad Gateway").toString().getBytes();
			_utils.getAgent().muxData(connection, header, data, 0, data.length);
			demuxSocket = null;
		}
	}

	private Level getErrorLevel(Throwable err) {
		if (err instanceof HttpTimeoutException || 
			err instanceof ProtocolException || 
			err instanceof PortUnreachableException ||
			err instanceof UnknownHostException ||
			err instanceof ConnectException) {
			return Level.DEBUG;
		} else {
			return Level.WARN;
		}
	}

	void proxySocketError(int errno, String reason) {
		if (_logger.isDebugEnabled())
			_logger.debug(this + " proxySocketError " + errno);
		response.setAttribute(com.nextenso.proxylet.http.HttpResponse.ERROR_REASON_ATTR, new RuntimeException(reason));
		if (isH2) {
			synchronized (this) {
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
			}
		} else {
			MuxHeaderV0 header = new MuxHeaderV0();
			header.set(sessionId, clientSockId, Utils.POST_FILTER_FLAGS | Utils.DATA);
			byte[] data = buildError(errno, reason).toString().getBytes();
			_utils.getAgent().muxData(connection, header, data, 0, data.length);
			demuxSocket = null;
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
	public void setHttpProtocol(String protocol) {
	}

	@Override
	public void addHttpCookie(HttpCookie cookie) {
		SessionPolicy policy = _utils.getAgent().getSessionPolicy();
		if (policy.getPolicy() == Policy.COOKIE) {
			if (cookie.getName().equals(policy.getName())) {
				String value = cookie.getValue();
				long hash = policy.hash64(value);
				if (hash != client.getId()) {
					Hashtable<Long, Client> clients = _utils.getClients();
					synchronized (clients) {
						clients.remove(client.getId());
						client.switchId(hash);
						clients.put(hash, client);
					}
				}
			}
		}
	}

	@Override
	public void addHttpHeader(String name, String val) {
		if (wsConditions > 0) {
			if ("connection".equalsIgnoreCase(name) && "upgrade".equalsIgnoreCase(val))
				wsConditions++;
			if ("upgrade".equalsIgnoreCase(name) && "websocket".equalsIgnoreCase(val))
				wsConditions++;
		}
	}

	@Override
	protected void pushHttp2Body(HttpRequestFacade request, boolean lastPart) {
		if (_logger.isDebugEnabled()) _logger.debug("Pushing HTTP2 data... lastPart : " + lastPart);
		
		if (publisher == null) {
			throw new IllegalStateException("received body chunks for HTTP2 query but the publisher is null");
		}
		ByteBuffer bb = ByteBuffer.wrap(request.getBody().getContent());
		publisher.publish(bb, lastPart);
		request.clearContent();
	}

	@Override
	public void addHttpHeader(HttpHeaderDescriptor hdrDesc, String val) {
	}

	@Override
	public void addHttpBody(InputStream in, int size) throws IOException {
	}

	@Override
	public void setHttpResponseStatus(int status) {
		if (status == SWITCHING_PROTOCOL)
			wsConditions++;
	}

	@Override
	public void setHttpResponseReason(String reason) {
	}

	/*****************************************
	 * Inner class that checks Socket Timeout
	 *****************************************/
	private class ServerTimeout implements Runnable {

		@Override
		public void run() {
			if (timeoutTask.isCancelled() || isClosed() || isH2)
				return;

			long elapsed = (System.currentTimeMillis() - lastAccessTime) / 1000;
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
				timeoutTask = scheduleTimeout(new ServerTimeout(), (int) remaining);
			}
		}

	}

	private class RequestParserHandler implements HttpRequestHandler {

		HttpRequestFacade connectRequest;
		boolean secure;

		public RequestParserHandler(boolean secure) {
			this.secure = secure;
		}

		@Override
		public void setHttpRequestMethod(String method) {
			if (HttpUtils.METHOD_CONNECT.equals(method)) {
				connectRequest = new HttpRequestFacade();
			}
			responseParser = new HttpParser();
			SessionPolicy policy = _utils.getAgent().getSessionPolicy();
			switch (policy.getPolicy()) {
			case NONE:
				sessionId = ((long) policy.getUID()) << 32;
				break;

			case CLIENT_IP:
				sessionId = policy.hash64(remoteIp);
				break;

			case COOKIE:
				sessionId = policy.getTmpId();
				break;

			case HTTP_HEADER:
				sessionId = policy.getTmpId();
				break;

			default:
				break;
			}
		}

		@Override
		public void setHttpRequestUri(String uri, boolean relativeUrl) throws MalformedURLException {

			if ((connectRequest != null) && !relativeUrl) {
				if (secure) {
					connectRequest.setURL(new HttpURL("https://" + uri));
				} else {
					connectRequest.setURL(new HttpURL("http://" + uri));
				}
			}
			SessionPolicy policy = _utils.getAgent().getSessionPolicy();
			if (policy.getPolicy() == Policy.COOKIE) {
				String name = policy.getName().toLowerCase();
				if (uri.contains(name)) {
					HttpURL url = new HttpURL(uri);
					Object objSessionId = url.getParameterValue(name);
					if ((objSessionId != null) && (objSessionId instanceof String)) {
						sessionId = policy.hash64((String) objSessionId);
					}
				}
			}
		}

		@Override
		public void setHttpProtocol(String protocol) {
			if (connectRequest != null)
				connectRequest.setProtocol(protocol);
		}

		@Override
		public void addHttpCookie(HttpCookie cookie) {
			SessionPolicy policy = _utils.getAgent().getSessionPolicy();
			if (policy.getPolicy() == Policy.COOKIE) {
				if (cookie.getName().equals(policy.getName())) {
					sessionId = policy.hash64(cookie.getValue());
				}
			}
		}

		@Override
		public void addHttpHeader(String name, String value) {
			SessionPolicy policy = _utils.getAgent().getSessionPolicy();
			if (policy.getPolicy() == Policy.HTTP_HEADER) {
				if (name.equals(policy.getName())) {
					sessionId = policy.hash64(value);
				}
			}
		}

		@Override
		public void addHttpHeader(HttpHeaderDescriptor hdrDesc, String val) {
		}

		@Override
		public void addHttpBody(InputStream in, int size) throws IOException {
		}

		@Override
		public void setHttpRequestUrlAuthority(String host) throws MalformedURLException {
		}

	}

	private class DnsListener implements Listener<RecordAddress> {

		private final List<ByteBuffer> buffers;

		public DnsListener(List<ByteBuffer> buffers) {
			this.buffers = buffers;
		}

		@Override
		public void requestCompleted(String query, List<RecordAddress> records) {
			if (records.isEmpty()) {
				proxySocketError(BAD_GW, outgoingAddress.getHostString() + " cannot be resolved");
				request.clearContent();
			} else {
				lock.lock();
				try {
					sendProxyRequest(records.get(0).getAddress(), outgoingAddress.getPort(),
							buffers.toArray(new ByteBuffer[0]));
					buffers.clear();
					dnsInProgress = false;
				} finally {
					lock.unlock();
				}
			}
		}

	}

	private class H2BodyHandler implements BodyHandler<byte[]> {
		H2BodyHandler() {
			h2BodyResponseBuffer = new LinkedList<ByteBuffer>();
			activeH2BodyListener = new H2BodySubscriber(h2BodyResponseBuffer);
		}

		@Override
		public BodySubscriber<byte[]> apply(ResponseInfo responseInfo) {
			_utils.getHttpExecutor().execute(() -> {
				synchronized (HttpPipeline.this) {
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
				}
			}, ExecutorPolicy.INLINE_HIGH);
			return activeH2BodyListener;
		}
	}

	private class H2BodySubscriber implements BodySubscriber<byte[]> {

		private final Queue<ByteBuffer> activeBuf;
		private volatile boolean canceled;
		private volatile Subscription subscription;

		public H2BodySubscriber(Queue<ByteBuffer> buf) {
			activeBuf = buf;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			this.subscription = subscription;
		}

		@Override
		public void onNext(List<ByteBuffer> item) {
			_utils.getHttpExecutor().execute(() -> {
				try {
					synchronized (HttpPipeline.this) {
						if (!_requestDone && !canceled) {
							activeBuf.addAll(item);
							handleResponse();
						}
					}
				} catch (Throwable e) {
					proxySocketError(e);
				}
			}, ExecutorPolicy.INLINE_HIGH);
		}

		@Override
		public void onError(Throwable e) {
			_utils.getHttpExecutor().execute(() -> proxySocketError(e), ExecutorPolicy.INLINE_HIGH);
		}

		@Override
		public void onComplete() {
			_utils.getHttpExecutor().execute(() -> {
				try {
					synchronized (HttpPipeline.this) {
						if (!_requestDone && !canceled) {
							activeBuf.add(H2_BUFFER_FINISHED);
							handleResponse();
						}
					}
				} catch (Exception e) {
					proxySocketError(e);
				}
			}, ExecutorPolicy.INLINE_HIGH);
		}

		@Override
		public CompletionStage<byte[]> getBody() {
			return CompletableFuture.completedFuture(new byte[0]);
		}

		public void cancel() {
			_utils.getHttpExecutor().execute(() -> {
				canceled = true;
				Subscription sub = this.subscription;
				if (sub != null) {
					sub.cancel();
				}
			}, ExecutorPolicy.INLINE_HIGH);
		}

	}

	private class MyPublisher implements BodyPublisher, Subscription {

		private Subscriber<? super ByteBuffer> sub;
		private final List<ByteBuffer> buffer = new ArrayList<>();
		private final SerialExecutor executor = new SerialExecutor(_logger);
		private boolean over = false;

		@Override
		public void subscribe(Subscriber<? super ByteBuffer> subscriber) {
			if (sub != null) {
				subscriber.onError(new IllegalStateException("Publisher support only a single sub"));
				return;
			}
			executor.execute(() -> {
				sub = subscriber;
				subscriber.onSubscribe(this);
				buffer.forEach((i) -> sub.onNext(i));
				buffer.clear();

				if (over) {
					sub.onComplete();
				}
			});
		}

		public void publish(ByteBuffer buf, boolean lastChunk) {
			executor.execute(() -> {
				if (sub == null) {
					if (!over) {
						buffer.add(buf);
						over = lastChunk;
					}
				} else {
					if (buffer.size() > 0) {
						buffer.forEach((i) -> sub.onNext(i));
						buffer.clear();
					}

					sub.onNext(buf);

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

		}
	}

	private class DnsTunnelListener implements Listener<RecordAddress> {

		@Override
		public void requestCompleted(String query, List<RecordAddress> records) {
			if (records.isEmpty()) {
				StringBuilder error = buildError(BAD_GW, "Bad Gateway");
				connection.sendTcpSocketData(clientSockId, false, ByteBuffer.wrap(error.toString().getBytes()));
			} else {
				createTunnel(records.get(0).getAddress(), outgoingAddress.getPort());
			}
		}

	}

}
