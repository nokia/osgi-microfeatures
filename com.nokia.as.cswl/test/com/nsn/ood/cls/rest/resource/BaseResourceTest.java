/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import static com.nsn.ood.cls.model.test.LinkTestUtil.link;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import javax.ws.rs.core.Application;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.rest.query.Conditions2QueryStringConverter;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class BaseResourceTest {

	@Test
	public void testLink() throws Exception {
		{
			final BaseResource baseResource = new BaseResource();
			baseResource.init(null, "a", "b");
			assertEquals(link("/a/b"), baseResource.link());
			assertEquals(link("/a/b/c"), baseResource.link("c"));
			assertEquals(link("/a/b/c/d"), baseResource.link("c", "d"));
		}
		{
			final BaseResource baseResource = new BaseResource();
			baseResource.init();
			assertEquals(link(""), baseResource.link());
			assertEquals(link("/a"), baseResource.link("a"));
			assertEquals(link("/a/b"), baseResource.link("a", "b"));
		}
	}

	@Test
	public void testLinks() throws Exception {
		testLinks(ConditionsBuilder.create().build(), 0, false, false);
		testLinks(ConditionsBuilder.create().offset(0).limit(3).build(), 10, true, false);
		testLinks(ConditionsBuilder.create().offset(1).limit(3).build(), 10, true, true);
		testLinks(ConditionsBuilder.create().offset(3).limit(3).build(), 10, true, true);
		testLinks(ConditionsBuilder.create().offset(6).limit(3).build(), 10, true, true);
		testLinks(ConditionsBuilder.create().offset(7).limit(3).build(), 10, false, true);
		testLinks(ConditionsBuilder.create().offset(9).limit(3).build(), 10, false, true);
	}

	private void testLinks(final Conditions conditions, final int total, final boolean next, final boolean prev)
			throws Exception {
		final Converter<Conditions, String> converterMock = createMock(Conditions2QueryStringConverter.class);

		expect(converterMock.convertTo(conditions)).andReturn("?querySelf");
		if (next) {
			expect(converterMock.convertTo(conditions)).andReturn("?queryNext");
		}
		if (prev) {
			expect(converterMock.convertTo(conditions)).andReturn("?queryPrev");
		}

		replayAll();
		final BaseResource baseResource = new BaseResource();
		baseResource.init(null, "a", "b");
		setInternalState(baseResource, "conditions2QueryStringConverter", converterMock);
		assertLinks(baseResource.links(conditions, total),//
				link("/a/b?querySelf"),//
				next ? link("/a/b?queryNext") : null,//
				prev ? link("/a/b?queryPrev") : null);
		verifyAll();
	}

	private void assertLinks(final Links links, final Link selfLink, final Link nextLink, final Link prevLink) {
		assertEquals(selfLink, links.getSelfLink());
		assertEquals(nextLink, links.getNextLink());
		assertEquals(prevLink, links.getPrevLink());
	}

	@Test
	public void testApplication() throws Exception {
		{
			final BaseResource baseResource = new BaseResource();
			setInternalState(baseResource, new Application());
			assertNull(baseResource.application());
		}
		{
			final BaseResource baseResource = new BaseResource();
			final CLSApplication clsApplication = new CLSApplication();
			setInternalState(baseResource, clsApplication);
			assertSame(clsApplication, baseResource.application());
		}
	}
}
