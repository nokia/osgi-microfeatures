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
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

 class FlattableField extends FlatField {
     @SuppressWarnings("unchecked")
	static  Field getField(Class<? extends Flattable> c, String name) {
         Field field = null;
         try {
             field = c.getDeclaredField(name);
         } catch (SecurityException e) {
         } catch (NoSuchFieldException e) {
             @SuppressWarnings("rawtypes")
			Class superclass = c.getSuperclass();
             if (superclass != null) {
                 return getField(superclass, name);
             }
         }
         return field;
     }
    private String _aggregation=null;
    public FlattableField(Field f, String key, Class<? extends Flattable> type, String aggField) {
        super(f, key, type, false);
        if (!"".equals(aggField)) _aggregation=aggField;
    }

    public void write(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor) throws IllegalArgumentException,
            IllegalAccessException {
        Flattable value = (Flattable) object.readField(_field);
        if (value != null) {
                String key=_key.insert(0, object.key(0)).toString();
                _key.delete(0,_key.length()-_size);
                Object flattableRef=writeFlattable(value, map, visitor);
                if (flattableRef!=null) map.put(key, flattableRef);
        }
    }

    /**
     * @param map
     * @param object
     * @param objects
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws SecurityException
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
	public static void readObjects(Map<String, Object> map, Flattable object,
            List<Flattable> objects, RecoveryServiceSupport support)
            throws IllegalArgumentException, SecurityException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException,
            ClassNotFoundException {
        objects.add(object);
        if (_logger.isDebugEnabled()) {
            _logger.debug("readObjects ("+object.key(0)+")"+object.getClass());
        }
        for (FlatField f : object.fields()) {
            if (_logger.isDebugEnabled()) {
                _logger.debug("readObject -> "+f);
            }
            f.readObject(map, object, objects, support);
        }
        if (_logger.isDebugEnabled()) {
            _logger.debug("readObjects ("+object.key(0)+")done.");
        }
    }

    @SuppressWarnings("rawtypes")
	public void readObject(Map<String, Object> map, Flattable object,
            List<Flattable> objects, RecoveryServiceSupport support)
            throws IllegalArgumentException, SecurityException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException,
            ClassNotFoundException {
        String key=_key.insert(0, object.key(0)).toString();
        _key.delete(0,_key.length()-_size);
        Integer keyvalue = (Integer) map.get(key);
        if (keyvalue != null) {
            Flattable subobject = findObject(objects, keyvalue);
            if (subobject == null) {
                subobject = support.createSub(keyvalue, _type);
                if (_aggregation!=null) {
                    subobject.writeField(FlattableField.getField(subobject.getClass(),_aggregation), object);
                }
                FlattableField.readObjects(map,subobject,objects,support);
            }
            object.writeField(_field, subobject);
        }
    }
    public void read(Flattable flattable, Map<String, Object> map) throws IllegalArgumentException, IllegalAccessException {
    }
}
