package com.alcatel.as.ioh.impl.client;

import java.io.PrintStream;
import java.util.*;
import java.net.*;
import java.util.concurrent.*;
import java.lang.reflect.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import com.alcatel.as.ioh.client.*;
import com.alcatel.as.ioh.impl.tools.*;
import com.alcatel.as.ioh.impl.conf.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;

import org.apache.log4j.Logger;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.service.component.annotations.Component;
import org.osgi.framework.BundleContext;

public class GogoCommands extends GogoCommandsUtils {

    private static final Logger LOGGER = Logger.getLogger ("as.ioh.gogo.client");
    public static final String[] DESC_CLIENT_FIELDS_FILTER = {
	"DEF", "DEF_VERSIONED", "VERSION", "ID"
    };
    public static final String[] DESC_DEST_FIELDS_ALL = {
	"ID", "STATE", "SERVER", "LOCAL-ADDR", "REMOTE-ADDR", "LOCAL-IP", "REMOTE-IP", "SINCE", "SEND-BUFFER-SIZE"
    };
    public static final String[] DESC_DEST_FIELDS_FILTER = {
	"ID", "STATE", "SERVER", "LOCAL-ADDR", "REMOTE-ADDR", "LOCAL-IP", "REMOTE-IP"
    };
    public static final String[] DESC_CLOSED_DEST_FIELDS_ALL = {
	"ID", "STATE", "SERVER", "REMOTE-ADDR", "REMOTE-IP", "SINCE"
    };
    public static final String[] DESC_CLOSED_DEST_FIELDS_FILTER = {
	"ID", "STATE", "SERVER", "REMOTE-ADDR", "REMOTE-IP"
    };

    public static final String NO_DESTINATION = "-";

    public static void registerCommands (BundleContext ctx){
	registerCommand (clientCommand, ctx);
	registerCommand (listCommand, ctx);
    }
    
    private static LinkedHashMap getDestinationInfo (TcpClient.Destination dest, String destFilter) throws Exception {
	LinkedHashMap ret = new LinkedHashMap ();
	String[] filterFields = null;
	if (dest.isOpen ()){
	    for (String s : DESC_DEST_FIELDS_ALL)
		ret.put (s, getSocketInfo (s, dest.getChannel (), dest.getProperties ()));
	    filterFields = DESC_DEST_FIELDS_FILTER;
	} else {
	    for (String s : DESC_CLOSED_DEST_FIELDS_ALL)
		ret.put (s, getClosedSocketInfo (s, dest.getRemoteAddress (), dest.getProperties ()));
	    filterFields = DESC_CLOSED_DEST_FIELDS_FILTER;
	}
	if (filter (destFilter, filterFields, ret) == false)
	    return null;
	return ret;
    }

    /*******************************
     * TCP Commands
     ******************************/
    
    public static IOHCommand listCommand = new IOHCommand ("ioh.client.tcp", "list", "Lists TCP clients"){
	    @Descriptor ("Lists TCP clients")
	    public void list (CommandSession session){ list (session, null); }
	    @Descriptor ("Lists TCP clients")
	    public void list (CommandSession session, @Descriptor("A client(s) selection filter") String clientFilter) { list (session, clientFilter, null); }
	    @Descriptor ("Lists TCP clients")
	    public void list (CommandSession session, @Descriptor("A client(s) selection filter") String clientFilter, @Descriptor("A destination(s) selection filter") String destFilter) { list (session, clientFilter, destFilter, null); }
	    @Descriptor ("Lists TCP clients")
	    public void list (CommandSession session, @Descriptor("A client(s) selection filter") String clientFilter, @Descriptor("A destination(s) selection filter") String destFilter, @Descriptor ("The fields to return") String fields) {
		clientCommand.execute (session, "info", clientFilter, destFilter, fields);
	    }
	};
    
    public static IOHCommand clientCommand = new IOHCommand ("ioh.client.tcp", "run", "Runs commands on TCP clients"){
	    @Descriptor ("Runs commands on TCP clients")
	    public void run (CommandSession session, @Descriptor("The command to run") String command) { run (session, command, null); }
	    @Descriptor ("Runs commands on TCP clients")
	    public void run (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A client(s) selection filter") String clientFilter) { run (session, command, clientFilter, NO_DESTINATION); }
	    @Descriptor ("Runs commands on TCP clients")
	    public void run (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A client(s) selection filter") String clientFilter, @Descriptor("A destination(s) selection filter") String destFilter) { run (session, command, clientFilter, destFilter, null); }
	    @Descriptor ("Runs commands on TCP clients")
	    public void run (CommandSession session, @Descriptor("The command to run") String command, @Descriptor("A client(s) selection filter") String clientFilter, @Descriptor("A destination(s) selection filter") String destFilter, @Descriptor ("The command argument") String arg) { execute (session, command, clientFilter, destFilter, arg); }
	    @Override
	    public void execute (final CommandSession session, String... args){
		final String command = args[0];
		final String clientFilter = args[1];
		final String destFilter = args[2];
		final String arg = args[3];
		if (LOGGER.isInfoEnabled ())
		    LOGGER.info ("TcpClient GogoCommand : execute : "+command+"/"+clientFilter+"/"+destFilter+"/"+arg);
		final boolean noDest = NO_DESTINATION.equals (destFilter);
		Map<String, TcpClientImpl> clients = null;
		// we clone to avoid a deadlock
		synchronized (TcpClientImpl._clients){
		    clients = (Map<String, TcpClientImpl>) TcpClientImpl._clients.clone ();
		}
		Iterator<Map.Entry<String, TcpClientImpl>> it = clients.entrySet ().iterator ();
		while (it.hasNext ()){
		    Map.Entry<String, TcpClientImpl> entry = it.next ();
		    final TcpClientImpl client = entry.getValue ();
		    Callable<Map<Object, Object>> call = new Callable<Map<Object, Object>> (){
			public Map<Object, Object> call (){
			    try{
				Map<Object, Object> map = new HashMap <Object, Object> ();
				LinkedHashMap clientInfo = new LinkedHashMap ();
				String def = client.getId ();
				clientInfo.put ("DEF", def);
				String version = (String) client.getProperties ().get ("client.version");
				if (version == null)
				    clientInfo.put ("DEF_VERSIONED", def+".");
				else {
				    clientInfo.put ("VERSION", version);
				    clientInfo.put ("DEF_VERSIONED", def+"."+version);
				}
				clientInfo.put ("ID", client.getUniqueId ());
				if (filter (clientFilter, DESC_CLIENT_FIELDS_FILTER, clientInfo) == false)
				    return null;
				// we use DEF_VERSION for filtering only : remove before display info
				clientInfo.remove ("DEF_VERSION");
				map.put (client, clientInfo);
				TcpClientListener listener = client.getListener ();
				Method listenerMethod = getCommandMethod (command, listener.getClass (), TcpClient.class, List.class, String.class, Map.class);
				List<TcpClient.Destination> destinations = null;
				// we copy the destinations so the listener may modify the original dest list
				if (!noDest){
				    destinations = new ArrayList<TcpClient.Destination> ();
				    int count = 0;
				    for (TcpClient.Destination dest : client.getDestinations ()){
					LinkedHashMap destInfo = getDestinationInfo (dest, destFilter);
					if (destInfo == null) continue;
					map.put (dest, destInfo);
					destinations.add (dest);
					count++;
				    }
				    if (count == 0)
					return null;
				}
				boolean ok = true;
				if (listenerMethod != null){
				    ok = invokeCommand (listenerMethod, listener, client, destinations, arg, map);
				}
				if (!ok) return map;
				Method thisMethod = getCommandMethod (command, GogoCommands.class, CommandSession.class, TcpClient.class, List.class, String.class, Map.class);
				if (thisMethod != null){
				    ok = invokeCommand (thisMethod, null, session, client, destinations, arg, map);
				}
				if (!ok) return map;
				// placeholder for other processing one day...
				return map;
			    }catch(Throwable t){
				LOGGER.warn ("Exception while running command for : "+client, t);
				return null;
			    }
			}
		    };
		    try{
			Map<Object, Object> map = client.execute (call).get ();
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
			LOGGER.warn ("Exception while running command", t);
			return;
		    }
		}
	    }
	};

    /*******************************
     * close command
     ******************************/
    
    public static boolean closeCommand (CommandSession session, TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	if (destinations == null){
	    map.put ("System.out", client+":close");
	    client.close ();
	} else {
	    map.put ("System.out", destinations+":close");
	    for (TcpClient.Destination dest : destinations){
		client.getDestinations ().remove (dest);
		dest.close ();
	    }
	}
	return true;
    }

    /*******************************
     * info command
     ******************************/

    public static boolean infoCommand (CommandSession session, TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	Map clientInfo = (Map) map.get (client);
	Map out = new LinkedHashMap ();
	if (arg == null || "*".equals (arg)){
	    putAll (out, clientInfo, null);
	    if (destinations != null){
		for (TcpClient.Destination dest : destinations){
		    Map destInfo = (Map) map.get (dest);
		    String id = (String) destInfo.get ("ID");
		    putAll (out, destInfo, "."+id);
		}
	    }
	} else {
	    String[] fields = arg.split (" ");
	    for (String field : fields){
		if (field.endsWith (".")) continue;
		out.put (field, clientInfo.get (field));
	    }
	    if (destinations != null){
		for (TcpClient.Destination dest : destinations){
		    Map destInfo = (Map) map.get (dest);
		    String id = (String) destInfo.get ("ID");
		    for (String field : fields){
			if (!field.endsWith (".")) continue;
			out.put (field+id, destInfo.get (field.substring (0, field.length () - 1)));
		    }
		}
	    }
	}
	map.put ("System.out", getFormatter (session).format (session, out));
	return true;
    }
    private static Map putAll (Map dest, Map src, String suffix){
	if (src == null) return dest;
	Iterator it = src.keySet ().iterator ();
	while (it.hasNext ()){
	    Object key = it.next ();
	    Object value = src.get (key);
	    dest.put (suffix != null ? key+suffix : key, value);
	}
	return dest;
    }

    /*******************************
     * add command
     ******************************/

    public static boolean addCommand (CommandSession session, TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	Map<String, Object> destMap = getProps (arg);
	if (destMap == null){
	    map.put ("System.out", "Invalid Argument : "+arg);
	    return false;
	}
	try{
	    client.addDestination (null, null, destMap);
	}catch(Exception e){
	    map.put ("System.out", "Invalid Argument : "+arg);
	    return false;
	}
	map.put ("System.out", client+" : Added : "+destMap);
	return true;
    }

    /*******************************
     * update command / DOES NOT WORK IN MULTITHREADED CLIENT
     ******************************/
    
    public static boolean updateCommand (CommandSession session, TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	Map<String, Object> destMap = getProps (arg);
	if (destMap == null){
	    map.put ("System.out", "Invalid Argument : "+arg);
	    return false;
	}
	for (TcpClient.Destination dest : destinations){
	    for (String key: destMap.keySet ()){
		dest.getProperties ().put (key, destMap.get (key));
	    }
	}
	map.put ("System.out", client+" : Updated : "+destinations);
	return true;
    }

    /*******************************
     * reconnect command
     ******************************/
    
    public static boolean reconnectCommand (CommandSession session, TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	if (destinations == null) destinations = client.getDestinations ();
	for (TcpClient.Destination dest : destinations){
	    AsyncChannel channel = dest.getChannel ();
	    if (channel != null) channel.close ();
	}
	map.put ("System.out", client+" : Reconnecting : "+destinations);
	return true;
    }

    /*******************************
     * config command
     ******************************/
    
    public static boolean configCommand (CommandSession session, TcpClient client, List<TcpClient.Destination> destinations, String arg, Map<Object, Object> map){
	StringBuffer sb = new StringBuffer ();
	sb.append (client.getProperties ());
	if (destinations != null){
	    for (TcpClient.Destination dest : destinations){
		sb.append ("/").append (dest.getProperties ());
	    }
	}
	map.put ("System.out", sb.toString ());
	return true;
    }

    ///////////////////////// UTILITIES ///////////////////////////

    private static Map<String, Object> getProps (String arg){
	if (arg == null) return null;
	String[] toks = arg.split (" ");
	Map<String, Object> destMap = new HashMap<String, Object> ();
	for (String tok: toks){
	    int index = tok.indexOf (':');
	    if (index == -1) index = tok.indexOf ('=');
	    String key = null;
	    String value = null;
	    if (index == -1){
		key = tok.trim ();
	    } else if (index == 0){
		return null;
	    } else if (index == tok.length ()){
		key = tok.trim ();
	    } else {
		key = tok.substring (0, index).trim ();
		value = tok.substring (index+1).trim ();
	    }
	    destMap.put (key, value);
	}
	return destMap;
    }
    
}
