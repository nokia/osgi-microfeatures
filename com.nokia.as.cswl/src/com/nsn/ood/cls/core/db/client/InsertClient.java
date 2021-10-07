/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.client;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;

import com.nsn.ood.cls.core.db.SimpleUpdate;
import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class InsertClient extends SimpleUpdate {
	private static final int CLIENT_ID = 1;
	private static final int TARGET_TYPE = 2;
	private static final int KEEP_ALIVE_TIME = 3;
	private static final int EXPIRES = 4;
	private static final int ETAG = 5;

	private final Client client;
	private final ClientTag clientTag;
	private final Converter<Timestamp, DateTime> timestamp2DateTimeConverter;

	public InsertClient(final Client client, final ClientTag clientTag, final Converter<Timestamp, DateTime> timestamp2DateTimeConverter) {
		super("insert into cls.clients (clientid, targettype, keepalivetime, expires, etag) values (?, ?, ?, ?, ?)");
		this.client = client;
		this.clientTag = clientTag;
		this.timestamp2DateTimeConverter = timestamp2DateTimeConverter;
	}

	@Override
	public void prepare(final PreparedStatement statement) throws SQLException {
		statement.setString(CLIENT_ID, this.client.getClientId());
		statement.setString(TARGET_TYPE, Strings.emptyToNull(this.client.getTargetType()));
		statement.setLong(KEEP_ALIVE_TIME, this.client.getKeepAliveTime());
		statement.setTimestamp(EXPIRES, timestamp2DateTimeConverter.convertFrom(this.clientTag.getExpires()));
		statement.setString(ETAG, this.clientTag.getETag());
	}
}
