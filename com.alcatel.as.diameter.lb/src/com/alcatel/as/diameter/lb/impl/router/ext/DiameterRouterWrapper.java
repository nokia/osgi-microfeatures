// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.router.ext;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.apache.log4j.Logger;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;
import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.impl.router.*;

import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.concurrent.*;

public class DiameterRouterWrapper extends DiameterRouter {

    protected DiameterRouter _router;

    public DiameterRouterWrapper setWrapped (DiameterRouter router){ _router = router; return this;}
    
    public void clientOpened (DiameterClient client){ _router.clientOpened (client); }
    public void clientClosed (DiameterClient client){ _router.clientClosed (client); }

    public void serverOpened (DiameterClient client, Destination server){ _router.serverOpened (client, server); }
    public void serverClosed (DiameterClient client, Destination server){ _router.serverClosed (client, server); }
    
    public void doClientRequest (DiameterClient client, DiameterMessage msg){ _router.doClientRequest (client, msg); }
    public void doServerRequest (DiameterClient client, Destination server, DiameterMessage msg){ _router.doServerRequest (client, server, msg); }
    public void doClientResponse (DiameterClient client, DiameterMessage msg){ _router.doClientResponse (client, msg); }
    public void doServerResponse (DiameterClient client, Destination server, DiameterMessage msg){ _router.doServerResponse (client, server, msg); }
    public boolean checkClientOverload (DiameterClient client, DiameterMessage msg){ return _router.checkClientOverload (client, msg); }
    public boolean checkServerOverload (DiameterClient client, Destination server, DiameterMessage msg){ return _router.checkServerOverload (client, server, msg); }
    
    public void clientBlocked (DiameterClient client){ _router.clientBlocked (client); }
    public void clientUnblocked (DiameterClient client){ _router.clientUnblocked (client); }
    public void serverBlocked (DiameterClient client, Destination server){ _router.serverBlocked (client, server); }
    public void serverUnblocked (DiameterClient client, Destination server){ _router.serverUnblocked (client, server); }

    public void addAttachments (DiameterClient client, Object[] attachments){
	Object[] oldA = client.attachment ();
	Object[] newA = new Object[DefDiameterRouter.ATTACHMENTS_LEN + attachments.length];
	System.arraycopy (oldA, 0, newA, 0, DefDiameterRouter.ATTACHMENTS_LEN);
	int pos = DefDiameterRouter.ATTACHMENTS_LEN;
	for (Object o : attachments) newA[pos++] = o;
	client.attach (newA);
    }
}
