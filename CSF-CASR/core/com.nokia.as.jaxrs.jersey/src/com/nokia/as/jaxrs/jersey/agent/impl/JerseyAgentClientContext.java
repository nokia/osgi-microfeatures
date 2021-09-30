package com.nokia.as.jaxrs.jersey.agent.impl;

import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP10_100;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP10_404;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP11_100;
import static com.nokia.as.jaxrs.jersey.common.impl.JaxRsResourceRegistration.HTTP11_404;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

import javax.ws.rs.core.UriBuilder;

import org.apache.log4j.Logger;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;

import com.alcatel.as.http.parser.AccessLog;
import com.alcatel.as.http.parser.CommonLogFormat;
import com.alcatel.as.http.parser.HttpMessageImpl;
import com.alcatel.as.http.parser.HttpMeters;
import com.alcatel.as.http.parser.HttpParser;
import com.alcatel.as.ioh.MessageParser;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.nextenso.mux.MuxConnection;
import com.nokia.as.jaxrs.jersey.common.ClientContext;
import com.nokia.as.jaxrs.jersey.common.ServerContext;
import com.nokia.as.jaxrs.jersey.common.impl.HttpBodyInputStream;
import com.nokia.as.jaxrs.jersey.common.impl.ResponseWriter;

final class JerseyAgentClientContext implements ClientContext {

	private ServerContext _serverCtx;
	private MuxConnection _connection;
	private boolean _ignoreData;
	private MessageParser<HttpMessageImpl> _httpParser = new HttpParser().skipChunkDelimiters();
	private ContainerRequest _containerRequest;
	private HttpBodyInputStream _bodyStream;
	private Logger _log;
	private HttpMeters _meters;
	private String _alias;
	private URI _baseUri;
	private ApplicationHandler _appHandler;
	private int _sockId;
	private String _remoteIP;
	private int _remotePort;
	private DefaultAgentSecurityContext securityContext = new DefaultAgentSecurityContext();
	private final Executor _resourceExec;

	public JerseyAgentClientContext(ServerContext serverCtx, MuxConnection connection, String remoteIP, int remotePort, int sockId, URI baseUri, Executor resourceExec) {
		_serverCtx = serverCtx;
		_connection = connection;
		_remoteIP = remoteIP;
		_remotePort = remotePort;
		_sockId = sockId;
		_log = _serverCtx.getLogger();
		_baseUri = baseUri;
		_meters = _serverCtx.getMeters();
		_meters.getOpenChannelsMeter().inc(1);
		_alias = _serverCtx.getAlias();
		_appHandler = _serverCtx.getApplicationHandler();
		_resourceExec = resourceExec;
	}

	public String toString() { 
		return new StringBuilder().append("JerseyAgentClientContext[").append(_baseUri).append(' ')
			.append(_remoteIP).append ('/').append (_remotePort).append (" sockId=").append (_sockId).append(']').toString();
	}

	public ServerContext getServerContext() {
		return _serverCtx;
	}

	public int messageReceived(ByteBuffer data) {

		if (_ignoreData) {
			data.position(data.limit());
			return 0;
		}

		int init = data.position(); // in case of error to log the buffer
		try {
			// parseHttpMessage
			HttpMessageImpl req;
			URI uri = null;

			while ((req = _httpParser.parseMessage(data)) != null) {
				if (_log.isDebugEnabled())
					_log.debug(this + " : client messageReceived:\n" + req);

				if (req.isFirst()) {
					req.setHasMore(); // required by the parser - so next read does not return the req if no new data
					_meters.getReadReqMeter(req.getMethod()).inc(1);

					// log request
					AccessLog al = new AccessLog();
					CommonLogFormat clf = _serverCtx.getCommonLogFormat();

					if (clf.isEnabled())
						al.request(req).remoteIP(_connection.getRemoteAddress().getAddress());

					// map aliases to resource path
					String initialUrl = req.getURL();
					if (!initialUrl.startsWith(_alias)) {
						return respond(404, req.getVersion() == 0 ? HTTP10_404 : HTTP11_404, al, clf, true);
					}

					String expect = req.getHeaderValue("expect");
					if (expect != null && expect.contains("100-continue")) {
						respond(100, req.getVersion() == 0 ? HTTP10_100 : HTTP11_100, null, null, false);
					}

					String withoutAlias = initialUrl.replaceFirst(_alias, "/");
					uri = UriBuilder.fromUri(withoutAlias).build();

					_containerRequest = new ContainerRequest(_baseUri, uri, req.getMethod(),
							securityContext, new MapPropertiesDelegate());

					boolean keepAlive;
					String ka = req.getHeaderValue("connection");
					if (req.getVersion() == 1) {
						keepAlive = (ka == null) || (ka.toLowerCase().indexOf("close") == -1);
					} else {
						keepAlive = ka != null && ka.toLowerCase().indexOf("keep-alive") > -1;
					}

					_containerRequest.setWriter(new ResponseWriter(this, keepAlive, req.getVersion(), al));

					req.iterateHeaders(_containerRequest::header);

					_bodyStream = new HttpBodyInputStream();
				}
				_bodyStream.addBody(req.getBody());
				if (req.isLast()) {
					if (_bodyStream.available() > 0) {
						_containerRequest.setEntityStream(_bodyStream);
					}
					// do request
					ContainerRequest creq = _containerRequest;
					_resourceExec.execute(() -> _appHandler.handle(creq));
					_containerRequest = null;
					_bodyStream = null;	
				}
			}
		} catch (Exception e) {
			if (_log.isInfoEnabled()) {
				data.position(init);
				String s = ByteBufferUtils.toUTF8String(true, data);
				_log.info(this + " : exception while parsing\n" + s, e);
			}
			_meters.getParserErrorMeter().inc(1);
			data.position(data.limit());
			close();
			_ignoreData = true; // disable future reads until closed is called back
		}

		return 0;
	}

	public void send(ByteBuffer data, boolean copy) {
		_connection.sendTcpSocketData(_sockId, copy, data);
	}

	protected int respond(int status, byte[] data, AccessLog log, CommonLogFormat cl, boolean close) {
		send(ByteBuffer.wrap(data), false);
		_meters.getWriteRespMeter(status).inc(1);
		if (log != null && !log.isEmpty()) {
			cl.log(log.responseStatus(status).responseSize(0));
		}
		if (close) {
			_ignoreData = true; // disable future reads until closed is called back
			close();
		}
		return 0;
	}

	public void close() {
		if (_log.isDebugEnabled())
			_log.debug(this + " : close client");
		_connection.sendTcpSocketClose(_sockId);
	}

	public void setSuspendTimeout(long timeOut) {
	}

	public void closed (){
		if (_log.isDebugEnabled()) {
			_log.debug(this + " : clientClosed");
		}
		_meters.getOpenChannelsMeter().inc(-1);
		_meters.getClosedChannelsMeter().inc(1);
	}
}
