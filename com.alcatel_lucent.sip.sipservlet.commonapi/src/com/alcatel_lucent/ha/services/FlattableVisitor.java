// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.ha.services;

import java.util.Map;


/**
 * 
 * The inspector  that is able to visit a Flattable to read  or write the @Flat
 * fields into a map according to the Flattable semantics
 * 
 */
public interface FlattableVisitor {
    /**
     * used for writing the differences of a Flattable object into a map (visitor called for every  subsequent writing)
     * @param map
     * @param flattableObject
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchFieldException
     */
    public void visitDiff(Map<String, Object> map, Flattable object)
            throws IllegalArgumentException, IllegalAccessException,
            SecurityException, NoSuchFieldException;

    /**
     * used for writing the content of a Flattable object into a map (visitor called only for initial writing)
     *  @param map
     * @param flattableObject
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchFieldException
  
     */
    void visit(Map<String, Object> map, Flattable flattableObject)
            throws IllegalArgumentException, IllegalAccessException;
    /**
     * used for writing the content of  Flattable field with a key in  a map
     * @param map
     * @param value
     */
    void visit( Flattable flattableObject,Map<String, Object> map)
            throws IllegalArgumentException, IllegalAccessException;

    public int registerOnVisiting(Flattable value) ;

    public boolean unregisterOnVisiting(Flattable value);

}
