// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.mbeanparser.impl;

import static org.osgi.framework.Constants.BUNDLE_NAME;
import static org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME;
import static org.osgi.framework.Constants.BUNDLE_VERSION;
import static org.osgi.framework.Constants.SERVICE_PID;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import alcatel.tess.hometop.gateways.utils.Utils;

import com.alcatel.as.service.metatype.InstanceProperties;
import com.alcatel.as.service.metatype.PropertiesDescriptor;
import com.alcatel.as.service.metatype.PropertyDescriptor;
import com.alcatel.as.service.metatype.PropertyFactory;
import static com.alcatel.as.service.metatype.PropertyDescriptor.*;

/**
 * PropertiesDescriptor implementation.
 * one PropertiesDescriptor = one MBD = one pid
 * {
 *   bsn: symbolic.name,
 *   bv: bundle-version,
 *   filename: bsn-bv.jar,
 *   pid: pid,
 *   properties: {
 *     property.name: { //one PropertyDescriptor
 *         help: help1,
 *         title: title1,
 *         section: section,
 *         value: value1
 *     }
 *   }
 * }
 */
public class MBeanDescriptorImpl extends AbstractPropertyImpl implements PropertiesDescriptor
{
    private static final long serialVersionUID = 1L;
    public static final String PROPERTIES_KEY = "properties";
    public static final String BUNDLE_FILENAME = "bundle-filename";


    /**
     * constructor used by MBeanParserImpl
     * @param pid the pid for this "mbeans-descriptors"
     * @param l a list of maps; each map is the meta data for one property
     * @param bn origin bundle name
     * @param bsn origin bundle symbolic name
     * @param bv origin bundle version
     * @param fn origin file name
     */
    MBeanDescriptorImpl(String pid,
                        List<? extends Map<String, Object>> l,
                        String bn,
                        String bsn,
                        String bv,
                        String fn)
    {
        this(pid, bn, bsn, bv, fn);
        createProperties(l);
    }

    private MBeanDescriptorImpl(String pid, String bn, String bsn, String bv, String fn)
    {
        super();
        put(SERVICE_PID, pid);
        put(BUNDLE_NAME, bn);
        put(BUNDLE_SYMBOLICNAME, bsn);
        put(BUNDLE_VERSION, (bv != null ? bv : "0.0.0"));
        put(BUNDLE_FILENAME, fn);
        put(ID, new StringBuilder(fn.substring(0, fn.lastIndexOf('.'))) // bsn-bv : but safer in case of mismatch when loading blueprints
            .append(SLASH).append(pid)
            .toString());
    }

    // for deserialization
    public MBeanDescriptorImpl() { super(); }

    private MBeanDescriptorImpl(Map<String, Object> m) { super(m); }

    @Override
    protected AbstractPropertyImpl newInstance(Map<String, Object> m) 
    {
      return new MBeanDescriptorImpl(m);
    }

    @Override
    protected void check() {}

    public String getBundleName()
    {
        return (String)get(BUNDLE_NAME);
    }

    public String getBundleSymbolicName()
    {
        return (String)get(BUNDLE_SYMBOLICNAME);
    }

    public String getBundleVersion()
    {
        return (String)get(BUNDLE_VERSION);
    }

    public String getPid()
    {
        return (String)get(SERVICE_PID);
    }

    public String getId()
    {
        return (String)get(ID);
    }

    /**
     * @param l a list of maps; each map is the meta data for one property
     */
    private void createProperties(List<? extends Map<String, Object>> l)
    {
        Map<String, PropertyDescriptor> properties = new HashMap<String, PropertyDescriptor>();
        for (Map<String, Object> m : l)
        {
            m.put(DEFAULT_VALUE, m.get(VALUE));
            properties.put((String)m.get(NAME), new PropertyDescriptorImpl(m));
        }
        put(PROPERTIES_KEY, properties);
    }

    public Map<String, PropertyDescriptor> getProperties()
    {
        return (Map<String, PropertyDescriptor>)get(PROPERTIES_KEY);
    }

    public InstanceProperties instantiate(String p, String g, String c, String i)
    {
        return new InstancePropertiesImpl(p, g, c, i, this);
    }
}
