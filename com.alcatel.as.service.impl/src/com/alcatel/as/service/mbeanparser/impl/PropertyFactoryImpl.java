package com.alcatel.as.service.mbeanparser.impl;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.JSONException;

import com.alcatel.as.service.metatype.PropertyFactory;

public class PropertyFactoryImpl implements PropertyFactory
{
    static final String CLASS = "class";
    static final String VALUE = "value";

    public static final Map<String, PropertyFactory> factories = new HashMap<String, PropertyFactory>()
    {
        {
            put(MBeanDescriptorImpl.class.getSimpleName(), new MBeanDescriptorImpl());
            put(MBeanDescriptorImpl.class.getSimpleName()+MBeanDescriptorImpl.PROPERTIES_KEY, 
                new PropertyDescriptorImpl());
            put(PropertyDescriptorImpl.class.getSimpleName(), new PropertyDescriptorImpl());
            put(InstancePropertiesImpl.class.getSimpleName(), new InstancePropertiesImpl());
            put(InstancePropertiesImpl.class.getSimpleName()+InstancePropertiesImpl.PROPERTIES_KEY, 
                new InstancePropertyImpl());
            put(InstancePropertyImpl.class.getSimpleName(), new InstancePropertyImpl());
        }
    };

    public Object loadJson(String json) throws IllegalArgumentException
    {
        try
        {
            return loadJson(new JSONObject(json));
        }
        catch (Exception e)
        {
            throw (IllegalArgumentException) new IllegalArgumentException(json).initCause(e);
        }
    }

    public Object loadJson(JSONObject json) throws IllegalArgumentException
    {
        try
        {
            String clazz = json.getString(CLASS);
            JSONObject value = json.getJSONObject(VALUE);
            PropertyFactory innerfactory = factories.get(clazz);
            if (innerfactory != null)
                return innerfactory.loadJson(value);
            else
                throw new IllegalArgumentException("Unrecognized object " + json.toString());
        }
        catch (Exception e)
        {
            throw (IllegalArgumentException) new IllegalArgumentException(json.toString()).initCause(e);
        }
    }

    public String toJson(Object obj) throws IllegalArgumentException
    {
        try {
          return toJSONObject(obj).toString(2);
        } catch(JSONException e) {
          throw (IllegalArgumentException) new IllegalArgumentException(obj.toString()).initCause(e);
        }
    }

    public JSONObject toJSONObject(Object obj) throws IllegalArgumentException
    {
        if (obj == null) throw new IllegalArgumentException("null argument!");

        if (PropertyFactory.class.isAssignableFrom(obj.getClass()))
            try
            {
                return new JSONObject().put(CLASS, obj.getClass().getSimpleName())
                        .put(VALUE, ((PropertyFactory) obj).toJSONObject(obj));
            }
            catch (Exception e)
            {
                throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
            }
        else
            throw new IllegalArgumentException("Unrecognized object " + obj.getClass().getName());
    }
}
