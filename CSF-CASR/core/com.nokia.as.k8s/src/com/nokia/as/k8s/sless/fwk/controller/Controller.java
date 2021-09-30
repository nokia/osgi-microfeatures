package com.nokia.as.k8s.sless.fwk.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.SimpleMonitorable;
import com.nokia.as.k8s.controller.CustomResource;
import com.nokia.as.k8s.controller.ResourceService;
import com.nokia.as.k8s.sless.fwk.FunctionResource;
import com.nokia.as.k8s.sless.fwk.RouteResource;

@Component(service={Controller.class})
public class Controller {

	private final static Logger LOGGER = Logger.getLogger("sless.controller");

	private List<CustomResource> _routesUnResolved = new ArrayList<> (); // the function is not known
	private List<CustomResource> _routesResolved = new ArrayList<> (); // the function is known
	private Map<String, CustomResource> _functions = new HashMap<> ();

	private List<Controlled> _controlleds = new ArrayList<> ();

	private ResourceService _resService;
	private MeteringService _metering;
	private Meter _routesResolvedMeter, _routesUnResolvedMeter, _functionsMeter, _controlledMeter;
	private BundleContext _osgi;

	public String toString (){ return "Controller";}

	@Reference
	public void setResourceService (ResourceService rs){
		_resService = rs;
	}
	@Reference
	public void setMetering (MeteringService ms){
		_metering = ms;
	}

	@Activate
	protected void activate(BundleContext osgi, Map<String, String> conf) {
		LOGGER.debug(this+" : activate");
		_osgi = osgi;
		SimpleMonitorable mon = new SimpleMonitorable ("sless.controller", "Serverless Controller");
		Meter routesMeter = mon.createIncrementalMeter (_metering, "routes", null);
		_routesResolvedMeter = mon.createIncrementalMeter (_metering, "routes.resolved", routesMeter);
		_routesUnResolvedMeter = mon.createIncrementalMeter (_metering, "routes.unresolved", routesMeter);
		_functionsMeter = mon.createIncrementalMeter (_metering, "functions", null);
		_controlledMeter = mon.createIncrementalMeter (_metering, "runtimes", null);
		mon.start (osgi);

		_resService.watch (FunctionResource.CRD, 
				f -> {   //FUNCTION ADDED
					LOGGER.warn ("setFunction : "+ f);
					synchronized (this){
						FunctionResource function = FunctionResource.of(f);
						String fname = function.name;
						_functionsMeter.inc (1);
						_functions.put (fname, f);
						for (int i=0; i<_routesUnResolved.size ();){
							CustomResource item = _routesUnResolved.get(i);
							RouteResource route = RouteResource.of(item);
							if (fname.equals (route.function.name)){
								_routesUnResolved.remove (i);
								_routesResolved.add (item);
								_routesUnResolvedMeter.inc (-1);
								_routesResolvedMeter.inc (1);
								for (Controlled controlled : _controlleds){
									if (controlled.match (route)) controlled.push (route, function);
								}
							} else {
								i++;
							}
						}
					}
				}, 
				f -> {}, //FUNCTION MODIFIED
				f -> {   //FUNCTION DELETED
					LOGGER.warn ("unsetFunction : " + f);
					synchronized (this){
						FunctionResource function = FunctionResource.of(f);
						String fname = function.name;
						_functionsMeter.inc (-1);
						_functions.remove (fname);
						for (int i=0; i<_routesResolved.size ();){
							CustomResource item = _routesResolved.get(i);
							RouteResource route = RouteResource.of(item);
							if (fname.equals (route.function.name)){
								_routesResolved.remove (i);
								_routesUnResolved.add (item);
								_routesResolvedMeter.inc (-1);
								_routesUnResolvedMeter.inc (1);
								for (Controlled controlled : _controlleds){
									if (controlled.match (route)) controlled.unpush (route);
								}
							} else {
								i++;
							}
						}
					}
				});
		
		_resService.watch (RouteResource.CRD, 
				r -> {   //ROUTE ADDED
					LOGGER.warn ("setRoute : "+ r);
					synchronized (this){
						RouteResource route = RouteResource.of(r);
						CustomResource functionR = _functions.get (route.function.name);
						if (functionR == null){
							_routesUnResolvedMeter.inc (1);
							_routesUnResolved.add (r);
							return;
						}
						FunctionResource function = FunctionResource.of(functionR);
						_routesResolvedMeter.inc (1);
						_routesResolved.add (r);
						for (Controlled controlled : _controlleds){
							if (controlled.match (route)) controlled.push (route, function);
						}
					}
				},
				r -> {}, //ROUTE MODIFIED
				r -> {   //ROUTE DELETED
					LOGGER.warn ("unsetRoute : "+ r);
					synchronized (this){
						if (_routesUnResolved.remove (r)){
							_routesUnResolvedMeter.inc (-1);
							return;
						}
						_routesResolvedMeter.inc (-1);
						_routesResolved.remove (r);
						RouteResource route = RouteResource.of(r);
						for (Controlled controlled : _controlleds){
							if (controlled.match (route)) controlled.unpush (route);
						}
					}
				});
	}

	public synchronized void addControlled (Controlled controlled){
		LOGGER.warn ("setControlled : "+controlled);
		_controlledMeter.inc (1);
		_controlleds.add (controlled);
		controlled.start ();
		for (CustomResource resource : _routesResolved){
			RouteResource route = RouteResource.of (resource);
			if (controlled.match (route)){
				controlled.push (route, FunctionResource.of((_functions.get (route.function.name))));
			}
		}
	}
	public synchronized void removeControlled (Controlled controlled){
		LOGGER.warn ("unsetControlled : "+controlled);
		_controlledMeter.inc (-1);
		_controlleds.remove (controlled);
		controlled.stop ();
	}
}
