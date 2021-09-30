/**
 * 
 */
package com.alcatel_lucent.ha.services;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

 class MapField extends FlatField {
    public MapField(Field f, String key) {
        super(f, key);
        _key.append(Flattable.MAPSEP);
        _size = _key.length();
    }

    @SuppressWarnings("rawtypes")
	public void readObject(Map<String, Object> map, Flattable object,
            List<Flattable> objects, RecoveryServiceSupport support)
            throws IllegalArgumentException, SecurityException,
            InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException,
            ClassNotFoundException {
        String thekey=_key.insert(0, object.key(0)).toString();
        _key.delete(0,_key.length()-_size);
        @SuppressWarnings("unchecked")
		Map<String, Object> submap = (Map<String, Object>) object
                .readField(_field);
        for (Map.Entry<String, Object> keyentry : map.entrySet()) {
            if (keyentry.getKey().startsWith(thekey)) {
                String[] keyvalues = keyentry.getValue().toString().split(
                        Flattable.VALSEP);
                if (keyvalues.length == 2) {

                    Flattable subobject = findObject(objects, keyvalues[0]);
                    if (subobject == null) {
                        subobject = createObject(support, keyvalues[0],
                                keyvalues[1]);
                        objects.add(subobject);
                    }
                    if (submap == null)
                        submap = new HashMap<String, Object>();
                    submap.put(keyentry.getKey().split(Flattable.MAPSEP)[1],
                            subobject);
                    object.writeField(_field, submap);
                }
            }
        }
    }

    public void read(Flattable flattable, Map<String, Object> map)
            throws IllegalArgumentException, IllegalAccessException {

        String thekey = _key.insert(0, flattable.key(0)).toString();
        _key.delete(0, _key.length() - _size);
        @SuppressWarnings("unchecked")
		Map<String, Object> submap = (Map<String, Object>) flattable
                .readField(_field);
        for (String key : map.keySet()) {
            if (key.startsWith(thekey)) {
                if (_logger.isDebugEnabled())
                    _logger.debug("visit map.key: " + key);
                Object keyvalue = map.get(key);
                // don't read flattable placeholder
                if (keyvalue == null
                        || keyvalue.toString().indexOf(Flattable.VALSEP) <= 0) {
                    submap.put(key.split(Flattable.MAPSEP)[1], keyvalue);
                }
            }
        }
    }

    public void write(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor) throws IllegalArgumentException,
            IllegalAccessException {
        Object value = object.readField(_field);
        if (value != null) {
            int size = _size;
            try {
                _key.insert(0, object.key(0));
                size = _key.length();
                @SuppressWarnings("unchecked")
				Map<String, Object> mapvalue = (Map<String, Object>) value;
                Object keyvalue = null;
                String key = null;
                for (String keymap : mapvalue.keySet()) {
                    keyvalue = mapvalue.get(keymap);
                    if (keyvalue != null) {
                        key = _key.append(keymap).toString();
                        _key.delete(size, _key.length());
                        if (Flattable.class.isAssignableFrom(keyvalue
                                .getClass())) {
                            _key.delete(0, size - _size);
                            Object flattableRef=writeFlattable((Flattable) keyvalue,
                                    map, visitor);
                            if (flattableRef!=null) map.put(key, flattableRef);
                            _key.insert(0, object.key(0));
                        } else {
                            map.put(key, keyvalue);
                        }
                    }
                }
            } finally {
                _key.delete(0, size - _size);
            }
        }
    }

    public void writeDiff(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor, String mapkey)
            throws IllegalArgumentException, IllegalAccessException {
        Object value = object.readField(_field);
        if (value != null) {
            _key.insert(0, object.key(0));
            int size = _key.length();
            @SuppressWarnings("unchecked")
			Map<String, Object> mapvalue = (Map<String, Object>) value;
            Object keyvalue = mapvalue.get(mapkey);
            if (keyvalue != null) {
                String key = _key.append(mapkey).toString();
                _key.delete(size, _key.length());
                _key.delete(0, size - _size);
                if (Flattable.class.isAssignableFrom(keyvalue.getClass())) {
                    Object flattableRef=writeFlattable((Flattable) keyvalue, map,
                            visitor);
                    if (flattableRef!=null) map.put(key,flattableRef );

                } else {
                    map.put(key, keyvalue);
                }

            }
        } else {
            throw new IllegalStateException("diff detected on map field null "
                    + toString());
        }
    }

    public void remove(Flattable object, Map<String, Object> map,
            FlattableVisitor visitor, String mapkey)
            throws IllegalArgumentException, IllegalAccessException {
        Object value = object.readField(_field);
        if (value != null) {
            _key.insert(0, object.key(0));
            int size = _key.length();
            try {
                // possible issues when removing a map that refers a Flattable :
                // how to guess that
                // solution 1: the setAttributes do something special
                // solution 2 : when removing all the ha context references are
                // checked better "invariant" like condition
                // solution 3: check the map removed type ... is it possible
                // from DS ?
                map.remove(_key.append(mapkey).toString());
                _key.delete(size, _key.length());
            } finally {
                _key.delete(0, size - _size);
            }
        }
    }
}
