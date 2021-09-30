package com.alcatel.as.service.discovery;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Filter;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import java.io.Closeable;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.net.InetSocketAddress;
import com.alcatel.as.util.config.ConfigConstants;

public class AdvertisementTracker implements ServiceTrackerCustomizer, Closeable {

    protected static Object VOID = new Object ();

    public static final String GROUP_TARGET = "group.target";

    protected Listener _listener;
    protected ServiceTracker _tracker;
    protected BundleContext _osgi;
    protected String _targetFilter;
    protected Object _attachment;
    protected Map<String, String> _filter;
    protected Map<String, String> _antifilter;

    public interface Listener {
	Object up (AdvertisementTracker tracker, InetSocketAddress addr, ServiceReference ref);
	void down (AdvertisementTracker tracker, ServiceReference ref, Object ctx);
    }
    
    	
    public AdvertisementTracker (Listener listener){
	_listener = listener;
    }
    
    public AdvertisementTracker addModuleIdFilter (String moduleId, boolean required){
	return addFilter (ConfigConstants.MODULE_ID, moduleId, required);
    }
    public AdvertisementTracker addModuleNameFilter (String name, boolean required){
	return addFilter (ConfigConstants.MODULE_NAME, name, required);
    }
    public AdvertisementTracker addInstanceNameFilter (String name, boolean required){
	return addFilter (ConfigConstants.INSTANCE_NAME, name, required);
    }
    public AdvertisementTracker addGroupNameFilter (String name, boolean required){
	return addFilter (ConfigConstants.GROUP_NAME, name, required);
    }
    public AdvertisementTracker addGroupIdFilter (String id, boolean required){
	return addFilter (ConfigConstants.GROUP_ID, id, required);
    }
    public AdvertisementTracker setRequiredFilter (Map<String, String> props){
	_filter = props;
	return this;
    }
    public AdvertisementTracker setExcludedFilter (Map<String, String> props){
	_antifilter = props;
	return this;
    }
    public AdvertisementTracker addFilter (String name, String value, boolean required){
	if (required){
	    if (_filter == null) _filter = new HashMap<String, String> ();
	    _filter.put (name, value);
	}else{
	    if (_antifilter == null) _antifilter = new HashMap<String, String> ();
	    _antifilter.put (name, value);
	}
	return this;
    }
    public AdvertisementTracker addTargetGroupFilter (String name){
	_targetFilter = name;
	return this;
    }

    public AdvertisementTracker attach (Object attachment){ _attachment = attachment; return this;}
    public <T> T attachment (){ return (T) _attachment;}
    
    public AdvertisementTracker open (BundleContext osgi){
	_osgi = osgi;
	StringBuilder s = new StringBuilder ()
	    .append ("(&(objectClass=")
	    .append (Advertisement.class.getName())
	    .append (')')
	    .append ("(provider=*)"); // this is to skip locally emitted Adverts : only those sent by FC are tracked
	if (_filter != null)
	    for (String key : _filter.keySet ())
		s.append ('(').append (key).append ('=').append (_filter.get (key)).append (')');
	if (_antifilter != null){
	    for (String key : _antifilter.keySet ())
		s.append ("(!").append ('(').append (key).append ('=').append (_antifilter.get (key)).append ("))");
	}
	s.append (')');
	String filter = s.toString ();
	try {
	    Filter f = osgi.createFilter(filter);
	    _tracker = new ServiceTracker(osgi, f, this);
	    _tracker.open ();
	} catch (Exception e) {
	    throw new IllegalArgumentException ("Illegal Filter : "+_filter);
	}
	return this;
    }
      
    public void close() {
	if (_tracker != null) _tracker.close();
    }
   
    /**
     * A new Advert is registering in the OSGi registry
     */
    @Override
    public Object addingService(final ServiceReference ref) {
	if (_targetFilter != null){
	    if (isTargetedGroup (ref, _targetFilter) == false)
		return VOID;
	}
	Advertisement advert = (Advertisement) _osgi.getService(ref);
	InetSocketAddress addr = new InetSocketAddress (advert.getIp(), advert.getPort());
	return _listener.up (this, addr, ref);
    }
   
    /**
     * An old Advert is unregistering from the OSGi registry
     */
    @Override
    public void removedService(ServiceReference ref, Object attachment) {
	if (attachment == VOID) return;
	_listener.down (this, ref, attachment);
    }
   
    /**
     * An existing advertisement is modified (not possible for now ...)
     */
    @Override
    public void modifiedService(ServiceReference ref, Object attachment) {
    }

    public static boolean isTargetedGroup (ServiceReference ref, String group){
	String targets = (String) ref.getProperty (GROUP_TARGET);
	if (targets != null){
	    StringTokenizer st = new StringTokenizer (targets, ", ");
	    while (st.hasMoreTokens ()){
		if (st.nextToken ().equals (group))
		    return true;
	    }
	}
	return false;
    }
}
