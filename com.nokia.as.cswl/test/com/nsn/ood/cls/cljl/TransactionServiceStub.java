// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.cljl;

import java.sql.Connection;

import org.osgi.service.transaction.control.TransactionControl;
import static org.powermock.api.easymock.PowerMock.createMock;

import com.nsn.ood.cls.util.osgi.transaction.TransactionService;

public class TransactionServiceStub extends TransactionService {
	
	private Connection conn = createMock(Connection.class);
	private TransactionControl txControl = createMock(TransactionControl.class);
	
	public Connection getConnection() {
		return conn;
	}
	
	public TransactionControl txControl() {
		return txControl;
	}

}
