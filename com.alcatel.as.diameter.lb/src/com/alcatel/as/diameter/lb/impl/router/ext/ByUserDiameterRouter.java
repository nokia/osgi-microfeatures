package com.alcatel.as.diameter.lb.impl.router.ext;

import java.util.*;
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

@Component(service={}, configurationPolicy=ConfigurationPolicy.REQUIRE)
public class ByUserDiameterRouter extends DiameterRouterWrapper {

    @IntProperty(title="Number of groups",
		 defval=2,
		 min=1,
		 required=true,
		 dynamic=true,
		 section="User Based Routing ",
		 help="Indicates the number of groups to dispatch across. The group names must be <b><i>group-N</i></b> with N between 1 and the configured number of groups. This property may be used by the UserLocator or not (depending on its type).")
	public final static String CONF_NB_GROUPS = "groups.number";

    protected DefDiameterRouter _def;
    protected BundleContext _osgi;
    protected Hashtable<String, UserLocator> _locators = new Hashtable<> ();
    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.router.by-user");

    @Override
    public String toString (){
	return "ByUserDiameterRouter";
    }
    
    @Reference(target="(router.id=def)")
    public void setDefDiameterRouter(DiameterRouter def){
	_def = (DefDiameterRouter) def;
	LOGGER.info (this+" : setDefDiameterRouter : "+def);
    }
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    public synchronized void bindUserLocator (UserLocator locator, Map<String, String> properties){
	String id = properties.get ("locator.id");
	LOGGER.warn (this+" : bindUserLocator : id="+id+" : "+locator);
	_locators.put (id, locator);
	if (_osgi != null) register (id, locator);
    }
    public synchronized void unbindUserLocator (UserLocator locator){
    }
    
    @Activate
    public synchronized void init (BundleContext osgi, Map<String, String> conf){
	_osgi = osgi;
	for (String id: _locators.keySet ()) register (id, _locators.get (id));
    }

    @Modified
    public void updated (Map<String, String> conf){
    }

    private void register (String id, UserLocator locator){
	String rid = "by-user."+id;
	LOGGER.info (this+" : Registering router : "+rid);
	Dictionary props = new Hashtable ();
	props.put ("router.id", rid);
	ByUserDiameterRouterInstance router = new ByUserDiameterRouterInstance (_def, locator);
	_osgi.registerService (DiameterRouter.class.getName (), router, props);
    }
    
    public class ByUserDiameterRouterInstance extends DiameterRouterWrapper {

	private UserLocator _locator;
	private String _toString;

	private ByUserDiameterRouterInstance (DefDiameterRouter def, UserLocator locator){
	    super ();
	    _locator = locator;
	    setWrapped (def);
	    _toString = ByUserDiameterRouter.this.toString ()+"["+locator+"]";
	}

	@Override
	public String toString (){
	    return _toString;
	}
	
	@Override
	public void doClientRequest (final DiameterClient client, final DiameterMessage msg){
	    final Meter processingMeter = _def.asyncTaskStart (client, msg);
	    if (processingMeter == null)
		return;
	    int[] username = msg.indexOf (1, 0);
	    Consumer<String> callback = new Consumer<String> (){
		    public void accept(final String location){
			Runnable r = () -> {
			    processingMeter.dec (1);
			    if (LOGGER.isDebugEnabled ()){
				if (username != null)
				    LOGGER.debug (ByUserDiameterRouter.this+" : UserLocator : username : "+new String (msg.getBytes (), username[2], username[3], DefDiameterRouter.UTF8)+", location : "+location);
				else
				    LOGGER.debug (ByUserDiameterRouter.this+" : UserLocator : username : null, location : "+location);
			    }
			    _def.doClientRequest (client, msg, location != null ? client.getDestinationManager (location) : null);
			};
			client.getExecutor ().execute (r, ExecutorPolicy.INLINE);
			//client.getExecutor ().schedule (r, 1000, TimeUnit.MILLISECONDS); // for testing
		    }
		};
	    if (username == null){
		_locator.getLocation (null, -1, 0, callback);
	    } else {
		_locator.getLocation (msg.getBytes (), username[2], username[3], callback);
	    }
	}
    }
}
