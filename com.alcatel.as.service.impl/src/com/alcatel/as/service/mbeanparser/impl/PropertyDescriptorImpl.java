package com.alcatel.as.service.mbeanparser.impl;

import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.Map;

import com.alcatel.as.service.metatype.InstanceProperty;
import com.alcatel.as.service.metatype.PropertyDescriptor;

public class PropertyDescriptorImpl extends AbstractPropertyImpl implements PropertyDescriptor
{

    private static final long serialVersionUID = -6451092557333509681L;
    

    public PropertyDescriptorImpl(Map<String, Object> m)
    {
        super(m);
        check();
    }

    // for deserialization
    public PropertyDescriptorImpl()
    {
        super();
    }

    @Override
    protected AbstractPropertyImpl newInstance(Map<String, Object> m)
    {
        return new PropertyDescriptorImpl(m);
    }

    public InstanceProperty instantiate(String p, String g, String c, String i)
    {
        return new InstancePropertyImpl(this, p, g, c, i);
    }

    public boolean isDynamic()
    {
        return "true".equalsIgnoreCase(getAttribute(DYNAMIC));
    }

    public boolean isModified()
    {
        return !getValue().equals(getAttribute(DEFAULT_VALUE));
    }
}
