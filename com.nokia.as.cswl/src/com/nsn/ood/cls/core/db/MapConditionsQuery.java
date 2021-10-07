/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public abstract class MapConditionsQuery<K, V> extends ConditionsQuery<V> {
	private final Map<K, V> map = new LinkedHashMap<>();

	protected abstract void handleRow(ResultSet resultSet) throws SQLException;

	protected MapConditionsQuery(final String sql, final Conditions conditions, final ConditionsMapper mapper,
			final String idFilterName) throws ConditionProcessingException {
		super(sql, conditions, mapper, idFilterName);
	}

	@Override
	public void handle(final ResultSet resultSet) throws SQLException {
		while (resultSet.next()) {
			handleRow(resultSet);
		}
	}

	protected boolean contains(final K key) {
		return this.map.containsKey(key);
	}

	protected V get(final K key) {
		return this.map.get(key);
	}

	protected void put(final K key, final V value) {
		this.map.put(key, value);
	}

	@Override
	public List<V> getList() {
		return new ArrayList<>(this.map.values());
	}
}
