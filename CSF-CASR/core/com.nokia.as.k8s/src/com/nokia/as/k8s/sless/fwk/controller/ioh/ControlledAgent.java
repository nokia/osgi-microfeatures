package com.nokia.as.k8s.sless.fwk.controller.ioh;


import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import com.alcatel.as.service.metering2.Meter;
import com.nextenso.mux.MuxHandler;
import com.nextenso.mux.MuxHeader;
import com.nextenso.mux.MuxHeaderV0;
import com.nokia.as.k8s.sless.fwk.FunctionResource;
import com.nokia.as.k8s.sless.fwk.Mux;
import com.nokia.as.k8s.sless.fwk.RouteResource;
import com.nokia.as.k8s.sless.fwk.controller.Controlled;

public class ControlledAgent extends Controlled {

    private MuxHandler _agent;
    private Meter _pushMeter, _unpushMeter;

    public ControlledAgent (MuxHandler agent, String id, String protocol, Meter pushMeter, Meter unpushMeter){
	super (id, protocol);
	_agent = agent;
	_pushMeter = pushMeter;
	_unpushMeter = unpushMeter;
    }
    
    public void muxData(MuxHeader header,
			java.nio.ByteBuffer buffer){
	switch (header.getFlags ()){
	case Mux.FLAG_GOGO_RESP:
	    long id = header.getSessionId ();
	    String resp = Mux.getUTF8 (buffer);
	    gogoResponse (id, resp);
	    return;
	}
    }
    
    protected void push (RouteResource route, FunctionResource function){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : push : "+route+" / "+function);
	ByteBuffer buffer = Mux.toByteBuffer (RouteResource.of(route), FunctionResource.of(function));
	MuxHeaderV0 h = new MuxHeaderV0 ();
	h.set (0L, 0, Mux.FLAG_PUSH);
	_agent.muxData (null, h, buffer);
	_pushMeter.inc (1);
    }
    protected void unpush (RouteResource route){
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : unpush : "+route);
	ByteBuffer buffer = ByteBuffer.wrap (route.name.getBytes (Mux.UTF_8));
	MuxHeaderV0 h = new MuxHeaderV0 ();
	h.set (0L, 0, Mux.FLAG_UNPUSH);
	_agent.muxData (null, h, buffer);
	_unpushMeter.inc (1);
    }

    protected CompletableFuture<String> gogoRequest (long id, String request, CompletableFuture<String> cf){
	ByteBuffer buffer = ByteBuffer.wrap (request.getBytes (Mux.UTF_8));
	MuxHeaderV0 h = new MuxHeaderV0 ();
	h.set (id, 0, Mux.FLAG_GOGO_REQ);
	_agent.muxData (null, h, buffer);
	return cf;
    }
}
