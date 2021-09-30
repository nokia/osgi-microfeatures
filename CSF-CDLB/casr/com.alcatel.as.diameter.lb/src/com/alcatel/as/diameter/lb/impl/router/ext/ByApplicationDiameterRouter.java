package com.alcatel.as.diameter.lb.impl.router.ext;

import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.apache.log4j.Logger;
import java.nio.charset.Charset;
import org.osgi.framework.BundleContext;

import com.alcatel.as.ioh.*;
import com.alcatel.as.ioh.tools.*;
import com.alcatel.as.ioh.client.TcpClient.Destination;

import com.alcatel.as.diameter.lb.*;
import com.alcatel.as.diameter.lb.impl.router.*;
import alcatel.tess.hometop.gateways.reactor.*;
import org.osgi.service.component.annotations.*;
import com.alcatel.as.service.concurrent.*;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.config.*;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.alcatel_lucent.as.management.annotation.alarm.*;
import com.alcatel.as.service.reporter.api.AlarmService;

import com.alcatel.as.service.metering2.*;
import com.alcatel.as.service.metering2.util.*;

@Component(service={DiameterRouter.class}, property={"router.id=by-app"}, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class ByApplicationDiameterRouter extends DiameterRouterWrapper {

    protected DefDiameterRouter _def;
    protected Map<Long, String> _routes;
    protected String _defRoute;
    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.router.by-app");
    
    @FileDataProperty(title="Routing Table",
		      fileData="defRoutingTable.props",
		      required=true,
		      dynamic=true,
		      section="Application Based Routing",
		      help="Indicates the destination groups by diameter application.")
    public final static String CONF_ROUTING_TABLE = "routing.table";
    
    @Override
    public String toString (){
	return "ByApplicationDiameterRouter";
    }
    
    @Reference(target="(router.id=def)")
    public void setDefDiameterRouter(DiameterRouter def){
	_def = (DefDiameterRouter) def;
	LOGGER.info (this+" : setDefDiameterRouter : "+def);
    }
    
    @Activate
    public void init (BundleContext osgi, Map<String, String> conf){
	setWrapped (_def);
	updated (conf);
    }

    @Modified
    public void updated (Map<String, String> conf){
	Map map = new HashMap ();
	try{
	    for (String line : ConfigHelper.getLines (conf.get (CONF_ROUTING_TABLE), "route")){
		boolean isDef = ConfigHelper.getFlag (line, false, "-def", "-default");
		String group = ConfigHelper.getParam (line, true, "-g", "-group");
		for (String appS : ConfigHelper.getParams (isDef ? " -app 0" : line, true, "-app", "-application")){
		    long appL = 0L;
		    if (appS.startsWith ("0x"))
			appL= Long.parseLong (appS.substring (2), 16);
		    else
			appL= Long.parseLong (appS);
		    map.put (appL, group);
		}
	    }
	}catch(Exception e){
	    LOGGER.error (this+" : Failed to load routes", e);
	    if (_routes == null) _routes = new HashMap (); // init case
	    return;
	}
	_routes = map;
	_defRoute = _routes.get (0L);
	if (LOGGER.isInfoEnabled ()) LOGGER.info (this+" : ready : "+_routes);
    }

    @Override
    public void doClientRequest (DiameterClient client, DiameterMessage msg){
	long application = msg.getApplicationID ();
	String location = _routes.get (application);
	if (location == null) location = _defRoute;
	_def.doClientRequest (client, msg, location != null ? client.getDestinationManager (location) : null);
    }
    
}
