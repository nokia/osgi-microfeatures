// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.ha.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alcatel_lucent.ha.services.annotation.Flat;
/**
 * 
 * A helper class that helps standard objects to implement Flattable interface.
 */
public class FlattableSupport {
    private final static Logger _logger = Logger
    .getLogger(RecoveryServiceSupport.class.getName());
    @SuppressWarnings("rawtypes")
	static private List<Field> collectFields(List<Field> fields, Class c) {
        Field[] collectedfields = c.getDeclaredFields();
        fields.addAll(Arrays.asList(collectedfields));
        Class superclass = c.getSuperclass();
        if (superclass != null) {
            return collectFields(fields, superclass);
        } else {
            return fields;
        }
    }
    @SuppressWarnings("rawtypes")
	static public List<FlatField> buildFields(Class c) {
        List<Field> fields = collectFields(
                new ArrayList<Field>(), c);
        List<FlatField> flatfields = new ArrayList<FlatField>();
        for (Field f : fields) {
            Flat annotation = f.getAnnotation(Flat.class);
            if (annotation != null) {
                Class type = f.getType();
                if (Map.class.isAssignableFrom(type)) {
                    flatfields.add(new MapField(f, annotation.key()));
                } else {
                    if (Flattable.class.isAssignableFrom(type)) {
                        flatfields.add(new FlattableField(f, annotation.key(),
                                annotation.type(), annotation.aggField()));
                    } else if (Collection.class.isAssignableFrom(type)) {
                        flatfields.add(new CollectionField(f, annotation.key(),
                                annotation.type(), annotation.col(), annotation
                                        .aggField()));
                    } else {
                        flatfields.add(new FlatField(f, annotation.key(),
                                annotation.type(), annotation.stringable()));
                    }
                }
            }
        }
        if (_logger.isInfoEnabled())
            _logger.info("buildFields ( " + flatfields.size() + ") for " + c);
        return flatfields;
    }
    public static Set buildDiff() {
        return new DiffableSet();
    }
    static  Object remove(Map<String, Object> themap, String key, Object value) {
        Object ovalue = themap.get(key);
        if (ovalue != null) {
            String[] values = ovalue.toString().split(Flattable.VALSEP);
            String svalue = value.toString();
            StringBuilder builder=new StringBuilder();
            for (String v : values) {
                if (v.equals(svalue))
                    ovalue=v;
                else 
                    builder.append(v);
            }
        }
        return ovalue;
    }
    static  FlatField getFlatField(List<FlatField> fields, String fieldname) {
        for (FlatField field : fields) {
            if (field._field.getName().equals(fieldname))
                return field;
        }
        throw new IllegalArgumentException("fieldname unknown" + fieldname);
    }
}
