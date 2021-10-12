// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.alcatel.as.service.concurrent.PlatformExecutors;
import com.nextenso.mux.MuxConnection;
import com.nextenso.proxylet.radius.AuthenticationManager;
import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.radius.agent.engine.RadiusProxyletContainer;
import com.nextenso.radius.agent.engine.RadiusProxyletEngine;
import com.nextenso.radius.agent.impl.RadiusMessageFacade;

public class Utils {

	public static final int APP_RADIUS_AGENT = 3;
	public static final int APP_RADIUS_STACK = 263;
	public static final String MODULE_RADIUS_AGENT = "RadiusAgent";
	public static final int APP_RADIUS_IOH = 296;	

	public static final String ATTACHMENT_COMMAND_ATT = "radius.commands";
	public static final String ATTACHMENT_ID_ATT = "radius.id";
	private static final Logger LOGGER = Logger.getLogger("agent.radius.utils");
	private static PlatformExecutors PLATFORM_EXECUTORS = null;
	private static PlatformExecutor THREAD_POOL;
	private static volatile AuthenticationManager _authManager;

	private static final AtomicInteger SOCK_ID = new AtomicInteger (0);
	
	/**
	 * Sets the socket identifier associated to a connection.
	 * 
	 * @param connection The connection.
	 * @param sockId The socket identifier.
	 */
	public static boolean setSockId(MuxConnection connection, int sockId) {
		Object attachment = connection.attachment();
		Map map = null;
		if (attachment instanceof Map) {
			map = (Map) attachment;
		} else {
			map = new HashMap();
			connection.attach(map);
		}
		List<Integer> sockIds = (List<Integer>) map.get (ATTACHMENT_ID_ATT);
		if (sockIds == null)
		    map.put(ATTACHMENT_ID_ATT, sockIds = new ArrayList<> ());
		sockIds.add (Integer.valueOf(sockId));
		return sockIds.size () == RadiusProperties.getClientSrcPortRange ();
	}

	/**
	 * Gets the socket identifier associated to a connection.
	 * 
	 * @param connection The connection.
	 * @return The socket identifier or -1 if not found.
	 */
	public static int getSockId(MuxConnection connection) {
		Object attachment = connection.attachment();
		if (attachment != null && attachment instanceof Map) {
			Map map = (Map) attachment;
			List<Integer> res = (List<Integer>) map.get(ATTACHMENT_ID_ATT);
			return res.get ((SOCK_ID.getAndIncrement () & 0x7FFFFFFF) % RadiusProperties.getClientSrcPortRange ());
		}
		return -1;
	}

	private static Map<Object, Command> getCommands(MuxConnection connection) {
		Object attachment = connection.attachment();
		Map<Object, Command> commands = null;
		if (attachment instanceof Map) {
			Map map = (Map) attachment;
			commands = (Map<Object, Command>) map.get(ATTACHMENT_COMMAND_ATT);
		}
		return commands;
	}

	private static Map<Object, Command> setCommands(MuxConnection connection) {
		Object attachment = connection.attachment();
		Map map = null;
		if (attachment instanceof Map) {
			map = (Map) attachment;
		} else {
			map = new HashMap();
		}
		Map<Object, Command> res = new ConcurrentHashMap<>();
		map.put(ATTACHMENT_COMMAND_ATT, res);
		connection.attach(map);
		//final Map fmap = res;
		//new Thread (new Runnable (){
		//	public void run (){
		//	    try{
		//		while (true){
		//		    Thread.sleep (1000);
		//		    System.out.println ("map size="+fmap.size ());
		//		}
		//	    }catch(Throwable t){
		//		t.printStackTrace ();
		//	    }
		//	}
		//    }).start ();
		return res;
	}

    // used for incoming requests
    public static Command getCommand(MuxConnection connection, Key key){
		Map<Object, Command> commands = getCommands(connection);
		Command res = null;
		if (commands != null) {
		    res = commands.get(key);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getCommand:  id=" + key + ", res=" + res);
		}
		return res;

	}
    // used for incoming responses
    public static Command getCommand(MuxConnection connection, long id){
		Map<Object, Command> commands = getCommands(connection);
		Command res = null;
		if (commands != null) {
		    res = commands.get(id);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getCommand:  id=" + id + ", res=" + res);
		}
		return res;

	}

	/**
	 * Stores a command for a connection.
	 * 
	 * @param connection The connection.
	 * @param command The command.
	 */
    public static void putCommand(MuxConnection connection, Command command){
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("putCommand:  id=" + command.getId() + ", connection=" + connection);
		}
		Map<Object, Command> commands = getCommands(connection);
		if (commands == null) {
			commands = setCommands(connection);
		}
		commands.put(command.getKey(), command);
		commands.put(command.getKey().getId(), command);
	}

	/**
	 * Removes a command for a connection.
	 * 
	 * @param connection The connection.
	 * @param sessionId The identifier.
	 */
	public static void removeCommand(MuxConnection connection, Key key) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeCommand:  id=" + key + ", connection=" + connection);
		}
		Map<Object, Command> commands = Utils.getCommands(connection);
		if (commands != null) {
		    commands.remove (key);
		    commands.remove (key.getId ());
		}
	}

	/**
	 * Removes all the commands for a connection.
	 * 
	 * @param connection The connection.
	 */
	public static void removeCommands(MuxConnection connection) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("removeCommands:  all ids for connection=" + connection);
		}
		Map<Object, Command> commands = Utils.getCommands(connection);
		if (commands != null) {
			commands.clear();
		}
	}

	/**
	 * Removes the local value from the Proxy-State attribute.
	 * 
	 * @param message The RADIUS message.
	 */
	public static void handleResponseProxyState(RadiusMessageFacade message) {
		RadiusAttribute ps = message.getRadiusAttribute(RadiusUtils.PROXY_STATE);
		if (ps == null) {
			return;
		}
		ps.removeValue(ps.getValueSize() - 1);
		if (ps.getValueSize() == 0)
			message.removeRadiusAttribute(RadiusUtils.PROXY_STATE);
	}

	public static long getRequestId(byte[] b, int off, int len) {
		if (len != 8) {
			return -1L;
		}
		long l = ((long) b[off]) << 56;
		l |= ((long) b[off + 1] << 48) & 0x00FF000000000000L;
		l |= ((long) b[off + 2] << 40) & 0x0000FF0000000000L;
		l |= ((long) b[off + 3] << 32) & 0x000000FF00000000L;
		l |= ((long) b[off + 4] << 24) & 0x00000000FF000000L;
		l |= ((long) b[off + 5] << 16) & 0x0000000000FF0000L;
		l |= ((long) b[off + 6] << 8) & 0x000000000000FF00L;
		l |= b[off + 7] & 0x00000000000000FFL;
		return l;
	}

	public static void setRequestId(long id, byte[] res, int offset) {
		res[offset] = (byte) (id >> 56);
		res[offset + 1] = (byte) (id >> 48);
		res[offset + 2] = (byte) (id >> 40);
		res[offset + 3] = (byte) (id >> 32);
		res[offset + 4] = (byte) (id >> 24);
		res[offset + 5] = (byte) (id >> 16);
		res[offset + 6] = (byte) (id >> 8);
		res[offset + 7] = (byte) id;
	}

	/**
	 * Copy the array to another new one.
	 * 
	 * @param src The source array.
	 * @return The copied array.
	 */
	public static byte[] copyArray(byte[] src) {
		if (src == null) {
			return null;
		}

		int len = src.length;
		byte[] res = new byte[len];
		System.arraycopy(src, 0, res, 0, len);
		return res;
	}

	public static AuthenticationManager getAuthenticationManager() {
		return _authManager;
	}
	
	public static void setAuthenticationManager(AuthenticationManager authManager) {
		_authManager = authManager;
	}

	public static void setPlatformExecutors(PlatformExecutors pfe) {
		PLATFORM_EXECUTORS = pfe;
		THREAD_POOL = pfe.getIOThreadPoolExecutor();
	}

	public static PlatformExecutors getPlatformExecutors() {
		return PLATFORM_EXECUTORS;
	}
	public static PlatformExecutor getCurrentExecutor (){
		return PLATFORM_EXECUTORS.getCurrentThreadContext ().getCurrentExecutor ();
	}

	public static void start(Runnable task) {
		THREAD_POOL.execute(task);
	}

	private static RadiusProxyletContainer CONTAINER = null;

	public static void setContainer(RadiusProxyletContainer container) {
		CONTAINER = container;
	}

	public static RadiusProxyletContainer getContainer() {
		return CONTAINER;
	}

	public static RadiusProxyletEngine getEngine() {
		if (Utils.getContainer() == null) {
			return null;
		}
		return Utils.getContainer().getRadiusProxyletEngine();
	}

    
    public static class Key {
	private static final AtomicLong SEED = new AtomicLong (0);
	private long _msgId, _kId, _auth1, _auth2;
	public Key (long msgId, byte[] authenticator, int index){
	    _kId = SEED.getAndIncrement ();
	    _msgId = msgId;
	    for (int i=index; i<(index + 8); i++){
		_auth1 <<= 8;
		_auth1 |= ((long)authenticator[i]) & 0xFFL;
	    }
	    for (int i=index+8; i<(index + 16); i++){
		_auth2 <<= 8;
		_auth2 |= ((long)authenticator[i]) & 0xFFL;
	    }
	}
	@Override
	public int hashCode (){ return (int) _msgId;}
	@Override
	public boolean equals (Object o){
	    if (o instanceof Key){
		Key other = (Key) o;
		return (other._msgId == _msgId &&
			other._auth1 == _auth1 &&
			other._auth2 == _auth2);
	    }
	    return false;
	}
	@Override
	public String toString (){
	    return new StringBuilder ()
		.append ("Key[id=")
		.append (_kId)
		.append (", msgId=")
		.append (_msgId)
		.append (", auth1=")
		.append (_auth1)
		.append (", auth2=")
		.append (_auth2)
		.append ("]")
		.toString ();
	}

	public long getId (){ return _kId;}
	
    }

}
