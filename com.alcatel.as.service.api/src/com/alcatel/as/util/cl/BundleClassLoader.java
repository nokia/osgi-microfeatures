// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.cl;

// Osgi
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.osgi.framework.Bundle;

/**
 * This class loader delegates class loading to a bundle class loader.
 */
@SuppressWarnings("unchecked")
public class BundleClassLoader extends ClassLoader
{
    public BundleClassLoader(Bundle bundle)
    {
        if (bundle == null)
        {
            throw new NullPointerException("Bundle passed to the BundleClassLoader constructor is null");
        }
        _bundle = bundle;
    }

    @Override
    public String toString()
    {
        return "BundleClassLoader[" + _bundle.getSymbolicName() + "/id=" + _bundle.getBundleId() + "]";
    }

    @Override
    public synchronized URL getResource(String name)
    {
        return _bundle.getResource(name);
    }

    @Override
    public synchronized Enumeration<URL> getResources(String name) throws IOException
    {
        Enumeration<URL> e = _bundle.getResources(name);
        if (e == null)
        {
            return EMPTY_ENUMERATION;
        }
        return e;
    }

    @Override
    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        return _bundle.loadClass(name);
    }

    /** Our bundle which is used to load classes from inside it */
    private final Bundle _bundle;

    /** Helper used for empty enumerations. */
    private final static Enumeration<URL> EMPTY_ENUMERATION = new EmptyEnumeration<URL>();

    /** Helper class used to return an empty enumeration. */
    private static class EmptyEnumeration<T> implements Enumeration<T>
    {
        public boolean hasMoreElements()
        {
            return false;
        }

        public T nextElement() throws NoSuchElementException
        {
            throw new NoSuchElementException();
        }
    }
}
