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
public interface StatementExecutor<S extends Statement<S>> {

	public void executeAndHandle(PreparedStatement preparedStatement, S statement) throws SQLException;
	public void execute(final S statement) throws SQLException;
	
}
