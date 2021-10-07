/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import java.sql.SQLException;


/**
 * @author marynows
 * 
 */
public interface Update extends Statement<Update> {

	void handle(int affectedRows) throws SQLException;
}
