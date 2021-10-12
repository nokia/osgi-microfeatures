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
import java.util.Collection;
import java.util.List;
import java.util.Map;

 class CollectionField extends FlatField {
    @SuppressWarnings("rawtypes")
	Class<? extends Collection> _coltype=null;
    String _aggregation=null;
    @SuppressWarnings("rawtypes")
	public CollectionField(Field f, String key,Class<? extends Flattable> type, Class<? extends Collection>  coltype,String aggField) {
        super(f, key,type,false);
        _coltype=coltype;
        if (!"".equals(aggField)) _aggregation=aggField;
    }
    @SuppressWarnings("unchecked")
	public void write(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor) throws IllegalArgumentException,
            IllegalAccessException {
        Object value = object.readField(_field);
        if (value != null) {
            Collection<Flattable> flattables = (Collection<Flattable>) value;
            if (flattables.size()>0) {
                String key=_key.insert(0, object.key(0)).toString();
                _key.delete(0,_key.length()-_size);
                StringBuilder collectionvalue=new StringBuilder();
                Object flattableRef=null;
                for (Flattable flattable : flattables) {
                    if (flattableRef!=null)  collectionvalue.append(flattableRef).append(Flattable.VALSEP);
                     flattableRef=writeFlattable(flattable,map,visitor);
                }
                if (flattableRef!=null)  collectionvalue.append(flattableRef).append(Flattable.VALSEP);
                if (collectionvalue.length()!=0) map.put(key, collectionvalue.toString());
            }
        }
    }
    public void readObject(Map<String, Object> map, Flattable object,
            List<Flattable> objects, @SuppressWarnings("rawtypes") RecoveryServiceSupport support) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
            String key=_key.insert(0, object.key(0)).toString();
            _key.delete(0,_key.length()-_size);
            Object keyvalue = map.get(key);
            if (keyvalue!= null) {
                @SuppressWarnings("unchecked")
				Collection<Flattable> sublist = (Collection<Flattable>) _coltype.getConstructor().newInstance();
                String subkeys=keyvalue.toString();
                for (String subkey : subkeys.split(Flattable.VALSEP)) {
                    Flattable subobject = findObject(objects, subkey);
                    if (subobject == null) {
                        subobject = support.createSub(subkey, _type);
                        if (_aggregation!=null) {
                            subobject.writeField(FlattableField.getField(subobject.getClass(),_aggregation), object);
                        }
                        FlattableField.readObjects(map,subobject,objects,support);
                    }
                     sublist.add(subobject);
                }
                object.writeField(_field, sublist);
            }
    }
    public void read(Flattable flattable, Map<String, Object> map) throws IllegalArgumentException, IllegalAccessException {
    }
}
