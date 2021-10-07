package com.alcatel.as.diameter.ioh.impl;

import java.util.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.MuxClientList;

public class MuxClientMap {

    protected Map<String, MuxClientList> _lists = new HashMap<> ();
    protected MuxClientList _defList = new MuxClientList ();

    public MuxClientMap (){
    }

    public MuxClientList getMuxClientList (String group){
	if (group == null)
	    return _defList;
	MuxClientList list = _lists.get (group);
	if (list == null) _lists.put (group, list = new MuxClientList ());
	return list;
    }

    public void add (MuxClient agent, boolean stopped){
	getMuxClientList ((String)null).add (agent, stopped);
	getMuxClientList (agent.getGroupName ()).add (agent, stopped);
	getMuxClientList (agent.getInstanceName ()).add (agent, stopped);
    }
    public void remove (MuxClient agent){
	getMuxClientList ((String)null).remove (agent);
	getMuxClientList (agent.getGroupName ()).remove (agent);
	MuxClientList list = getMuxClientList (agent.getInstanceName ());
	list.remove (agent);
	if (list.size () == 0) _lists.remove (agent.getInstanceName ());
    }
    
}
