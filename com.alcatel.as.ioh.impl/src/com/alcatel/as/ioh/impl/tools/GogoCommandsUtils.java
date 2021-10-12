// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.impl.tools;

import java.io.PrintStream;
import java.util.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.concurrent.*;
import java.text.*;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.server.*;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.nokia.as.log.service.admin.LogAdmin;

import org.apache.log4j.*;


import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.service.component.annotations.Component;
import org.osgi.framework.BundleContext;

public class GogoCommandsUtils extends Constants {

	private final static Logger _log = Logger.getLogger(GogoCommandsUtils.class);
    private static final Logger LOGGER = Logger.getLogger ("as.ioh.gogo");
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat ("yyyy-MM-dd@HH:mm:ss,SSSS");
    
    public static boolean _inited;
    public static PlatformExecutors _execs;
	private static List<LogAdmin> _logAdmins = new CopyOnWriteArrayList<>();

    protected static final Formatter FORMAT_TSV = new DelimSeparatedFormatter ("\t");
    protected static final Formatter FORMAT_CSV = new DelimSeparatedFormatter (",");
    protected static final Formatter DEF_FORMAT = FORMAT_TSV;
    
    public static interface Formatter {
	String format (CommandSession session, Map info);
    }
    protected static Formatter getFormatter (CommandSession session){
	String format = (String) session.get ("gogo.ioh.format");
	if (format == null) return DEF_FORMAT;
	switch (format){
	case "tsv" : return FORMAT_TSV;
	case "csv" : return FORMAT_CSV;
	case "dsv" : String del = (String) session.get ("gogo.ioh.format.delimiter");
	    LOGGER.warn ("GogoCommands : missing property : gogo.ioh.format.delimiter");
	    return del != null ? new DelimSeparatedFormatter (del) : FORMAT_TSV;
	}
	return DEF_FORMAT;
    }
    protected static class DelimSeparatedFormatter implements Formatter {
	private String _delimiter;
	private DelimSeparatedFormatter (String delimiter){ _delimiter = delimiter;}
	private boolean printKeys (CommandSession session){
	    Object o = session.get ("gogo.ioh.format.keys");
	    return !"false".equals (o);
	}
	private String getKeyDelimiter (CommandSession session){
	    Object o = session.get ("gogo.ioh.format.keys.delimiter");
	    return o != null ? (String) o : ":";
	}
	public String format (CommandSession session, Map info){
	    boolean printKeys = printKeys (session);
	    String keyDelimiter = getKeyDelimiter (session);
	    StringBuilder sb = new StringBuilder ();
	    Iterator it = info.keySet ().iterator ();
	    boolean notFirst = false;
	    while (it.hasNext ()){
		Object key = it.next ();
		Object value = info.get (key);
		if (notFirst) sb.append (_delimiter);
		else notFirst = true;
		if (printKeys)
		    sb.append (key).append (keyDelimiter).append (value != null ? value.toString () : "_");
		else
		    sb.append (value != null ? value.toString () : "_");
	    }
	    return sb.toString ();
	}
    }
    
    public static synchronized void registerCommands (BundleContext ctx){
	if (_inited) return;
	registerCommand (dateCommand, ctx);
	registerCommand (quitCommand, ctx);
	registerCommand (propSetCommand, ctx);
	registerCommand (propUnsetCommand, ctx);
	registerCommand (debugCommand, ctx);
	registerCommand (setLevelCommand, ctx);	
	registerCommand (gcCommand, ctx);
	_inited = true;
    }
    public static void setPlatformExecutors (PlatformExecutors execs){_execs = execs;}
	public static void addLogAdmin(LogAdmin logAdmin) { _logAdmins.add(logAdmin); }
	public static void removeLogAdmin(LogAdmin logAdmin) { _logAdmins.remove(logAdmin); }
    public static void registerCommand (final IOHCommand command, BundleContext ctx){
	Hashtable t = new Hashtable();
	t.put (CommandProcessor.COMMAND_SCOPE, command.getScope ());
	t.put (CommandProcessor.COMMAND_FUNCTION, command.getName ());
	ctx.registerService (Object.class.getName(), command, t);
    }

    protected static class IOHCommand {
	protected String _scope, _name, _desc;
	protected IOHCommand (String scope, String name, String desc){
	    _scope = scope;
	    _name = name;
	    _desc = desc;
	}
	public String getScope() { return _scope; }
	public String getName() { return _name; }
	public String getUsage() { return _desc; }
	public String getShortDescription() { return _desc; }
	public void execute(String commandLine, PrintStream out, PrintStream err) {}
	protected boolean verbose (CommandSession session){return Boolean.TRUE.equals (session.get ("gogo.shell"));}
	public void execute (CommandSession session, String... args){}
    }
    
    protected static String getSocketInfo (String key, AsyncChannel channel, Map<String, Object> props) throws Exception {
	boolean isTcp = channel instanceof TcpChannel;
	TcpChannel tcp = isTcp ? (TcpChannel) channel : null;
	SctpChannel sctp = isTcp ? null : (SctpChannel) channel;
	switch (key.toLowerCase ()){
	case "state": return "OPEN";
	case "type" : return isTcp ? "tcp":"sctp";
	case "id": return (String) props.get (PROP_CLIENT_ID);
	case "server" : return (String) props.get ("server.name");
	case "processor": return (String) props.get ("processor.id");
	case "since": return (String) props.get (PROP_CLIENT_SINCE_STRING);
	case "send-buffer-size": return String.valueOf (channel.getSendBufferSize ());
	case "local-addr":
	    return isTcp ? tcp.getLocalAddress ().getAddress().getHostAddress() + ":" + tcp.getLocalAddress ().getPort() :
		getAddressesAsString (sctp.getLocalAddresses (), true);
	case "remote-addr":
	    return isTcp ? tcp.getRemoteAddress ().getAddress().getHostAddress() + ":" + tcp.getRemoteAddress ().getPort ():
		getAddressesAsString (sctp.getRemoteAddresses (), true);
	case "local-ip":
	    return isTcp ? tcp.getLocalAddress ().getAddress ().getHostAddress () :
		getAddressesAsString (sctp.getLocalAddresses (), false);
	case "remote-ip":
	    return isTcp ? tcp.getRemoteAddress ().getAddress ().getHostAddress ():
		getAddressesAsString (sctp.getRemoteAddresses (), false);
	}
	Object o = props.get (key);
	return (o != null) ? o.toString () : null;
    }
    protected static String getClosedSocketInfo (String key, InetSocketAddress remoteAddr, Map<String, Object> props) throws Exception {
	switch (key.toLowerCase ()){
	case "state": return "CLOSED";
	case "id": return (String) props.get (PROP_CLIENT_ID);
	case "server" : return (String) props.get ("server.name");
	case "processor": return (String) props.get ("processor.id");
	case "since": return (String) props.get (PROP_CLIENT_SINCE_STRING);
	case "remote-addr":
	    return remoteAddr.getAddress().getHostAddress() + ":" + remoteAddr.getPort();
	case "remote-ip":
	    return remoteAddr.getAddress ().getHostAddress ();
	}
	Object o = props.get (key);
	return (o != null) ? o.toString () : null;
    }
    
    protected static boolean filter (String filter, String[] keys, Map values){
	boolean doFilter = (filter != null && !"*".equals (filter));
	if (!doFilter) return true;
	String[] filters = filter.trim ().split (" "); // we must match one of them
	for (String filterItem : filters){
	    for (String key : keys){
		if (filterItem.equals (values.get (key))){
		    return true;
		}
	    }
	}
	return false;
    }
    protected static boolean filter (String filter, List<String> aliases){
	boolean doFilter = (filter != null && !"*".equals (filter));
	if (!doFilter) return true;
	String[] filters = filter.trim ().split (" "); // we must match one of them
	for (String filterItem : filters){
	    for (String alias : aliases){
		if (filterItem.equals (alias)){
		    return true;
		}
	    }
	}
	return false;
    }

    public static final long getDuration (String value, long def){
	try{
	    long unit = 1000L;
	    if (value.endsWith ("ms")){
		value = value.substring (0, value.length () - 2);
		unit = 1L;
	    } else if (value.endsWith ("s")){
		value = value.substring (0, value.length () - 1);
		unit = 1000L;
	    }
	    return Long.parseLong (value) * unit;
	}catch(Exception e){
	    LOGGER.info ("Invalid duration : "+value+" : returning def : "+def);
	    return def;
	}
    }

    public static final String getAddressesAsString (Set<SocketAddress> list){
	return getAddressesAsString (list, true);
    }
    public static final String getAddressesAsString (Set<SocketAddress> list, boolean withPort){
	Iterator<SocketAddress> it = list.iterator ();
	StringBuilder sb = new StringBuilder ();
	boolean first = true;
	while (it.hasNext ()){
	    if (first) first = false;
	    else sb.append ("/");
	    if (withPort) {
	    	InetSocketAddress addr = (InetSocketAddress)it.next ();
	    	sb.append (addr.getAddress().getHostAddress() + ":" + addr.getPort());
	    }
	    else
		sb.append (((InetSocketAddress)it.next ()).getAddress ().getHostAddress ());
	}
	return sb.toString ();
    }

    public static final IOHCommand dateCommand = new IOHCommand ("casr.system.utils", "date", "Displays the date"){
	    @Descriptor ("Displays the date with the default format")
	    public void date (CommandSession session){ run (session, DATE_FORMAT); }
	    @Descriptor ("Displays the date with a given format")
	    public void date (CommandSession session, String pattern){
		SimpleDateFormat format = null;
		try{
		    format = new SimpleDateFormat (pattern);
		}catch(Exception e){
		    LOGGER.warn ("Invalid date format : "+pattern);
		    format = DATE_FORMAT;
		}
		run (session, format);
	    }
	    private void run (CommandSession session, SimpleDateFormat format){
		System.out.println (format.format (new Date ()));
	    }
	};
    
    public static final IOHCommand quitCommand = new IOHCommand ("casr.system.utils", "quit", "Exits gogo."){
	    @Descriptor ("Quits gogo")
	    public void quit (CommandSession session){
		((AsyncChannel)session.get ("gogo.ioh.channel")).close ();
	    }
	};
    public static final IOHCommand propSetCommand = new IOHCommand ("casr.system.utils", "set", "Manages System Properties."){
	    @Descriptor ("Sets a system property")
	    public void set (CommandSession session, String name, String value){
		System.setProperty (name, value);
	    }
	};
    public static final IOHCommand propUnsetCommand = new IOHCommand ("casr.system.utils", "unset", "Manages System Properties."){
	    @Descriptor ("Unsets a system property")
	    public void unset (CommandSession session, String name){
		System.clearProperty (name);
	    }
	};
  public static final IOHCommand gcCommand = new IOHCommand ("casr.system.utils", "gc", "Performs a full GC."){
      @Descriptor ("Performs a full GC")
      public void gc (CommandSession session){
          System.gc();
      }
  };

	private static class Log extends IOHCommand {
		Map<String, LogState> _states = new HashMap<>();

		class LogState implements Runnable {
			String _logger;
			Map<LogAdmin, String> _originalLevels = new HashMap<>();
			Future _future;

			LogState(String logger) {
				_logger = logger;
				String level = null;
				for (LogAdmin logAdmin : _logAdmins) {
					level = logAdmin.getLevel(logger);
					_originalLevels.put(logAdmin, level);					
				}
			}

			public void run() {
				reset(this);
			}			
		}

	    protected Log(String command, String help) {
			super("casr.service.log", command, help);
		}

		private synchronized void reset(LogState state) {
			if (Thread.currentThread().isInterrupted())
				return;
			for (LogAdmin logAdmin : _logAdmins) {
				String originalLevel = state._originalLevels.remove(logAdmin);
				if (originalLevel != null) {
					_log.warn("Logger " + state._logger + " set back to level : " + originalLevel + " upon timer.");
					logAdmin.setLevel(state._logger, originalLevel);
				}
			}
			_states.remove(state._logger);
		}

		protected synchronized void setLogLevel(String loggerName, String level, long delay) {
			try {
			if (delay <= 0L)
				delay = 1L;
			
			LogState state = _states.get(loggerName);
			if (state == null) {
				// first command
				state = new LogState(loggerName);
				_states.put(loggerName, state);
				state._future = _execs.getProcessingThreadPoolExecutor().schedule(state, delay,
						java.util.concurrent.TimeUnit.SECONDS);
			} else {
				state._future.cancel(true); // interrupt if running
				state._future = _execs.getProcessingThreadPoolExecutor().schedule(state, delay,
						java.util.concurrent.TimeUnit.SECONDS);
			}
			LogState theState = state;
			_logAdmins.forEach(logAdmin -> logAdmin.setLevel(theState._logger, level));
			_log.debug("Logger Set to " + level + " by Gogo command for : " + delay + " seconds");
			System.out.println(loggerName + " : set to " + level + " for : " + delay + " seconds.");
			} catch (Exception e) {
				_log.warn("exception", e);
			}
		}
	};    


    public static final IOHCommand debugCommand = new Debug();

	private static class Debug extends Log {
		protected Debug() {
			super("debug", "Sets a logger to DEBUG level.");
		}

		@Descriptor("Sets a logger to DEBUG level for a limited time.")
		public synchronized void debug(
				@Descriptor("logger name") String loggerName, 
				@Descriptor("delay in seconds") long delay) {
			setLogLevel(loggerName, "DEBUG", delay);
		}
	};    
    
    public static final IOHCommand setLevelCommand = new LogLevel();
    
	private static class LogLevel extends Log {
		protected LogLevel() {
			super("loglevel", "Sets a logger to a specific level.");
		}

		@Descriptor("Sets a logger to a specific level for a limited time.")
		public synchronized void loglevel(
				@Descriptor("logger name") String loggerName, 
				@Descriptor("log level [ERROR | WARN | INFO | DEBUG | TRACE | ALL]") String levelName, 
				@Descriptor("delay in seconds") long delay) {
			setLogLevel(loggerName, levelName, delay);
		}
	};    

    public static Method getCommandMethod (String name, Class target, Class... args) {
	try{
	    Method method = target.getMethod (name+"Command", args);
	    method.setAccessible (true);
	    return method;
	}catch(Throwable t){
	    return null;
	}
    }

    public static <T> T invokeCommand (Method method, Object target, Object... args) throws Exception {
	return (T) method.invoke (target, args);
    }
}
