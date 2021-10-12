// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.Dictionary;
import java.util.Queue;
import java.util.concurrent.Executor;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.util.config.ConfigConstants;
import com.nextenso.http.agent.client.impl.RedirectClient;
import com.nextenso.http.agent.demux.client.HttpConnectionDemux;
import com.nextenso.http.agent.engine.HttpProxyletChain;
import com.nextenso.http.agent.engine.HttpProxyletContext;
import com.nextenso.http.agent.engine.HttpProxyletEngine;
import com.nextenso.http.agent.engine.PushletOutputStream;
import com.nextenso.http.agent.engine.PushletOutputStreamFactory;
import com.nextenso.http.agent.ext.WebSocketHandler;
import com.nextenso.http.agent.impl.HttpMessageException;
import com.nextenso.http.agent.impl.HttpMessageFacade;
import com.nextenso.http.agent.impl.HttpMessageManager;
import com.nextenso.http.agent.impl.HttpRequestFacade;
import com.nextenso.http.agent.impl.HttpResponseFacade;
import com.nextenso.http.agent.impl.HttpSessionFacade;
import com.nextenso.http.agent.impl.HttpSessionManager;
import com.nextenso.http.agent.parser.HttpParser;
import com.nextenso.http.agent.parser.HttpParserException;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.socket.Socket;
import com.nextenso.mux.socket.SocketManager;
import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.ProxyletException;
import com.nextenso.proxylet.engine.AsyncProxyletManager;
import com.nextenso.proxylet.engine.ProxyletEngineException;
import com.nextenso.proxylet.http.HttpRequestProxylet;
import com.nextenso.proxylet.http.HttpUtils;

import alcatel.tess.hometop.gateways.utils.ByteOutputStream;
import alcatel.tess.hometop.gateways.utils.Charset;
import alcatel.tess.hometop.gateways.utils.Constants;
import alcatel.tess.hometop.gateways.utils.Recyclable;

/**
 * A channel on which we receive http request/response/close messages. Several
 * clients may share the same channel. HttpChannel does not support pipelining:
 * that is: there are never more that one pending http request on a given
 * channel.
 */
public abstract class HttpChannel
		implements AsyncProxyletManager.ProxyletResumer, PushletOutputStreamFactory, Socket, Recyclable {

	/*************************************************
	 * Public methods
	 *************************************************/

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public void recycled() {
		_h2BodyResponseBuffer = null;
		request = null;
		response = null;
		clientSocket.recycled();
		serverSocket.recycled();
	}

	public HttpChannel(Utils utils) {
		_utils = utils;
		this.clientSocket = new HttpSocket();
		this.serverSocket = new HttpSocket();
	}

	public void init(int clientSockId, MuxConnection connection) {
		this.clientSockId = clientSockId;
		this.connection = connection;
		this.state = ST_READY_REQ;
		this.busy = false;
		this.pendingCloseAck = false;
		this.clientSocket.init(true, this);
		this.serverSocket.init(false, this);

		HttpConnectionDemux cnxDemux = connection.attachment();
		if (cnxDemux != null) {
			this.meters = cnxDemux.getMeters();
		} else {
			this.meters = null;
		}

		_requestDone = true;
		mayBlockCode = 0;
		resumeStatus = 0;
		reqNeedsThread = respNeedsThread = clientClosed = serverClosed = closedPerformed = redirected = respPassed = false;
		earlyResponse = EARLY_RESP_NONE;
		remoteIp = null;
		isH2 = false;
		_h2BodyResponseBuffer = null;
		isH2ResponseFull = false;
		websocket = false;
		
		if (_logger.isDebugEnabled()) {
			_logger.debug(this + " created");
		}
	}

	public void init(int clientSockId, MuxConnection connection, String remoteIp) {
		init(clientSockId, connection);
		this.remoteIp = remoteIp;
	}

	public static HttpChannel getChannel(MuxConnection connection, int clientSockId) {
		SocketManager mgr = connection.getSocketManager();
		return (HttpChannel) mgr.getSocket(Socket.TYPE_TCP, clientSockId);
	}

	public static void setAccessLog(Logger accessLog) {
		_accessLog = accessLog;
	}

	public int getId() {
		return clientSockId;
	}

	public long getSessionId() {
		return 0; // FIXME not used anymore
	}

	public void handleResponse(byte[] buf, int off, int len) {
		_serial.execute(buf, off, len, (data, offset, length) -> {
			//client.accessed();
			fillsResponseInput(data, offset, length);
			handleResponse();
		});
	}

	public void handleRespWentThrough() {
		_serial.execute(() -> {
			//client.accessed();
			respPassed = true;
			if (busy)
				return;
			respWentThrough();
		});
	}

	public void handleClientClose() {
		_serial.execute(this::_handleClientClose);
	}

	private void _handleClientClose() {
		HttpRequestFacade request = this.request;

		if (!_requestDone && request != null) {
			if (meters != null) {
				meters.incAbortedRequests();
			}
			if (response.getAttribute(HttpResponseFacade.ATTR_PUSHLET_OS) == null) {
				StringBuilder sb = new StringBuilder();
				sb.append("Request aborted: ");
				if (request.getProlog() != null) {
					sb.append("URL=").append(request.getProlog().getURL());
					sb.append(", ");
				}
				sb.append("count=").append(meters.getAbortedRequests());
				sb.append(", pending=").append(meters.getPendingRequests());
				sb.append(", agent=").append(_systemConfig.get(ConfigConstants.GROUP_NAME)).append("__")
						.append(_systemConfig.get(ConfigConstants.INSTANCE_NAME));
				sb.append(", host=").append(_systemConfig.get(ConfigConstants.HOST_NAME));
				sb.append(", pid=").append(_systemConfig.get(ConfigConstants.INSTANCE_PID));
				_logger.warn(sb.toString());
			} else {
				if (_pushletLogger.isInfoEnabled()) {
					StringBuilder sb = new StringBuilder();
					sb.append("Request aborted: ");
					if (request.getProlog() != null) {
						sb.append("URL=").append(request.getProlog().getURL());
						sb.append(", ");
					}
					sb.append("count=").append(meters.getAbortedRequests());
					sb.append(", agent=").append(_systemConfig.get(ConfigConstants.GROUP_NAME)).append("__")
							.append(_systemConfig.get(ConfigConstants.INSTANCE_NAME));
					sb.append(", host=").append(_systemConfig.get(ConfigConstants.HOST_NAME));
					sb.append(", pid=").append(_systemConfig.get(ConfigConstants.INSTANCE_PID));
					_pushletLogger.info(sb.toString());
				}
			}
		}
		clientClosed = true;
		if (busy) {
			abortRequest();
		} else {
			closeChannel();
		}
	}

	public void handleServerClose() {
		_serial.execute(() -> {
			serverClosed = true;
			if (busy)
				return;
			serverClose(true);
		});
	}

	public void handleAck(int flags) {
		// 3 acks are expected:
		// - we acked a client_close
		// - we acked a server_close (
		// - we sent a client or server because of an exception.
		// Tell to the client that our channel is closed
		_serial.execute(() -> {
			if (!_requestDone)
				requestDone(); // will deregister this channel and will eventually close the client.
		});
	}

	public void sendMuxData(MuxHeader hdr, boolean copy, ByteBuffer... buffers) {
		if (_logger.isDebugEnabled())
			_logger.debug(this + " sendMuxData hdr=" + hdr);
		connection.sendMuxData(hdr, copy, buffers);
	}

	/*************************************************
	 * Private methods
	 *************************************************/

	private void fillsRequestInput(byte[] buff, int off, int len) {
		if (pendingCloseAck)
			return;
		clientSocket.fillsInput(buff, off, len, false);
	}

	private void fillsResponseInput(byte[] buff, int off, int len) {
		if (pendingCloseAck)
			return;
		serverSocket.fillsInput(buff, off, len, false);
	}

	protected void handleException(Throwable t, boolean pre) {
		// Log the exception.

		if (_logger.isEnabledFor(Level.WARN)) {
			StringBuilder text = new StringBuilder();
			if (t instanceof HttpParserException) {
				text.append("A problem occurred while parsing the ");
				if (pre) {
					text.append("request ");
				} else {
					text.append("response ");
				}
			}
			text.append("(clid/sockId/state/request_done/").append(Long.toHexString(getSessionId())).append("/").append(clientSockId)
				.append("/").append(state)
				.append("/").append(_requestDone)
				.append("):");
			
			if (t instanceof ProxyletEngineException) {
				ProxyletEngineException exc = (ProxyletEngineException) t;
				Proxylet pxlet = exc.getProxylet();
				if (pxlet != null) text.append(" Proxylet: ").append(exc.getProxylet().getProxyletInfo());
				switch (exc.getType()) {
				case ProxyletEngineException.ENGINE:
					text.append(" : ").append(exc.getMessage());
					_logger.error(text);
					break;
				case ProxyletEngineException.PROXYLET:
					log(Level.WARN, text, exc.getThrowable());
					break;
				}
			} else if (t instanceof HttpParserException) {
				text.append(" Http Parser exception. ");

				if (_logger.isInfoEnabled()) {
					// log the full stacktrace if we are at least in INFO log level
					dumpBody(pre, text);
					_logger.info(text, t);
				}
			} else {
				log(Level.WARN, text, t);
			}
		}

		// If the exception comes from a request proxylet, then try to send a response
		// in case the pxlet
		// did setup the response.

		if (t instanceof ProxyletEngineException) {
			ProxyletEngineException exc = (ProxyletEngineException) t;
			switch (exc.getType()) {
			case ProxyletEngineException.PROXYLET:
				Throwable cause = exc.getThrowable();
				Proxylet pxlet = exc.getProxylet();
				if (cause != null && cause instanceof ProxyletException && pxlet instanceof HttpRequestProxylet) {
					int status = ((ProxyletException) cause).getStatus();
					if (status != -1 && serverSocket.getBytesSent() == 0) {
						try {
							// We did not send a response: we can sent the provided status, then close the
							// socket.
							_logger.info("Got proxylet exception: trying to send response before closing socket");
							if (request != null) {
								request.getResponse().getProlog().setStatus(status);
								request.getResponse().getHeaders().setHeader(HttpUtils.WARNING, cause.getMessage());
								request.getResponse().getHeaders().setHeader(HttpUtils.CONTENT_TYPE, "text/plain");
								request.getResponse().getHeaders().setHeader(HttpUtils.CONNECTION, "close");
								request.getResponse().getBody().setContent(cause.getMessage().getBytes("UTF-8"), false);
								response.writeTo(serverSocket.getOutputStream(), true);
								sendClose(pre, false /* don't close */);
								// pendingCloseAck is now set to true and handleRequest will ignore any further
								// request until
								// we get the close ACK.
								return;
							}
						} catch (IOException e) {
							_logger.warn("Could not send proxylet response", e);
						}
					}
				}
				break;
			}
		}

		// Don't remove the Command
		// send a close_error that will be acknowledged
		// set an unidentified state so abort will be called in close()
		//
		// send a close_error to the stack and tell to the client that we are done with
		// a
		// pending request.
		state = -1;
		sendClose(pre, true /* close */);
	}

	private void log(Level level, StringBuilder text, Throwable err) {
		if (err instanceof HttpMessageException) {
			if (_logger.isInfoEnabled()) {
				_logger.log(level, text, err);
			} else {
				text.append(": ").append(err.getMessage());
				_logger.log(level, text);
			}
		} else {
			_logger.log(level, text, err);
		}
	}

	private StringBuilder dumpBody(boolean pre, StringBuilder sb) {
		byte[] data = (pre) ? clientSocket.getInternalBuffer() : serverSocket.getInternalBuffer();
		if (data != null) {
			sb.append(System.getProperty("line.separator"));
			try {
				sb.append(new String(data, 0, Math.min(data.length, 80), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
			}
		}
		return sb;
	}

	protected abstract void sendOutgoingRequest() throws IOException;

	protected abstract void sendOutgoingHeaders() throws IOException;

	protected void requestDone() {
		// refresh session if found, and possibly register a request session with Set-Cookie header
		if (response.getSession(false) != null || request.getSession(false) != null) {
			// if a Set-Cookie header is found from response, the session may be updated with new session id
			HttpSessionManager.instance().getSession(false, response);
		}
		_requestDone = true;
		Socket channel = null;
		channel = connection.getSocketManager().getSocket(Socket.TYPE_TCP, clientSockId);
		boolean activeChannel = (channel == null) ? false : true;
		updateRequestCountersAndLog(activeChannel);
		HttpMessageManager.release(request); 
		request = null;
		response = null;
	}

	protected void updateRequestCountersAndLog(boolean activeChannel) {
		if (activeChannel && meters != null) {
			meters.decPendingRequests();
			if (!clientClosed) { // if not aborted (see handleClientClose method)
				meters.incProcessedRequests();
			}
		}
		if (_accessLog.isInfoEnabled()) {
			_accessLog.info(response);
		}
	}

	protected void sendClose(boolean pre, boolean close) {
		pendingCloseAck = true;
		if (close) {
			closeChannel();
		}
		_utils.getAgent().sendClose(connection, clientSockId);
	}

	private boolean isSuspended() {
		switch (state) {
		case ST_RESUME_HDRS_REQ:
		case ST_RESUME_MSG_REQ:
		case ST_RESUME_HDRS_RESP:
		case ST_RESUME_MSG_RESP:
			return true;

		default:
			return false;
		}
	}

	private void serverClose(boolean mainThread) {
		if (redirected)
			return;
		if (serverSocket.hasAvailableInput() || serverSocket.needsInput()) {
			serverSocket.fillsInput(null, 0, 0, true);
			if (mainThread) {
				handleResponse();
				if (busy)
					return;
			} else {
				handleResponse(true);
			}
			if (redirected)
				return;
		}
		if (closedPerformed)
			// a close_error was sent
			return;
		sendClose(false /* post close */, true /* close */);
	}

	private void closeChannel() {
		if (closedPerformed)
			return;
		if (!pendingCloseAck) {
			// we received a client_close
			sendClose(true /* pre close */, false /* don't close */);
		}
		// we release the request
		abortRequest();
		reqNeedsThread = respNeedsThread = false;
		closedPerformed = true;
	}

	private void respWentThrough() {
		if (state == ST_READY_RESP) {
			// the response passed by - we can reset
			respPassed = false;
			state = ST_READY_REQ;
			reqNeedsThread = respNeedsThread = false;
			requestDone();
		}
	}

	protected void abortRequest() {
		HttpRequestFacade request = this.request;
		if (!_requestDone && request != null) {
			request.aborted();
		}
	}

	/*************************************************
	 * Request Handling
	 *************************************************/

	public void handleRequest(byte[] data, int off, int len) {
		_serial.execute(data, off, len, (bytes, offset, length) -> {
			fillsRequestInput(bytes, offset, length);
			_handleRequest();
		});
	}

	private void _handleRequest() {
		if (_logger.isDebugEnabled()) {
			_logger.debug(this + ".handleRequest()");
		}
		if (websocket) {
			_logger.error(this + ".handleRequest() on websocket channel!");
			return;
		}
		if (busy) {
			// we let the thread take it
			return;
		}
		if (!clientSocket.hasAvailableInput())
			// the thread already handled the data
			return;
		if (reqNeedsThread) { // a blocking stream proxylet(not currently running) needs a new
								// thread
			busy = true;
			try {
				scheduleProxylets();
			} catch (Exception e) {
				handleException(e, true);
			}
		} else {
			handleRequest(false);
			if (respPassed && !busy /*
									 * if busy is true: it means we have been suspended in handleRequest()
									 */)
				respWentThrough();
		}
	}

	private void handleRequest(boolean ignoreMayBlock) {
		if (pendingCloseAck) {
			return;
		}
		switch (state) {
		case ST_READY_REQ:
			if (earlyResponse != EARLY_RESP_NONE) {
				switch (earlyResponse) {
				case EARLY_RESP_WAITING_BODY_REQ:
					handleBodyRequest();
					return;
				case EARLY_RESP_REDIRECT_REQ:
					handleIgnorableRequest();
					return;
				}
			}
			request = HttpMessageManager.makeRequest(remoteIp);
			response = (HttpResponseFacade) request.getResponse();

			if (meters != null) {
				meters.incPendingRequests();
			}
			PushletOutputStream.setFactory(this, response);
		case ST_WAITING_HEADERS_REQ:
			handleNewRequest(ignoreMayBlock);
			break;
		case ST_WAITING_BODY_REQ:
		case ST_RESPOND_REQ:
			handleBodyRequest();
			break;
		case ST_REDIRECT_REQ:
			handleIgnorableRequest();
			break;
		case ST_BUFFER_REQ:
			handleBufferedRequest(ignoreMayBlock);
			break;
		case ST_WAITING_HEADERS_RESP:
		case ST_WAITING_BODY_RESP:
		case ST_RESPOND_RESP:
		case ST_BUFFER_RESP:
			switch (earlyResponse) {
			case EARLY_RESP_WAITING_BODY_REQ:
				handleBodyRequest();
				return;
			case EARLY_RESP_REDIRECT_REQ:
				handleIgnorableRequest();
				return;
			}
		default:
			handleException(new IllegalStateException("Illegal State for Request: " + state), true);
		}
	}

	private void handleNewRequest(boolean ignoreMayBlock) {
		try {
			boolean full = false;
			switch (clientSocket.readRequest(request)) {
			case HttpParser.READING_HEADERS:
				state = ST_WAITING_HEADERS_REQ;
				return;
			case HttpParser.READING_BODY:
				full = false;
				break;
			case HttpParser.PARSED:
				full = true;
				break;
			}
			if (request.getProtocol().endsWith("2.0")) {
				isH2 = true;
			}
			_requestDone = false;
			
		    _utils.getContainer().init(request);
		    		    
		    // check CONNECT (tunnel request)
			if (request.getProlog().getMethod().equals(HttpUtils.METHOD_CONNECT)) {
				handleConnect();
				return;
			}
			
			// try to lookup an existing session, keep-alive it, and store it in the request
			HttpSessionFacade session = HttpSessionManager.instance().getSession(false, request);
			request.setSession(session);
			
			if (full) {
				if (_accessLog.isInfoEnabled()) {
					_accessLog.info(request);
				}
				processRequest(_utils.getEngine().handleRequest(request, ignoreMayBlock));
			} else {
				if (buffReq)
					state = ST_WAITING_HEADERS_REQ;
				else {
					if (_accessLog.isInfoEnabled()) {
						_accessLog.info(request);
					}
					processRequestHeaders(_utils.getEngine().handleHeaders(request, ignoreMayBlock));
				}
			}
		} catch (Throwable t) {
			handleException(t, true);
		}
	}

	protected abstract void handleConnect();

	private void handleBodyRequest() {
		if (_logger.isDebugEnabled())
			_logger.debug(this + ".handleBodyRequest()");
		try {
			boolean lastPart = false;
			switch (clientSocket.readRequest(request)) {
			case HttpParser.READING_HEADERS:
				handleException(new IllegalStateException("Unexpected state while parsing request"), true);
				return;
			case HttpParser.READING_BODY:
				// we bufferize to avoid small packets
				// Support https
				// no buffering for https
				if (!request.getMethod().equals(HttpUtils.METHOD_CONNECT)) {
					if (waitForMoreBody(request, clientSocket.getRemainingBytes()))
						return;
				}
				lastPart = false;
				break;
			case HttpParser.PARSED:
				lastPart = true;
				break;
			}
			if (_logger.isDebugEnabled()) {
				_logger.debug("request pxlet size=" + request.getProxyletsSize());
			}
			// handleBody WILL return BODY
			if (request.getProxyletsSize() > 0)
				_utils.getEngine().handleBody(request, lastPart, true);

			// Don't send the body back to the stack if we have previously sent a response
			// from a request pxlet.
			boolean sendBody = (state != ST_RESPOND_REQ);
			if (lastPart) {
				if (state == ST_RESPOND_REQ) { 
					// a request proxylet has responded to the request
					// we can now send the response since we have received the last chunk.
					checkWebSocketUpgrade();
					response.writeTo(serverSocket.getOutputStream(), true);
					requestDone();
				}

				if (earlyResponse == EARLY_RESP_NONE)
					state = ST_READY_RESP;
				else
					earlyResponse = EARLY_RESP_NONE;
				reqNeedsThread = false;
			}

			if (sendBody) {
				if (_logger.isDebugEnabled()) {
					_logger.debug("sending body request: lastPart=" + lastPart + ", state= " + state);
				}
				if (HttpUtils.HTTP_20.equals(request.getProtocol())) {
					pushHttp2Body(request, lastPart);
				} else {
					request.writeBodyTo(clientSocket.getOutputStream(), lastPart);
				}
			}
			request.clearContent();
		} catch (Throwable t) {
			handleException(t, true);
		}
	}

	protected void pushHttp2Body(HttpRequestFacade request2, boolean lastPart) {
		throw new UnsupportedOperationException("HTTP2 Handling is implemented in HttpPipeline only");
	}

	private void handleIgnorableRequest() {
		try {
			if (!clientSocket.flushRequestBody())
				return;
			switch (state) {
			case ST_REDIRECT_REQ:
				state = ST_READY_RESP;
				return;
			case ST_RESPOND_REQ:
				state = ST_READY_REQ;
				return;
			default:
				// an early response was received
				earlyResponse = EARLY_RESP_NONE;
				return;
			}
		} catch (Throwable t) {
			handleException(t, true);
		}
	}

	private void handleBufferedRequest(boolean ignoreMayBlock) {
		try {
			switch (clientSocket.readRequest(request)) {
			case HttpParser.READING_HEADERS:
				handleException(new IllegalStateException("Unexpected state while parsing request"), true);
				return;
			case HttpParser.READING_BODY:
				return;
			case HttpParser.PARSED:
				break;
			}
			processRequest(_utils.getEngine().handleBody(request, true, ignoreMayBlock));
		} catch (Throwable t) {
			handleException(t, true);
		}
	}

	private boolean handleRequestResumed(boolean ignoreMayBlock) {
		try {
			int nextStatus = _utils.getEngine().handleRequestResumed(request, resumeStatus);

			if (_logger.isDebugEnabled()) {
				_logger.debug(this + ".handleRequestResumed(resumeStatus=" + resumeStatus + ", nextEngineStatus="
						+ nextStatus + ")");
			}

			switch (nextStatus) {
			case HttpProxyletEngine.REQUEST:
				switch (state) {
				case ST_RESUME_HDRS_REQ:
					return processRequestHeaders(_utils.getEngine().handleHeaders(request, ignoreMayBlock));

				case ST_RESUME_MSG_REQ:
					return processRequest(_utils.getEngine().handleRequest(request, ignoreMayBlock));

				default:
					throw new IllegalStateException("state does not reflect a suspended state:" + state);
				}

			case HttpProxyletEngine.RESPONSE:
				switch (state) {
				case ST_RESUME_HDRS_REQ:
				case ST_RESUME_MSG_REQ:
					int status = _utils.getEngine().handleResponse(response, ignoreMayBlock);
					if (status == HttpProxyletEngine.RESPONSE) {
						state = ST_READY_REQ;
						respNeedsThread = false;
						checkWebSocketUpgrade();
						response.writeTo(serverSocket.getOutputStream(), true);
						requestDone();
						return true;
					} else {
						return processResponse(status, ignoreMayBlock);
					}

				default:
					throw new IllegalStateException("state does not reflect a suspended state:" + state);
				}

			default:
				throw new IllegalStateException(
						"method engine.handleRequestResumed returned unexpected code: " + nextStatus);
			}
		}

		catch (Exception e) {
			_logger.error("could not resume proxylet", e);
			return false;
		}
	}

	private boolean handleResponseResumed(boolean ignoreMayBlock) {
		try {
			int nextStatus = _utils.getEngine().handleResponseResumed(response, resumeStatus);

			if (_logger.isDebugEnabled())
				_logger.debug(this + ".processResponseResumed(resumedStatus=" + resumeStatus + ", nextEngineStatus="
						+ nextStatus + ")");

			switch (nextStatus) {
			case HttpProxyletEngine.RESPONSE:
				switch (state) {
				case ST_RESUME_HDRS_RESP:
					return processResponseHeaders(_utils.getEngine().handleHeaders(response, ignoreMayBlock),
							ignoreMayBlock);
				case ST_RESUME_MSG_RESP:
					return processResponse(_utils.getEngine().handleResponse(response, ignoreMayBlock), ignoreMayBlock);

				default:
					throw new IllegalStateException("state does not reflect a suspended state:" + state);
				}

			case HttpProxyletEngine.REQUEST:
				state = ST_REDIRECT_RESP;
				respNeedsThread = false;
				redirect(false);
				return true;
			}

			throw new IllegalArgumentException("Invalid response resume status: " + resumeStatus);
		}

		catch (Exception e) {
			_logger.error("could not resume proxylet", e);
			return false;
		}
	}

	// Return true if the request has been fully processed by ALL proxylet within
	// the request
	// chain, false if
	// not(I.e: a proxylet returned SUSPEND, or a thread has been started in order
	// to process a
	// blocking proxylet).
	private boolean processRequest(int status) throws Exception {
		if (_logger.isDebugEnabled())
			_logger.debug(this + ".processRequest(" + status + ")");

		switch (status) {
		case HttpProxyletEngine.SUSPEND_REQUEST:
			busy = true;
			state = ST_RESUME_MSG_REQ;
			AsyncProxyletManager.suspend(request, this);
			return false;
		case HttpProxyletEngine.SUSPEND_RESPONSE:
			busy = true;
			state = ST_RESUME_MSG_RESP;
			AsyncProxyletManager.suspend(response, this);
			return false;
		case HttpProxyletEngine.REQUEST: // send the request
			state = ST_READY_RESP;
			reqNeedsThread = false;
			sendOutgoingRequest();
			return true;
		case HttpProxyletEngine.PUSHLET_RESPOND_FIRST:
			// Fall through
		case HttpProxyletEngine.PUSHLET_RESPOND_LAST:
			processPushletResponse(status);
			return true;
		case HttpProxyletEngine.RESPONSE: // send the response
			state = ST_READY_REQ;
			reqNeedsThread = false;
			checkWebSocketUpgrade();
			response.writeTo(serverSocket.getOutputStream(), true);
			requestDone();
			return true;
		case HttpProxyletEngine.MAY_BLOCK_REQUEST:
			state = ST_MAYBLOCK_MSG_REQ;
			mayBlockCode = status;
			busy = true;
			scheduleProxylets();
			return false;
		case HttpProxyletEngine.MAY_BLOCK_RESPONSE:
			state = ST_MAYBLOCK_MSG_RESP;
			mayBlockCode = status;
			busy = true;
			scheduleProxylets();
			return false;
		}

		return false;
	}

	/**
	 * Process a pushlet response status code (PUSHLET_RESPOND_FIRST or
	 * PUSHLET_RESPOND_LAST). This method implements the following logic:
	 * 
	 * if(the pushlet returns RESPOND_FIRST) { if(pxlet response chain is not empty
	 * || access logs are enabled) { Submit the puhslet response to the pxlet
	 * response chain: this will invoke the "requestDone" method which will log
	 * access. } else { Send the pushlet response directly to the io handler } }
	 * else { // Don't run the response chain if (access logs are disabled) { Send
	 * pushlet response directly to the io handler } else { // we must log access
	 * Parse the pushlet response, but make sure that the response chain won't be
	 * executed (parsing the pushlet response will invoke the "requestDone" method,
	 * which will log access). } }
	 * 
	 * @param pushletStatus RESPOND_FIRST if the pushlet response must be submitted
	 *                      on the resp chain, or RESPOND_LAST if the pushlet don't
	 *                      want to apply its response on the resp chain.
	 * @throws IOException on any exception.
	 */
	private void processPushletResponse(int pushletStatus) throws IOException {
		// Must the pushlet response be handled by the resp chain ?
		boolean sendToClientDirectly = true;
		// Must the pushlet response be parsed without submitting it to the resp chain ?
		boolean parseResponseOnly = false;

		// Does the pushlet response need to be handled by the response chain ?
		if (pushletStatus == HttpProxyletEngine.PUSHLET_RESPOND_FIRST) {
			// Submit the pushlet response to the pxlet resp chain if the resp chain is
			// *NOT* empty or if the access logs are enabled.
			int respChainSize = ((HttpProxyletContext) _utils.getEngine().getProxyletContainer().getContext())
					.getResponseChain().getSize();
			if (respChainSize > 0 || _accessLog.isInfoEnabled()) {
				sendToClientDirectly = false;
			}
		} else {
			// The pushlet does not allow its response to be handled by the resp chain.
			// But if the access must be logged, then we still must parse the pushlet
			// response,
			// because the "requestDone" method is called while parsing the response, and it
			// will then log access. However, we must not submit the pushlet resp to the
			// pxlet
			// resp chain.
			if (_accessLog.isInfoEnabled()) {
				// Make sure the pxlet resp chain is not invoked while parsing the pushlet
				// response.
				parseResponseOnly = true;
				sendToClientDirectly = false;
			}
		}

		// Now, activate the pushlet output stream (we'll have to configure it either to
		// send
		// the bytes directly to the client, or to submit (and parse) the response to
		// our pxlet
		// response chain.
		PushletOutputStream pushletOS = (PushletOutputStream) response.getAttribute(HttpResponseFacade.ATTR_PUSHLET_OS);

		if (sendToClientDirectly) {
			state = ST_READY_REQ;
			reqNeedsThread = false;
			pushletOS.activate(false); // send pushlet response directly to the client.
		} else {
			state = parseResponseOnly ? ST_READY_RESP_PARSE_ONLY : ST_READY_RESP;
			reqNeedsThread = false;
			serverSocket.setRequestMethod(request.getMethod());
			// Don' buffer pushlet response chunk, while parsing response.
			request.setAttribute(ATTR_DONT_BUFFER_CHUNKS, Boolean.TRUE);
			response.setAttribute(ATTR_DONT_BUFFER_CHUNKS, Boolean.TRUE);
			pushletOS.activate(true); // pushlet response will be submitted on resp chain
		}
	}

	// return true if the whole chain processed the request header, of false if
	// not(I.e: if the
	// chain was
	// suspended or if the processing has been delagated to another thread(for
	// blocking
	// proxylets).
	private boolean processRequestHeaders(int status) throws Exception {
		if (_logger.isDebugEnabled())
			_logger.debug(this + ".processRequestHeaders(" + status + ")");

		switch (status) {
		case HttpProxyletEngine.SUSPEND_REQUEST:
			busy = true;
			state = ST_RESUME_HDRS_REQ;
			AsyncProxyletManager.suspend(request, this);
			return false;
		case HttpProxyletEngine.SUSPEND_RESPONSE:
			busy = true;
			state = ST_RESUME_HDRS_RESP;
			AsyncProxyletManager.suspend(response, this);
			return false;
		case HttpProxyletEngine.REQUEST:// send the request
			state = ST_REDIRECT_REQ;
			reqNeedsThread = false;
			sendOutgoingRequest();
			return true;
		case HttpProxyletEngine.RESPONSE: // send the response
			// A pxlet is responding to a request which is being received: we'll buffer the
			// pxlet response until we
			// the initial request is fully received.
			state = ST_RESPOND_REQ;
			// If reqNeedsThread is true, we leave it to true, in order to handle further
			// request body parts in the tpool
			return true;
		case HttpProxyletEngine.HEADERS:// send the headers
			state = ST_WAITING_BODY_REQ;
			sendOutgoingHeaders();
			if (!waitForMoreBody(request, clientSocket.getRemainingBytes())) {
				// handleBody WILL return BODY
				_utils.getEngine().handleBody(request, false, true);
				// send the body
				request.writeBodyTo(clientSocket.getOutputStream(), false);
				request.clearContent();
			}
			return true;
		case HttpProxyletEngine.BUFFER:
			state = ST_BUFFER_REQ;
			return false;
		case HttpProxyletEngine.MAY_BLOCK_HEADERS:
		case HttpProxyletEngine.MAY_BLOCK_REQUEST:
		case HttpProxyletEngine.MAY_BLOCK_RESPONSE:
			state = ST_MAYBLOCK_HDRS_REQ;
			mayBlockCode = status;
			busy = true;
			reqNeedsThread = true;
			scheduleProxylets();
			return false;
		}

		return true;
	}
	
	private void checkWebSocketUpgrade() {
		// check Web Socket upgrade, and request a web socket tunneling if so.
		if (! websocket && response.getProlog().getStatus() == 101 /* SWITCHING PROTOCOL */) {
			String connectionHdr = response.getHeader("Connection");
			if ("Upgrade".equalsIgnoreCase(connectionHdr)) {
				String upgradeHdr = response.getHeader("Upgrade");
				if ("WebSocket".equalsIgnoreCase(upgradeHdr)) {
					if (_logger.isDebugEnabled())
						_logger.debug(this + ": switching to websocket");
					switchToWebSocket();
					MuxHeaderV0 wsHdr = new MuxHeaderV0();
					wsHdr.set(0, getId(), Utils.POST_FILTER_FLAGS | Utils.WEBSOCKET);
					connection.sendMuxData(wsHdr, true);
				}					
			}
		}
	}

	/*************************************************
	 * Response Handling
	 *************************************************/

	private void scheduleProxylets() {
		int myClientSockId = this.clientSockId;			
		Executor serial = _serial;
		Utils.getThreadPool().execute(() -> {
			serial.execute(() -> {
				// we need to re check if the client socket is still connected, maybe it has already been closed
				// while our task was scheduled in the io treadpool.
				if (connection.getSocketManager().getSocket(Socket.TYPE_TCP, myClientSockId) != null) {
					run();
				} else {
					if (_logger.isDebugEnabled()) {
						_logger.debug("ignoring requests/responses: socket closed on sockid " + myClientSockId); // TODO log in debug
					}
					busy = false;
				}
			});
		});
	}

	protected void handleResponse() {
		if (busy) // we let the thread take it
			return;
		if (!isH2 && !serverSocket.hasAvailableInput() && !serverClosed) {
			// the thread already handled the data
			return;
		}
		if (respNeedsThread) { // we need a new thread
			busy = true;
			try {								
				scheduleProxylets();
			} catch (Exception e) {
				handleException(e, false);
			}
		} else {
			handleResponse(false);
		}
	}

	protected void handleResponse(boolean ignoreMayBlock) {
		if (pendingCloseAck)
			return;
		switch (state) {
		case ST_WAITING_BODY_REQ:
		case ST_REDIRECT_REQ:
			earlyResponse = (state == ST_WAITING_BODY_REQ) ? EARLY_RESP_WAITING_BODY_REQ : EARLY_RESP_REDIRECT_REQ;
		case ST_READY_RESP:
		case ST_WAITING_HEADERS_RESP:
		case ST_REDIRECT_RESP:
			handleNewResponse(ignoreMayBlock);
			break;
		case ST_WAITING_BODY_RESP:
			handleBodyResponse();
			break;
		case ST_RESPOND_RESP:
			handleIgnorableResponse();
			break;
		case ST_BUFFER_RESP:
			handleBufferedResponse(ignoreMayBlock);
			break;
		case ST_READY_RESP_PARSE_ONLY:
			handleResponseParseOnly();
			break;
		default:
			handleException(new IllegalStateException("Illegal State for Response: " + state), false);
		}
	}

	/**
	 * Handle a response, but only parse it, without submitting it to the response
	 * chain. This method is called when we are in the ST_READY_RESP_PARSE_ONLY,
	 * which corresponds to the state when a pushlet response has to be parsed for
	 * logging access, but must not be submitted to the response chain.
	 */
	private void handleResponseParseOnly() {
		try {
			boolean full = false;

			if (isH2) {
				full = drainHTTP2ResponseBodyBuffer();
			} else {
				switch (serverSocket.readResponse(response)) {
				case HttpParser.READING_HEADERS:
					// wait for more date to get fully parsed headers
					return;
				case HttpParser.READING_BODY:
					// ok: we'll send these response bytes to the io handler
					break;
				case HttpParser.PARSED:
					full = true;
					break;
				}
			}

			// Send the response (either the full headers or hdrs+body, or body)
			checkWebSocketUpgrade();
			response.writeTo(serverSocket.getOutputStream(), true);

			if (full) {
				requestDone();
				state = ST_READY_REQ;
			}
		}

		catch (Throwable t) {
			handleException(t, false);
		}
	}

	private void handleNewResponse(boolean ignoreMayBlock) {
		try {
			boolean full = false;
			if (!isH2) {
				switch (serverSocket.readResponse(response)) {
				case HttpParser.READING_HEADERS:
					state = ST_WAITING_HEADERS_RESP;
					return;
				case HttpParser.READING_BODY:
					full = false;
					break;
				case HttpParser.PARSED:
					full = true;
					break;
				}
			} else {
				full = drainHTTP2ResponseBodyBuffer();
			}

			if (_logger.isDebugEnabled()) _logger.debug("response FULL ? " + full);
			
			if (full)
				processResponse(_utils.getEngine().handleResponse(response, ignoreMayBlock), ignoreMayBlock);
			else {
				if (buffResp)
					state = ST_WAITING_HEADERS_RESP;
				else
					processResponseHeaders(_utils.getEngine().handleHeaders(response, ignoreMayBlock), ignoreMayBlock);
			}
		} catch (Throwable t) {
			handleException(t, false);
		}
	}

	private void handleBodyResponse() {
		if (_logger.isDebugEnabled())
			_logger.debug(this + ".handleBodyResponse()");

		try {
			boolean lastPart = false;
			int parserStatus;
			if (!isH2) {
				parserStatus = serverSocket.readResponse(response);
				switch (parserStatus) {
				case HttpParser.READING_HEADERS:
					handleException(new IllegalStateException("Unexpected state while parsing response"), false);
					return;
				case HttpParser.READING_BODY:
					// Support https
					// no buffering for https
					if (!response.getRequest().getProlog().getMethod().equals(HttpUtils.METHOD_CONNECT)) {
						// we bufferize to avoid small packets
						if (waitForMoreBody(response, serverSocket.getRemainingBytes())) {
							return;
						}
					}
					lastPart = false;
					break;
				case HttpParser.PARSED:
					lastPart = true;
					break;
				}
			} else {
				lastPart = drainHTTP2ResponseBodyBuffer();
				parserStatus = HttpParser.READING_BODY;
			}

			// handleBody WILL return BODY
			if (_logger.isDebugEnabled()) {
				_logger.debug("response pxlet size=" + response.getProxyletsSize());
			}
			if (response.getProxyletsSize() > 0) {
				_utils.getEngine().handleBody(response, lastPart, true);
			}

			if (lastPart) {
				state = ST_READY_REQ;
				respNeedsThread = false;
			}
			if (_logger.isDebugEnabled()) {
				_logger.debug("sending body resp: lastPart=" + lastPart + ", state= " + state);
			}
			response.writeBodyTo(serverSocket.getOutputStream(), lastPart);
			if (lastPart) {
				requestDone();
			} else
				response.clearContent();
		} catch (Throwable t) {
			handleException(t, false);
		}
	}

	private void handleIgnorableResponse() {
		try {
			if (!serverSocket.flushResponseBody())
				return;
			// we know state == ST_RESPOND_RESP:
			state = ST_READY_REQ;
		} catch (Throwable t) {
			handleException(t, false);
		}
	}

	private void handleBufferedResponse(boolean ignoreMayBlock) {
		try {
			if (isH2) {
				if (!drainHTTP2ResponseBodyBuffer()) {
					return;
				}
			} else {
				switch (serverSocket.readResponse(response)) {
				case HttpParser.READING_HEADERS:
					handleException(new IllegalStateException("Unexpected state while parsing response"), false);
					return;
				case HttpParser.READING_BODY:
					return;
				case HttpParser.PARSED:
					break;
				}
			}
			processResponse(_utils.getEngine().handleBody(response, true, ignoreMayBlock), ignoreMayBlock);
		} catch (Throwable t) {
			handleException(t, false);
		}
	}

	protected boolean processResponse(int status, boolean ignoreMayBlock) throws Exception {
		if (_logger.isDebugEnabled())
			_logger.debug(this + ".processResponse(" + status + ")");

		switch (status) {
		case HttpProxyletEngine.SUSPEND_RESPONSE:
			busy = true;
			state = ST_RESUME_MSG_RESP;
			AsyncProxyletManager.suspend(response, this);
			return false;
		case HttpProxyletEngine.SUSPEND_REQUEST:
			busy = true;
			state = ST_RESUME_MSG_REQ;
			response.removeHeaders();
			response.clearContent();
			AsyncProxyletManager.suspend(request, this);
			return false;
		case HttpProxyletEngine.REQUEST:// send the request
			state = ST_REDIRECT_RESP;
			respNeedsThread = false;
			redirect(ignoreMayBlock);
			return true;
		case HttpProxyletEngine.RESPONSE:// send the response
			state = ST_READY_REQ;
			respNeedsThread = false;
			checkWebSocketUpgrade();
			response.writeTo(serverSocket.getOutputStream(), true);
			requestDone();
			return true;
		case HttpProxyletEngine.MAY_BLOCK_REQUEST:
		case HttpProxyletEngine.MAY_BLOCK_RESPONSE:
			state = ST_MAYBLOCK_MSG_RESP;
			mayBlockCode = status;
			busy = true;
			scheduleProxylets();
			return false;
		}

		return false;
	}

	protected boolean processResponseHeaders(int status, boolean ignoreMayBlock) throws Exception {
		if (_logger.isDebugEnabled())
			_logger.debug(this + ".processResponseHeaders(" + status + ")");

		switch (status) {
		case HttpProxyletEngine.SUSPEND_RESPONSE:
			state = ST_RESUME_HDRS_RESP;
			busy = true;
			AsyncProxyletManager.suspend(response, this);
			return false;
		case HttpProxyletEngine.SUSPEND_REQUEST:
			state = ST_RESUME_HDRS_REQ;
			busy = true;
			response.removeHeaders();
			response.clearContent();
			AsyncProxyletManager.suspend(request, this);
			return false;
		case HttpProxyletEngine.REQUEST:// send the request
			state = ST_REDIRECT_RESP;
			respNeedsThread = false;
			redirect(ignoreMayBlock);
			return true;
		case HttpProxyletEngine.RESPONSE:// send the response
			state = ST_RESPOND_RESP;
			respNeedsThread = false;
			checkWebSocketUpgrade();
			response.writeTo(serverSocket.getOutputStream(), true);
			requestDone();
			return true;
		case HttpProxyletEngine.HEADERS:// send the headers
			checkWebSocketUpgrade();
			response.writeHeadersTo(serverSocket.getOutputStream(), true);
			state = ST_WAITING_BODY_RESP;
			if (!waitForMoreBody(response, serverSocket.getRemainingBytes())) {
				// handleBody WILL return BODY
				_utils.getEngine().handleBody(response, false, true);
				// send the body
				response.writeBodyTo(serverSocket.getOutputStream(), false);
				response.clearContent();
			}
			return true;
		case HttpProxyletEngine.BUFFER:
			state = ST_BUFFER_RESP;
			return false;
		case HttpProxyletEngine.MAY_BLOCK_HEADERS:
		case HttpProxyletEngine.MAY_BLOCK_REQUEST:
		case HttpProxyletEngine.MAY_BLOCK_RESPONSE:
			state = ST_MAYBLOCK_HDRS_RESP;
			mayBlockCode = status;
			busy = true;
			respNeedsThread = true;
			scheduleProxylets();
			return false;
		}

		return true;
	}

	protected void sendRedirectedRequest() throws Throwable {
		RedirectClient r_client = new RedirectClient(request, _utils);
		r_client.redirect();
	}

	private void redirect(boolean ignoreMayBlock) throws IOException {
		// we know that state == ST_REDIRECT_RESP
		if (!ignoreMayBlock) {
			// we are in the main thread
			state = ST_MAYBLOCK_REDIRECT;
			busy = true;
			try {
				scheduleProxylets();
			} catch (Exception e) {
				handleException(e, false);
			} finally {
			}
			return;
		}

		redirected = true;
		// we re-initialize the response
		response.removeHeaders();
		response.clearContent();
		response.initProxyletState();
		response.removeAttribute(com.nextenso.proxylet.http.HttpResponse.ERROR_REASON_ATTR);

		_logger.debug("Redirecting request");

		try {
			sendRedirectedRequest();
			if (isH2) {
				return; // H2 client is always asynchronous
			}

			switch (_utils.getEngine().handleResponse(response, true)) {
			case HttpProxyletEngine.SUSPEND_REQUEST:
			case HttpProxyletEngine.SUSPEND_RESPONSE:
				throw new IllegalStateException("Can not suspend a redirecting proxylet(not supported)");
			case HttpProxyletEngine.REQUEST:
				redirect(true);
				return;
			case HttpProxyletEngine.RESPONSE:// send the response
				checkWebSocketUpgrade();
				response.writeTo(serverSocket.getOutputStream(), true);
				sendClose(false /* post close */, true /* close */);
				requestDone();
				return;
			}
		} catch (Throwable t) {
			handleException(t, false);
		}

		return;
	}

	/*****************************************************
	 * Misc. methods
	 *****************************************************/

	private synchronized boolean drainHTTP2ResponseBodyBuffer() { // returns true if response is fully parsed
		if (isH2ResponseFull) {
			return true;
		}

		if (_h2BodyResponseBuffer == null) {
			return false;
		}

		ByteBuffer b;
		while ((b = _h2BodyResponseBuffer.poll()) != null) {
			if (b == H2_BUFFER_FINISHED) {
				isH2ResponseFull = true;
			} else {
				try {
					response.appendContent(b);
				} catch (IOException e) {
					_logger.warn(e);
				}
			}
		}

		return isH2ResponseFull;
	}

	private boolean waitForMoreBody(HttpMessageFacade msg, int left) {
		return false;

// FIXME COMMENTED CODE ALWAYS RETURN "false"
//    if (msg.getAttribute(ATTR_DONT_BUFFER_CHUNKS) == Boolean.TRUE) {
//      return false;
//    }
//    int size = msg.getSize();
//    if (true)
//      return false;
//    if (left == -1)
//      return (size < BODY_CHUNK_SIZE_DEF);
//    if (size < BODY_CHUNK_SIZE_DEF)
//      return true;
//    return (left < BODY_CHUNK_SIZE_DEF);
	}

	// returns true == thread exits
	// returns false == thread continues
	private boolean checkThreadExit() {
		if (isSuspended()) {
			return true; // busy flag is left to true.
		}

		if (pendingCloseAck) {
			busy = false;
			return true;
		}

		if (closedPerformed) {
			busy = false;
			return true;
		}

		if (clientClosed) {
			busy = false;
			closeChannel();
			return true;
		}

		if (serverClosed) {
			busy = false;
			serverClose(false);
			return true;
		}

		if (respPassed) {
			respWentThrough();
			// we let the check on the sockets
		}

		if (earlyResponse != EARLY_RESP_NONE) {
			// we can have request or response data available
			if ((!clientSocket.hasAvailableInput()) && (!serverSocket.hasAvailableInput())) {
				// no data available from client or server side.
				busy = false;
				return true;
			}
		} else {
			if (state < ST_READY_RESP) {
				if (!clientSocket.hasAvailableInput()) {
					busy = false;
					return true;
				}
			} else {
				if (isH2) {
					if (_h2BodyResponseBuffer == null) {
						busy = false;
						return true;
					}
					if (_h2BodyResponseBuffer.isEmpty() && !isH2ResponseFull) {
						busy = false;
						return true;
					}
				} else if (!serverSocket.hasAvailableInput()) {
					busy = false;
					return true;
				}
			}
		}
		return false;
	}

	/*************************************************
	 * Execute proxylet container from the IO threadpool for blocking proxylets
	 *************************************************/

	/**
	 * Executes proxylet container from the IO threadpool for blocking proxylets.
	 * Note : this method is called from IO threadpool, and from HttpChannel serial queue.
	 * Also, the client socket is currently opened while the run method is called (see 
	 * schedule() method)
	 */
	private void run() {		
		// we resume
		wh: while (true) {
			switch (state) {
			case ST_READY_REQ:
			case ST_WAITING_HEADERS_REQ:
			case ST_REDIRECT_REQ:
			case ST_RESPOND_REQ:
			case ST_BUFFER_REQ:
			case ST_WAITING_BODY_REQ:
				// new request data arrived with the thread running
				try {
					handleRequest(true);
					if (checkThreadExit()) {
						return;
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, true);
					return;
				}
			case ST_MAYBLOCK_MSG_REQ:
				try {
					processRequest(_utils.getEngine().resume(request, mayBlockCode));
					if (checkThreadExit()) {
						return;
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, true);
					return;
				}
			case ST_MAYBLOCK_HDRS_REQ:
				try {
					processRequestHeaders(_utils.getEngine().resume(request, mayBlockCode));
					if (checkThreadExit()) {
						return;
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, true);
					return;
				}
			case ST_READY_RESP:
			case ST_WAITING_HEADERS_RESP:
			case ST_REDIRECT_RESP:
			case ST_RESPOND_RESP:
			case ST_BUFFER_RESP:
			case ST_WAITING_BODY_RESP:
				// new response data arrived with the thread running
				try {
					handleResponse(true);
					if (checkThreadExit()) {
						return;
					}

					// if we are handling early response, we still have to handle evantual
					// pending POST
					// data from request !
					boolean handleRequest = false;
					if (clientSocket.hasAvailableInput()) {
						handleRequest = true;
					}
					if (handleRequest) {
						handleRequest(true);
						if (checkThreadExit()) {
							return;
						}
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, false);
					return;
				}
			case ST_MAYBLOCK_MSG_RESP:
				try {
					processResponse(_utils.getEngine().resume(response, mayBlockCode), true);
					if (checkThreadExit()) {
						return;
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, false);
					return;
				}
			case ST_MAYBLOCK_HDRS_RESP:
				try {
					processResponseHeaders(_utils.getEngine().resume(response, mayBlockCode), true);
					if (checkThreadExit()) {
						return;
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, false);
					return;
				}
			case ST_MAYBLOCK_REDIRECT:
				try {
					state = ST_REDIRECT_RESP;
					redirect(true);
					// we known we closed - so we can exit
					busy = false;
					return;
				} catch (Throwable t) {
					handleException(t, false);
					return;
				}

			case ST_RESUME_HDRS_REQ:
			case ST_RESUME_MSG_REQ:
				try {
					handleRequestResumed(true);
					if (checkThreadExit()) {
						return;
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, true);
					return;
				}

			case ST_RESUME_HDRS_RESP:
			case ST_RESUME_MSG_RESP:
				try {
					handleResponseResumed(true);
					if (checkThreadExit()) {
						return;
					}
					continue wh;
				} catch (Throwable t) {
					handleException(t, false);
					return;
				}
			} // end switch
		} // end while
	}

	/******************************************************************************
	 * HttpProxyletContext.ProxyletResumer interface
	 ******************************************************************************/

	/**
	 * Called when the proxylet resumes. The caller thread can be the channel queue,
	 * or the IO threadpool
	 */
	public void resumeProxylet(ProxyletData msg, int status) {
		_serial.execute(() -> _resumeProxylet(msg, status));
	}

	private void _resumeProxylet(ProxyletData msg, int status) {
		if (!isSuspended()) {
			_logger.warn("can not resume a proxylet which is not suspended");
			return;
		}

		this.resumeStatus = status;

		PlatformExecutor currExecutor = Utils.getPlatformExecutors().getCurrentThreadContext().getCurrentExecutor();
		if (currExecutor.isThreadPoolExecutor()) {
			// We are running within the tpool ! resume the pxlet from our run() method.
			run();
			return;
		}

		// We are running in http reactor. First reset busy to false, meaning that we
		// are not
		// suspended any more.
		busy = false;

		// Now proceed with the next proxylet.
		boolean resumingRequest = true;
		try {
			switch (state) {
			case ST_RESUME_HDRS_REQ:
			case ST_RESUME_MSG_REQ:
				if (!handleRequestResumed(false)) { // the next pxlet has suspended us again (and busy
													// == true)
					return;
				}
				break;

			case ST_RESUME_HDRS_RESP:
			case ST_RESUME_MSG_RESP:
				resumingRequest = false;
				if (!handleResponseResumed(false)) {
					return;
				}
				break;
			}

			// At this point, all proxylets in the chain have been invoked. Now, check if a
			// close has
			// been performed.

			if (closedPerformed)
				return;

			if (clientClosed) {
				closeChannel();
				return;
			}
			if (serverClosed) {
				serverClose(true);
				return;
			}

			// Now check if we have received a "went-through" message.

			if (respPassed) {
				respWentThrough();
			}
		}

		catch (Throwable t) {
			handleException(t, resumingRequest);
			return;
		}

		// Now, check if the http stack sent us some request/response chunks ...
		// (remember that the http stack breaks pipelining.

		try {
			if (clientSocket.hasAvailableInput()) {
				_handleRequest();
			}
		} catch (Throwable t) {
			handleException(t, true);
			return;
		}

		try {
			if (serverSocket.hasAvailableInput()) {
				handleResponse();
			}
		} catch (Throwable t) {
			handleException(t, false);
			return;
		}
	}

	/*************************************************
	 * PushletOutputStreamFactory interface
	 *************************************************/

	public PushletOutputStream createPushletOutputStream() {
		// This will let any pushlet retrieve the socket id for the current pushlet
		// request.
		long socketUid = ((clientSockId & 0xFFFFFFFFL) << 32)
				| (Agent.getConnectionUid(connection) & ((long) 0xFFFFFFFFL));
		request.setAttribute("pushlet.channelUID", Long.valueOf(socketUid));
		request.setAttribute("pushlet.channelId", Integer.valueOf(clientSockId)); // deprecated
		return new PushletOutputStreamImpl();
	}

	/*************************************************
	 * Websocket handling
	 *************************************************/
	public void handleWebSocket(ByteBuffer buffer) {
		if (_logger.isDebugEnabled()) {
			_logger.debug(this + ".handleWebSocket()");
		}

		if (pendingCloseAck)
			return;

		if (request != null) {
			if (!websocket) {
				if (!switchToWebSocket()) {
					_logger.error("cannot handle websocket data on this channel " + request.getProlog().getURL());
					return;
				}
			}

			HttpProxyletChain chain = ((HttpProxyletContext) _utils.getEngine().getProxyletContainer().getContext())
					.getRequestChain();
			HttpRequestProxylet proxylet = null;
			boolean handled = false;
			while ((proxylet = (HttpRequestProxylet) chain.nextProxylet(request)) != null) {
				if (proxylet instanceof WebSocketHandler) {
					WebSocketHandler handler = (WebSocketHandler) proxylet;
					handler.onFrame(request, buffer);
					handled = true;
					break;
				}
				chain.shift(request, 1);
			}
			if (!handled) {
				_logger.warn("request chain cannot handle websocket data");
			}
		} else {
			_logger.error("receiving websocket data w/o initial request");
		}
	}

	protected boolean switchToWebSocket() {
		websocket = true;
		if (meters != null) {
			meters.incWebSockets();
		}
		// Switch output stream to by-pass the response chain
		OutputStream out = request.getResponse().getBody().getOutputStream();
		if (out instanceof PushletOutputStreamImpl) {
			((PushletOutputStreamImpl) out).switchToWebSocket();
			return true;
		}
		return false;
	}
	
	private boolean doKeepAlive(long clid) {
		// if the first 32bits ==0, then no keep alive
		long masked = clid & 0xFFFFFFFFL;
		return (masked != 0L);
	}

	protected static int getConnectionUid(MuxConnection connection) {
		return System.identityHashCode(connection);
	}

	/*************************************************
	 * Private attributes
	 *************************************************/
	protected final Utils _utils;

	// The default size for a body chunk before handling it to the proxylets
	@SuppressWarnings("unused")
	private static final int BODY_CHUNK_SIZE_DEF = 4096;

	// Socket command states ...
	protected static final int ST_READY_REQ = 1;
	private static final int ST_WAITING_HEADERS_REQ = ST_READY_REQ + 1;
	private static final int ST_WAITING_BODY_REQ = ST_WAITING_HEADERS_REQ + 1;
	private static final int ST_REDIRECT_REQ = ST_WAITING_BODY_REQ + 1;
	private static final int ST_RESPOND_REQ = ST_REDIRECT_REQ + 1; // A request proxylet has returned
																	// RESPOND_XX_PROXYLET
	private static final int ST_BUFFER_REQ = ST_RESPOND_REQ + 1;
	private static final int ST_MAYBLOCK_HDRS_REQ = ST_BUFFER_REQ + 1;
	private static final int ST_MAYBLOCK_MSG_REQ = ST_MAYBLOCK_HDRS_REQ + 1;
	private static final int ST_RESUME_HDRS_REQ = ST_MAYBLOCK_MSG_REQ + 1;
	private static final int ST_RESUME_MSG_REQ = ST_RESUME_HDRS_REQ + 1;
	private static final int ST_READY_RESP = 101;
	protected static final int ST_WAITING_HEADERS_RESP = ST_READY_RESP + 1;
	private static final int ST_WAITING_BODY_RESP = ST_WAITING_HEADERS_RESP + 1;
	private static final int ST_REDIRECT_RESP = ST_WAITING_BODY_RESP + 1;
	private static final int ST_RESPOND_RESP = ST_REDIRECT_RESP + 1;
	private static final int ST_BUFFER_RESP = ST_RESPOND_RESP + 1;
	private static final int ST_MAYBLOCK_HDRS_RESP = ST_BUFFER_RESP + 1;
	private static final int ST_MAYBLOCK_MSG_RESP = ST_MAYBLOCK_HDRS_RESP + 1;
	private static final int ST_RESUME_HDRS_RESP = ST_MAYBLOCK_MSG_RESP + 1;
	private static final int ST_RESUME_MSG_RESP = ST_RESUME_HDRS_RESP + 1;
	private static final int ST_READY_RESP_PARSE_ONLY = ST_RESUME_MSG_RESP + 1;

	// Misc constants ...
	private static final Object ATTR_DONT_BUFFER_CHUNKS = new Object();
	private static final int ST_MAYBLOCK_REDIRECT = 150;
	private static final int EARLY_RESP_NONE = 0;
	private static final int EARLY_RESP_REDIRECT_REQ = 1;
	private static final int EARLY_RESP_WAITING_BODY_REQ = 2;

	// Booleans telling if we must buffer request/response.
	protected static boolean buffReq;
	protected static boolean buffResp;

	// Loggers
	private final static Logger _pushletLogger = Logger.getLogger("agent.http.pushlet");
	protected final static Logger _logger = Utils.logger;

	// Instance variables
	protected int state;
	protected boolean busy; // If true: either a thread is alive, or we are suspended.
	protected int clientSockId;
	protected MuxConnection connection;
	protected HttpCnxMeters meters = null;
	// flags telling if we are expecting an close ack.
	// We expect close ack when: 1/ we acked a close 2/ we sent a close.
	private boolean pendingCloseAck;
	protected HttpRequestFacade request;
	protected HttpResponseFacade response;
	protected final HttpSocket clientSocket;

	protected final HttpSocket serverSocket;
	private int mayBlockCode, resumeStatus;
	protected boolean reqNeedsThread, respNeedsThread;
	private boolean clientClosed, serverClosed, closedPerformed, redirected, respPassed;
	private int earlyResponse = EARLY_RESP_NONE;
	/** Flag telling if we have processed and fully replied to an http request */
	protected boolean _requestDone;
	protected String remoteIp;

	/** Indicate if the request being handled by the HTTP2 Client */
	protected boolean isH2 = false;

	protected Queue<ByteBuffer> _h2BodyResponseBuffer = null;
	protected boolean isH2ResponseFull = false;
	protected final static ByteBuffer H2_BUFFER_FINISHED = ByteBuffer.allocate(0);
	private static Logger _accessLog;

	private static Dictionary<String, String> _systemConfig;

	protected boolean websocket;
	protected final ReentrantSerialExecutor _serial = new ReentrantSerialExecutor();

	/*************************************************
	 * Pushlet Output Stream.
	 *************************************************/

	private final static int PUSHLET_ACTIVE = 1;
	private final static int PUSHLET_FILTER = 2;
	private final static int PUSHLET_DO_CHUNK = 4;
	private final static int PUSHLET_HEADERS_SENT = 8;
	private final static int PUSHLET_CLOSED = 16;
	private final static int PUSHLET_FLUSHING = 32;
	private final static int PUSHLET_CLOSING = 64;
	private final static int PUSHLET_LAST_CHUNK_SENT = 128;

	private final static int PUSHLET_FLG_RESP = 0x41; // HTTP response
	private final static int PUSHLET_FLG_WEBSOCKET = Utils.POST_FILTER_FLAGS | Utils.WEBSOCKET; // WebSocket response
	private final static int PUSHLET_FLG_SRVCLOSE = 0x42; // server close

	class PushletOutputStreamImpl extends PushletOutputStream {
		// The state of our pushlet output stream (see flags above).
		private char _pushletState;
		private int flags = PUSHLET_FLG_RESP;

		PushletOutputStreamImpl() {
		}

		// Invoked when the pushlet has returned from its doRequest method.
		@Override
		public synchronized void activate(boolean filter) throws IOException {
			if (filter) {
				_pushletState |= PUSHLET_FILTER;
			}
			_pushletState |= PUSHLET_ACTIVE;

			if ((_pushletState & PUSHLET_FLUSHING) != 0) {
				flush();
			}

			if ((_pushletState & PUSHLET_CLOSING) != 0) {
				close();
			}
		}

		@Override
		public boolean isDirect() {
			return ((_pushletState & PUSHLET_FILTER) == 0);
		}

		public synchronized void switchToWebSocket() {
			_pushletState = PUSHLET_ACTIVE | PUSHLET_FLUSHING;
			flags = PUSHLET_FLG_WEBSOCKET;
		}

		public synchronized void flush() throws IOException {
			if (clientClosed)
				throw new ClosedChannelException();
			if (serverClosed)
				throw new ClosedChannelException();
			if ((_pushletState & PUSHLET_ACTIVE) == 0) {
				_pushletState |= PUSHLET_FLUSHING;
				return;
			}
			if ((_pushletState & PUSHLET_CLOSED) != 0) {
				throw new IOException("Pushlet closed");
			}
			doFlush(false);
		}

		public synchronized void close() throws IOException {
			if ((_pushletState & PUSHLET_ACTIVE) == 0) {
				_pushletState |= PUSHLET_CLOSING;
				return;
			}
			if ((_pushletState & PUSHLET_CLOSED) != 0) {
				return;
			}
			_pushletState |= PUSHLET_CLOSED;
			doFlush(true);
		}

		// Write a couple of NIO buffers atomically.
		@Override
		public synchronized void write(ByteBuffer... bufs) throws IOException {
			// TODO send the buffers directly to mux, in order to avoid useless buffer
			// duplications.
			Utils.copyTo(this, bufs);
			flush();
		}

		private int getSockId() {
			return getId();
		}

		// Method called by already synchronized methods: see flush/close.
		private void doFlush(final boolean doClose) {
			try {
				final ByteOutputStream out = new ByteOutputStream();

				// Must we send headers ?
				if ((_pushletState & PUSHLET_HEADERS_SENT) == 0) {
					_pushletState |= PUSHLET_HEADERS_SENT;

					// If client did not fill response headers, just write bytes in raw mode
					if (request.getResponse().getProlog().getStatus() != -1) {
						// If the pushlet has set the Transfer-Encoding Chunked header, turn on
						// streaming.
						HttpResponseFacade response = (HttpResponseFacade) request.getResponse();
						if (response.hasChunkedHeader()) {
							_pushletState |= PUSHLET_DO_CHUNK;
							response.setStreaming(true);
						}
						checkWebSocketUpgrade();
						response.writeHeadersTo(out, true);

						// Reset response headers: it will be used by handleResponse() method !
						response.getProlog().setStatus(-1);
						response.getProlog().setProtocol(null);
						response.getProlog().setReason(null);
						response.getHeaders().removeHeaders();
						response.getBody().clearContent();
					}
				}

				// Must we send a body part ?
				if (super.count > 0) {
					// If the pushlet has set the transfer encoding to chunk, we have to set the
					// chunk
					// length ...
					if ((_pushletState & PUSHLET_DO_CHUNK) != 0) {
						out.write(Charset.makeBytes(Integer.toHexString(super.count), Constants.ASCII)); // write
						// chunk
						// length
						out.write(Constants.CRLF_B);
						out.write(super.buf, 0, super.count);
						out.write(Constants.CRLF_B);
					} else {
						out.write(super.buf, 0, super.count);
					}
					super.reset();
				}

				// Must we send a 0-length chunk header ?
				boolean noDataToSend = (out.size() == 0);
				if ((_pushletState & PUSHLET_DO_CHUNK) != 0 && (doClose || noDataToSend)) {
					if ((_pushletState & PUSHLET_LAST_CHUNK_SENT) == 0) {
						_pushletState |= PUSHLET_LAST_CHUNK_SENT;
						out.write(Constants._0_CRLF_CRLF_B, 0, Constants._0_CRLF_CRLF_B.length);
					}
				}

				if (out.size() > 0 || doClose) {
					if ((_pushletState & PUSHLET_FILTER) == 0) {
						// No need to run the pxlet resp-chain: send the body to the client
						// right away.
						doFlushDirect(out.toByteArray(false), 0, out.size(), doClose);
					} else {
						// Need to run the pxlet resp-chain in the http agent reactor thread.
						_utils.getHttpExecutor().execute(new Runnable() { // TODO check executor to use ?
							public void run() {
								doFlushThroughResponseChain(out.toByteArray(true), 0, out.size(), doClose);
							}
						},
								/**
								 * We must rechedule in the http agent, because the processRequest must fully
								 * terminate its execution, before we can process with the response
								 */
								ExecutorPolicy.SCHEDULE);
					}
				}
			}

			catch (Throwable t) {
				_pushletLogger.error("Exception while flushing pushlet outputstream", t);
			}
		}

		/**
		 * Send the response directly to the client, without running the response chain.
		 * 
		 * @param data    the data to be sent to the client
		 * @param doClose true if the socket has to be closed, false if not.
		 */
		private void doFlushDirect(byte[] data, int off, int len, boolean doClose) {
			try {
				if (len > 0) {
					if (_pushletLogger.isDebugEnabled()) {
						String log = alcatel.tess.hometop.gateways.utils.Utils
								.dumpByteArray("PushletOutputStream: flushing data clientSockId=" + clientSockId
										+ " len=" + len + "\n", data, off, len, 1024);
						_pushletLogger.debug(log);
					}
					MuxHeaderV0 hdr = new MuxHeaderV0();
					hdr.set(getSessionId(), getSockId(), flags);
					sendMuxData(hdr, false, ByteBuffer.wrap(data, off, len));
				}

				if (doClose) {
					if (clientClosed) {
						if (_pushletLogger.isDebugEnabled()) {
							_pushletLogger.debug("PushletOutputStream.doFlush0/Close: client already closed socket");
						}
						return; // The client closed its socket, and the request was aborted.
					}

					if (serverClosed) {
						if (_pushletLogger.isDebugEnabled()) {
							_pushletLogger.debug("PushletOutputStream.doFlush0/Close: socket already closed (timeout)");
						}
						return; // The http stack closed the client socket because of a socket
						// inactivity
						// timeout.
					}

					if (_pushletLogger.isDebugEnabled()) {
						_pushletLogger.debug("PushletOutputStream: closing");
					}
					// Send close
					_utils.getAgent().sendClose(connection, getSockId());
				}
			}

			catch (Throwable t) {
				_pushletLogger.error("Exception while flushing pushlet outputstream", t);
			}
		}

		/**
		 * Send the response to the client, but before, apply it to the response chain.
		 * This method must be called within the http agent reactor thread.
		 */
		private void doFlushThroughResponseChain(byte[] data, int off, int len, boolean doClose) {
			try {
				if (data.length > 0) {
					if (_pushletLogger.isDebugEnabled()) {
						String log = alcatel.tess.hometop.gateways.utils.Utils.dumpByteArray(
								"PushletOutputStream: flushing data len=" + len + "\n", data, off, len, 1024);
						_pushletLogger.debug(log);
					}
					MuxHeaderV0 hdr = new MuxHeaderV0();
					hdr.set(getSessionId(), getSockId(), PUSHLET_FLG_RESP);

					_utils.getAgent().muxData(connection, hdr, data, off, len);
				}

				if (doClose) {
					if (clientClosed) {
						if (_pushletLogger.isDebugEnabled()) {
							_pushletLogger.debug("PushletOutputStream.doFlush0/Close: client already closed socket");
						}
						return; // The client closed its socket, and the request was aborted.
					}

					if (serverClosed) {
						if (_pushletLogger.isDebugEnabled()) {
							_pushletLogger.debug("PushletOutputStream.doFlush0/Close: socket already closed (timeout)");
						}
						return; // The http stack closed the client socket because of a socket
						// inactivity
						// timeout.
					}

					if (_pushletLogger.isDebugEnabled()) {
						_pushletLogger.debug("PushletOutputStream: closing");
					}
					MuxHeaderV0 hdr = new MuxHeaderV0();
					hdr.set(getSessionId(), getSockId(), PUSHLET_FLG_SRVCLOSE);
					_utils.getAgent().muxData(connection, hdr, null, 0, 0);
				}
			}

			catch (Throwable t) {
				_pushletLogger.error("Exception while flushing pushlet outputstream", t);
			}
		}
	}

	public static void bindSystemConfig(Dictionary<String, String> systemConf) {
		_systemConfig = systemConf;
	}

	@Override // Socket
	public boolean close() {
		return false;
	}

	@Override // Socket
	public int getLocalIP() {
		return 0;
	}

	@Override // Socket
	public String getLocalIPString() {
		return "";
	}

	@Override // Socket
	public int getLocalPort() {
		return 0;
	}

	@Override // Socket
	public int getSockId() {
		return clientSockId;
	}

	@Override // Socket
	public int getType() {
		return Socket.TYPE_TCP;
	}

	public boolean isServerClosed() {
		return serverClosed;
	}

	protected boolean isClosed() {
		return serverClosed || clientClosed || closedPerformed;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [sockId=" + clientSockId + ", clid=" + Long.toHexString(getSessionId())
				+ ", busy=" + busy + ", state=" + state + "]";
	}

}
