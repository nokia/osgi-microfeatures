package com.alcatel.as.service.mbeanparser.impl;

import static com.alcatel.as.service.metatype.PropertyDescriptor.FILEDATA;
import static com.alcatel.as.service.metatype.PropertyDescriptor.FILENAME;
import static com.alcatel.as.service.metatype.PropertyDescriptor.NAME;
import static com.alcatel.as.service.metatype.PropertyDescriptor.TYPE;
import static com.alcatel.as.service.metatype.PropertyDescriptor.VALUE;
import static com.alcatel.as.service.metatype.PropertyDescriptor.SCOPE;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.HashMap;
import java.util.Properties;
import java.util.Iterator;
import java.util.Map;
import java.util.Collection;
import java.io.StringReader;
import java.io.StringWriter;

import org.json.JSONObject;

import org.apache.commons.configuration.PropertiesConfiguration;

import com.alcatel.as.service.metatype.InstanceProperty;
import com.alcatel.as.service.metatype.PropertyDescriptor;

public class InstancePropertyImpl extends AbstractPropertyImpl implements InstanceProperty
{

    private static final long serialVersionUID = 4076577099105872596L;
    static final String INSTANCES = "instances";

    //deserialization
    public InstancePropertyImpl()
    {
        super();
    }

    @Override
    protected AbstractPropertyImpl newInstance(Map<String, Object> m)
    {
        return new InstancePropertyImpl(m);
    }

    protected InstancePropertyImpl(Map<String, Object> m)
    {
        super(m);
        check();
    }

    // instantiation
    protected InstancePropertyImpl(PropertyDescriptor h, String p, String g, String c, String i)
    {
        //do not copy description and validation info
        put(NAME, h.getName());
        put(VALUE, h.getValue());
        //put(SERVICE_PID, h.getPid());
        //put(BUNDLE_SYMBOLICNAME, h.get(BUNDLE_SYMBOLICNAME));
        //put(BUNDLE_VERSION, h.get(BUNDLE_VERSION));
        if (h.getAttribute(SCOPE) != null) put(SCOPE, h.getAttribute(SCOPE)); //needed at runtime for setProperty
        put(TYPE, h.getAttribute(TYPE)); //needed for filedata
        if (FILEDATA.equalsIgnoreCase(h.getAttribute(TYPE)))
        {
            put(FILENAME, h.getAttribute(FILENAME));
        }
        put(INSTANCES, ((Map) h).get(INSTANCES)); // may happen during deserialization
        addInstance(i);
        check();
    }

    public void addInstance(String i)
    {
        if (i == null) return;
        if (instanceValues().get(i) != null) throw new IllegalArgumentException("instance "+i+" already exists");
        instanceValues().put(i, null);
    }

    public void removeInstance(String i)
    {
        if (i == null) return;
        instanceValues().remove(i);
    }

    public Map<String, String> instanceValues()
    {
        Map<String, String> instanceValues = (Map<String, String>) get(INSTANCES);
        if (instanceValues == null)
        {
            instanceValues = new HashMap<String, String>();
            put(INSTANCES, instanceValues);
        }
        return instanceValues;
    }

    @Override
    public JSONObject toJSONObject()
    {
        JSONObject json = new JSONObject(this);
        try
        {
            json.put(INSTANCES, instanceValues());
        }
        catch (Exception e)
        {
            logger.warn("JSON serialisation error!", e);
        }
        return json;
    }

    @Override
    public void setValue(String instance, String s)
    {
        //TODO property validation according to type and other attributes ? 
        if (instance == null || "*".equals(instance))
        { //set value at component level
            super.setValue(s);
        }
        else if ("*.force".equals(instance))
        { //set for all instances => clear all overriden values!
            super.setValue(s);
            for (String i : instanceValues().keySet())
                instanceValues().put(i, null);
        }
        else if (!instanceValues().containsKey(instance))
        {
            throw new IllegalArgumentException("Unknown instance " + instance);
            // if value == default value => just remove instance value to use default!
        }
        // ? else if (s != null && s.equals(super.getValue()))
        //    instanceValues().put(instance, null);
        else
            instanceValues().put(instance, s);
    }

    @Override
    public String getValue(String instance)
    {
        if (instance == null || "*".equals(instance))
            return super.getValue();
        if (!instanceValues().containsKey(instance))
            throw new IllegalArgumentException("Unknown instance " + instance);
        String iv = instanceValues().get(instance);
        return (iv != null ? iv : super.getValue());
    }

    @Override
    public String getPropertiesValue(String instance, String key) throws Exception
    {
      Properties p = new Properties();
      p.load(new StringReader(getValue(instance)));
      return p.getProperty(key);
    }

    @Override
    public String setPropertiesValue(String instance, String key, String value) throws Exception
    {
      //use of commons-configuration API allows to preserve comments and layout
      PropertiesConfiguration p = new PropertiesConfiguration();
      p.load(new StringReader(getValue(instance)));
      String oldval = p.getString(key);
      p.setProperty(key, value);
      StringWriter sw = new StringWriter();
      p.save(sw);
      setValue(instance, sw.toString());
      return oldval;
    }

    @Override
    public void resetDefaultValue(String instance)
    {
        throw new RuntimeException("FIXME not implemented!");//lookup default val in original descriptor
    }

    public void updateComponent()
    {
        throw new RuntimeException("FIXME not implemented!");//use OSGi EventAdmin
    }

    public void updateGroup()
    {
        throw new RuntimeException("FIXME not implemented!");//use OSGi EventAdmin
    }

    public void updatePlatform()
    {
        throw new RuntimeException("FIXME not implemented!");//use OSGi EventAdmin
    }

    public void commitProperties()
    {
        throw new RuntimeException("FIXME not implemented!");//use OSGi EventAdmin
    }

    //TODO:
    //setScope(platform|group|comp|inst) => always automatically apply to the scope
}
