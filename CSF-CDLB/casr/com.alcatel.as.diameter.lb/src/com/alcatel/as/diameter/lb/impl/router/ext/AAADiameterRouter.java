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

@Component(service={DiameterRouter.class}, property={"router.id=AAA"}, configurationPolicy=ConfigurationPolicy.OPTIONAL)
public class AAADiameterRouter extends DiameterRouterWrapper {

    protected static AtomicInteger SEED = new AtomicInteger (0);
    protected DefDiameterRouter _def;
    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.router.AAA");
    private static final int CDLB = (('C' & 0xFF) << 24) | (('D' & 0xFF) << 16) | (('L' & 0xFF) << 8) | ('B' & 0xFF);

    @Override
    public String toString (){
	return "AAADiameterRouter";
    }
    
    @Reference(target="(router.id=def)")
    public void setDefDiameterRouter(DiameterRouter def){
	_def = (DefDiameterRouter) def;
	LOGGER.info (this+" : setDefDiameterRouter : "+def);
    }
    
    @Activate
    public void init (BundleContext osgi, Map<String, String> conf){
	setWrapped (_def);
    }

    @Modified
    public void updated (Map<String, String> conf){
    }

    @Override
    public void clientOpened (DiameterClient client){
	super.clientOpened (client);
	Object[] x = client.attachment ();
	Object[] y = new Object[x.length+1];
	System.arraycopy (x, 0, y, 0, x.length);
	y[x.length] = new HashMap<Object, Destination> ();
	client.attach (y);
    }
    protected Map<Object, Object> getDestinationsById (DiameterClient client){
	return client.attachment (DefDiameterRouter.ATTACHMENTS_LEN);
    }
    
    @Override
    public void serverOpened (DiameterClient client, Destination server){
	//int id = SEED.getAndIncrement ();
	int id = server.getRemoteAddress ().hashCode (); // so reconnections of the same server are OK
	server.getProperties ().put ("aaa.server.id", id);
	if (client.getLogger ().isDebugEnabled ())
	    client.getLogger ().debug (client+" : AAA assigned server:"+server+" server.id="+id);
	getDestinationsById (client).put (id, server);
	super.serverOpened (client, server);
    }
    @Override
    public void serverClosed (DiameterClient client, Destination server){
	Integer id = (Integer) server.getProperties ().get ("aaa.server.id");
	String group = (String) server.getProperties ().get ("server.group");
	if (group == null)
	    getDestinationsById (client).remove (id);
	else
	    // if i meet this id again --> i will at least go to the same group
	    getDestinationsById (client).put (id, group);
	super.serverClosed (client, server);
    }

    @Override
    public void doClientRequest (DiameterClient client, DiameterMessage msg){
	// check class avp
	byte[] content = msg.getBytes ();
	int off = 0;
	boolean skipHeader = true;
	String group = null;
	while (true){
	    int[] pos = msg.indexOf (25, 0, content, off, content.length - off, skipHeader);
	    if (pos == null) break;
	    if (skipHeader) skipHeader = false;
	    if (pos[3] == 8 && DiameterMessage.getIntValue (content, pos[2], 4) == CDLB){
		int id = DiameterMessage.getIntValue (content, pos[2]+4, 4);
		// remove class avp
		msg.removeValue (pos[0], pos[1]);
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug (client+" : found class avp : server.id="+id);
		Object o = getDestinationsById (client).get (id);
		if (o == null){
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug (client+" : specified class avp cannot be used");
		    break;
		}
		if (o instanceof Destination){
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug (client+" : routing according to class avp : dest="+o);
		    _def.doClientRequest (client, msg, (Destination) o);
		    return;
		} else {
		    group = o.toString ();
		    if (LOGGER.isDebugEnabled ())
			LOGGER.debug (client+" : routing according to class avp : group="+o);
		}
		break;
	    }
	}

	// group may be set above
	int id = _def._sessions.getId (msg);
	while (id == -1) {id = java.util.concurrent.ThreadLocalRandom.current ().nextInt ();}	
	DestinationManager mgr = group != null ? client.getDestinationManager (group) : client.getDestinationManager (id); // we first look up the group
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug (client+" : route : "+msg+" id="+id+" destManager="+mgr);
	// active/active mode : destManager is simpleDestManager : we remain sticky on id
	// active/standby mode : destManager is weightDestManager : get(int) actually returns getAny() which will return the active by default
	Destination dest = mgr.get (id);
	_def.doClientRequest (client, msg, dest);
    }

    @Override
    public void doServerResponse (DiameterClient client, Destination server, DiameterMessage msg){
	// look for class avp
	//int[] pos = msg.indexOf (25, 0);
	if (true){//pos != null){
	    Integer id = (Integer) server.getProperties ().get ("aaa.server.id");
	    if (client.getLogger ().isDebugEnabled ()){
		client.getLogger ().debug (client+" : insert class avp : server.id="+id);
	    }
	    byte[] content = msg.getBytes ();
	    int origLen = content.length;
	    byte[] newContent = new byte[origLen + 16];	    
	    System.arraycopy (content, 0, newContent, 0, origLen);
	    DiameterUtils.setIntValue (25, newContent, origLen);
	    DiameterUtils.setIntValue ((0x40 << 24) | 16, newContent, origLen + 4); // M flag then length
	    DiameterUtils.setIntValue (CDLB, newContent, origLen+8);
	    DiameterUtils.setIntValue (id, newContent, origLen + 12);
	    msg.updateContent (newContent);
	}
	super.doServerResponse (client, server, msg);
    }
}
