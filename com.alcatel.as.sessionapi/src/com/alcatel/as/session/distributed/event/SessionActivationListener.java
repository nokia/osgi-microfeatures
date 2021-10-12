// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.event;

import com.alcatel.as.session.distributed.Session;
/**
   Activation listeners are specific listeners that are fired when a session is activated on an agent. 

   <P>A session is activate when its master home agent died. An activation listener gives you the opportunity to
   take some action on you own.

*/
public interface SessionActivationListener {

    /**
     * Called when a session is about to be deactivated.
     * @param session the session about to be deactivated. The session is provided outside the scope of a transaction. 
     * 
     */
    public void sessionWillPassivate(Session session);
    
    /**
     * Called when a session activated on this agent.
     * @param session the session about to be reactivated. The session is provided outside the scope of a transaction. 
     */
    public void sessionDidActivate(Session session);
}
