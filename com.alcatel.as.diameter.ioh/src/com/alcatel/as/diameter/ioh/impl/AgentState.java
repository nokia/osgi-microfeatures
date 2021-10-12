// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.ioh.impl;

import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClientState;

public class AgentState {

    protected boolean _active, _terminating, _stopped;
    protected MuxClient _agent;
    protected ClientContext _clientContext;
    
    protected AgentState (ClientContext client, MuxClient agent, MuxClientState state){
	_clientContext = client;
	_agent = agent;
	_stopped = state.stopped ();
    }

    // CER/CEA done
    protected void active (){
	_active = true;
	if (!_stopped)
	    _clientContext.getActiveAgents ().add (_agent, false);
    }
    // agent closed
    protected void closed (){
	_clientContext.getActiveAgents ().remove (_agent);
	_active = false;
	_terminating = false;
    }
    protected boolean isActive (){ return _active;}
    
    
    // received agentStopped
    public void stopped (){
	_clientContext.getActiveAgents ().remove (_agent);
	_stopped = true;
    }
    // received agentUnStopped
    public void unstopped (){
	_stopped = false;
	if (_active)
	    _clientContext.getActiveAgents ().add (_agent, false);
    }
    
    public void terminating (){
	_terminating = true;
    }
    public boolean isTerminating (){
	return _terminating;
    }

}

