package com.alcatel.as.service.mbeanparser.impl;

import static org.osgi.framework.Constants.SERVICE_PID;
import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_NAME;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_NAME;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import com.alcatel.as.service.metatype.InstanceProperties;
import com.alcatel.as.service.metatype.InstanceProperty;
import com.alcatel.as.service.metatype.PropertiesDescriptor;
import com.alcatel.as.service.metatype.PropertyDescriptor;
import com.alcatel.as.service.metatype.PropertyFactory;

public class InstancePropertiesImpl extends AbstractPropertyImpl implements InstanceProperties
{
    private static final long serialVersionUID = 1L;
    static final String PROPERTIES_KEY = "properties";
    public final static String DESC_ID = "desc-id";
    public final static String BUNDLE_NAME = "bundle-name";
    public final static String BUNDLE_VERSION = "bundle-version";

    InstancePropertiesImpl(String p, String g, String c, String i, PropertiesDescriptor props)
    {
        this(p, g, c, props.getPid());
        Map<String, InstanceProperty> ips = new HashMap<String, InstanceProperty>();
        for (PropertyDescriptor prop : props.getProperties().values())
        {
            ips.put(prop.getName(), prop.instantiate(p, g, c, i));
        }
        put(PROPERTIES_KEY, ips);
        put(DESC_ID, props.getId());
	put(BUNDLE_NAME, props.getBundleName());
	put(BUNDLE_VERSION, props.getBundleVersion());
	put(ID, new StringBuilder(p)
            .append(SLASH)
            .append(g)
            .append(SLASH)
            .append(c)
            .append(SLASH)
            .append(props.getPid())
            .toString());

    }

    private InstancePropertiesImpl(String p, String g, String c, String pid, Map<String, InstanceProperty> ips)
    {
        this(p, g, c, pid);
        put(PROPERTIES_KEY, ips);
    }

    private InstancePropertiesImpl(String p, String g, String c, String pid)
    {
        super();
        put(SERVICE_PID, pid);
        put(PLATFORM_NAME, p);
        put(GROUP_NAME, g);
        put(COMPONENT_NAME, c);
    }

    // for deserialization
    public InstancePropertiesImpl() { super(); }

    private InstancePropertiesImpl(Map<String, Object> m) { super(m); }

    @Override
    protected AbstractPropertyImpl newInstance(Map<String, Object> m) 
    {
      return new InstancePropertiesImpl(m);
    }

    @Override
    protected void check() {}

    public InstanceProperties addInstance(String instance_name)
    {
        for (InstanceProperty ip : getProperties().values())
            ip.addInstance(instance_name);
        return this;
    }

    public InstanceProperties removeInstance(String instance_name)
    {
        for (InstanceProperty ip : getProperties().values())
            ip.removeInstance(instance_name);
        return this;
    }

    /** map<'name', InstanceProperty> */
    public Map<String, InstanceProperty> getProperties()
    {
        return (Map<String, InstanceProperty>) get(PROPERTIES_KEY);
    }

    public String getPid()
    {
        return (String) get(SERVICE_PID);
    }

    public String getPlatform()
    {
        return (String) get(PLATFORM_NAME);
    }

    public String getGroup()
    {
        return (String) get(GROUP_NAME);
    }

    public String getComponent()
    {
        return (String) get(COMPONENT_NAME);
    }

    public String getDescriptorId()
    {
        return (String) get(DESC_ID);
    }

}
