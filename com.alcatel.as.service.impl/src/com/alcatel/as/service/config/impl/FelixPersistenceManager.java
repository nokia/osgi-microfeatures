// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.config.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.cm.PersistenceManager;

public class FelixPersistenceManager implements PersistenceManager
{
    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<String, Dictionary> _cmstore = new ConcurrentHashMap<String, Dictionary>();

    public void delete(String pid) throws IOException
    {
        _cmstore.remove(pid);
    }

    public boolean exists(String pid)
    {
        return _cmstore.containsKey(pid);
    }

    @SuppressWarnings("unchecked")
    public Enumeration getDictionaries() throws IOException
    {
        Vector<Dictionary> v = new Vector<Dictionary>();
        for (Dictionary d : _cmstore.values())
        {
            v.add(clone(d));
        }
        return v.elements();
    }

    @SuppressWarnings("unchecked")
    public Dictionary load(String pid) throws IOException
    {
        Dictionary d = _cmstore.get(pid);
        return d == null ? new Hashtable() : clone(d);
    }

    @SuppressWarnings("unchecked")
    public void store(String pid, Dictionary dic) throws IOException
    {
        _cmstore.put(pid, clone(dic));
    }

    @SuppressWarnings("unchecked")
    private Dictionary clone(Dictionary d)
    {
        Hashtable h = new Hashtable();
        Enumeration e = d.keys();
        while (e.hasMoreElements())
        {
            Object key = e.nextElement();
            Object val = d.get(key);
            h.put(key, val);
        }
        return h;
    }
}
