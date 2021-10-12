// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package com.alcatel_lucent.ha.services;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 
 * An object that is able to be mapped into a map
 * 
 */
public interface Flattable extends Diffable {
    final String KEYSEP = ".";
    final String VALSEP = "%%";
     final String KEYROOT = "R";
    final String MAPSEP = "!";
    /**
     *   set the value of the field f to the value v
     * @param f
     * @param value
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    void writeField(Field f, Object value) throws IllegalArgumentException,
            IllegalAccessException;
    /**
     *   return the list of of the flat field for the current class. You should use FlattableSupport.buildFields
     * @param f
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    List<FlatField> fields();
    /**
     *   return the value of the field f
     * @param f
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    Object readField(Field f) throws IllegalArgumentException, IllegalAccessException;
    /**
     * primary callbacks invoked by the framework. Allow the object to control and complete the flattening procedure.
     * the method should invoke : 
     * <code>visitor.visit(map, this);</code
     * <code>diff(true);// active the diff mode</code> 
     * @param map
     * @param visitor
     * @return true if write has to be done
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    boolean write(Map<String, Object> map, FlattableVisitor v)
            throws IllegalArgumentException, IllegalAccessException;
    /**
     * primary callbacks invoked by the framework. Allow the object to control and complete the unflattening procedure.
     * the method should invoke : visitor.visit(this, map);
     * @param visitor
     * @param map
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    void read(FlattableVisitor visitor, Map<String, Object> map)
            throws IllegalArgumentException, IllegalAccessException;
    /**
     * key setter/getter invoked by the framework to attribute a context key to the flattable object.
     * first invocation is a setter, second invocation is the getter  
     * @param key ; maybe 0 to get the value
     * @return the ha context key
     */
    int key(int key);
    /**
     * 
     */
    void readDone();
}
