package com.alcatel.as.http.ioh;

import org.apache.log4j.Logger;
import org.osgi.service.component.annotations.*;
import alcatel.tess.hometop.gateways.reactor.*;

import java.util.*;
import java.nio.*;
import java.net.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

import com.alcatel.as.http.parser.*;
import com.alcatel.as.ioh.engine.*;
import com.alcatel.as.ioh.engine.IOHEngine.MuxClient;
import com.alcatel.as.ioh.tools.ChannelWriter;
import com.alcatel.as.ioh.tools.ByteBufferUtils;
import com.alcatel.as.session.distributed.*;
import com.alcatel.as.service.concurrent.*;

import static com.alcatel.as.ioh.tools.ByteBufferUtils.getUTF8;

@Component(service={HttpIOHRouterFactory.class}, property={"router.id=src"}, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class SrcHttpIOHRouterFactory extends HttpIOHRouterFactory {
    
    @Modified
    public void updated (Map<String, String> conf){
    }
    
    @Activate
    public void activate(Map<String, String> conf) {
    }
    
    @Deactivate
    public void stop() {	
    }
    
    @Override
    public String toString (){
	return "SrcHttpIOHRouterFactory";
    }
    
    @Override
    public HttpIOHRouter newHttpIOHRouter (){
	return new SrcHttpIOHRouter (this, LOGGER);
    }

    public static class SrcHttpIOHRouter extends HttpIOHRouter {

	public SrcHttpIOHRouter (SrcHttpIOHRouterFactory factory, Logger logger){
	    super (factory, logger);
	}
    
	/************************* The public methods called by the HttpIOH ********************************/
    
	

	/****************************************************************************************************/
    
	/************************* routing methods ********************************/

	@Override
	protected boolean routeCustom (HttpIOHChannel channel, HttpMessage msg){
	    return false;
	}
    }
}
