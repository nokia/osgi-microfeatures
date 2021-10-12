// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.ExecutorPolicy;
import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.alcatel.as.service.concurrent.TimerService;
import com.alcatel.as.service.metering.Counter;
import com.alcatel.as.service.metering.Gauge;
import com.alcatel.as.service.metering.Meter;
import com.alcatel.as.service.metering.MeteringService;
import com.alcatel.as.service.metering.Rate;
import com.nextenso.diameter.agent.engine.DiameterProxyletEngine;
import com.nextenso.diameter.agent.impl.DiameterMessageFacade;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.diameter.agent.impl.DiameterResponseFacade;
import com.nextenso.diameter.agent.peer.Capabilities;
import com.nextenso.diameter.agent.peer.LocalPeer;
import com.nextenso.diameter.agent.peer.Peer;
import com.nextenso.diameter.agent.peer.RouteTableManager;
import com.nextenso.diameter.agent.peer.TableManager;
import com.nextenso.mux.MuxConnection;
import com.nextenso.mux.util.MuxConnectionManager;
import com.nextenso.proxylet.diameter.CapabilitiesListener;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterConnectionFilter;
import com.nextenso.proxylet.diameter.DiameterFilterTable;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.util.AddressFormat;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.IdentityFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;
import com.nextenso.proxylet.engine.ProxyletEngineException;

public class Utils {

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.utils");

	private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();
	private static final int VERSION = 1;
	public static final String ENC_LEVEL_REQUIRED_S = "required";
	public static final String ENC_LEVEL_FORBIDDEN_S = "forbidden";
	public static final String ENC_LEVEL_OPTIONAL_S = "optional";
	//public static final String ENC_LEVEL_PREFERRED_S = "preferred";

	private final static MuxConnectionManager CONNECTION_MANAGER = new MuxConnectionManager();
	private static volatile PlatformExecutor TPOOL_EXECUTOR;
	private static volatile PlatformExecutors PLATEFORM_EXECUTORS = null;
	private static DiameterProxyletEngine PROXYLET_ENGINE = null;

	private static Map<String, Meter> METERS = Utils.newConcurrentHashMap();

	private static final long SESSION_ID_AVP_CODE = DiameterBaseConstants.AVP_SESSION_ID.getAVPCode();

	private static Capabilities SUPPORTED_APPLICATIONS = new Capabilities();

	private static boolean CHECK_LOOP = true;

	public static boolean isCheckLoop() {
		return CHECK_LOOP;
	}

	public static void setCheckLoop(boolean value) {
		CHECK_LOOP = value;
	}

	public static int getEncLevel(String encLevelS) {
		if (encLevelS.equalsIgnoreCase(ENC_LEVEL_REQUIRED_S))
			return DiameterPeer.ENC_LEVEL_REQUIRED;
		else if (encLevelS.equalsIgnoreCase(ENC_LEVEL_FORBIDDEN_S))
			return DiameterPeer.ENC_LEVEL_FORBIDDEN;
		else if (encLevelS.equalsIgnoreCase(ENC_LEVEL_OPTIONAL_S))
			return DiameterPeer.ENC_LEVEL_OPTIONAL;
		//else if (encLevelS.equalsIgnoreCase (ENC_LEVEL_PREFERRED_S))
		//return DiameterPeer.ENC_LEVEL_PREFERRED;
		else
			throw new IllegalArgumentException("Invalid Encryption Level : " + encLevelS);
	}

	public static void logProxyletException(ProxyletEngineException exc) {
		String text = "A problem occurred while processing a message for  Proxylet=" + exc.getProxylet();
		switch (exc.getType()) {
			case ProxyletEngineException.ENGINE:
				text += " : " + exc.getMessage();
				LOGGER.error(text);
				break;
			case ProxyletEngineException.PROXYLET:
				LOGGER.error(text, exc.getThrowable());
				break;
			default:
		}
	}

	/**
	 * Copies the avp list from the source message to the destination message
	 * 
	 * @param src The source message.
	 * @param dst The destination message
	 * @param defs The AVP definition list.
	 */
	public static void cloneAvps(DiameterMessage src, DiameterMessage dst, DiameterAVPDefinition[] defs) {
		for (int i = 0; i < defs.length; i++) {
			cloneAvp(src, dst, defs[i]);
		}
	}

	/**
	 * Copies the avp from the source message to the destination message
	 * 
	 * @param src The source message.
	 * @param dst The destination message
	 * @param def The AVP definition.
	 */
	public static void cloneAvp(DiameterMessage src, DiameterMessage dst, DiameterAVPDefinition def) {
		DiameterAVP avp = src.getDiameterAVP(def);
		if (avp != null) {
			dst.addDiameterAVP((DiameterAVP) avp.clone());
		}
	}

	public static void start(Runnable runnable) {
		try {
			TPOOL_EXECUTOR.execute(runnable);
		}
		catch (RejectedExecutionException e) {
			LOGGER.error("Unexpected exception while starting runnable", e);
		}
	}

	/**************** message and AVP settings **************/

	/**
	 * Adds routing and origin AVPs for responses.
	 * 
	 * @param response
	 */
	public static void handleRoutingAVPs(DiameterResponseFacade response) {
		// we initiated the response
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("handleRoutingAVPs (response): isLocal=" + response.isLocalOrigin());
		}
		if (response.isLocalOrigin()) {
			setOriginAVPs(response);
			setProxyInfo(response);
		} else {
			// the response is proxied
			if (DiameterProperties.routeRecordInResponses ()) addRouteRecordAVP(response);
			unsetLocalProxyInfo(response);
		}
	}

	/**
	 * Adds routing and origin AVPs for requests.
	 * 
	 * @param request
	 */
	public static void handleRoutingAVPs(DiameterRequestFacade request) {
		// we forward the request
		if (request.isLocalOrigin()) {
			setOriginAVPs(request);
		} else {
			addRouteRecordAVP(request);
		}
		setLocalProxyInfo(request);
	}

	/**
	 * Creates a concurrent hash map. The concurrency level used is 16 by default,
	 * or the number of available processors if this number if greater than 16.
	 * 
	 * @return a new concurrent hash map.
	 */
	public static <K, V> ConcurrentHashMap<K, V> newConcurrentHashMap() {
		int concurrencyLevel = Math.max(16, PROCESSORS);
		return new ConcurrentHashMap<K, V>(16, 0.75f, concurrencyLevel);
	}

	private static void addRouteRecordAVP(DiameterMessageFacade message) {
		LOGGER.debug("addRouteRecordAVP...");
		if (! message.isSupportingRouteRecord()) {
			LOGGER.debug("addRouteRecordAVP: message does not support Route-Record AVP -> do not add a value.");
			return;
		}
		
		DiameterAVP originHostAvp = null;
		String hostToVerify = null;
		if (message.isRequest()) {
			if (message.getClientPeer() != null) {
				originHostAvp = ((Peer) message.getClientPeer()).getOriginHostAvp();
			}
			if (DiameterProperties.isSupportingNoRouteRecordForHost()) {
				DiameterAVP avp = message.getRequestFacade().getDiameterAVP(DiameterBaseConstants.AVP_DESTINATION_HOST);
				if (avp != null) {
					hostToVerify = IdentityFormat.getIdentity(avp.getValue());
				}
			}
		} else {
			if (message.getServerPeer() != null) {
				originHostAvp = ((Peer) message.getServerPeer()).getOriginHostAvp();
			}
			if (DiameterProperties.isSupportingNoRouteRecordForHost()) {
				DiameterAVP avp = message.getRequestFacade().getDiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
				if (avp != null) {
					hostToVerify = IdentityFormat.getIdentity(avp.getValue());
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addRouteRecordAVP: message=" + message + "\nclient peer=" + message.getClientPeer() + "\nserver peer=" + message.getServerPeer()
					+ "\norigin host AVP=" + originHostAvp + ", host to verify=" + hostToVerify);
		}

		if (originHostAvp == null) {
			LOGGER.warn("Cannot add a route record for a message=" + message);
			return;
		}

		if (DiameterProperties.hasNoRouteRecordForHost(hostToVerify)) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("addRouteRecordAVP: do not add a Route-Record because of the configuration");
			}
			return;
		}

		DiameterAVP avp = message.getDiameterAVP(DiameterBaseConstants.AVP_ROUTE_RECORD);
		byte[] value = originHostAvp.getValue();
		if (avp == null) {
			avp = new DiameterAVP(DiameterBaseConstants.AVP_ROUTE_RECORD);
			message.addDiameterAVP(avp);
		} else {
			byte[] lastValue=avp.getValue(avp.getValueSize()-1);
			if (Arrays.equals(value, lastValue)) {
				value = null;
			}

		}
		if (value !=null) {
			avp.addValue(value, true);
		}
	}

	private static void setOriginAVPs(DiameterMessageFacade message) {
		message.setOriginHostAVP();
		message.setOriginRealmAVP();
	}

	private static void setLocalProxyInfo(@SuppressWarnings("unused") DiameterMessageFacade message) {
		// not used yet
	}

	private static void setProxyInfo(DiameterMessageFacade message) {
		DiameterAVP requestAVP = message.getRequestFacade().getDiameterAVP(DiameterBaseConstants.AVP_PROXY_INFO);
		if (requestAVP == null) {
			// OK
		} else {
			DiameterAVP responseAVP = message.getDiameterAVP(DiameterBaseConstants.AVP_PROXY_INFO);
			if (responseAVP == null) {
				message.addDiameterAVP((DiameterAVP) requestAVP.clone());
			}
		}
	}

	private static void unsetLocalProxyInfo(@SuppressWarnings("unused") DiameterMessageFacade message) {
		// not used yet
	}

	/**
	 * Gets the handler name.
	 * 
	 * @param connection The mux connection.
	 * @return The handler name.
	 */
	public static String getHandlerName(MuxConnection connection) {
		// in legacy : getStackInstance returns group__instance --> hence instance is the token after __
		// in blueprint : getStackInstance returns platform.group__component.instance --> hence instance is the last token
		// to accomodate both cases : we try to spot the first '.' after the '__'
		String res = connection.getStackInstance();
		int index = res.lastIndexOf("__"); // or indexOf should work too
		if (index >= 0) {
			index+=2;
			int nextIndex = res.indexOf ('.', index);
			if (nextIndex != -1)
				index = nextIndex+1;
			res = res.substring(index);
		}
		return res;
	}

	/**
	 * Gets the local peer.
	 * 
	 * @param handlerName The handler name.
	 * @return The local peer or null if not found.
	 */
	public static LocalPeer getClientLocalPeer(String handlerName) {
		return (LocalPeer) Utils.getTableManager().getLocalDiameterPeer(handlerName);
	}

	/**
	 * --------------------------------------------------------------------------
	 */
	private static final String MACRO_LEGACY = "%LEGACY%";
	private static final String MACRO_AGENT = "%AGENT%";
	private static final String MACRO_HANDLER = "%STACK%";
	private static final String MACRO_HANDLER_ALIAS = "%HANDLER%";

	/**
	 * Builds the origin host when the agent acts as a server according to the
	 * handler name.
	 * 
	 * @param handlerName The handler name.
	 * @return The origin host.
	 */

	public static String getServerOriginHost(String handlerName) {
		String serverValue = DiameterProperties.getOriginHostServerParam();
		boolean useLegacy = (MACRO_LEGACY.equals(serverValue));
		String res = null;
		if (useLegacy) {
			String legacyValue = DiameterProperties.getOriginHostParam();
			if (legacyValue == null || "".equals(legacyValue.trim()) || ".".equals(legacyValue.trim())) {
				res = getAgentInstanceName();
			} else {
				res = legacyValue;
			}
		} else {
			if (serverValue == null || "".equals(serverValue.trim()) || ".".equals(serverValue.trim())) {
				serverValue = MACRO_HANDLER;
			}
			res = serverValue.trim().replaceAll(MACRO_HANDLER, getOriginHostHandlerName(handlerName)).replaceAll(MACRO_HANDLER_ALIAS, getOriginHostHandlerName(handlerName));
		}
		res = addRealm(res);

		return res;
	}

	private static String getOriginHostHandlerName(String handlerName) {
		if (handlerName == null) {
			return null;
		}
		String res = handlerName;
		int index = res.lastIndexOf("--");
		if (index >= 0) {
			res = res.substring(0, index);
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getOriginHostHandlerName: handlerName=" + handlerName + ", res=" + res);
		}

		return res;
	}

	/**
	 * Builds the origin host when the agent acts as a client according to the
	 * handler name and the agent name.
	 * 
	 * @param handlerName The handler name.
	 * @return The origin host.
	 */
	public static String getClientOriginHost(String handlerName) {
		String clientValue = DiameterProperties.getOriginHostClientParam();
		boolean useLegacy = (MACRO_LEGACY.equals(clientValue));
		String res = null;
		if (useLegacy) {
			String legacyValue = DiameterProperties.getOriginHostParam();
			if (legacyValue == null || "".equals(legacyValue.trim()) || ".".equals(legacyValue.trim())) {
				res = getAgentInstanceName();
			} else {
				res = legacyValue;
			}
		} else {
			if (clientValue == null || "".equals(clientValue.trim()) || ".".equals(clientValue.trim())) {
				clientValue = MACRO_HANDLER + "." + MACRO_AGENT;
			}
			res = clientValue.trim().replaceAll(MACRO_HANDLER, getOriginHostHandlerName(handlerName)).replaceAll(MACRO_HANDLER_ALIAS, getOriginHostHandlerName(handlerName));
			res = res.replaceAll(MACRO_AGENT, getAgentName());
		}
		res = addRealm(res);
		return res;
	}

	/**
	 * Adds the realm if configured.
	 * 
	 * @param originHost The origin host.
	 * @return The origin host with the realm if needed.
	 */
	private static String addRealm(String originHost) {

		String originRealm = DiameterProperties.getOriginRealm();
		boolean appendRealm = DiameterProperties.isRealmAppended();
		String res = originHost;
		if (appendRealm) {
			if (!res.endsWith(originRealm)) {
				if (!res.endsWith(".")) {
					res += ".";
				}
				res += originRealm;
			}
		}

		return res;
	}

	/**
	 * -------------------------------------------------------------------------
	 */

	private static String AGENT_NAME = null;
	private static String AGENT_INSTANCE_NAME = null;

	private static String getAgentName() {
		return AGENT_NAME;
	}

	private static String getAgentInstanceName() {
		return AGENT_INSTANCE_NAME;
	}

	public static void setAgentInstanceName(String name) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setAgentInstanceName: name=" + name);
		}
		if (AGENT_INSTANCE_NAME == null && name != null) {
			AGENT_INSTANCE_NAME = name;

			String res = AGENT_INSTANCE_NAME;
			int begin = res.lastIndexOf("__");
			int end = res.lastIndexOf("-");
			if (end < 0) {
				end = res.length();
			}
			if (begin < end) {
				if (begin > 0) {
					res = res.substring(begin + 2, end);
				} else {
					res = res.substring(0, end);
				}
			}
			AGENT_NAME = res;

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("setAgentInstanceName: instance=" + getAgentInstanceName() + ", agent=" + getAgentName());
			}
		}

	}

	/**
	 * -------------------------------------------------------------------------
	 */
	static final private List<String> HANDLER_NAMES = new CopyOnWriteArrayList<String>();
	static final private Map<String, MuxConnection> CONNECTIONS_BY_HANDLERNAME = new HashMap<String, MuxConnection>();
	static final private AtomicInteger INDEX_HANDLERNAME = new AtomicInteger(0);

	private static Thread THREAD;

	private static MeteringService METERING_SERVICE = null;
	
	public static MuxConnection getMuxConnectionByHandlerName(String handler) {
		return CONNECTIONS_BY_HANDLERNAME.get(handler);
	}

	/**
	 * Adds a handler name.
	 * 
	 * @param handler The handler name to be added.
	 * @param connection
	 */
	public static void addHandler(String handler, MuxConnection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("addHandler: handler=" + handler + ", connection=" + connection);
		}
		if (handler == null) {
			return;
		}
		HANDLER_NAMES.add(handler);
		CONNECTIONS_BY_HANDLERNAME.put(handler, connection);
	}

	/**
	 * Removes an handler.
	 * 
	 * @param handler The handler name to be removed.
	 * @return The name of the handler.
	 */
	public static boolean removeHandler(String handler) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeHandler: handler=" + handler);
		}
		CONNECTIONS_BY_HANDLERNAME.remove(handler);
		return HANDLER_NAMES.remove(handler);
	}

	public static List<String> getHandlerNames() {
		List<String> res = new ArrayList<String>();
		res.addAll(HANDLER_NAMES);
		return res;
	}

	/**
	 * Gets the next handler name to be used.
	 * 
	 * @return The name of the next handler.
	 */
	public static String getNextHandlerName() {
		String res = null;
		List<String> names = getHandlerNames();
		if (!names.isEmpty()) {
			int index = (INDEX_HANDLERNAME.incrementAndGet() & 0x7FFFFFFF)% names.size();
			res = names.get(index);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getNextHandlerName: res=" + res);
		}

		return res;
	}

	/**
	 * Gets the next handler name to be used.
	 * 
	 * @param name The name of the current handler.
	 * 
	 * @return The name of the next handler to be used.
	 */
	public static String getNextHandlerName(String name) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getNextHandlerName(String): name=" + name);
		}
		String res = null;
		List<String> names = getHandlerNames();
		if (!names.isEmpty()) {
			int index = names.indexOf(name);
			index = ++index % names.size();
			res = names.get(index);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getNextHandlerName(String): res=" + res);
		}

		return res;
	}

	public static int getHandlerNumber() {
		return HANDLER_NAMES.size();
	}

	public static MuxConnectionManager getConnectionManager() {
		return CONNECTION_MANAGER;
	}

	public static void clearConnectionManager() {
		MuxConnectionManager manager = getConnectionManager();
		List<Integer> l = new ArrayList<Integer>();

		for (Enumeration<MuxConnection> enumeration = manager.getMuxConnections(); enumeration.hasMoreElements();) {
			MuxConnection connection = enumeration.nextElement();
			l.add(connection.getId());
		}
		for (int id : l) {
			manager.removeMuxConnection(id);
		}
	}

	public static MuxConnection getMuxConnection(String handlerName) {
		if (handlerName != null) {
			MuxConnectionManager manager = getConnectionManager();

			for (Enumeration<MuxConnection> enumeration = manager.getMuxConnections(); enumeration.hasMoreElements();) {
				MuxConnection connection = enumeration.nextElement();
				String name = getHandlerName(connection);
				if (handlerName.equals(name)) {
					return connection;
				}
			}

		}
		return null;
	}

	public static int getVersion() {
		return VERSION;
	}

	public static void setPlatformExecutors(PlatformExecutors pfe) {
		PLATEFORM_EXECUTORS = pfe;
		TPOOL_EXECUTOR = pfe.getIOThreadPoolExecutor();
	}

	public static PlatformExecutors getPlatformExecutors (){
		return PLATEFORM_EXECUTORS;
	}

	public static PlatformExecutor getCurrentExecutor() {
		return PLATEFORM_EXECUTORS.getCurrentThreadContext().getCurrentExecutor();
	}

	public static PlatformExecutor getCallbackExecutor() {
		return PLATEFORM_EXECUTORS.getCurrentThreadContext().getCallbackExecutor();
	}

	public static void setAgentPlatformThread() {
		THREAD = Thread.currentThread();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setAgentPlatformThread: thread=" + THREAD.getName() + " " + THREAD);
		}
	}

	public static Thread getAgentPlatformThread() {
		return THREAD;
	}

	/**
	 * Looks up a DiameterAVP in a Grouped AVP value.
	 * 
	 * @param def The AVP definition.
	 * @param data The content.
	 * @param offset The offset.
	 * @param length The length.
	 * @return true if the AVP is present in the data.
	 */
	public static boolean hasDiameterAVP(DiameterAVPDefinition def, byte[] data, int offset, int length) {
		if (length < 8)
			return false;
		int off = offset;
		int len = length;
		while (len > 0) {
			long code = Unsigned32Format.getUnsigned32(data, off);
			int flags = data[off + 4] & 0xFF;
			int dataLength = (data[off + 5] & 0xFF) << 16;
			dataLength |= (data[off + 6] & 0xFF) << 8;
			dataLength |= data[off + 7] & 0xFF;
			off += 8;
			len -= 8;
			dataLength -= 8;
			long vendorId = 0L;
			if ((flags & DiameterAVP.V_FLAG) == DiameterAVP.V_FLAG) {
				if (len < 4)
					return false;
				vendorId = Unsigned32Format.getUnsigned32(data, off);
				off += 4;
				len -= 4;
				dataLength -= 4;
			}
			if (code == def.getAVPCode() && vendorId == def.getVendorId()) {
				return true;
			}
			off += dataLength;
			len -= dataLength;
			// pad
			int pad = dataLength % 4;
			if (pad > 0) {
				pad = 4 - pad;
				off += pad;
				len -= pad;
			}
		}
		return false;
	}

	/**
	 * Creates and returns a DWA with a filled Result code set to
	 * DIAMETER_SUCCESS.
	 * 
	 * @param dwr The request.
	 * @return The DWA.
	 */
	public static DiameterMessageFacade createDWA(DiameterMessageFacade dwr) {
		DiameterResponseFacade dwa = dwr.getResponseFacade();
		dwa.setResultCode(DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS);
		dwa.setLocalOrigin(true);
		return dwa;
	}

	/**
	 * Creates a filled CER.
	 * 
	 * @param peer
	 * 
	 * @return The new CER.
	 */
	public static DiameterRequestFacade createCER(Peer peer, InetSocketAddress[] localIPs) {
		DiameterRequestFacade cer = new DiameterRequestFacade(DiameterBaseConstants.COMMAND_CER, peer);
		cer.setDefaultOriginAVPs();
		setLocalCapabilities(cer, localIPs);

		for (CapabilitiesListener listener : getTableManager().getCapabilitiesListeners()) {
			listener.handleCapabilities(cer);
		}

		return cer;
	}

	private static void setLocalCapabilities(DiameterMessageFacade message, InetSocketAddress[] localIPs) {

		// localIPs = the list of IPs of the endpoint where the msg was received (1 IP for TCP, many for SCTP)
	    
		DiameterAVP hostsAvp = null;
		boolean useUniqueHost = DiameterProperties.useUniqueHostId();
		if (useUniqueHost && localIPs != null){ // check localIPs for safety : should never be null

			// useUniqueHost : we only advert the IP(s) of the endpoint : not all IPs from the jdiameter
			
			if (LOGGER.isDebugEnabled()) {
			    LOGGER.debug("setLocalCapabilities: use localIPs only :");
			    for (InetSocketAddress addr : localIPs) LOGGER.debug (addr);
			}

			if (localIPs != null) {
			    for (InetSocketAddress localIP : localIPs){
				InetAddress address = localIP.getAddress();
				byte[] valueB = address.getAddress();
				byte[] data = null;
				if (address instanceof Inet4Address) {
					data = AddressFormat.toAddress(AddressFormat.IPV4, valueB, 0, 4);
				} else if (address instanceof Inet6Address) {
					data = AddressFormat.toAddress(AddressFormat.IPV6, valueB, 0, valueB.length);
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("setLocalCapabilities: (useUniqueHost) address=" + address + ", data=" + Arrays.toString(data));
				}

				if (data != null) {
					if (hostsAvp == null) hostsAvp = new DiameterAVP(DiameterBaseConstants.AVP_HOST_IP_ADDRESS);
					hostsAvp.addValue(data, false);
				}
			    }
			}
		}

		if (hostsAvp == null) {

			// uniqueHostId = false
			// jdiameter legacy : we take all IPs from the local peer : reactor.ip (which, if not present, defaults to 0.0.0.0 which is resolved)
			// jdiameter with groups : we now take all the IPs that are listening on a port in the group
			
			LocalPeer localPeer = Utils.getClientLocalPeer(message.getHandlerName());
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("setLocalCapabilities: local peer=" + localPeer);
			}
			DiameterAVP localPeerHosts = localPeer.getHostIPAddressesAvp();
			if (localPeerHosts != null) hostsAvp = (DiameterAVP) localPeerHosts.clone(); // may be null if there is no listen ip : jdiameter acting as client only
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setLocalCapabilities: hostsAvp=" + hostsAvp);
		}

		if (hostsAvp != null) message.addDiameterAVP(hostsAvp);
		message.addDiameterAVP((DiameterAVP) DiameterProperties.getVendorIdAvp().clone());
		message.addDiameterAVP((DiameterAVP) DiameterProperties.getProductNameAvp().clone());
		message.addDiameterAVP((DiameterAVP) DiameterProperties.getOriginStateIdAvp().clone());
		DiameterAVP firmware = DiameterProperties.getFirmwareRevisionAvp();
		if (firmware != null) {
			message.addDiameterAVP((DiameterAVP) firmware.clone());
		}

		getCapabilities().fillCapabilitiesMessage(message);
	}

	public static Capabilities getCapabilities() {
		return SUPPORTED_APPLICATIONS;
	}

	/**
	 * 
	 * @param cea
	 * @param resultCode
	 */
	public static long fillCEA(DiameterResponseFacade cea, long resultCode) {
		cea.setDefaultOriginAVPs();
		cea.setResultCode(resultCode);

		// NOTE : E bit not required (even if error)
		// cea.setErrorFlag(false);
		InetSocketAddress[] receptionAddresses = cea.getRequestFacade ().getReceptionAddresses();
		setLocalCapabilities(cea, receptionAddresses);
		cea.setLocalOrigin(true);

		String msg = null;
		if (resultCode == DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
			// no error message is needed
		} else if (resultCode == DiameterBaseConstants.RESULT_CODE_DIAMETER_NO_COMMON_SECURITY) {
			msg = "Invalid Encryption Level";
		} else if (resultCode == DiameterBaseConstants.RESULT_CODE_DIAMETER_UNKNOWN_PEER) {
			msg = "Unknown Peer";
		} else if (resultCode == DiameterBaseConstants.RESULT_CODE_DIAMETER_NO_COMMON_APPLICATION) {
			msg = "No common application";
		}

		if (msg != null) {
			DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_ERROR_MESSAGE);
			avp.setValue(UTF8StringFormat.toUtf8String(msg), false);
			cea.addDiameterAVP(avp);
		}

		for (CapabilitiesListener listener : getTableManager().getCapabilitiesListeners()) {
			listener.handleCapabilities(cea);
		}

		return cea.getResultCode (); // may have been changed by the capabilities listeners
	}

	public static long fillCEA(DiameterMessageFacade cer, DiameterMessageFacade.ParsingException pe){
		DiameterResponseFacade resp = cer.getResponseFacade ();
		resp.setLocalOrigin(true);
		resp.setResultCode (pe.result ());
		resp.setOriginHostAVP ();
		resp.setOriginRealmAVP ();
		resp.addDiameterAVP((DiameterAVP) DiameterProperties.getProductNameAvp().clone());
		resp.addDiameterAVP((DiameterAVP) DiameterProperties.getOriginStateIdAvp().clone());
		DiameterAVP errMsg = pe.errMessageAVP ();
		if (errMsg != null) resp.addDiameterAVP (errMsg);
		if (pe.failedAVP () != null) resp.addDiameterAVP (pe.failedAVP ());
		return pe.result ();
	}
	

	/**
	 * Creates and returns a DPA with a filled Result code set to
	 * DIAMETER_SUCCESS.
	 * 
	 * @param dpr The request.
	 * @return The filled DPA.
	 */
	public static DiameterMessageFacade createDPA(DiameterMessageFacade dpr) {
		DiameterResponseFacade dpa = dpr.getResponseFacade();
		dpa.setResultCode(DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS);
		dpa.setLocalOrigin(true);
		return dpa;
	}

	private static Rate getRate(String name) {
		Rate res = null;

		Meter m = METERS.get(name);
		if (m == null) {
			MeteringService meteringService = METERING_SERVICE;
			if (meteringService != null) {
				res = meteringService.getRate(name);
				METERS.put(name, res);
			}
		} else if (m instanceof Rate) {
			res = (Rate) m;
		}

		return res;
	}

	private static Counter getCounter(String name) {
		Counter res = null;

		Meter m = METERS.get(name);
		if (m == null) {
			MeteringService meteringService = METERING_SERVICE;
			if (meteringService != null) {
				res = meteringService.getCounter(name);
				METERS.put(name, res);
			}
		} else if (m instanceof Counter) {
			res = (Counter) m;
		}

		return res;
	}

	private static Gauge getGauge(String name) {
		Gauge res = null;

		Meter m = METERS.get(name);
		if (m == null) {
			MeteringService meteringService = METERING_SERVICE;
			if (meteringService != null) {
				res = meteringService.getGauge(name);
				METERS.put(name, res);
			}
		} else if (m instanceof Gauge) {
			res = (Gauge) m;
		}

		return res;
	}

	private static final String COUNTER_RETRANSMISSION_NB = "as.stat.diameter.retransmission.nb";

	public static Counter getRetransmissionNbCounter() {
		return getCounter(COUNTER_RETRANSMISSION_NB);
	}

	private static final String COUNTER = "as.stat.diameter.";
	private static final String PREFIX_REQUEST = COUNTER+"request.";
	private static final String PREFIX_RESPONSE = COUNTER+"response.";
	private static final String SUFFIX_INCOMING_SIZE = ".incoming.size";
	private static final String SUFFIX_INCOMING_RATE = ".incoming.nb";
	private static final String SUFFIX_OUTGOING_SIZE = ".outgoing.size";
	private static final String SUFFIX_OUTGOING_RATE = ".outgoing.nb";
	
	public static Counter getMsgSizeCounter(DiameterMessageFacade msg, boolean outgoing) {
		String prefix = msg.isRequest () ? PREFIX_REQUEST : PREFIX_RESPONSE;
		String suffix = outgoing ? SUFFIX_OUTGOING_SIZE : SUFFIX_INCOMING_SIZE;
		String counterName = new StringBuilder(prefix)
			.append(getCommandName (msg, false))
			.append(suffix)
			.toString ();

		Counter res = getCounter(counterName);
		return res;
	}

	public static Rate getMsgNbRate(DiameterMessageFacade msg, boolean outgoing) {
		String prefix = msg.isRequest () ? PREFIX_REQUEST : PREFIX_RESPONSE;
		String suffix = outgoing ? SUFFIX_OUTGOING_RATE : SUFFIX_INCOMING_RATE;
		String counterName = new StringBuilder(prefix)
			.append(getCommandName (msg, false))
			.append(suffix)
			.toString ();
		
		Rate res = getRate(counterName);
		return res;
	}

	private static final String GAUGE_NB_PENDING_MESSAGE = "as.stat.diameter.suspended";

	public static final void removePendingMessage() {
		Gauge gauge = getPendingMessageGauge();
		if (gauge != null) {
			LOGGER.debug("removePendingMessage");
			gauge.add(-1L);
		}
	}

	public static final void addPendingMessage() {
		Gauge gauge = getPendingMessageGauge();
		if (gauge != null) {
			LOGGER.debug("addPendingMessage");
			gauge.add(1L);
		}
	}

	public static final Gauge getPendingMessageGauge() {
		Gauge gauge = getGauge(GAUGE_NB_PENDING_MESSAGE);
		return gauge;
	}

	private static final String GAUGE_NB_SESSIONS_ID = "as.stat.diameter.session.active";

	public static Gauge getNbSessionsGauge() {
		Gauge res = getGauge(GAUGE_NB_SESSIONS_ID);
		return res;
	}

	public static Counter getParsingTimeCounter(DiameterMessageFacade msg) {
		if (msg == null) {
			return null;
		}
		String prefix = msg.isRequest () ? PREFIX_REQUEST : PREFIX_RESPONSE;
		String counterName = new StringBuilder(prefix)
			.append(getCommandName(msg, false))
			.append(".parser.time")
			.toString ();

		Counter res = getCounter(counterName);
		return res;
	}

	public static void setMeteringService(MeteringService service) {
		METERING_SERVICE = service;
	}

	public static String getCommandName(DiameterMessageFacade message, boolean upper) {
		String command = null;
		DiameterCommandDefinition def = DiameterCommandDefinition.Dictionary.getDiameterCommandDefinition(message.getDiameterCommand());
		if (def != null) {
			if (message.isRequest()) {
				command = def.getRequestString(upper);
			} else {
				command = def.getResponseString(upper);
			}
		}
		if (command == null) {
			command = Integer.toString(message.getDiameterCommand());
		}
		return command;
	}

	public static String getApplicationName(long application) {
		String res = null;
		DiameterApplicationDefinition def = DiameterApplicationDefinition.Dictionary.getDiameterApplicationDefinition((int) application);
		if (def != null) {
			res = def.getName();
		}
		return res;
	}

	private static volatile DiameterFilterTable FILTER_TABLE = null;

	private static volatile RouteTableManager ROUTE_TABLE_MANAGER = null;

	private static volatile TimerService _timerService;
	
	public static void setFilterTable(DiameterFilterTable filterTable) {
		FILTER_TABLE = filterTable;
	}
	
	public static DiameterFilterTable getFilterTable() {
		return FILTER_TABLE;
	}

	public static void setWhiteListFilters(List<DiameterConnectionFilter> peers) {
		DiameterFilterTable table = getFilterTable();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setWhiteListFilters: table=" + table);
		}
		if (table == null) {
			return;
		}
		Collection<DiameterConnectionFilter> list = table.getIncomingSocketWhiteList();
		synchronized (list) {
			list.addAll(peers);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("setWhiteListFilters: list=" + list);
			}
		}
	}

	public static long checkConnectionFilters(DiameterPeer peer) {
		DiameterConnectionFilter filter = getConnectionFilter(peer.getOriginHost(), peer.getOriginRealm());
		if (filter == null) {
			return DiameterBaseConstants.RESULT_CODE_DIAMETER_UNKNOWN_PEER;
		}
		if (checkEncryption(peer.isEncrypted(), filter) == false) {
			return DiameterBaseConstants.RESULT_CODE_DIAMETER_NO_COMMON_SECURITY;
		}
		return DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS;
	}

	public static void applyFilters() {

		TableManager table = getTableManager();
		for (DiameterPeer localPeer : table.getLocalDiameterPeers()) {
			List<DiameterPeer> peers = DiameterPeerTable.getDiameterPeerTable().getDiameterPeers(localPeer);
			for (DiameterPeer peer : peers) {
				if (!peer.isLocalDiameterPeer() && !peer.isLocalInitiator()) {
					long res = checkConnectionFilters(peer);
					if (res != DiameterBaseConstants.RESULT_CODE_DIAMETER_SUCCESS) {
						peer.disconnect(DiameterBaseConstants.VALUE_DISCONNECT_CAUSE_DO_NOT_WANT_TO_TALK_TO_YOU);
					}
				}
			}
		}

	}

	public static String toLowerCase(String s) {
		if (s == null) return null;
		return s.toLowerCase(Locale.ENGLISH);
	}
	public static String formatRealm(String realm) {
		if (realm == null) return null;
		if (realm.length() == 0) return "";
		if (realm.charAt(0) == '.') return toLowerCase(realm);
		return new StringBuilder (1+realm.length ()).append ('.').append (toLowerCase(realm)).toString ();
	}

	private static DiameterConnectionFilter getConnectionFilter(String originHost, String originRealm) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getConnectionFilter: originHost=" + originHost + ", originRealm=" + originRealm);
		}
		DiameterFilterTable table = getFilterTable();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getConnectionFilter: table=" + table);
		}

		if (table == null) {
			return null;
		}

		Collection<DiameterConnectionFilter> list = table.getIncomingSocketWhiteList();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getConnectionFilter: searching in white list=" + list);
		}

		DiameterConnectionFilter res = getFilter(list, originHost, originRealm);
		if (res == null) {
			LOGGER.debug("getConnectionFilter: the connection does not fit to a whitelist filter -> null");
			return null;
		}

		list = table.getIncomingSocketBlackList();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getConnectionFilter: searching in black list=" + list);
		}
		DiameterConnectionFilter refusedFilter = getFilter(list, originHost, originRealm);

		if (refusedFilter != null) {
			LOGGER.debug("getConnectionFilter: the connection does fit to a blacklist filter -> null");
			return null;
		}

		return res;
	}

	private static DiameterConnectionFilter getFilter(Collection<DiameterConnectionFilter> list, String originHost, String originRealm) {
		DiameterConnectionFilter res = null;
		originHost = toLowerCase (originHost);
		originRealm = formatRealm (originRealm);
		synchronized (list) {
			for (DiameterConnectionFilter filter : list) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("getFilter: tries filter=" + filter + " with originHost=" + originHost);
				}
				if (originHost != null) {
					String filterHost = toLowerCase (filter.getHost());
					if (filterHost != null) {
						if ("*".equals(filterHost) || originHost.equals(filterHost)) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("getFilter: returns filter=" + filter + " for  originHost=" + originHost);
							}
							return filter;
						}
					}
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("getFilter: tries filter=" + filter + " with originRealm=" + originRealm);
				}
				if (originRealm != null) {
					String filterRealm = formatRealm (filter.getRealm());
					if (filterRealm != null) {
						if (".*".equals(filterRealm)) {
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("getFilter: keeps  filter=" + filter + " for  realm=" + originRealm);
							}
							res = filter;
						} else {
							if (originRealm.equals(filterRealm)) {
								if (LOGGER.isDebugEnabled()) {
									LOGGER.debug("getFilter: keeps  filter=" + filter + " for  realm=" + originRealm);
								}
								res = filter;
							}
						}
					}
				}
			}

		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getFilter: returns  filter=" + res);
		}

		return res;

	}

	private final static boolean checkEncryption(boolean isEncrypted, DiameterConnectionFilter filter) {
		int encLevel = filter.getEncryptionLevel();
		if (isEncrypted) {
			return (encLevel != DiameterPeer.ENC_LEVEL_FORBIDDEN);
		}

		return (encLevel != DiameterPeer.ENC_LEVEL_REQUIRED);
	}

	public final static void setEngine(DiameterProxyletEngine engine) {
		PROXYLET_ENGINE = engine;
	}

	public final static DiameterProxyletEngine getEngine() {
		return PROXYLET_ENGINE;
	}

	public final static String parseSessionId(byte[] data) {

		String res = null;
		int len = data.length;
		int offset = 20;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("parseSessionId: len=" + len + ", offset=" + offset);
		}

		while (offset < len && res == null) {
			long code = Unsigned32Format.getUnsigned32(data, offset);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("parseSessionId: offset=" + offset + ", code=" + code);
			}
			int avpLen = data[offset + 5] & 0xFF;
			avpLen <<= 8;
			avpLen |= data[offset + 6] & 0xFF;
			avpLen <<= 8;
			avpLen |= data[offset + 7] & 0xFF;
			if (code == SESSION_ID_AVP_CODE) {
				int dataLen = avpLen - 8;
				int flags = data[offset + 4] & 0xFF;
				offset += 8;

				boolean hasVendorId = DiameterAVP.vFlagSet(flags);
				if (hasVendorId) {
					dataLen -= 4;
					offset += 4;
				}

				res = UTF8StringFormat.getUtf8String(data, offset, dataLen);
			} else {
				offset += avpLen;
			}

		}
		return res;
	}

	public static TableManager getTableManager() {
		return TableManager.getInstance();
	}

	public static int getRequestManagerKey(DiameterRequestFacade request) {
		return request.getOutgoingClientHopIdentifier();
	}

	public static void setRouteTableManager(RouteTableManager routeTable) {
		ROUTE_TABLE_MANAGER = routeTable;
	}

	public static RouteTableManager getRouteTableManager() {
		return ROUTE_TABLE_MANAGER;
	}

	public static Peer getPeer(String handlerName, String destHost, String destRealm, long applicationId, int type) {
                if (DiameterProperties.doAbsoluteClientRouting ()){
                        // new behavior : take absolute best
                        DiameterPeer localPeer = Utils.getTableManager().getLocalDiameterPeer(handlerName);
                        if (localPeer == null){
                                // unexpected though
                                localPeer = Utils.getTableManager().getLocalDiameterPeer(Utils.getNextHandlerName(handlerName));
                        }
                        // the following code already finds the absolute best
                        Peer peer = (Peer) Utils.getRouteTableManager().getDestinationPeer(localPeer, destHost, destRealm, applicationId, type, true);
                        if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("changePeer: found peer=" + peer);
                        }
                        return peer;
                } else {
                        // legacy behavior : take first matching peer
                        String name = handlerName;
                        Peer peer = null;
                        // +1  because the current handler can be removed
                        int nbHandlers = Utils.getHandlerNumber() + 1;
                        do {
                                if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("changePeer: Trying to find a peer this handler=" + name);
                                }
                                DiameterPeer localPeer = Utils.getTableManager().getLocalDiameterPeer(name);

                                if (localPeer != null) {
                                        peer = (Peer) Utils.getRouteTableManager().getDestinationPeer(localPeer, destHost, destRealm, applicationId, type, false);
                                }
                                if (LOGGER.isDebugEnabled()) {
                                        LOGGER.debug("changePeer: found peer=" + peer);
                                }
                                if (localPeer == null || peer == null) {
                                        name = Utils.getNextHandlerName(name);
                                }

                                nbHandlers--;
                                if (nbHandlers <= 0) {
                                        if (LOGGER.isDebugEnabled()) {
                                                LOGGER.debug("changePeer: no handler has available peer");
                                        }
                                }

                        }
                        while (peer == null && name != null && nbHandlers > 0);
                        return peer;
                }
        }

    public static void setTimerService(TimerService timerService) {
        _timerService = timerService;        
    }
    
    public static ScheduledFuture<?> schedule(Executor exec, Runnable task, long delay, TimeUnit unit) {
        return  _timerService.schedule(exec, task, delay, unit);
    }
    
    public static ScheduledFuture<?> scheduleAtFixedRate(Executor exec, Runnable task, long initDelay, long delay, TimeUnit unit) {
        return  _timerService.scheduleAtFixedRate(exec, task, initDelay, delay, unit);
    }
    
    public static ScheduledFuture<?> scheduleWithFixedDelay(Executor exec, Runnable task, long initDelay, long delay, TimeUnit unit) {
        return  _timerService.scheduleWithFixedDelay(exec, task, initDelay, delay, unit);
    }

    public static void schedule(Runnable task, String workerId) {
	    if (workerId != null)
		    PLATEFORM_EXECUTORS.getProcessingThreadPoolExecutor (workerId).execute (task, ExecutorPolicy.SCHEDULE); 
	    else
		    PLATEFORM_EXECUTORS.getProcessingThreadPoolExecutor ().execute (task, ExecutorPolicy.SCHEDULE); 
    }
}
