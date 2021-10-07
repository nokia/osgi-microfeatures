package com.alcatel.as.service.metering2.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.osgi.service.http.HttpService;

import com.alcatel.as.service.metering2.Meter;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.Monitorable;
import com.alcatel.as.service.metering2.util.MeterIterator;
import com.alcatel.as.service.metering2.util.MeteringRegistry;
import com.alcatel.as.service.metering2.util.Meters;
import com.alcatel.as.service.metering2.util.MonitorableIterator;
import com.alcatel.as.util.cl.ClassLoaderHelper;
import com.alcatel.as.util.config.ConfigHelper;

public class MeteringServlet extends HttpServlet {
    
    protected Logger _logger = Logger.getLogger("as.service.metering2.servlet");
    private MeteringService _metering; // injected
    private MeteringRegistry _reg; // injected
    private boolean _servletEnabled;

    public void updated(final Dictionary props) {
        _servletEnabled = ConfigHelper.getBoolean(props, Configuration.ENABLE_SERVLET, false);
    }
    
    // called after all required dependencies have been injected (because optional callbacks are always invoked after required dependencies)
    public void setHttpService (HttpService http) throws Exception {
        _logger.info ("MeteringServlet : setHttp");
        if (_servletEnabled) {
            // Register the servlet using our bundle class loader. The current TCCL will be restored automatically by ClassLoaderHelper. 
            ClassLoaderHelper.executeWithClassLoader(() -> {
                try{
                    _logger.info ("MeteringServlet : registering servlet.");
                    http.registerServlet("/meters", this, null, null);
                }
                catch (Exception e) {
                    _logger.warn ("MeteringServlet : init", e);
                }
            }, MeteringServlet.class.getClassLoader());
        } else {
            _logger.info ("MeteringServlet : servlet not enabled by configuration.");
        }
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	String monName = req.getParameter ("mon");
	String meterName = req.getParameter ("meter");
	String monsName = req.getParameter ("mons");
	String metersName = req.getParameter ("meters");
	String monPattern = Meters.toPattern (monName, monsName);
	final String meterPattern = Meters.toPattern (meterName, metersName);
	final boolean hash = req.getParameter ("hash") != null;
	final boolean flat = req.getParameter ("flat") != null;
	//final boolean sort = req.getParameter ("sort") != null;
	//final boolean desc = req.getParameter ("desc") != null;
	final boolean debug = _logger.isDebugEnabled ();
	if (debug) _logger.debug ("New request : monName="+monName+", meterName="+meterName+", monsName="+monsName+", metersName="+metersName);

	final AtomicInteger hashAll = hash ? new AtomicInteger (0) : null;
	JSONObject root = new JSONObject ();
	JSONObject metering = new JSONObject ();
	try{root.put ("metering", metering);}catch(Exception e){}
	MonitorableIterator<JSONObject> it = new MonitorableIterator<JSONObject> (){
		public JSONObject next (Monitorable monitorable, JSONObject root){
		    String name = monitorable.getName ();
		    if (debug) _logger.debug ("iterateMonitorable : "+name);
		    if (hash) hashAll.set (hashAll.get () + name.hashCode ());
		    final AtomicInteger hashMon = hash ? new AtomicInteger (0) : null;
		    int index = flat ? -1 : name.indexOf (':'); // if flat --> dont break meters into json objects
		    JSONObject parent = root;
		    JSONObject mon = new JSONObject ();
		    while (index != -1){
			String parentName = name.substring (0, index);
			name = name.substring (index+1);
			JSONObject tmp = parent.optJSONObject (parentName);
			if (tmp == null){
			    tmp = new JSONObject ();
			    try{parent.put (parentName, tmp);}catch(Exception e){}
			}
			parent = tmp;
			index = name.indexOf (':');
		    }
		    try{parent.put (name, mon);}catch(Exception e){}
		    try{mon.put ("description", monitorable.getDescription ());}catch(Exception e){}
		    final MeterIterator<JSONObject> mit = new MeterIterator<JSONObject> (){
			    public JSONObject next (Monitorable monitorable, Meter meter, JSONObject mon){
				long value = meter.getValue ();
				String name = meter.getName ();
				if (debug) _logger.debug ("iterateMeter : "+name);
				if (hash){
				    hashAll.set (hashAll.get () + name.hashCode () + (int)value + (int)(value >> 32));
				    hashMon.set (hashMon.get () + name.hashCode () + (int)value + (int)(value >> 32));
				}
				int index = flat ? -1 : name.indexOf (':');
				JSONObject met = mon;
				while (index != -1){
				    String parentName = name.substring (0, index);
				    name = name.substring (index+1);
				    JSONObject tmp = met.optJSONObject (parentName);
				    if (tmp == null){
					tmp = new JSONObject ();
					try{met.put (parentName, tmp);}catch(Exception e){}
				    }
				    met = tmp;
				    index = name.indexOf (':');
				}
				try{met.put (name, value);}catch(Exception e){}
				return mon;
			    }};
		    Meters.iterateMeters (monitorable, mit, meterPattern, mon);
		    if (hash) try{mon.put ("hash", hashMon.get ());}catch(Exception e){}
		    return root;
		}
	    };
	_reg.iterateMonitorables (monPattern, it, metering);
	resp.setStatus (200);
	resp.setHeader ("Content-Type", "application/json");
	if (hash) try{metering.put ("hash", hashAll.get ());}catch(Exception e){}
	try{resp.getOutputStream ().write (root.toString (3).getBytes ());}catch(Exception e){}
	resp.getOutputStream ().flush ();
    }

}
