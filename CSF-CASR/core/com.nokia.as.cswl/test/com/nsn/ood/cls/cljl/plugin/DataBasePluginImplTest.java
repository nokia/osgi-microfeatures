/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.cljl.plugin;

import static org.junit.Assert.assertSame;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.replayAll;

import java.sql.Connection;

import org.junit.Test;

import com.nsn.ood.cls.util.osgi.transaction.TransactionService;

/**
 * @author marynows
 * 
 */
public class DataBasePluginImplTest extends DataBasePluginImpl {

	@Test
	public void testGetConnection() throws Exception {
		Connection connection = createMock(Connection.class);
		this.txService = createMock(TransactionService.class);
		expect(txService.getConnection()).andReturn(connection).times(2);
		replayAll();

		final Connection connection1 = this.getConnection();
		final Connection connection2 = this.getConnection();

		assertSame(connection1, connection2);
		assertSame(connection, connection1);
	}

}
