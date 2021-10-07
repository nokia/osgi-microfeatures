/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public interface Statement<T extends Statement<T>> {

	String sql();

	void prepare(PreparedStatement statement) throws SQLException;

	T next();
}
