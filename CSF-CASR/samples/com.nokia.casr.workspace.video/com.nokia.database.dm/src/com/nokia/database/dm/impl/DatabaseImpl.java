package com.nokia.database.dm.impl;

import org.apache.felix.dm.annotation.api.Component;

import com.nokia.database.dm.IDatabase;

@Component
public class DatabaseImpl implements IDatabase {

	@Override
	public String get() {
		return "get";
	}
}
