package com.nsn.ood.cls.core.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.nsn.ood.cls.core.util.ApiVersionChooser.API_VERSION;


public class ApiVersionChooserTest {

	private ApiVersionChooser bean;

	@Before
	public void setUp() throws Exception {
		this.bean = new ApiVersionChooser();
	}

	@Test
	public void testDefaultValue() throws Exception {
		assertEquals(API_VERSION.VERSION_1_1, this.bean.getCurrentVersion());
	}

	@Test
	public void testGetCurrentVersion() throws Exception {
		Whitebox.setInternalState(this.bean, API_VERSION.VERSION_1_0);
		assertEquals(API_VERSION.VERSION_1_0, this.bean.getCurrentVersion());
	}

	@Test
	public void testSetCurrentVersion() throws Exception {
		this.bean.setCurrentVersion(API_VERSION.VERSION_1_0);
		assertEquals(API_VERSION.VERSION_1_0, Whitebox.getInternalState(this.bean, API_VERSION.class));
	}

}
