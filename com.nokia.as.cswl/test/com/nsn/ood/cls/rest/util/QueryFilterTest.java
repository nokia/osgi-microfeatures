// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.rest.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


public class QueryFilterTest {

	private QueryFilter bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new QueryFilter();
	}

	@Test
	public void testFilter() throws Exception {
		assertEquals("dadad,dasdad", this.bean.filterStarCharacter("dadad❄dasdad"));
		assertEquals(",q21w,DSQQ,ad", this.bean.filterStarCharacter("❄q21w❄DSQQ❄ad"));

	}

	@Test
	public void testNoFilter() throws Exception {
		assertEquals("dadad,dasdad", this.bean.filterStarCharacter("dadad,dasdad"));
		assertEquals("dasdadsa,a", this.bean.filterStarCharacter("dasdadsa,a"));

	}

}
