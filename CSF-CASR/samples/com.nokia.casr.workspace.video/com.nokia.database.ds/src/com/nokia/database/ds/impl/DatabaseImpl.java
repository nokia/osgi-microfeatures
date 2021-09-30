package com.nokia.database.ds.impl;

import org.osgi.service.component.annotations.*;

import com.nokia.database.ds.IDatabase;

@Component
public class DatabaseImpl implements IDatabase {

	@Override
	public String get() {
		return "get";
	}
}
