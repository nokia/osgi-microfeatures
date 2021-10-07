/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author marynows
 * 
 */
public final class MapBuilder<K, V> {
	private final Map<K, V> map;

	public static <K, V> MapBuilder<K, V> linkedMap(final K key, final V value) {
		return new MapBuilder<K, V>(new LinkedHashMap<K, V>()).put(key, value);
	}

	public static <K, V> MapBuilder<K, V> hashMap(final K key, final V value) {
		return new MapBuilder<K, V>(new HashMap<K, V>()).put(key, value);
	}

	private MapBuilder(final Map<K, V> map) {
		this.map = map;
	}

	public Map<K, V> build() {
		return this.map;
	}

	public MapBuilder<K, V> put(final K key, final V value) {
		this.map.put(key, value);
		return this;
	}
}
