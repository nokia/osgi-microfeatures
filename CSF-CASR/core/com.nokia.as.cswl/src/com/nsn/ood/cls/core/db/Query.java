/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public interface Query extends Statement<Query> {

	void handle(ResultSet resultSet) throws SQLException;
}
