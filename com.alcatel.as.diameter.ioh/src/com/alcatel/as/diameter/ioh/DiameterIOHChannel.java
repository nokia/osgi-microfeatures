// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.ioh;

import com.alcatel.as.ioh.engine.*;
import org.apache.log4j.Logger;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.diameter.parser.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.service.metering2.*;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface DiameterIOHChannel {

    public static enum TYPE {
	TCP, SCTP
    }

    public boolean isOpen ();
    public String getDiameterId ();
    public boolean incoming ();
    public TYPE getType ();
    public <T extends AsyncChannel> T getChannel ();
    public IOHChannel getIOHChannel ();
    public Logger getLogger ();
    public PlatformExecutor getPlatformExecutor ();
    public boolean sendAgent (IOHEngine.MuxClient agent, DiameterMessage msg, long sessionId);
    public boolean sendOut (boolean checkBuffer, DiameterMessage msg);
    public IOHEngine.MuxClient pickAgent (String group, Object preferenceHint);
    public void attach (Object attachment);
    public <T> T attachment ();
    public void close ();
    public SimpleMonitorable getMonitorable ();
    public DiameterUtils.Avp getIOHOriginHost ();
    public DiameterUtils.Avp getIOHOriginRealm ();
    public String getOriginHost ();
    public String getOriginRealm ();
}
