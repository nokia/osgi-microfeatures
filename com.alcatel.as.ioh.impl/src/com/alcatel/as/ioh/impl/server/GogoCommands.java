// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.impl.server;

import java.io.PrintStream;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;
import java.lang.reflect.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.impl.tools.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import org.apache.log4j.Logger;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.service.component.annotations.Component;
import org.osgi.framework.BundleContext;

public class GogoCommands extends GogoCommandsUtils {

    private static final Logger LOGGER = Logger.getLogger ("as.ioh.gogo.server");
    public static final String[] DESC_SERVER_FIELDS_ALL = {
	"TYPE", "ID", "NAME", "ADDRESS", "PROCESSOR", "SINCE", "OPEN", "ACCEPTED", "IP"
    };
    public static final String[] DESC_SERVER_FIELDS_DISPLAY = {
	"TYPE", "ID", "NAME", "ADDRESS", "PROCESSOR", "SINCE", "OPEN", "ACCEPTED"
    };
    public static final String[] DESC_SERVER_FIELDS_FILTER = {
	"TYPE", "ID", "NAME", "ADDRESS", "PROCESSOR", "IP"
    };
    public static final String[] DESC_CLIENT_FIELDS_DISPLAY = {
	"TYPE", "ID", "SERVER", "LOCAL-ADDR", "REMOTE-ADDR", "PROCESSOR", "SINCE", "SEND-BUFFER-SIZE"
    };
    public static final String[] DESC_CLIENT_FIELDS_FILTER = {
	"TYPE", "ID", "SERVER", "LOCAL-ADDR", "REMOTE-ADDR", "PROCESSOR", "LOCAL-IP", "REMOTE-IP"
    };
    public static final String[] DESC_CLIENT_FIELDS_ALL = {
	"TYPE", "ID", "SERVER", "LOCAL-ADDR", "REMOTE-ADDR", "PROCESSOR", "LOCAL-IP", "REMOTE-IP", "SINCE", "SEND-BUFFER-SIZE"
    };
    
    public static void registerCommands (BundleContext ctx){
	registerCommand (runServerCommand, ctx);
	registerCommand (runClientCommand, ctx);
	registerCommand (tcpRunServerCommand, ctx);
	registerCommand (tcpRunClientCommand, ctx);
	registerCommand (sctpRunServerCommand, ctx);
	registerCommand (sctpRunClientCommand, ctx);
    }
    
    protected static Map<Object, Object> getAllClientInfo (AsyncChannel channel, Map<String, Object> props, String filter) throws Exception{
	if (channel.isClosed ()) return null;
	Map<Object, Object> info = new LinkedHashMap<Object, Object> ();
	for (String s : DESC_CLIENT_FIELDS_ALL)
	    info.put (s, getSocketInfo (s, channel, props));
	if (filter (filter, DESC_CLIENT_FIELDS_FILTER, info))
	    return info;
	Method aliasMethod = null;
	Object listener = null;
	if (channel instanceof TcpChannel){
	    TcpServerProcessor proc = (TcpServerProcessor) props.get (PROP_SERVER_PROCESSOR);
	    listener = proc.getChannelListener ((TcpChannel) channel);
	    aliasMethod = getCommandMethod ("alias", listener.getClass (), TcpChannel.class, List.class);
	} else {
	    SctpServerProcessor proc = (SctpServerProcessor) props.get (PROP_SERVER_PROCESSOR);
	    listener = proc.getChannelListener ((SctpChannel) channel);
	    aliasMethod = getCommandMethod ("alias", listener.getClass (), SctpChannel.class, List.class);
	}
	if (aliasMethod != null){
	    List<String> aliases = new ArrayList<String> ();
	    if (channel instanceof TcpChannel)
		invokeCommand (aliasMethod, listener, (TcpChannel) channel, aliases);
	    else
		invokeCommand (aliasMethod, listener, (SctpChannel) channel, aliases);
	    if (filter (filter, aliases))
		return info;
	}
	return null;
    }

    protected static Map<Object, Object> getAllServerInfo (ServerImpl server, Channel channel, String filter) throws Exception{
	if (channel == null) return null;
	Map<Object, Object> info = new LinkedHashMap<Object, Object> ();
	for (String s : DESC_SERVER_FIELDS_ALL)
	    info.put (s, getServerInfo (s, server, channel));
	if (filter (filter, DESC_SERVER_FIELDS_FILTER, info) == false)
	    return null;
	return info;
    }
    protected static String getServerInfo (String key, ServerImpl server, Channel channel){
	switch (key.toLowerCase ()){
	case "type" : return server.getType ();
	case "id": return server.getId ();
	case "name" : return (String) server.getProperties ().get (ServerImpl.PROP_SERVER_NAME); // maybe null
	case "processor": return server.getProperties ().get ("processor.id").toString ();
	case "since": return (String) server.getProperties ().get (ServerImpl.PROP_SERVER_SINCE_STRING);
	case "open": return String.valueOf (server.getOpenConnections ());
	case "accepted": return String.valueOf (server.getAcceptedConnections ());
	case "address":
	    try{
		if (channel instanceof TcpServerChannel) {
			InetSocketAddress local = ((TcpServerChannel)channel).getLocalAddress ();
		    return (local.getAddress().getHostAddress() + ":" + local.getPort());
		}
		if (channel instanceof SctpServerChannel)
		    return getAddressesAsString (((SctpServerChannel)channel).getAllLocalAddresses (), true);
	    }catch(Throwable t){return null;}
	case "ip":
	    try{
		if (channel instanceof TcpServerChannel)
		    return ((TcpServerChannel)channel).getLocalAddress ().getAddress ().getHostAddress();
		if (channel instanceof SctpServerChannel)
		    return getAddressesAsString (((SctpServerChannel)channel).getAllLocalAddresses (), false);
	    }catch(Throwable t){return null;}
	}
	Object o = server.getProperties ().get (key);
	if (o == null) return null;
	return o.toString ();
    }
    
    public static IOHCommand runServerCommand = new IOHCommand ("ioh.server", "run", "Runs a command on TCP/SCTP servers"){
	    public void run (CommandSession session, String command){ run (session, command, null); }
	    public void run (CommandSession session, @Descriptor ("The command to run") String command, @Descriptor("A server(s) selection key (address, name, ...)") String filter) { run (session, command, filter, null); }
	    public void run (CommandSession session, @Descriptor ("The command to run") String command, @Descriptor("A server(s) selection key (address, name, ...)") String filter, @Descriptor ("The command argument") String arg) {
		tcpRunServerCommand.execute (session, command, filter, arg);
		sctpRunServerCommand.execute (session, command, filter, arg);
	    }
	};

    public static IOHCommand runClientCommand = new IOHCommand ("ioh.server", "client", "Runs a command on TCP/SCTP clients"){
	    public void client (CommandSession session, String command){ client (session, command, null); }
	    public void client (CommandSession session, @Descriptor ("The command to run") String command, @Descriptor("A client(s) selection key (address, name, ...)") String filter) { client (session, command, filter, null); }
	    public void client (CommandSession session, @Descriptor ("The command to run") String command, @Descriptor("A client(s) selection key (address, name, ...)") String filter, @Descriptor ("The command argument") String arg) {
		tcpRunClientCommand.execute (session, command, filter, arg);
		sctpRunClientCommand.execute (session, command, filter, arg);
	    }
	};
    
    
    /*******************************
     * TCP Commands
     ******************************/
    
    public static IOHCommand tcpRunServerCommand = new IOHCommand ("ioh.server.tcp", "run", "Runs a command on TCP servers"){
	    public void run (CommandSession session, @Descriptor("The command to run") String command){ run (session, command, null); }
	    public void run (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A server(s) selection filter (address, name, ...)") String filter) { run (session, command, filter, null); }
	    public void run (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A server(s) selection filter (address, name, ...)") String filter, @Descriptor ("The command argument") String arg) { execute (session, command, filter, arg);}
	    @Override
	    public void execute (final CommandSession session, String... args){
		final String command = args[0];
		final String filter = args[1];
		final String arg = args[2];
		// we cannot synchronize on TcpServerImpl._servers for the callable --> else possible deadlock
		List<TcpServerImpl> servers = new ArrayList<TcpServerImpl> ();
		synchronized (TcpServerImpl._servers){
		    servers.addAll (TcpServerImpl._servers);
		}
		for (final TcpServerImpl server : servers){
		    Callable<Map<Object, Object>> call = new Callable<Map<Object, Object>> (){
			public Map<Object, Object> call (){
			    try{
				Map<Object, Object> info = getAllServerInfo (server, server.getServerChannel (), filter);
				if (info == null) return null;
				Map<Object, Object> map = new HashMap<Object, Object> ();
				map.put (server, info);
				TcpServerProcessor proc = server.getProcessor ();
				Method procMethod = getCommandMethod (command, proc.getClass (), TcpServer.class, String.class, Map.class);
				if (procMethod != null){
				    if (invokeCommand (procMethod, proc, server, arg, map) == Boolean.FALSE)
					return map;
				}
				Method thisMethod = getCommandMethod (command, GogoCommands.class, CommandSession.class, TcpServer.class, String.class, Map.class);
				if (thisMethod != null){
				    invokeCommand (thisMethod, null, session, server, arg, map);
				}
				return map;
			    }catch(Throwable t){
				LOGGER.warn ("Exception while getting info for : "+server, t);
				return null;
			    }
			}
		    };
		    try{
			Map<Object, Object> map = server.execute (call).get ();
			if (map == null) continue;
			Object out = map.get ("System.out");
			if (out != null){
			    String s = out.toString ();
			    if (s.length () > 0){
				System.out.println (out.toString ().trim ());
				LOGGER.info (out);
			    }
			}
		    }catch(Throwable t){ // interrupted exception
			return;
		    }
		}
	    }
	};
    
    public static IOHCommand tcpRunClientCommand = new IOHCommand ("ioh.server.tcp", "client", "Runs a command on TCP clients"){
	    public void client (CommandSession session, @Descriptor("The command to run") String command){ client (session, command, null); }
	    public void client (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A client(s) selection filter (address, name, ...)") String filter) { client (session, command, filter, null); }
	    public void client (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A client(s) selection filter (address, name, ...)") String filter, @Descriptor ("The command argument") String arg) { execute (session, command, filter, arg);}
	    @Override
	    public void execute (final CommandSession session, String... args){
		final String command = args[0];
		final String filter = args[1];
		final String arg = args[2];
		Map<TcpChannel, Map<String, Object>> clients = null;
		// we clone to avoid a deadlock
		clients = new ConcurrentHashMap<>(TcpServerImpl._connections);
		Iterator<Map.Entry<TcpChannel, Map<String, Object>>> it = clients.entrySet ().iterator ();
		while (it.hasNext ()){
		    Map.Entry<TcpChannel, Map<String, Object>> client = it.next ();
		    final TcpChannel channel = client.getKey ();
		    final Map<String, Object> props = client.getValue ();
		    Callable<Map<Object, Object>> call = new Callable<Map<Object, Object>> (){
			public Map<Object, Object> call (){
			    try{
				Map<Object, Object> info = getAllClientInfo (channel, props, filter);
				if (info == null) return null;
				Map<Object, Object> map = new HashMap<Object, Object> ();
				map.put (channel, info);
				TcpServerProcessor proc = (TcpServerProcessor) props.get (PROP_SERVER_PROCESSOR);
				TcpChannelListener listener = proc.getChannelListener (channel);
				Method listenerMethod = getCommandMethod (command, listener.getClass (), TcpChannel.class, String.class, Map.class);
				if (listenerMethod != null){
				    if (invokeCommand (listenerMethod, listener, channel, arg, map) == Boolean.FALSE)
					return map;
				}
				Method thisMethod = getCommandMethod (command, GogoCommands.class, CommandSession.class, AsyncChannel.class, String.class, Map.class);
				if (thisMethod != null){
				    invokeCommand (thisMethod, null, session, channel, arg, map);
				}
				return map;
			    }catch(Throwable t){
				LOGGER.warn ("Exception while getting info for : "+channel, t);
				return null;
			    }
			}
		    };
		    try{
			PlatformExecutor exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
			Map<Object, Object> map = exec.submit (call, ExecutorPolicy.INLINE).get (); // INLINE !!! else deadlock for local gogo socket
			if (map == null) continue;
			Object out = map.get ("System.out");
			if (out != null){
			    String s = out.toString ();
			    if (s.length () > 0){
				System.out.println (out.toString ().trim ());
				LOGGER.info (out);
			    }
			}
		    }catch(Throwable t){ // interrupted exception
			return;
		    }
		}
	    }
	};
    
    /*******************************
     * SCTP Commands
     ******************************/
    
    public static IOHCommand sctpRunServerCommand = new IOHCommand ("ioh.server.sctp", "run", "Runs a command on SCTP servers"){
	    public void run (CommandSession session, @Descriptor("The command to run") String command){ run (session, command, null); }
	    public void run (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A server(s) selection filter (address, name, ...)") String filter) { run (session, command, filter, null); }
	    public void run (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A server(s) selection filter (address, name, ...)") String filter, @Descriptor ("The command argument") String arg) { execute (session, command, filter, arg);}
	    @Override
	    public void execute (final CommandSession session, String... args){
		final String command = args[0];
		final String filter = args[1];
		final String arg = args[2];
		// we cannot synchronize on SctpServerImpl._servers for the callable --> else possible deadlock
		List<SctpServerImpl> servers = new ArrayList<SctpServerImpl> ();
		synchronized (SctpServerImpl._servers){
		    servers.addAll (SctpServerImpl._servers);
		}
		for (final SctpServerImpl server : servers){
		    Callable<Map<Object, Object>> call = new Callable<Map<Object, Object>> (){
			public Map<Object, Object> call (){
			    try{
				Map<Object, Object> info = getAllServerInfo (server, server.getServerChannel (), filter);
				if (info == null) return null;
				Map<Object, Object> map = new HashMap<Object, Object> ();
				map.put (server, info);
				SctpServerProcessor proc = server.getProcessor ();
				Method procMethod = getCommandMethod (command, proc.getClass (), SctpServer.class, String.class, Map.class);
				if (procMethod != null){
				    if (invokeCommand (procMethod, proc, server, arg, map) == Boolean.FALSE)
					return map;
				}
				Method thisMethod = getCommandMethod (command, GogoCommands.class, CommandSession.class, SctpServer.class, String.class, Map.class);
				if (thisMethod != null){
				    invokeCommand (thisMethod, null, session, server, arg, map);
				}
				return map;
			    }catch(Throwable t){
				LOGGER.warn ("Exception while getting info for : "+server, t);
				return null;
			    }
			}
		    };
		    try{
			Map<Object, Object> map = server.execute (call).get ();
			if (map == null) continue;
			Object out = map.get ("System.out");
			if (out != null){
			    String s = out.toString ();
			    if (s.length () > 0){
				System.out.println (out.toString ().trim ());
				LOGGER.info (out);
			    }
			}
		    }catch(Throwable t){ // interrupted exception
			return;
		    }
		}
	    }
	};
    
    public static IOHCommand sctpRunClientCommand = new IOHCommand ("ioh.server.sctp", "client", "Runs a command on SCTP clients"){
	    public void client (CommandSession session, @Descriptor("The command to run") String command){ client (session, command, null); }
	    public void client (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A client(s) selection filter (address, name, ...)") String filter) { client (session, command, filter, null); }
	    public void client (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A client(s) selection filter (address, name, ...)") String filter, @Descriptor ("The command argument") String arg) { execute (session, command, filter, arg);}
	    @Override
	    public void execute (final CommandSession session, String... args){
		final String command = args[0];
		final String filter = args[1];
		final String arg = args[2];
		Map<SctpChannel, Map<String, Object>> clients = null;
		// we clone to avoid a deadlock
		clients = new ConcurrentHashMap<>(SctpServerImpl._connections);
		
		Iterator<Map.Entry<SctpChannel, Map<String, Object>>> it = clients.entrySet ().iterator ();
		while (it.hasNext ()){
		    Map.Entry<SctpChannel, Map<String, Object>> client = it.next ();
		    final SctpChannel channel = client.getKey ();
		    final Map<String, Object> props = client.getValue ();
		    Callable<Map<Object, Object>> call = new Callable<Map<Object, Object>> (){
			public Map<Object, Object> call (){
			    try{
				Map<Object, Object> info = getAllClientInfo (channel, props, filter);
				if (info == null) return null;
				Map<Object, Object> map = new HashMap<Object, Object> ();
				map.put (channel, info);
				SctpServerProcessor proc = (SctpServerProcessor) props.get (PROP_SERVER_PROCESSOR);
				SctpChannelListener listener = proc.getChannelListener (channel);
				Method listenerMethod = getCommandMethod (command, listener.getClass (), SctpChannel.class, String.class, Map.class);
				if (listenerMethod != null){
				    if (invokeCommand (listenerMethod, listener, channel, arg, map) == Boolean.FALSE)
					return map;
				}
				Method thisMethod = getCommandMethod (command, GogoCommands.class, CommandSession.class, AsyncChannel.class, String.class, Map.class);
				if (thisMethod != null){
				    invokeCommand (thisMethod, null, session, channel, arg, map);
				}
				return map;
			    }catch(Throwable t){
				LOGGER.warn ("Exception while getting info for : "+channel, t);
				return null;
			    }
			}
		    };
		    try{
			PlatformExecutor exec = (PlatformExecutor) props.get (Server.PROP_READ_EXECUTOR);
			Map<Object, Object> map = exec.submit (call, ExecutorPolicy.INLINE).get (); // INLINE !!! else deadlock for local gogo socket
			if (map == null) continue;
			Object out = map.get ("System.out");
			if (out != null){
			    String s = out.toString ();
			    if (s.length () > 0){
				System.out.println (out.toString ().trim ());
				LOGGER.info (out);
			    }
			}
		    }catch(Throwable t){ // interrupted exception
			return;
		    }
		}
	    }
	};
    
    
    //////////////////////// COMMON COMMANDS ////////////////////////////
    
    public static boolean stopCommand (CommandSession session, TcpServer server, String arg, Map<Object, Object> map){
	server.stopListening (false);
	map.put ("System.out", "StopListening : "+info (session, (Map) map.get (server), arg));
	return true;
    }
    public static boolean stopAllCommand (CommandSession session, TcpServer server, String arg, Map<Object, Object> map){
	server.stopListening (true);
	map.put ("System.out", "Stoplistening / Close All : "+info (session, (Map) map.get (server), arg));
	return true;
    }
    public static boolean closeCommand (CommandSession session, TcpServer server, String arg, Map<Object, Object> map){
	server.close ();
	map.put ("System.out", "Close : "+info (session, (Map) map.get (server), arg));
	return true;
    }
    public static boolean infoCommand (CommandSession session, TcpServer server, String arg, Map<Object, Object> map){
	map.put ("System.out", info (session, (Map) map.get (server), arg));
	return true;
    }

    public static boolean stopCommand (CommandSession session, SctpServer server, String arg, Map<Object, Object> map){
	server.stopListening (false);
	map.put ("System.out", "StopListening : "+info (session, (Map) map.get (server), arg));
	return true;
    }
    public static boolean stopAllCommand (CommandSession session, SctpServer server, String arg, Map<Object, Object> map){
	server.stopListening (true);
	map.put ("System.out", "Stoplistening / Close All : "+info (session, (Map) map.get (server), arg));
	return true;
    }
    public static boolean closeCommand (CommandSession session, SctpServer server, String arg, Map<Object, Object> map){
	server.close ();
	map.put ("System.out", "Close : "+info (session, (Map) map.get (server), arg));
	return true;
    }
    public static boolean infoCommand (CommandSession session, SctpServer server, String arg, Map<Object, Object> map){
	map.put ("System.out", info (session, (Map) map.get (server), arg));
	return true;
    }
    
    public static boolean infoCommand (CommandSession session, AsyncChannel client, String arg, Map<Object, Object> map){
	map.put ("System.out", info (session, (Map) map.get (client), arg));
	return true;
    }
    private static String info (CommandSession session, Map<Object, Object> info, String arg){
	if (arg == null || "*".equals (arg)){
	    return getFormatter (session).format (session, info);
	} else {
	    Map<Object, Object> info2 = new LinkedHashMap<Object, Object> ();
	    String[] fields = arg.split (" ");
	    for (String field : fields){
		info2.put (field, info.get (field));
	    }
	    return getFormatter (session).format (session, info2);
	}
    }
    public static boolean closeCommand (CommandSession session, AsyncChannel client, String arg, Map<Object, Object> map){
	client.close ();
	map.put ("System.out", info (session, (Map) map.get (client), arg));
	return true;
    }
    public static boolean abortCommand (CommandSession session, AsyncChannel client, String arg, Map<Object, Object> map){
	client.shutdown ();
	map.put ("System.out", info (session, (Map) map.get (client), arg));
	return true;
    }
    public static boolean writeCommand (CommandSession session, AsyncChannel client, String arg, Map<Object, Object> map){
	writeTo (client, arg, false);
	return true;
    }
    public static boolean writelnCommand (CommandSession session, AsyncChannel client, String arg, Map<Object, Object> map){
	writeTo (client, arg != null ? arg+"\n" : "\n", false);
	return true;
    }
    public static boolean writecrlfCommand (CommandSession session, AsyncChannel client, String arg, Map<Object, Object> map){
	writeTo (client, arg != null ? arg+"\r\n" : "\r\n", false);
	return true;
    }
    public static boolean writebinCommand (CommandSession session, AsyncChannel client, String arg, Map<Object, Object> map){
	writeTo (client, arg, true);
	return true;
    }
    private static void writeTo (AsyncChannel client, String arg, boolean binary){
	if (arg == null) return;
	try{
	    if (binary){
		int len = arg.length () / 2;
		int x = 0;
		byte[] b = new byte[len];
		for (int i=0; i<arg.length (); i+=2){
		    String s = arg.substring (i, i+2);
		    int hex = Integer.parseInt (s, 16);
		    b[x++] = (byte) hex;
		}
		client.send (b, false);
	    } else {
		byte[] b = arg.getBytes ("ascii");
		client.send (b, false);
	    }
	}catch(Exception e){
	    LOGGER.warn ("Failed to writeTo : "+arg+" on "+client, e);
	}
    }
}
