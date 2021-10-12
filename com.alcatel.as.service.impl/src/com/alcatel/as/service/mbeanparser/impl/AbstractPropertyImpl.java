// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.mbeanparser.impl;

import static com.alcatel.as.service.metatype.PropertyDescriptor.DYNAMIC;
import static com.alcatel.as.service.metatype.PropertyDescriptor.NAME;
import static com.alcatel.as.service.metatype.PropertyDescriptor.TYPE;
import static com.alcatel.as.service.metatype.PropertyDescriptor.VALUE;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.alcatel.as.service.metatype.PropertyFactory;

/** base class for all Property[Factory] implems */
public abstract class AbstractPropertyImpl extends HashMap<String, Object> implements PropertyFactory
{
    static final Logger logger = Logger.getLogger("as.management.config.property");
    protected final static char DASH = '-';
    protected final static char SLASH = '/';
    protected final static String ID = "id";
    protected final static String SV_KEY = "sv"; //serialization version
    private static final long serialVersionUID = 1L;

    public AbstractPropertyImpl()
    {
        put(SV_KEY, serialVersionUID);
    }

    public AbstractPropertyImpl(Map<String, Object> h)
    {
        super(h);
    }

    // PropertyFactory implementation
    public Object loadJson(String jsonstr) throws IllegalArgumentException
    {
        try
        {
            return loadJson(new JSONObject(jsonstr));
        }
        catch (Exception e)
        {
            throw (IllegalArgumentException) new IllegalArgumentException(jsonstr).initCause(e);
        }
    }

    public String toJson(Object property) throws IllegalArgumentException
    {
      try 
      {
        return toJSONObject(property).toString(2);
      }
      catch (Exception e)
      {
        throw (IllegalArgumentException) new IllegalArgumentException(property.toString()).initCause(e);
      }
    }

    public JSONObject toJSONObject()
    {
        try
        {
            return toJSONObject(this);
        }
        catch (Exception e)
        {
            logger.warn("", e);
            return null;
        } //impossible!
    }

    public JSONObject toJSONObject(Object property) throws IllegalArgumentException
    {
        if (!(property instanceof AbstractPropertyImpl))
            throw new IllegalArgumentException(property.toString());
        return new JSONObject((HashMap)property);
    }

    public Object loadJson(JSONObject json) throws IllegalArgumentException
    {
      try 
      {
        return newInstance(loadJsonMap(json, null));
      }
      catch (Exception e)
      {
        throw (IllegalArgumentException) new IllegalArgumentException(json.toString()).initCause(e);
      }
    }

    protected Map<String, Object> loadJsonMap(JSONObject json, PropertyFactory factory) throws Exception
    {
        Map<String, Object> attrs = new HashMap<String, Object>();
        for (Iterator i = json.keys(); i.hasNext();) 
        {
          String k = (String) i.next();
          Object v = json.get(k); //could be a JSONObject.NULL
          if (v == null || v == JSONObject.NULL)
          {
            attrs.put(k, null);
          }
          else if (v instanceof String)
          {
            attrs.put(k, (String) v);
          }
          else if (v instanceof JSONObject) // handle inner maps recursively
          {
            if (factory != null) 
            {
              attrs.put(k, factory.loadJson((JSONObject)v));
            }
            else 
            {
              // case of inner properties objects:
              // MBeanParserImpl.properties -> map of PropertyDescriptorImpl
              // InstancePropertiesImpl.properties -> map of InstancePropertyImpl
              // if innerfactory is null, decode as a regular map
              PropertyFactory innerfactory = PropertyFactoryImpl.factories.get(this.getClass().getSimpleName()+k);
              attrs.put(k, loadJsonMap((JSONObject)v, innerfactory));
            }
          }
          else 
          {
            handleJson(attrs, k, v);
          }
        }
        return attrs;
    }

    protected void handleJson(Map attrs, String k, Object v) throws Exception
    {
        //override if needed
        attrs.put(k, v);
    }

    protected abstract AbstractPropertyImpl newInstance(Map<String, Object> m);

    protected void check()
    {
        if (getName() == null)
            throw new NullPointerException("missing property name in " + super.toString());
        /*
        if (getPid() == null)
            throw new NullPointerException("missing service pid in " + super.toString());
         * if(getBundleName() == null) throw new NullPointerException("missing bundle name in "+super.toString());
         * if(getAttribute(BUNDLE_VERSION) == null) { put(BUNDLE_VERSION, "0.0.0"); }
         */
        String t = getAttribute(TYPE);
        put("type", t == null ? "string" : t.toLowerCase(Locale.getDefault()));
    }

    public Map<String, Object> getAttributes()
    {
        return Collections.unmodifiableMap(this);
    }

    public String getAttribute(String key)
    {
        Object v = get(key);
        return v == null ? null : v.toString();
    }

    public String getName()
    {
        return getAttribute(NAME);
    }

    public String getPid()
    {
        return getAttribute(SERVICE_PID);
    }

    public String getValue()
    {
        return getAttribute(VALUE);
    }

    public void setValue(String s)
    {
        put(VALUE, s);
        if (logger.isDebugEnabled())
        {
            logger.debug("Property " + toString() + " updated with value " + s);
        }
    }

    public String toString()
    {
        return new StringBuilder(getClass().getSimpleName())
          .append("[")
          .append(get(NAME) != null
              ? get(NAME)
              : get(ID))
          .append("]")
          .toString();
    }
}
