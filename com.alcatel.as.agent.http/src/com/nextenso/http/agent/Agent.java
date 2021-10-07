package com.nextenso.http.agent;

import static com.nextenso.http.agent.Utils.logger;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.url.URLConstants;
import org.osgi.service.url.URLStreamHandlerService;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.alcatel.as.util.config.ConfigHelper;
import com.nextenso.http.agent.client.HttpConnection;
import com.nextenso.http.agent.client.impl.HttpClientFactoryImpl;
import com.nextenso.http.agent.client.impl.HttpConnectionImpl;
import com.nextenso.http.agent.config.HeaderProperties;
import com.nextenso.http.agent.demux.ConnectionPool;
import com.nextenso.http.agent.demux.DemuxClientSocket;
import com.nextenso.http.agent.demux.DemuxSocket;
import com.nextenso.http.agent.demux.HttpPipeline;
import com.nextenso.http.agent.demux.NextHopEvaluator;
import com.nextenso.http.agent.demux.client.HttpConnectionDemux;
import com.nextenso.http.agent.engine.HttpProxyletContainer;
import com.nextenso.http.agent.engine.HttpProxyletContext;
import com.nextenso.http.agent.engine.HttpProxyletEngine;
import com.nextenso.http.agent.ext.HttpChannelEvent;
import com.nextenso.http.agent.impl.HttpResponseFacade;
import com.nextenso.http.agent.impl.HttpSessionFacade;
import com.nextenso.http.agent.impl.HttpUrlStreamHandlerService;
import com.nextenso.mux.DNSParser;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.MuxContext;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nextenso.mux.socket.Socket;
import com.nextenso.mux.util.DNSManager;
import com.nextenso.mux.util.MuxHandlerMeters;
import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.engine.ProxyletApplication;
import com.nextenso.proxylet.engine.criterion.FalseCriterion;
import com.nextenso.proxylet.engine.criterion.TrueCriterion;
import com.nextenso.proxylet.http.HttpClientFactory;

@SuppressWarnings({ "rawtypes", "unchecked", "deprecation", "unused" })
@Component(service = { MuxHandler.class }, property = { "protocol=Http", "autoreporting=false" })
public class Agent extends MuxHandler implements AgentProperties, HeaderProperties {
	private static final int VERSION = ((4 << 16) | 0); // version 3, release 0
	public static final int APP_NHTTP_STACK = 286;
	private final static Logger _logger = Logger.getLogger("agent.http");
	private final static SimpleDateFormat listDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private int doReq = 1; // values : 1(yes), 0(no), -1(auto : not supported yet)
	private int doResp = -1; // values : 1(yes), 0(no), -1(auto)
	private boolean doClient;
	private String stackInstance = STACK_ANY; // FIXME this is broken
	private boolean anyStack = true; // FIXME this is broken
	private byte[] reqCritBytes, respCritBytes;
	private volatile HttpProxyletContainer proxyletContainer;
	private volatile Dictionary _agentConf;
	private volatile Dictionary _systemConfig;
	private ProxyletApplication _app; // injected
	private volatile boolean _initialized; // Our init() method has been called

	// accesslog not yet ported to artifactory, we use our own access log
	private final static Logger _accessLog = Logger.getLogger("agent.http.accesslog");
	// private volatile Logger _accessLog; // Injected (used to log access)

	@Reference
	private MeteringService meteringService;

	private BundleContext bctx;

	private volatile TimerService timerService;
	private volatile HttpClientFactoryImpl hcf;
	private volatile String viaPseudonym = "";
	private volatile boolean selfConnection;
	private SessionPolicy sessionPolicy;
	private NextHopEvaluator nextHopEvaluator;
	private ConnectionPool connectionPool;

	private HttpMeters httpMeters;
	private MuxHandlerMeters agentBasicMeters;
	private SimpleMonitorable agentMon;
	private com.alcatel.as.http2.client.api.HttpClientFactory _h2Factory;

	private final Utils _utils = new Utils();

	// accesslog not yet ported to artifactory, we use our own access log
	/*
	 * @Reference(target = "(name=as.service.accesslog.http)") void
	 * bindAccessLog(Logger al) { _accessLog = al; }
	 */

	@Reference
	void bindHttp2ClientFactory(com.alcatel.as.http2.client.api.HttpClientFactory h2Factory) {
		_logger.info("HTTP2 Client injected");
		_h2Factory = h2Factory;
	}

	@Reference(target = "(service.pid=httpagent)", cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
	void bindAgentConf(Dictionary agentConf) {
		_agentConf = agentConf;
		updateAgentConfig(_agentConf);
	}

	void unbindAgentConf(Dictionary agentConf) {
	}

	@Reference
	protected void bindPlatformExecutors(PlatformExecutors pfExecutors) {
		Utils.setPlatformExecutors(pfExecutors);
	}

	@Reference(target = "(strict=false)")
	public void setTimerService(TimerService timerService) {
		this.timerService = timerService;
	}

	@Reference(target = "(service.pid=system)", policy = ReferencePolicy.DYNAMIC)
	protected void bindSystemConfig(Dictionary systemConf) {
		_systemConfig = systemConf;
		HttpChannel.bindSystemConfig(systemConf);
	}

	protected void unbindSystemConfig(Dictionary systemConf) {
	}

	@Reference(target = "(service.pid=com.nextenso.http.agent.config.HeaderProperties)", cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC)
	void bindHeaderConf(Dictionary conf) {
		_utils.loadHeaderConfig(conf);
	}

	void unbindHeaderConf(Dictionary conf) {
	}

	@Reference(target = "(protocol=http)", unbind = "unbindProxyletApplication")
	protected void bindProxyletApplication(ProxyletApplication app) {
		if (logger.isDebugEnabled())
			logger.debug("binding HTTP ProxyletApplication "
					+ Arrays.asList(app.getProxylets(ProxyletApplication.REQUEST_CHAIN)));
		_app = app;
	}

	@Reference(target = "(id=http)")
	protected void bindHttpExecutor(PlatformExecutor httpExecutor) {
		_utils.setHttpExecutor(httpExecutor);
	}

	protected void unbindProxyletApplication(ProxyletApplication app) {
		_logger.warn("Unbound Http Proxylet Applications");
	}

	@Reference
	protected void bindHttpClientFactory(HttpClientFactory hcf) {
		this.hcf = (HttpClientFactoryImpl) hcf;
	}

	@Activate
	protected void activate(BundleContext bctx, Map<String, String> cnf) {
		// _utils.setContainerIndex(cnf.get("composite.scope"));
		this.bctx = bctx;
		_utils.setContainerIndex("1");
		agentMon = new SimpleMonitorable("http.agent", "HTTP Proxylet Agent");
		agentBasicMeters = new MuxHandlerMeters(meteringService, agentMon);
		httpMeters = new HttpMeters(meteringService, agentMon);

		String[] l4Proto = new String[] { "tcp" };
		agentBasicMeters.initMeters(l4Proto);

		agentMon.start(bctx);
		Utils.setMonitorable(httpMeters);

		if (ConfigHelper.getBoolean(_agentConf, WRAP_HTTPURLCONNECTION, true)) {
			_logger.debug("enabling HttpUrlConnection stream handler");
			Dictionary<String, Object> props = new Hashtable<>();
			props.put(URLConstants.URL_HANDLER_PROTOCOL, new String[] { "http", "https" });
			HttpUrlStreamHandlerService urlHandler = new HttpUrlStreamHandlerService();
			urlHandler.bindHttpClientFactory(hcf);
			bctx.registerService(URLStreamHandlerService.class.getName(), urlHandler, props);
		}

		_logger.info("Activating http agent " + _utils.getContainerIndex());
	}

	@Deactivate
	protected void deactivate() {
		_logger.info("Deactivating http agent");
	}

	// Invoked by the callout server.
	public void init(int appId, String appName, String appInstance, MuxContext muxContext) {
		try {
			super.init(appId, appName, appInstance, muxContext);
			HttpChannel.setAccessLog(_accessLog);

			int[] stackIds = new int[1];
			stackIds[0] = APP_NHTTP_STACK;
			getMuxConfiguration().put(CONF_THREAD_SAFE, Boolean.TRUE);
			getMuxConfiguration().put(CONF_STACK_ID, stackIds);
			getMuxConfiguration().put(CONF_DEMUX, Boolean.FALSE);
			getMuxConfiguration().put(CONF_TCP_PARSER, StandaloneTcpParser.getInstance());
			getMuxConfiguration().put(CONF_L4_PROTOCOLS, new String[] { "tcp" });
			getMuxConfiguration().put(CONF_HANDLER_METERS, agentBasicMeters);
			getMuxConfiguration().put(CONF_IPV6_SUPPORT, Boolean.TRUE);

			//
			// Initialize specific http messages sizes
			//
			HttpResponseFacade.CONTENT_MAX_SIZE = 32 * 1024; // 32K

			// Initialize http proxylet container.
			proxyletContainer = new HttpProxyletContainer();
			proxyletContainer.bindSystemConfig(_systemConfig);
			proxyletContainer.init(_app);
			_app.initDone();
			_utils.setContainer(proxyletContainer);
			_utils.setAgent(this);
			_utils.setTimerService(timerService);
			_utils.setEngine((HttpProxyletEngine) proxyletContainer.getProxyletEngine());

			//
			// we determine the filtering policy
			//
			String s = ConfigHelper.getString(_agentConf, RESP_FILTERING, "").toLowerCase();
			if (s.equals(YES))
				doResp = 1;
			else if (s.equals(NO))
				doResp = 0;
			else
				doResp = (proxyletContainer.responseProxyletsSize() != 0) ? -1 : 0;
			s = ConfigHelper.getString(_agentConf, REQ_FILTERING, "").toLowerCase();

			if (s.equals(YES))
				doReq = 1;
			else if (s.equals(NO))
				doReq = 0;
			else
				doReq = (doResp != 0 || proxyletContainer.requestProxyletsSize() != 0) ? 1 : 0;

			_logger.info("number of request proxylets: " + proxyletContainer.requestProxyletsSize());

			// http client policy
			doClient = ConfigHelper.getString(_agentConf, CLIENT_FILTER, "").equalsIgnoreCase(YES);
			if (doClient)
				_logger.info("HttpClient requests will be filtered");
			else
				_logger.info("HttpClient requests will NOT be filtered");

			// we set the request criterion
			String reqCrit = (doReq != 0) ? TrueCriterion.getInstance().toString()
					: FalseCriterion.getInstance().toString();
			reqCrit = Utils.req_filtering + com.nextenso.proxylet.engine.criterion.Utils.getHttpStackCriterion(reqCrit);
			reqCrit = alcatel.tess.hometop.gateways.utils.Utils.removeSpaces(reqCrit);
			reqCritBytes = reqCrit.getBytes();
			_logger.info("Criterion for sending requests: " + reqCrit);

			// we set the reponse criterion
			String respCrit = null;
			switch (doResp) {
			case -1:
				respCrit = proxyletContainer.getResponseCriterion().toString();
				break;
			case 0:
				respCrit = FalseCriterion.getInstance().toString();
				break;
			case 1:
				respCrit = TrueCriterion.getInstance().toString();
				break;
			}
			respCrit = Utils.resp_filtering
					+ com.nextenso.proxylet.engine.criterion.Utils.getHttpStackCriterion(respCrit);
			respCrit = alcatel.tess.hometop.gateways.utils.Utils.removeSpaces(respCrit);
			respCritBytes = respCrit.getBytes();
			_logger.info("Criterion for sending responses: " + respCrit);

			_initialized = true;
			updateAgentConfig(_agentConf);
		}

		catch (Exception e) {
			throw new RuntimeException("Could not initialize http agent", e);
		}
	}

	public void destroy() {
		_logger.info("Destroying http agent ...");
		try {
			// Stop our Session Timer Task.
			Client.shutdown();

			if (agentBasicMeters != null) {
				agentBasicMeters.stop();
			}

			// we send a soft kill command to the stacks
			synchronized (_utils.getConnectionManager()) {
				Enumeration enumer = _utils.getConnectionManager().getMuxConnections();
				while (enumer.hasMoreElements()) {
					MuxConnection connection = (MuxConnection) enumer.nextElement();
					_logger.info("Send stop to stack <" + connection.getStackInstance() + ">");
					connection.sendMuxStop();
				}
				killAllUsers(true);
			}
			proxyletContainer.destroy();
		} catch (Throwable t) {
			_logger.error("Unexpected exception while destroying http agent", t);
		}
	}

	/*************************************************************/
	/************************ Counters *************************/
	/*************************************************************/

	int getActiveClients() {
		Hashtable<Long, Client> clients = _utils.getClients();
		synchronized (clients) { // do we still need to synchronize ?
			return clients.size();
		}
	}

	int getActiveChannels() {
		int channels = 0;
		synchronized (_utils.getConnectionManager()) {
			Enumeration enumer = _utils.getConnectionManager().getMuxConnections();
			while (enumer.hasMoreElements()) {
				MuxConnection connection = (MuxConnection) enumer.nextElement();
				channels += connection.getSocketManager().getSocketsSize(Socket.TYPE_TCP);
			}
		}
		return channels;
	}

	public int[] getCounters() {
		throw new RuntimeException("deprecated method, should not be used anymore");
	}

	public int getMajorVersion() {
		return VERSION >>> 16;
	}

	public int getMinorVersion() {
		return VERSION & 0xFFFF;
	}

	public boolean doRequestFiltering() {
		return (doReq != 0);
	}

	public boolean doResponseFiltering() {
		return (doResp != 0);
	}

	/*************************************************************/
	/**************************** mux ****************************/
	/*************************************************************/

	private static boolean doKeepAlive(long clid) {
		// if the first 32bits ==0, then no keep alive
		long masked = clid & 0xFFFFFFFFL;
		return (masked != 0L);
	}

	public SessionPolicy getSessionPolicy() {
		return sessionPolicy;
	}

	public String getViaPseudonym() {
		return viaPseudonym;
	}

	public NextHopEvaluator getNextHopEvaluator() {
		return nextHopEvaluator;
	}

	public boolean selfConnectionProhibited() {
		return !selfConnection;
	}

	protected static int getConnectionUid(MuxConnection connection) {
		return System.identityHashCode(connection);
	}

	@Override
	public void muxOpened(MuxConnection connection) {
		_utils.getConnectionManager().addMuxConnection(connection);
		SimpleMonitorable sm = (SimpleMonitorable) connection.getMonitorable();

		HttpCnxMeters cnxMeters = null;
		if (sm != null) {
			cnxMeters = new HttpCnxMeters(meteringService, sm, httpMeters);
			sm.updated();
		}

		if (connectionPool == null)
			connectionPool = new ConnectionPool(_utils);
		connection.attach(new HttpConnectionDemux(connection, connectionPool, this, cnxMeters));

		// send configuration
		sendMuxConfig(connection, ByteBuffer.wrap(reqCritBytes));
		sendMuxConfig(connection, ByteBuffer.wrap(respCritBytes));
		sendSessionTimeout(connection);

		// send client config
		String s = Utils.set_filtering
				+ ((doClient) ? TrueCriterion.getInstance().toString() : FalseCriterion.getInstance().toString());
		byte[] buff = s.getBytes();
		sendMuxConfig(connection, ByteBuffer.wrap(buff));

		// notify HttpClientFactory that http agent is initialized and at least one mux
		// connection is available.
		if (connectionPool != null)
			connectionPool.addConnection(connection);
		hcf.activate(_utils);
		// UNUSED if (demux) connection.sendMuxStart();
	}

	private void sendSessionTimeout(MuxConnection connection) {
		String to = Utils.session_timeout + Client.getSessionTimeout(SESSION_TIMEOUT_MINIMUM);
		byte[] sessionToBytes;
		try {
			sessionToBytes = to.getBytes("ASCII");
		} catch (UnsupportedEncodingException e) {
			_logger.error("Could not send session timeout on mux connection: connection", e);
			return;
		}
		sendMuxConfig(connection, ByteBuffer.wrap(sessionToBytes));
	}

	@Override
	public void muxClosed(MuxConnection connection) {
		// fire server closed on each remaining channel
		if (logger.isDebugEnabled()) {
			logger.debug("send muxClosed advertisement to "
					+ connection.getSocketManager().getSocketsSize(Socket.TYPE_TCP) + " channel(s)");
		}
		Enumeration elements = connection.getSocketManager().getSockets(Socket.TYPE_TCP);
		int connectionUid = getConnectionUid(connection);
		while (elements.hasMoreElements()) {
			Object ochannel = elements.nextElement();
			if (ochannel instanceof HttpChannel) {
				HttpChannel channel = (HttpChannel) ochannel;
				fireHttpChannelEvent(HttpChannelEvent.Type.SERVER_SOCKET_CLOSE, channel.getId(), connectionUid);
			} else if (ochannel instanceof DemuxClientSocket) {
				((DemuxClientSocket) ochannel).socketClosed();
			}
		}

		_utils.getConnectionManager().removeMuxConnection(connection);
		HttpConnection hc = _utils.getHttpConnection(connection);
		hc.disconnected();
		if (connectionPool != null)
			connectionPool.removeConnection(connection);
	}

	/**
	 * Value of sessionId:
	 * ----------------------------------------------------------------------------------------
	 * Policy ! HTTP cookie ! clid ! comment
	 * ----------------------------------------------------------------------------------------
	 * None ! / ! x00000000 ! x=y++ (x can be the same for 2 stacks)
	 * ----------------------------------------------------------------------------------------
	 * Client_ip ! / ! hash64(client_ip) !
	 * ----------------------------------------------------------------------------------------
	 * Cookie ! no ! DDDDDDDDx ! x = hash64(nextenso+stackUid+(y++))
	 * !-------------!---------------------------------------------------------------
	 * ! yes ! hash64(cookie) !
	 * ----------------------------------------------------------------------------------------
	 */
	@Override
	public void muxData(MuxConnection connection, MuxHeader header, byte[] buf, int off, int len) {
		MuxHeaderV0 headerV0 = (MuxHeaderV0) header;
		long sessionId = headerV0.getSessionId();
		int channelId = headerV0.getChannelId();
		int flags = headerV0.getFlags();
		boolean logDebug = _logger.isDebugEnabled();
		String logDebug_text = null;
		if (logDebug)
			logDebug_text = getDataDescription(len, sessionId, channelId, flags);

		if ((flags & Utils.FILTER_MASK) == Utils.CLIENT_FLAGS) {
			// this is client traffic
			if (logDebug) {
				logDebug_text += " :\tHttp Client Data";
				_logger.debug(logDebug_text);
			}

			HttpConnection hc = _utils.getHttpConnection(connection);
			((HttpConnectionImpl) hc).dataReceived(headerV0, buf, off, len);

			// Eventually acknowledge a CLOSED message.
			if ((flags & Utils.NO_FILTER_MASK) == Utils.CLOSED) {
				if ((flags & Utils.ACK_MASK) == 0) {
					flags |= Utils.ACK_MASK;
					headerV0.set(headerV0.getSessionId(), headerV0.getChannelId(), flags);
					sendClose(connection, headerV0);
				}
			}
			return;
		}

		// this is proxy traffic
		try {
			Client client;
			HttpChannel channel;
			switch (flags) {

			case (Utils.PRE_FILTER_FLAGS | Utils.DATA): // received pre-data
				if (logDebug) {
					logDebug_text += " :\tClient Request";
					_logger.debug(logDebug_text);
				}
				channel = HttpChannel.getChannel(connection, channelId, sessionId, true, _utils);
				channel.storeRequestData(buf, off, len);

				Client oldClient = channel.getClient();
				client = getClient(connection, sessionId, true /* create if not exists */);
				// ==> IMSAS0FAG239497 + IMSAS0FAG258636
				if (_logger.isDebugEnabled() && oldClient != null)
					_logger.debug(
							"old=" + oldClient + ",new=" + client + ",tmp=" + oldClient.isTempClid() + ",keepalive="
									+ oldClient.isKeepAlive() + ",accessed=" + oldClient.getSession().isAccessed());
				if ((oldClient != null) && (client != oldClient) && (oldClient.isTempClid() || !oldClient.isKeepAlive())
						&& !oldClient.getSession().isAccessed()) {
					oldClient.close(false);
				}
				// <== IMSAS0FAG239497 + IMSAS0FAG258636
				channel.setClient(client);
				client.accessedOnReq();
				if (/* _haManager.isHaEnabled() */ false && client.isNew() && client.isKeepAlive()) {
					channel.retrieveSessionIdFromHeader();
					// try to recover the session before handling request
					// HAManager.getInstance().recoverSession(client, channel);
				} else {
					channel.handleRequest();
				}
				break;

			case (Utils.PRE_FILTER_FLAGS | Utils.CLOSED): // received client close message
				if (logDebug) {
					logDebug_text += " :\tClient Close";
					_logger.debug(logDebug_text);
				}
				fireHttpChannelEvent(HttpChannelEvent.Type.CLIENT_SOCKET_CLOSE, channelId,
						getConnectionUid(connection));
				channel = HttpChannel.getChannel(connection, channelId, sessionId, false, _utils);
				if (channel == null) {
					if (doKeepAlive(sessionId)) {
						if (_logger.isInfoEnabled()) {
							if (!logDebug)
								logDebug_text = getDataDescription(len, sessionId, channelId, flags)
										+ " :\tClient Close";
							_logger.info(logDebug_text + " - Cannot find matching Channel");
						}
					}
				} else {
					channel.handleClientClose();
				}
				break;

			case (Utils.POST_FILTER_FLAGS | Utils.DATA): // received post-data
				if (logDebug) {
					logDebug_text += " :\tServer Response";
					_logger.debug(logDebug_text);
				}

				channel = HttpChannel.getChannel(connection, channelId, sessionId, false, _utils);
				if (channel == null && !doRequestFiltering()) {
					// We are filtering responses only and we must now create the channel for the
					// response.
					channel = HttpChannel.getChannel(connection, channelId, sessionId, true, _utils);
					client = getClient(connection, sessionId, true);
					channel.setClient(client);
				}
				if (channel == null) {
					if (!logDebug)
						logDebug_text = getDataDescription(len, sessionId, channelId, flags) + " :\tServer Response";
					_logger.error(logDebug_text + " - Cannot find matching Channel");
					sendCloseError(connection, sessionId, channelId, false);
				} else {
					channel.handleResponse(buf, off, len);
				}
				break;

			case (Utils.POST_FILTER_FLAGS | Utils.CLOSED): // received server close message
				if (logDebug) {
					logDebug_text += " :\tServer Close";
					_logger.debug(logDebug_text);
				}
				fireHttpChannelEvent(HttpChannelEvent.Type.SERVER_SOCKET_CLOSE, channelId,
						getConnectionUid(connection));
				channel = HttpChannel.getChannel(connection, channelId, sessionId, false, _utils);
				if (channel != null) {
					channel.handleServerClose();
				} else {
					if (_logger.isInfoEnabled()) {
						if (!logDebug) {
							logDebug_text = getDataDescription(len, sessionId, channelId, flags) + " :\tServer Close";
						}
						_logger.info(logDebug_text + " - Cannot find matching Channel");
					}
					sendCloseError(connection, sessionId, channelId, false);
				}
				break;

			case (Utils.PRE_FILTER_FLAGS | Utils.WENT_THROUGH): // cannot happen for now
				if (logDebug) {
					logDebug_text += " :\tRequest went through";
					_logger.debug(logDebug_text);
				}
				// nothing to do yet
				break;

			case (Utils.POST_FILTER_FLAGS | Utils.WENT_THROUGH): // response passed by without being filtered
				if (logDebug) {
					logDebug_text += " :\tResponse went through";
					_logger.debug(logDebug_text);
				}
				if (doReq == 0)
					break;
				client = getClient(connection, sessionId, false);
				channel = HttpChannel.getChannel(connection, channelId, sessionId, false, _utils);
				if (client != null && channel != null)
					channel.handleRespWentThrough();
				else {
					if (!logDebug)
						logDebug_text = getDataDescription(len, sessionId, channelId, flags)
								+ " :\tResponse went through";
					logDebug_text += (client == null) ? " - Cannot find matching Client"
							: " - Cannot find matching Channel";
					_logger.error(logDebug_text);
				}
				break;

			case (Utils.PRE_FILTER_FLAGS | Utils.SESSION): // a session is modified
				if (logDebug) {
					logDebug_text += " :\tSession id switch";
					_logger.debug(logDebug_text);
				}
				Hashtable<Long, Client> clients = _utils.getClients();
				synchronized (clients) {
					Object o = clients.remove(sessionId);
					if (o != null) {
						client = (Client) o;
						long clid = MuxUtils.get_64(buf, off, false);
						client.switchId(clid);
						clients.put(clid, client);
						break;
					}
				}
				if (logDebug) {
					logger.debug(logDebug_text + " - Cannot find matching Client");
				}
				break;

			case (Utils.PRE_FILTER_FLAGS | Utils.WEBSOCKET): // WebSocket data
				if (logDebug) {
					logDebug_text += " :\tWebsocket PDU";
					_logger.debug(logDebug_text);
				}
				client = getClient(connection, sessionId, true /* create if not exists */);
				channel = HttpChannel.getChannel(connection, channelId, sessionId, false, _utils);
				if (channel != null) {
					channel.setClient(client);
					ByteBuffer pdu = ByteBuffer.allocate(len);
					pdu.put(buf, off, len);
					pdu.position(0);
					channel.handleWebSocket(pdu);
				} else {
					if (logDebug) {
						logDebug_text += " dropped (unknown channel)";
						_logger.debug(logDebug_text);
					}
				}
				break;

			default:
				// MAY BE DNS
				String[] dnsResponse = DNSParser.parseDNSResponse(flags, channelId, buf, off);
				if (dnsResponse != null) {
					DNSManager.notify(sessionId, dnsResponse, -channelId);
					break;
				}
				if ((flags & Utils.ACK_MASK) == Utils.ACK_MASK) { // Close Ack
					if (logDebug) {
						logDebug_text += " :\tAck";
						_logger.debug(logDebug_text);
					}
					channel = HttpChannel.getChannel(connection, channelId, sessionId, false, _utils);
					if (channel != null) {
						channel.handleAck(flags);
						break;
					}
					// it is likely a ack triggered by a timeout or a close error - silently discard
					// for
					// now
				} else {
					if (!logDebug)
						logDebug_text = getDataDescription(len, sessionId, channelId, flags) + " :\t???";
					_logger.error(logDebug_text + " - Unknown flags");
				}
				break;
			}
		} catch (Throwable t) {
			if (!logDebug)
				logDebug_text = getDataDescription(len, sessionId, channelId, flags) + " :\tUnknown Exception";
			_logger.error(logDebug_text, t);
			sendCloseError(connection, sessionId, channelId, true);
		}
	}

	// MODE DEMUX INTERFACE

	@Override
	public void tcpSocketAborted(MuxConnection connection, int sockId) {
		// Should never happen since "abort" is never called
		if (_logger.isDebugEnabled())
			_logger.debug("tcpSocketAborted sockId=" + sockId);
		tcpSocketClosed(connection, sockId);
	}

	@Override
	public void tcpSocketClosed(MuxConnection connection, int sockId) {

		if (_logger.isDebugEnabled())
			_logger.debug("tcpSocketClosed sockId=" + sockId);
		DemuxSocket socket = (DemuxSocket) connection.getSocketManager().removeSocket(Socket.TYPE_TCP, sockId);
		if (socket != null) {
			socket.socketClosed();
		} else {
			connectionPool.removeEndPoint(connection, sockId);
		}
		HttpConnectionDemux cnx = connection.attachment();
		HttpCnxMeters cnxMeters = cnx.getMeters();

		if (cnxMeters != null) {
			cnxMeters.channelClosed();
		}
	}

	@Override
	public void tcpSocketConnected(MuxConnection connection, int sockId, String remoteIP, int remotePort,
			String localIP, int localPort, String virtualIP, int virtualPort, boolean secure, boolean clientSocket,
			long connectionId, int errno) {
		handleTcpSocketConnected(connection, sockId, localIP, localPort, remoteIP, clientSocket, connectionId, secure,
				errno);
	}

	private void handleTcpSocketConnected(MuxConnection connection, int sockId, String localIP, int localPort,
			String remoteIP, boolean clientSocket, long connectionId, boolean secure, int errno) {
		if (_logger.isDebugEnabled())
			_logger.debug("tcpSocketConnected sockId=" + sockId + ",connectionId=" + connectionId + ",client="
					+ clientSocket + ", localIP=" + localIP + ", localPort=" + localPort);
		HttpConnectionDemux cnx = connection.attachment();
		HttpCnxMeters cnxMeters = cnx.getMeters();
		if (clientSocket) {
			if (!connectionPool.hasEndPoint(connection, localIP, localPort)) {
				// create endPoint as needed if the IP wasnt seen in the tcpSocketListening call
				connectionPool.addEndPoint(connection, localIP, localPort, secure, sockId);
			}
			long endPointId = ((localPort & 0xFFFFFFFFL) << 32) | (localIP.hashCode() & ((long) 0xFFFFFFFFL));
			HttpPipeline pipeline = new HttpPipeline(sockId, connection, endPointId, _utils, remoteIP, secure);
			connection.getSocketManager().addSocket(pipeline);
		} else { // PROXY or HttpClient
			if (errno == MuxUtils.ERROR_UNDEFINED)
				errno = MuxUtils.ERROR_CONNECTION_REFUSED;
			connectionPool.socketConnected(connection, connectionId, sockId, errno);
		}
		if (errno == 0 && cnxMeters != null)
			cnxMeters.channelConnected(clientSocket);
	}

	@Override
	public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, byte[] data, int off, int len) {
		if (_logger.isDebugEnabled())
			_logger.debug("tcpSocketData sockId=" + sockId + " data=\n" + new String(data, off, len));
		DemuxSocket socket = (DemuxSocket) connection.getSocketManager().getSocket(Socket.TYPE_TCP, sockId);
		if (socket != null) {
			socket.socketData(data, off, len);
		} else {
			if (_logger.isDebugEnabled())
				_logger.debug("tcpSocketData dropped cnx=" + connection + ",sockId=" + sockId);
		}
	}

	@Override
	public void tcpSocketData(MuxConnection connection, int sockId, long sessionId, ByteBuffer data) {
		tcpSocketData(connection, sockId, sessionId, data.array(), data.position(), data.remaining());
	}

	@Override
	public void tcpSocketListening(MuxConnection connection, int sockId, String localIP, int localPort, boolean secure,
			long listenId, int errno) {
		if (errno == 0) {
			if (_logger.isInfoEnabled())
				_logger.info("tcpSocketListening sockId=" + sockId + " on " + localIP + ":" + localPort);

			// Skip "unspecified" IP passed by the IOH when it listens to all interfaces
			if (!("0.0.0.0".equals(localIP)) && !("0:0:0:0:0:0:0:0".equals(localIP) && !("::".equals(localIP)))) {
				connectionPool.addEndPoint(connection, localIP, localPort, secure, sockId);
			}
		}
	}

	// =================================================================================

	public boolean sendMuxConfig(MuxConnection connection, ByteBuffer buffer) {
		return connection.sendMuxData(Utils.headerConfigure, true, buffer);
	}

	public boolean sendMuxData(MuxConnection connection, MuxHeader header, boolean copy, ByteBuffer... buffers) {
		if (_logger.isInfoEnabled())
			_logger.info("sendMuxData - dropping data: hdr=" + header, new Throwable());
		return true;
	}

	public boolean sendClose(MuxConnection connection, MuxHeader header) {
		int sockId = header.getChannelId();
		if (sockId != 0) {
			DemuxSocket socket = (DemuxSocket) connection.getSocketManager().getSocket(Socket.TYPE_TCP, sockId);
			if (socket != null) {
				if (_logger.isDebugEnabled())
					_logger.debug("sendClose hdr=" + header);
				return connection.sendTcpSocketClose(header.getChannelId());
			} else {
				if (_logger.isDebugEnabled())
					_logger.debug("socket already closed: ignore sendClose hdr=" + header);
				return true;
			}
		} else {
			if (_logger.isDebugEnabled())
				_logger.debug("ignore sendClose hdr=" + header);
			return true;
		}
	}

	public ConnectionPool getConnectionPool() {
		return connectionPool;
	}

	// =================================================================================

	private void sendCloseError(MuxConnection connection, long clid, int clientSockId, boolean pre) {
		MuxHeaderV0 header = new MuxHeaderV0();
		int PRE_OR_POST = (pre) ? Utils.PRE_FILTER_FLAGS : Utils.POST_FILTER_FLAGS;
		header.set(clid, clientSockId, Utils.CLOSED | PRE_OR_POST);
		sendClose(connection, header);
	}

	private String getDataDescription(int len, long clid, int cliSockId, int flags) {
		StringBuffer buff = new StringBuffer();
		buff.append("received proxy data (len/clid/sockId/flags ");
		buff.append(String.valueOf(len));
		buff.append('/');
		buff.append(Long.toHexString(clid));
		buff.append('/');
		buff.append(String.valueOf(cliSockId));
		buff.append('/');
		buff.append(String.valueOf(flags));
		buff.append(')');
		return buff.toString();
	}

	private void fireHttpChannelEvent(HttpChannelEvent.Type type, int channelId, int connectionUid) {
		HttpProxyletContext context = (HttpProxyletContext) proxyletContainer.getContext();
		if (context != null) { // null if no deployed http proxylets.
			long socketUid = ((channelId & 0xFFFFFFFFL) << 32) | (connectionUid & ((long) 0xFFFFFFFFL));
			HttpChannelEvent event = new HttpChannelEvent(context, context, type, socketUid);
			context.fireProxyletContextEvent(event, false /* synchronous */);
		}
	}

	public HttpMeters getAgentMeters() {
		return httpMeters;
	}

	/************************************
	 * Client management
	 ************************************/

	private Client getClient(MuxConnection connection, long clid, boolean create) {
		Hashtable<Long, Client> clients = _utils.getClients();
		boolean keepAlive = doKeepAlive(clid);
		if (!keepAlive) {
			// Add the connection id. for multi-stacks
			clid = (clid & (((long) 0xFFFFFFFFL) << 32)) | (getConnectionUid(connection) & ((long) 0xFFFFFFFFL));
		}
		synchronized (clients) {
			Client client = (Client) clients.get(clid);
			if (client == null || client.isWaitingForSwitchId()) {
				if (client != null) {
					if (logger.isInfoEnabled()) {
						logger.info("Session cookie was not set - close old false session: " + client);
					}
					client.close(false);
				}
				if (create) {
					client = new Client(clid, keepAlive, _utils);
					clients.put(clid, client);
				}
			} else {
				if ((client.getId() != clid) && client.isTempClid()) {
					// should never happen
					logger.error(Long.toHexString(clid) + " temp clid already used by " + client);
				}
			}
			return client;
		}
	}

	/******************************************************************
	 * session killing
	 ******************************************************************/

	void killAllUsers(boolean stop) {
		// Send close session to the stacks
		synchronized (_utils.getConnectionManager()) {
			Enumeration enumer = _utils.getConnectionManager().getMuxConnections();
			while (enumer.hasMoreElements()) {
				MuxConnection connection = (MuxConnection) enumer.nextElement();
				Enumeration channelEnum = connection.getSocketManager().getSockets(Socket.TYPE_TCP);
				while (channelEnum.hasMoreElements()) {
					Object channel = channelEnum.nextElement();
					if (channel instanceof HttpChannel)
						((HttpChannel) channel).sendSessionClose();
				}
			}
		}

		// close clients
		List<Client> killList = getAllClients();
		for (Client client : killList) {
			client.close(stop);
		}
	}

	/******************************************************************
	 * session listing
	 ******************************************************************/

	void listAllUsers() {
		List<Client> clients = getAllClients();
		StringWriter sw = new StringWriter();
		sw.write("HttpAgent: " + clients.size() + " sessions\n");
		for (Client client : clients) {
			HttpSessionFacade session = client.getSession();
			sw.write("\tid=");
			sw.write(session.getRemoteId());
			sw.write(" clid=");
			sw.write(Long.toHexString(client.getId()));
			if (!client.isKeepAlive()) {
				sw.write("(tmp)");
			}
			sw.write(" created=");
			sw.write(listDateFormat.format(new Date(session.getCreationTime())));
			sw.write(" accessed=");
			sw.write(listDateFormat.format(new Date(session.getLastAccessedTime())));
			sw.write(" timeout=");
			sw.write(Integer.toString(session.getMaxInactiveInterval()));
			sw.write(" pending=");
			sw.write(Integer.toString(client.getPendingRequests()));
			sw.write("\n");
		}
		logger.warn(sw.toString());
	}

	/******************************************************************
	 * Private methods
	 ******************************************************************/

	private List<Client> getAllClients() {
		List<Client> clientList = new ArrayList<Client>();
		Hashtable<Long, Client> clients;
		clients = _utils.getClients();
		synchronized (clients) {
			Enumeration enumer = clients.elements();
			while (enumer.hasMoreElements()) {
				Client client = (Client) enumer.nextElement();
				clientList.add(client);
			}
		}
		return clientList;
	}

	protected void updateAgentConfig(Dictionary agentConf) {
		try {
			if (!_initialized) {
				return;
			}
			if (agentConf.get(SESSION_TIMEOUT) != null) {
				long oldValue = Client.getSessionTimeout(0L);
				long newValue = ConfigHelper.getInt(agentConf, SESSION_TIMEOUT, (int) (oldValue / 1000)) * 1000;
				if (oldValue != newValue) {
					Client.setSessionTimeout(newValue);
					if (_logger.isInfoEnabled()) {
						_logger.info("Session timeout changed to: " + newValue + "ms");
					}
					synchronized (_utils.getConnectionManager()) {
						Enumeration enumer = _utils.getConnectionManager().getMuxConnections();
						while (enumer.hasMoreElements()) {
							MuxConnection connection = (MuxConnection) enumer.nextElement();
							sendSessionTimeout(connection);
						}
					}
				}
			}

			if (agentConf.get(REQ_BUFFERING) != null) {
				HttpChannel.buffReq = ConfigHelper.getBoolean(agentConf, REQ_BUFFERING);
				if (doReq != 0)
					_logger.info("Requests will be " + ((HttpChannel.buffReq) ? "Buffered" : "Streamed"));
			}

			if (agentConf.get(RESP_BUFFERING) != null) {
				HttpChannel.buffResp = ConfigHelper.getBoolean(agentConf, RESP_BUFFERING);
				if (doResp != 0)
					_logger.info("Responses will be " + ((HttpChannel.buffResp) ? "Buffered" : "Streamed"));
			}

			if (agentConf.get(STACK_INSTANCE) != null) {
				String stacks = ConfigHelper.getString(agentConf, STACK_INSTANCE, STACK_ANY).trim();
				if (stacks.equals(STACK_ANY)) {
					_logger.info("Available for any http stack");
					getMuxConfiguration().put(CONF_STACK_INSTANCE, new String[0]);
				} else {
					StringTokenizer st = new StringTokenizer(stacks);
					String[] stackInstance = new String[st.countTokens()];
					int k = 0;
					while (st.hasMoreTokens()) {
						stackInstance[k] = st.nextToken();
						_logger.info("Serving http stack: " + stackInstance[k]);
						++k;
					}
					getMuxConfiguration().put(CONF_STACK_INSTANCE, stackInstance);
				}
			}

			if (sessionPolicy == null) { // not dynamic
				String policy = ConfigHelper.getString(agentConf, SESSION_POLICY, POLICY_NONE).trim();
				String cookieName = ConfigHelper.getString(agentConf, SESSION_COOKIE_NAME, COOKIE_JSESSIOND).trim();
				if (cookieName.length() == 0)
					cookieName = COOKIE_JSESSIOND;
				String headerName = ConfigHelper.getString(agentConf, SESSION_HEADER_NAME, HEADER_SESSION).trim();
				if (headerName.length() == 0)
					headerName = HEADER_SESSION;
				sessionPolicy = SessionPolicy.getPolicy(policy, cookieName, headerName);
				if (_logger.isInfoEnabled())
					_logger.info(sessionPolicy);
			}

			viaPseudonym = ConfigHelper.getString(agentConf, VIA_PSEUDONYM);
			if (viaPseudonym != null) {
				if (viaPseudonym.trim().length() == 0) {
					viaPseudonym = null;
				} else {
					viaPseudonym = "\t, 1.1 " + viaPseudonym.trim() + " (Alcatel-Lucent ASR)";
				}
			}

			if (nextHopEvaluator == null) { // not dynamic
				String nextProxyFunction = ConfigHelper
						.getString(agentConf, NEXT_PROXY, NextHopEvaluator.DEFAULT_NEXT_PROXY).trim();
				if (nextProxyFunction.length() == 0)
					nextProxyFunction = NextHopEvaluator.DEFAULT_NEXT_PROXY;
				String connectTunnelingFunction = ConfigHelper
						.getString(agentConf, CONNECT_TUNNELING, NextHopEvaluator.DEFAULT_CONNECT_TUNNEL).trim();
				if (connectTunnelingFunction.length() == 0)
					nextProxyFunction = NextHopEvaluator.DEFAULT_CONNECT_TUNNEL;
				String nextServerFunction = ConfigHelper
						.getString(agentConf, NEXT_SERVER, NextHopEvaluator.DEFAULT_NEXT_SERVER).trim();
				if (nextServerFunction.length() == 0)
					nextServerFunction = NextHopEvaluator.DEFAULT_NEXT_SERVER;
				nextHopEvaluator = new NextHopEvaluator(nextProxyFunction, nextServerFunction,
						connectTunnelingFunction);
			}
			_utils.setHTTP2ClientConfig(agentConf);
			_utils.setHttp2ClientFactory(_h2Factory,
					ConfigHelper.getBoolean(_agentConf, AgentProperties.H2_TRAFFIC_MODE, true));

			HttpPipeline.setSocketTimeout(ConfigHelper.getInt(agentConf, SOCKET_TIMEOUT, 0));
			selfConnection = ConfigHelper.getBoolean(agentConf, SELF_CONNECTION, false);
		}

		catch (Throwable t) {
			_logger.error("Error while handling http agent configuration", t);
		}
	}

	public Utils getUtils() {
		return _utils;
	}
}
