/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.client;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.db.SimpleQuery;
import com.nsn.ood.cls.core.db.creator.ClientWithTagCreator;
import com.nsn.ood.cls.core.model.ClientWithTag;


/**
 * @author marynows
 * 
 */
public class QueryClient extends SimpleQuery<ClientWithTag> {
	private static final int CLIENT_ID = 1;

	private final String clientId;
	private final ClientWithTagCreator clientWithTagCreator;

	public QueryClient(final String clientId, final ClientWithTagCreator clientWithTagCreator) {
		super("select * from cls.clients where clientid = ?", null);
		this.clientId = clientId;
		this.clientWithTagCreator = clientWithTagCreator;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setString(CLIENT_ID, this.clientId);
	}

	@Override
	protected ClientWithTag handleValue(final ResultSet resultSet) throws SQLException {
		return this.clientWithTagCreator.createClient(resultSet);
	}
}
