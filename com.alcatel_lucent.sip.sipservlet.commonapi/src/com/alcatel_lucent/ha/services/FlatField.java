// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package com.alcatel_lucent.ha.services;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * 
 * FlatField is a representation of the @Flat field of a Flattable Object
 * It is internally used by the flattable framework 
 */

public class FlatField {
    protected Field _field;
    protected int _size;
    protected StringBuilder _key;
    private boolean istring = false;
    protected Class<? extends Flattable> _type = null;
    protected final static Logger _logger = Logger
    .getLogger(RecoveryServiceSupport.class.getName());

     FlatField(Field f, String key, Class<? extends Flattable> thetype,
            boolean stringable) {
        this(f, key);
        istring = stringable;
        _type = thetype == Flattable.class ? null : thetype;
    }

     FlatField(Field f, String key) {
        _field = f;
        _key = new StringBuilder(Flattable.KEYSEP);
        _key.append(key);
        _size=_key.length();
    }

    protected String typedValue(Flattable value) {
        com.alcatel_lucent.ha.services.annotation.Flattable flattableAnnotation = value
                .getClass()
                .getAnnotation(
                        com.alcatel_lucent.ha.services.annotation.Flattable.class);
        if (flattableAnnotation != null) {
            StringBuilder builder = new StringBuilder();
            builder.append(value.key(0));
            builder.append(com.alcatel_lucent.ha.services.Flattable.VALSEP);
            builder.append(flattableAnnotation.type().getName());
            return builder.toString();
        } else
            return null;
    }

    protected Object writeFlattable(Flattable value, Map<String, Object> map,
            FlattableVisitor visitor) throws IllegalArgumentException,
            IllegalAccessException {
        int keyvalue = value.key(0);
        if (keyvalue == 0) {
            keyvalue = visitor.registerOnVisiting(value);
            // don't write if write return false
            if (!value.write(map, visitor)) {
                    visitor.unregisterOnVisiting(value);
                    return null;
            }
        }
        String typedValue = _type != null ? null : typedValue(value);
        return typedValue != null ? typedValue : keyvalue;
    }

     void write(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor) throws IllegalArgumentException,
            IllegalAccessException {
        Object value = object.readField(_field);
      
        if (value != null) {
                String key=_key.insert(0, object.key(0)).toString();
                _key.delete(0,_key.length()-_size);
                if (Flattable.class.isAssignableFrom(value.getClass())) {
                    Object flattableref=writeFlattable((Flattable) value,
                            map, visitor);
                    if (flattableref!=null)  map.put(key, flattableref);
                } else {
                    map
                            .put(key, istring ? value.toString()
                                    : value);
                }
        }
    }
    protected Flattable findObject(List<Flattable> objects, Integer key) {
        for (Flattable object : objects) {
            if (key == object.key(0))
                return object;
        }
        return null;
    }

    protected Flattable findObject(List<Flattable> objects, String key) {
        return findObject(objects, Integer.parseInt(key));
    }
    @SuppressWarnings("rawtypes")
	protected Flattable createObject(RecoveryServiceSupport support,String key,String classname) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        return support.createSub(key, Thread
                .currentThread()
                .getContextClassLoader().loadClass(
                        classname));
    }
    /**
     * @param map
     * @param object
     * @param objects
     * @param support TODO
     * @throws ClassNotFoundException 
     * @throws NoSuchMethodException 
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     * @throws SecurityException 
     * @throws IllegalArgumentException 
     */
     @SuppressWarnings("rawtypes")
	void readObject(Map<String, Object> map, Flattable object,
            List<Flattable> objects, RecoveryServiceSupport support) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        if (Serializable.class.isAssignableFrom(_field.getType())) {
            Object keyvalue=null;
            try {
                 keyvalue = map.get(_key.insert(0,object.key(0)).toString());
            } finally {
                _key.delete(0, _key.length()-_size);
            }
            if (keyvalue != null) {
                String[] keyvalues = keyvalue.toString()
                        .split(Flattable.VALSEP);
                if (keyvalues.length == 2) {
                    Flattable subobject = findObject(objects, keyvalues[0]);
                    if (subobject==null) {
                        subobject=createObject(support,keyvalues[0],keyvalues[1]);
                        FlattableField.readObjects(map,subobject,objects,support);
                    }
                    object.writeField(_field, subobject);
                }
            }
        }
    }

    /**
     * @param flattable
     * @param map
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
    public void read(Flattable flattable, Map<String, Object> map) throws IllegalArgumentException, IllegalAccessException {
        Object keyvalue=null;
        try {
             keyvalue = map.get(_key.insert(0,flattable.key(0)).toString());
        } finally {
            _key.delete(0, _key.length()-_size);
        }
        // don't read flattable placeholder
        if (keyvalue != null
                && keyvalue.toString().indexOf(Flattable.VALSEP) <= 0) {
            if (_logger.isDebugEnabled())
                _logger.debug("visit field " + this + " with : "
                        + keyvalue);
            flattable.writeField(_field, keyvalue);
        } else {
            if (_logger.isDebugEnabled())
                _logger.debug("visit field IGNORED : " + _field);
        }
    }
    public String toString() {
      return _field.toString();  
    }

    /**
     * @param object
     * @param map
     * @param haContextImpl
     * @param string
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     */
     void writeDiff(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor, String mapkey) throws IllegalArgumentException, IllegalAccessException {
      }

    /**
     * @param object
     * @param map
     * @param haContextImpl
     * @param string
     */
     void remove(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor, String string) throws IllegalArgumentException, IllegalAccessException {
        throw new IllegalStateException("cannot remove no map field"+_field);
        
    }
}
