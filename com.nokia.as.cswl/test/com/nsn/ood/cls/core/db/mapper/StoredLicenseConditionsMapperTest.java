/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumn;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class StoredLicenseConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final LicenseModeValueConverter modeConverter = new LicenseModeValueConverter();
		final LicenseTypeValueConverter typeConverter = new LicenseTypeValueConverter();

		final StoredLicenseConditionsMapper mapper = new StoredLicenseConditionsMapper();
		setInternalState(mapper, "modeConverter", modeConverter);
		setInternalState(mapper, "typeConverter", typeConverter);
		mapper.start();

		assertMapperColumn(mapper, "customerName", "customername");
		assertMapperColumn(mapper, "customerId", "customerid");
		assertMapperColumn(mapper, "orderId", "orderid");
		assertMapperColumn(mapper, "user", "\"user\"");
		assertMapperColumn(mapper, "importDate", "importdate");
		assertMapperColumn(mapper, "remainingCapacity", "remaining");
	}
}
