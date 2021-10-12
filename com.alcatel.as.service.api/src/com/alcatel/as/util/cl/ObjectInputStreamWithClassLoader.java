// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.cl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

/**
 * Class used to deserialize some objects from an ObjectInputStream, but using some given class loaders.
 */
public class ObjectInputStreamWithClassLoader extends ObjectInputStream
{
    /**
     * Construct a new instance with the given classloader and input stream.
     *
     * @param in stream to read objects from
     * @param loader classloader used when deserializing the object input stream
     * @throws IOException on any errors
     */
    public ObjectInputStreamWithClassLoader(InputStream in, ClassLoader loader) throws IOException
    {
        this(in, new ClassLoader[] { loader });
    }

    /**
     * Construct a new instance with the given classloader and input stream.
     *
     * @param in stream to read objects from
     * @param loaders class loaders used when deserializing the object input stream
     * @throws IOException on any errors
     */
    public ObjectInputStreamWithClassLoader(InputStream in, ClassLoader... loaders) throws IOException
    {
        super(in);
        _loaders = loaders;
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException
    {
        String name = osc.getName();
        Class<?> clazz = (Class<?>) primClasses.get(name);
        return clazz != null ? clazz : loadClass(name);
    }

    @Override
    protected Class<?> resolveProxyClass(String[] interfaces) throws IOException, ClassNotFoundException
    {
        Class<?>[] interfacesClass = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++)
        {
            interfacesClass[i] = loadClass(interfaces[i]);
        }

        for (int i = 0; i < _loaders.length; i++)
        {
            try
            {
                return Proxy.getProxyClass(_loaders[i], interfacesClass);
            }
            catch (IllegalArgumentException e)
            {
                if (i == _loaders.length - 1)
                {
                    throw e;
                }
            }
        }
        throw new ClassNotFoundException(Arrays.toString(interfaces));
    }

    private final static Logger _logger = Logger.getLogger(ObjectInputStreamWithClassLoader.class);

    private Class<?> loadClass(String name) throws ClassNotFoundException
    {
      Class<?> clazz = cache.get(name);
      if (clazz != null) return clazz;
        for (int i = 0; i < _loaders.length; i++)
        {
            try
            {
                clazz = Class.forName(name, false, _loaders[i]);
                if (_logger.isDebugEnabled()) _logger.debug("loaded class " + clazz + " with classloader + " + _loaders[i]);
                cache.put(name, clazz);
                int size = cache.size();
                if ((size%1024)==0) _logger.warn("class cache size=" + size);
                return clazz;
            }
            catch (ClassNotFoundException cnfe)
            {
                if (i == _loaders.length - 1)
                {
                  if (_logger.isDebugEnabled()) _logger.debug("could not load class " + name + " using class loader " + _loaders[i]
                      + " (FATAL)");
                    throw cnfe;
                }
                else
                {
                  if (_logger.isDebugEnabled()) _logger.debug("could not load class " + name + " using class loader " + _loaders[i]
                      + " (trying with next class loader " + _loaders[i + 1]);
                }
            }
        }
        throw new ClassNotFoundException(name);
    }

    /**
     * The classloader to use when the default classloader cannot find
     * the classes in the stream.
     */
    private ClassLoader[] _loaders;
    
    private static ConcurrentHashMap<String, Class<?>> cache = new ConcurrentHashMap<String, Class<?>>(128);

    /**
     * Map of primitive type classes.
     */
    private static final HashMap<String, Class<?>> primClasses = new HashMap<String, Class<?>>();

    static
    {
        primClasses.put("boolean", boolean.class);
        primClasses.put("byte", byte.class);
        primClasses.put("char", char.class);
        primClasses.put("short", short.class);
        primClasses.put("int", int.class);
        primClasses.put("long", long.class);
        primClasses.put("float", float.class);
        primClasses.put("double", double.class);
        primClasses.put("void", void.class);
    }
}
