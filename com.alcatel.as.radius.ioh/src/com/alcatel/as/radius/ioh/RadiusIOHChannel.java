// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.radius.ioh;

import com.alcatel.as.ioh.engine.*;
import org.apache.log4j.Logger;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.radius.parser.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.service.metering2.*;

public interface RadiusIOHChannel {

    public <T extends AsyncChannel> T getChannel ();
    public IOHUdpChannel getIOHChannel ();
    public Logger getLogger ();
    public PlatformExecutor getPlatformExecutor ();
    public boolean sendAgent (IOHEngine.MuxClient agent, RadiusMessage msg);
    //public boolean sendOut (boolean checkBuffer, RadiusMessage msg);
    public IOHEngine.MuxClient pickAgent (Object preferenceHint);
    public void attach (Object attachment);
    public <T> T attachment ();
    public SimpleMonitorable getMonitorable ();
}
