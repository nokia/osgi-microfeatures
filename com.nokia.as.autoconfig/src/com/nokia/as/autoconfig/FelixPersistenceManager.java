// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.cm.PersistenceManager;

@SuppressWarnings("unchecked")
public class FelixPersistenceManager implements PersistenceManager {
    private ConcurrentHashMap<String, Dictionary> cmstore = new ConcurrentHashMap<String, Dictionary>();

    public void delete(String pid) throws IOException {
        cmstore.remove(pid);
    }

    public boolean exists(String pid) {
        return cmstore.containsKey(pid);
    }

    public Enumeration getDictionaries() throws IOException {
        Vector<Dictionary> v = new Vector<Dictionary>();
        for (Dictionary d : cmstore.values()){
            v.add(clone(d));
        }
        return v.elements();
    }

    public Dictionary load(String pid) throws IOException {
        Dictionary d = cmstore.get(pid);
        return d == null ? new Hashtable() : clone(d);
    }

    public void store(String pid, Dictionary dic) throws IOException {
        cmstore.put(pid, clone(dic));
    }

    private Dictionary clone(Dictionary d) {
        Hashtable h = new Hashtable();
        Enumeration e = d.keys();
        while (e.hasMoreElements()) {
            Object key = e.nextElement();
            Object val = d.get(key);
            h.put(key, val);
        }
        return h;
    }
}
