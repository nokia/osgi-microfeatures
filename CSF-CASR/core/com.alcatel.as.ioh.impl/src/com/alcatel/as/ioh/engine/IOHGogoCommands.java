package com.alcatel.as.ioh.engine;

import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.apache.felix.service.command.CommandProcessor;

import org.osgi.framework.BundleContext;

import java.util.Hashtable;
import java.util.Vector;
import java.util.List;
import jsr166e.CompletableFuture;
import java.util.concurrent.Phaser;

import com.alcatel.as.ioh.engine.IOHEngine.*;

import static com.alcatel.as.util.helper.AnsiFormatter.*;

public class IOHGogoCommands {

    private IOHEngine _engine;

    public IOHGogoCommands (IOHEngine engine){
	_engine = engine;
    }

    public void register (BundleContext osgi){
	Hashtable<String, Object> props = new Hashtable<>();
        props.put(CommandProcessor.COMMAND_SCOPE, _engine.fullName ());
        props.put(CommandProcessor.COMMAND_FUNCTION, new String[] {
		"channels", "agents", "history", "drain", "undrain", "state", "stopAgent", "unstopAgent"
	    });
	osgi.registerService (Object.class.getName(), this, props);
    }

    @Descriptor("Displays the connected channels.")
    public void channels(@Descriptor("Specifies if history should be returned") 
			 @Parameter(names = { "-nohistory" }, presentValue="true", absentValue = "false") 
			 final boolean nohistory,
			 @Descriptor("A channel Type to filter channels : all, tcp, sctp, tcp-in, tcp-out, sctp-in, sctp-out") 
			 @Parameter(names = { "-type" }, absentValue = "") 
			 final String type,
			 @Descriptor("A channel id") 
			 @Parameter(names = { "-id" }, absentValue = "") 
			 final String id,
			 @Descriptor("Requests a Pretty Print output") 
			 @Parameter(names = { "-pretty" , "-p"}, presentValue="true", absentValue = "false") 
			 final boolean pretty
			 ) {
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    final List<String> resTcp = new Vector<> ();
		    final List<String> resClientTcp = new Vector<> ();
		    final List<String> resSctp = new Vector<> ();
		    final List<String> resClientSctp = new Vector<> ();
		    final Phaser phaser = new Phaser() {
			    protected boolean onAdvance(int phase, int registeredParties) {
				// at this point, all tasks are done, finalize formatting and complete the completableFuture
				// no need to reschedule the following block in the engine Q
				StringBuilder sb = new StringBuilder ();
				int count = 0;
				if (resTcp.size () > 0) sb.append (pretty ? BOLD : "").append ("TcpChannels:\t").append (pretty ? BOLD_OFF : "").append (resTcp.size ()).append ('\n');
				if (resClientTcp.size () > 0) sb.append (pretty ? BOLD : "").append ("TcpClientChannels:\t").append (pretty ? BOLD_OFF : "").append (resClientTcp.size ()).append ('\n');
				if (resSctp.size () > 0) sb.append (pretty ? BOLD : "").append ("SctpChannels:\t").append (pretty ? BOLD_OFF : "").append (resSctp.size ()).append ('\n');
				if (resClientSctp.size () > 0) sb.append (pretty ? BOLD : "").append ("SctpClientChannels:\t").append (pretty ? BOLD_OFF : "").append (resClientSctp.size ()).append ('\n');
				for (List<String> res : new List[]{resTcp, resClientTcp, resSctp, resClientSctp}){
				    for (String s : res){
					if (count++ > 0) sb.append ('\n');
					if (pretty)
					    sb.append (BOLD).append ("---------------------").append (BOLD_OFF).append ('\n');
					else
					    sb.append ("---------------------\n");
					sb.append (s);
					if (pretty)
					    sb.append (BOLD).append ("---------------------").append (BOLD_OFF);
					else
					    sb.append ("---------------------");
				    }
				}
				cf.complete (sb.toString ());
				return true;
			    }
			};
		    phaser.register();
		    for (final IOHChannel channel : _engine.getTcpChannels ().values ()){
			Runnable rr = new Runnable (){
				public void run (){
				    if (match (channel, type, id)){
					(channel instanceof IOHTcpClientChannel ? resClientTcp : resTcp).add (channel.dump (!nohistory, pretty));
				    }
				    phaser.arrive();
				}};
			phaser.register();
			channel.schedule (rr);
		    }
		    for (final IOHChannel channel : _engine.getSctpChannels ().values ()){
			Runnable rr = new Runnable (){
				public void run (){
				    if (match (channel, type, id)){
					(channel instanceof IOHSctpClientChannel ? resClientSctp : resSctp).add (channel.dump (!nohistory));
				    }
				    phaser.arrive();
				}};
			phaser.register();
			channel.schedule (rr);
		    }
		    phaser.arrive ();
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }


    private boolean match (IOHChannel channel, String type, String id){
	if (type.length () > 0){
	    switch (type.toLowerCase ()){
	    case "all" : return true;
	    case "tcp" : return (channel instanceof IOHTcpChannel || channel instanceof IOHTcpClientChannel);
	    case "tcp-in" : return (channel instanceof IOHTcpChannel);
	    case "tcp-out" : return (channel instanceof IOHTcpClientChannel);
	    case "sctp" : return (channel instanceof IOHSctpChannel || channel instanceof IOHSctpClientChannel);
	    case "sctp-in" : return (channel instanceof IOHSctpChannel);
	    case "sctp-out" : return (channel instanceof IOHSctpClientChannel);
	    }
	    return false;
	}
	if (id.length () > 0){
	    return String.valueOf (channel.getSockId ()).equals (id);
	}
	return true;
    }

    
    @Descriptor ("Displays the connected agents")
    public void agents (@Descriptor("Requests a Pretty Print output") 
			@Parameter(names = { "-pretty" , "-p"}, presentValue="true", absentValue = "false") 
			final boolean pretty){
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    MuxClientList.Iterator<StringBuilder> it = new MuxClientList.Iterator<StringBuilder> (){
			    public StringBuilder next (MuxClient next, StringBuilder sb){
				return print (next, sb, pretty);
			    }
			};
		    cf.complete (_engine.getMuxClientList ().iterate (it, new StringBuilder ()).toString ());
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }
    @Descriptor ("Displays the engine history - Deprecated : use command 'state -h'")
    public void history (@Descriptor("Requests a Pretty Print output") 
			 @Parameter(names = { "-pretty" , "-p"}, presentValue="true", absentValue = "false") 
			 final boolean pretty){
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    MuxClientList.Iterator<StringBuilder> it = new MuxClientList.Iterator<StringBuilder> (){
			    public StringBuilder next (MuxClient next, StringBuilder sb){
				return print (next, sb, pretty);
			    }
			};
		    StringBuilder sb = new StringBuilder ();
		    if (pretty)
			sb.append (BOLD).append ("Agents Active : ").append (BOLD_OFF).append (_engine.getMuxClientList ().size ()).append ('\n');
		    else
			sb.append ("Agents Active : ").append (_engine.getMuxClientList ().size ()).append ('\n');
		    _engine.getMuxClientList ().iterate (it, sb);
		    writeTo (sb, _engine.getRecord ());
		    cf.complete (sb.toString ());
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }
    @Descriptor ("Displays the engine state")
    public void state (@Descriptor("Requests a Pretty Print output") 
		       @Parameter(names = { "-pretty" , "-p"}, presentValue="true", absentValue = "false") 
		       final boolean pretty,
		       @Descriptor("Dumps the history") 
		       @Parameter(names = { "-history" , "-h"}, presentValue="true", absentValue = "false") 
		       final boolean history){
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    MuxClientList.Iterator<StringBuilder> it = new MuxClientList.Iterator<StringBuilder> (){
			    public StringBuilder next (MuxClient next, StringBuilder sb){
				return print (next, sb, pretty);
			    }
			};
		    StringBuilder sb = new StringBuilder ();
		    if (_engine._suspended){
			if (pretty)
			    sb.append (BOLD).append ("Suspended : ").append (BOLD_OFF).append (BACKGROUND_BRIGHT_RED).append ("YES").append (RESET).append ('\n');
			else
			    sb.append ("Suspended : YES").append ('\n');
		    } else {
			if (pretty)
			    sb.append (BOLD).append ("Suspended : ").append (BOLD_OFF).append ("NO").append (RESET).append ('\n');
			else
			    sb.append ("Suspended : NO").append ('\n');
		    }
		    if (_engine._draining){
			if (pretty)
			    sb.append (BOLD).append ("Draining : ").append (BOLD_OFF).append (BACKGROUND_BRIGHT_RED).append ("YES").append (RESET).append ('\n');
			else
			    sb.append ("Draining : YES").append ('\n');
		    } else {
			if (pretty)
			    sb.append (BOLD).append ("Draining : ").append (BOLD_OFF).append ("NO").append (RESET).append ('\n');
			else
			    sb.append ("Draining : NO").append ('\n');
		    }
		    if (pretty)
			sb.append (BOLD).append ("Agents Active : ").append (BOLD_OFF).append (_engine.getMuxClientList ().size ()).append ('\n');
		    else
			sb.append ("Agents Active : ").append (_engine.getMuxClientList ().size ()).append ('\n');
		    _engine.getMuxClientList ().iterate (it, sb);
		    if (pretty)
			sb.append (BOLD).append ("Agents Pending Activation : ").append (BOLD_OFF);
		    else
			sb.append ("Agents Pending Activation : ");
		    sb.append (_engine._agentsPendingActivate.size ()).append ('\n');
		    print (_engine._agentsPendingActivate, sb, pretty);
		    if (pretty)
			sb.append (BOLD).append ("Agents Pending Connected : ").append (BOLD_OFF);
		    else
			sb.append ("Agents Pending Connected : ");
		    sb.append (_engine._agentsPendingConnected.size ()).append ('\n');
		    print (_engine._agentsPendingConnected, sb, pretty);

		    if (history)
			writeTo (sb, _engine.getRecord ());
		    
		    cf.complete (sb.toString ());
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }

    @Descriptor ("Drains the engine: equivalent to all agents stopped.")
    public void drain (){
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    _engine.drain ();
		    cf.complete ("Start draining");
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }
    @Descriptor ("Stops an agent")
    public void stopAgent (@Parameter(names = { "-agent" , "-a"}, absentValue = "") String agent){
	if (agent.length () == 0){
	    System.out.println ("Missing agent name");
	    return;
	}
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    _engine.drain (agent, "gogo");
		    cf.complete ("Stopping agent : "+agent);
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }
    @Descriptor ("Un-drains the engine: agents are restored to their natural states.")
    public void undrain (){
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    _engine.undrain ();
		    cf.complete ("Un-draining");
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }  
    @Descriptor ("Un-stops an agent")
    public void unstopAgent (@Parameter(names = { "-agent" , "-a"}, absentValue = "") String agent){
	if (agent.length () == 0){
	    System.out.println ("Missing agent name");
	    return;
	}
	final CompletableFuture<String> cf = new CompletableFuture<> ();
	Runnable r = new Runnable (){
		public void run (){
		    _engine.undrain (agent, "gogo");
		    cf.complete ("Un-Stopping agent : "+agent);
		}
	    };
	_engine.schedule (r);
	try{
	    System.out.println (cf.get ());
	}catch(Exception e){
	    System.out.println ("Exception while running command : "+e);
	}
    }
    
    private StringBuilder writeTo (StringBuilder sb, com.alcatel.as.service.recorder.Record record){
	java.time.format.DateTimeFormatter dateFormat = _engine.getRecord ().service ().getDateFormatter ();
	_engine.getRecord ().iterate ((index, event) -> {
		sb.append ('#').append (index).append (":\t")
		    .append (dateFormat.format (event.time ()))
		    .append (":\t")
		    .append (event.message ())
		    .append ('\n');			    
	    });
	return sb;
    }

    public static final StringBuilder print (List<MuxClient> agents, StringBuilder sb, boolean pretty){
	for (MuxClient agent : agents)
	    print (agent, sb, pretty);
	return sb;
    }
    
    public static final StringBuilder print (MuxClient agent, StringBuilder sb, boolean pretty){
	boolean isStopped = agent.isStopped ();
	if (pretty){
	    boolean isLocal = agent.isLocalAgent ();
	    if (isStopped){
		if (isLocal){
		    sb.append (BACKGROUND_BRIGHT_MAGENTA);
		} else {
		    sb.append (BACKGROUND_BRIGHT_RED);
		}
	    } else {
		if (isLocal){
		    sb.append (BACKGROUND_BRIGHT_CYAN);
		} else {
		    sb.append (BACKGROUND_BRIGHT_GREEN);
		}
	    }
	}
	sb.append (agent);
	if (isStopped) sb.append ("[Stopped]");
	if (pretty) sb.append (RESET);
	sb.append ('\n');
	return sb;
    }
}
