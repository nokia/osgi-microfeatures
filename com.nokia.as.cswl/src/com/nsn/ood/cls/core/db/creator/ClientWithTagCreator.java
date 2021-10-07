/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.joda.time.DateTime;

import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@Component(provides = ClientWithTagCreator.class)
public class ClientWithTagCreator {
	@ServiceDependency(filter = "(&(from=timestamp)(to=dateTime))")
	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;

	public ClientWithTag createClient(final ResultSet resultSet) throws SQLException {
		return new ClientWithTag()//
				.withObject(new Client()//
						.withClientId(resultSet.getString("clientid"))//
						.withTargetType(resultSet.getString("targettype"))//
						.withKeepAliveTime(resultSet.getLong("keepalivetime")))//
				.withClientTag(new ClientTag()//
						.withETag(resultSet.getString("etag"))//
						.withExpires(timestamp2DateTimeConverter.convertTo(resultSet.getTimestamp("expires"))));
	}
}
