/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;


/**
 * @author marynows
 * 
 */
public class UriInfo2ConditionsConverterTest {
	private static final Conditions EXPECTED_CONDITIONS = ConditionsBuilder.create().build();

	private FilterParser filterParserMock;
	private SortingParser sortingParserMock;
	private PaginationParser paginationParserMock;

	private UriInfo2ConditionsConverter converter;

	@Before
	public void setUp() throws Exception {
		this.filterParserMock = createMock(FilterParser.class);
		this.sortingParserMock = createMock(SortingParser.class);
		this.paginationParserMock = createMock(PaginationParser.class);

		this.converter = new UriInfo2ConditionsConverter();
		Whitebox.setInternalState(converter, filterParserMock, sortingParserMock, paginationParserMock);
	}

	@Test
	public void testEmptyQuery() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);

		expect(uriInfoMock.getQueryParameters()).andReturn(new MultivaluedHashMap<String, String>());

		replayAll();
		assertEquals(EXPECTED_CONDITIONS, this.converter.convertTo(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testFilterQuery() throws Exception {
		final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
		queryParameters.add("field1", "value1");
		queryParameters.add(" field2", " value2");
		queryParameters.add("field1", "value11");
		queryParameters.add("field3 ", "");
		queryParameters.add("field4", null);
		queryParameters.add(null, "value");

		final UriInfo uriInfoMock = createMock(UriInfo.class);

		expect(uriInfoMock.getQueryParameters()).andReturn(queryParameters);
		this.filterParserMock.parse(anyObject(ConditionsBuilder.class), eq("field1"), eq("value1"));
		this.filterParserMock.parse(anyObject(ConditionsBuilder.class), eq("field2"), eq(" value2"));
		this.filterParserMock.parse(anyObject(ConditionsBuilder.class), eq("field3"), eq(""));

		replayAll();
		assertEquals(EXPECTED_CONDITIONS, this.converter.convertTo(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testSortQuery() throws Exception {
		final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
		queryParameters.add("sort", "value1");
		queryParameters.add("sort", "value2");

		final UriInfo uriInfoMock = createMock(UriInfo.class);

		expect(uriInfoMock.getQueryParameters()).andReturn(queryParameters);
		this.sortingParserMock.parse(anyObject(ConditionsBuilder.class), eq("value1"));

		replayAll();
		assertEquals(EXPECTED_CONDITIONS, this.converter.convertTo(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testOffsetQuery() throws Exception {
		final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
		queryParameters.add("offset", "value");

		final UriInfo uriInfoMock = createMock(UriInfo.class);

		expect(uriInfoMock.getQueryParameters()).andReturn(queryParameters);
		this.paginationParserMock.parseOffset(anyObject(ConditionsBuilder.class), eq("value"));

		replayAll();
		assertEquals(EXPECTED_CONDITIONS, this.converter.convertTo(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testLimitQuery() throws Exception {
		final MultivaluedMap<String, String> queryParameters = new MultivaluedHashMap<>();
		queryParameters.add("limit", "value");

		final UriInfo uriInfoMock = createMock(UriInfo.class);

		expect(uriInfoMock.getQueryParameters()).andReturn(queryParameters);
		this.paginationParserMock.parseLimit(anyObject(ConditionsBuilder.class), eq("value"));

		replayAll();
		assertEquals(EXPECTED_CONDITIONS, this.converter.convertTo(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testConvertFrom() throws Exception {
		try {
			this.converter.convertFrom(null);
			fail();
		} catch (final CLSRuntimeException e) {
			assertFalse(e.getMessage().isEmpty());
		}
	}
}
