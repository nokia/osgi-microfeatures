/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.metadata;

import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonProperty;
import static com.nsn.ood.cls.model.test.JsonAnnotationTestUtil.assertJsonPropertyOrder;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class MetaDataListTest {
	private static final MetaDataList<String> META_DATA_LIST = new MetaDataList<>(Arrays.asList("test1", "test2"),
			metaData(50L, 10L));

	@Test
	public void testEmpty() throws Exception {
		final MetaDataList<String> mdl = new MetaDataList<>();
		assertTrue(mdl.getList().isEmpty());
		assertEquals(metaData(0L, 0L), mdl.getMetaData());
	}

	@Test
	public void testCreate() throws Exception {
		assertEquals(Arrays.asList("test1", "test2"), META_DATA_LIST.getList());
		assertEquals(metaData(50L, 10L), META_DATA_LIST.getMetaData());
	}

	@Test
	public void testAnnotations() throws Exception {
		assertJsonPropertyOrder(MetaDataList.class, "list", "metaData");
		assertJsonProperty(MetaDataList.class, "list", "list");
		assertJsonProperty(MetaDataList.class, "metaData", "metaData");
	}

	@Test
	public void testToString() throws Exception {
		assertFalse(META_DATA_LIST.toString().isEmpty());
	}

	@Test
	public void testEquals() throws Exception {
		assertFalse(META_DATA_LIST.equals(null));
		assertFalse(META_DATA_LIST.equals("test"));
		assertEquals(META_DATA_LIST, META_DATA_LIST);

		assertFalse(META_DATA_LIST.equals(new MetaDataList<>()));
		assertNotEquals(META_DATA_LIST.hashCode(), new MetaDataList<>().hashCode());

		final MetaDataList<String> mdl = new MetaDataList<>(Arrays.asList("test1", "test2"), metaData(50L, 10L));
		assertEquals(META_DATA_LIST, mdl);
		assertEquals(META_DATA_LIST.hashCode(), mdl.hashCode());
	}
}
