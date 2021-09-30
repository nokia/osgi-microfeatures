package com.alcatel.as.http.ioh;

import com.alcatel.as.ioh.engine.*;
import org.apache.log4j.Logger;
import alcatel.tess.hometop.gateways.reactor.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.http.parser.*;
import java.util.*;
import java.net.*;
import java.nio.*;
import com.alcatel.as.ioh.tools.ChannelWriter;


public interface HttpIOHChannel {

    public boolean incoming ();
    public <T extends AsyncChannel> T getChannel ();
    public IOHChannel getIOHChannel ();
    public Logger getLogger ();
    public PlatformExecutor getPlatformExecutor ();
    public boolean sendAgent (IOHEngine.MuxClient agent, HttpMessage msg);
    public boolean sendOut (HttpMessage message, boolean checkBuffer, ByteBuffer... msg);
    public IOHEngine.MuxClient getAgent (String name);
    public Map<Object, IOHEngine.MuxClient> getAgents ();
    public IOHEngine.MuxClient pickAgent (Object preferenceHint);
    public void attach (Object attachment);
    public <T> T attachment ();
    public boolean isRemoteIOHEngine ();
    public void close (HttpMessage msg);
    public void attachAgent (IOHEngine.MuxClient agent);
    public IOHEngine.MuxClient agentAttached ();
}
