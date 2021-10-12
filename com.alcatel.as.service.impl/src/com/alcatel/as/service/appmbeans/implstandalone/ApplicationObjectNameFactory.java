// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.appmbeans.implstandalone;

import java.util.Locale;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

class ApplicationObjectNameFactory
{
    private static String parent; // Injected from ApplicationMBeanFactoryImpl
    private static String pid; // Injected from ApplicationMBeanFactoryImpl

    static void setParent(String parent)
    {
        ApplicationObjectNameFactory.parent = parent;
    }

    static void setPid(String pid)
    {
        ApplicationObjectNameFactory.pid = pid;
    }

    private final static String DOMAIN_PREFIX = "com.alcatel_lucent.as.";

    /**
     * Create an ObjectName with pid/parent/appName/instance/version keys
     * @param key key
     * @param protocol protocol
     * @param name name
     * @param major major version
     * @param minor minor version
     * @return an ObjectName
     * @throws MalformedObjectNameException
     */
    protected ObjectName createObjectName(String key, String protocol, String name, int major, int minor)
        throws MalformedObjectNameException
    {
        if (parent == null || pid == null)
        {
            throw new IllegalStateException("ApplicationObjectNameFactory has not been configured with instance name and instance pid");
        }

        // strip eventual dash from key.
        key = key.replaceAll("-", "");
        String protU = protocol.toUpperCase(Locale.getDefault());
        StringBuffer oname = new StringBuffer(DOMAIN_PREFIX);
        oname.append(protU);
        oname.append(":pid=");
        oname.append(pid);
        oname.append(",parent=");
        oname.append(parent);
        oname.append('-');
        oname.append(protU);
        oname.append(",key=");
        oname.append(key);
        oname.append(",appName=");
        oname.append(name);
        oname.append(",instance=");
        oname.append(parent);
        oname.append('-');
        oname.append(protU);
        oname.append('-');
        oname.append(name);
        oname.append('-');
        oname.append(key);
        oname.append(",version=");
        oname.append(major);
        oname.append('.');
        oname.append(minor);
        return new ObjectName(oname.toString());
    }
}
