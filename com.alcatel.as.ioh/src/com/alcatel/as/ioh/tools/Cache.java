// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.ioh.tools;

import java.util.*;
import java.util.concurrent.atomic.*;


public class Cache<K, V> {
    
    Map<K, V> _entriesL1 = new HashMap<K, V> ();
    Map<K, V> _entriesL2 = new HashMap<K, V> ();

    public Cache (){
    }

    public int size (){
	return _entriesL1.size ()+_entriesL2.size ();
    }

    public void clear (){
	_entriesL1.clear ();
	_entriesL2.clear ();
    }
    
    public void put (K key, V value){
	_entriesL1.put (key, value);
    }

    public V get (K key){
	V v = _entriesL1.get (key);
	return v != null ? v : _entriesL2.get (key);
    }

    public V remove (K key){
	V v = _entriesL1.remove (key);
	if (v == null) v = _entriesL2.remove (key);
	return v;
    }
    
    public int tick (){
	int i = _entriesL2.size ();
	_entriesL2.clear ();
	Map<K, V> tmp = _entriesL1;
	_entriesL1 = _entriesL2;
	_entriesL2 = tmp;
	return i;
    }
}