/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.model.test;

import com.nsn.ood.cls.model.gen.metadata.MetaData;


/**
 * @author marynows
 * 
 */
public class MetaDataTestUtil {

	public static MetaData metaData() {
		return new MetaData();
	}

	public static MetaData metaData(final Long total, final Long filtered) {
		return metaData().withTotal(total).withFiltered(filtered);
	}
}
