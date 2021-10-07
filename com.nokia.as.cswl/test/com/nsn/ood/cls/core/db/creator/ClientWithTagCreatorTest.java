/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.assertClientTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.assertClient;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.ResultSet;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class ClientWithTagCreatorTest {
	private static final DateTime EXPIRES = new DateTime(2015, 5, 5, 11, 7);
	private static final Timestamp EXPIRES_TS = new Timestamp(EXPIRES.getMillis());

	private Converter<Timestamp, DateTime> converterMock;
	private ClientWithTagCreator creator;

	@Before
	public void setUp() throws Exception {
		this.converterMock = createMock(Timestamp2DateTimeConverter.class);
		this.creator = new ClientWithTagCreator();
		setInternalState(creator, converterMock);
	}

	@Test
	public void testCreateClient() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("clientid")).andReturn("12345");
		expect(resultSetMock.getString("targettype")).andReturn("TYPE");
		expect(resultSetMock.getLong("keepalivetime")).andReturn(3456L);
		expect(resultSetMock.getString("etag")).andReturn("eTag");
		expect(resultSetMock.getTimestamp("expires")).andReturn(EXPIRES_TS);
		expect(this.converterMock.convertTo(EXPIRES_TS)).andReturn(EXPIRES);

		replayAll();
		final ClientWithTag clientWithTag = this.creator.createClient(resultSetMock);
		verifyAll();

		assertNotNull(clientWithTag);
		assertClient(clientWithTag.getObject(), "12345", 3456L, "TYPE");
		assertClientTag(clientWithTag.getClientTag(), "eTag", EXPIRES);
	}
}
