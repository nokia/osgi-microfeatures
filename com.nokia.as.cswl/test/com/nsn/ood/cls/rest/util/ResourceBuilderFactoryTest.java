/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.errorsList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.link;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;


/**
 * @author marynows
 * 
 */
public class ResourceBuilderFactoryTest {
	private ResourceBuilderFactory factory;
	private ResourceBuilder resourceBuilderMock;

	@Before
	public void setUp() throws Exception {
		this.resourceBuilderMock = createMock(ResourceBuilder.class);

		this.factory = new ResourceBuilderFactory() {
			@Override
			protected ResourceBuilder createResourceBuilder() {
				super.createResourceBuilder();
				return ResourceBuilderFactoryTest.this.resourceBuilderMock;
			}
		};
	}

	@Test
	public void testCreate() throws Exception {
		replayAll();
		assertEquals(this.resourceBuilderMock, this.factory.create());
		verifyAll();
	}

	@Test
	public void testSelfLink() throws Exception {
		expect(this.resourceBuilderMock.selfLink(link("href"))).andReturn(this.resourceBuilderMock);

		replayAll();
		assertEquals(this.resourceBuilderMock, this.factory.selfLink(link("href")));
		verifyAll();
	}

	@Test
	public void testMetaData() throws Exception {
		expect(this.resourceBuilderMock.selfLink(link("self"))).andReturn(this.resourceBuilderMock);
		expect(this.resourceBuilderMock.nextLink(link("next"))).andReturn(this.resourceBuilderMock);
		expect(this.resourceBuilderMock.prevLink(link("prev"))).andReturn(this.resourceBuilderMock);
		expect(this.resourceBuilderMock.metaData(metaData())).andReturn(this.resourceBuilderMock);

		replayAll();
		assertEquals(this.resourceBuilderMock,
				this.factory.metaData(new Links(link("self"), link("next"), link("prev")), metaData()));
		verifyAll();
	}

	@Test
	public void testErrors() throws Exception {
		expect(this.resourceBuilderMock.errors(error(1L))).andReturn(this.resourceBuilderMock);

		replayAll();
		assertEquals(this.resourceBuilderMock, this.factory.errors(error(1L)));
		verifyAll();
	}

	@Test
	public void testErrorsList() throws Exception {
		expect(this.resourceBuilderMock.errors(errorsList(error(1L)))).andReturn(this.resourceBuilderMock);

		replayAll();
		assertEquals(this.resourceBuilderMock, this.factory.errors(errorsList(error(1L))));
		verifyAll();
	}
}
