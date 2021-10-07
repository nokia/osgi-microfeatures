/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumn;
import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumnConverter;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class ReservationConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final LicenseModeValueConverter modeConverter = new LicenseModeValueConverter();
		final LicenseTypeValueConverter typeConverter = new LicenseTypeValueConverter();

		final ReservationConditionsMapper mapper = new ReservationConditionsMapper();
		setInternalState(mapper, "modeConverter", modeConverter);
		setInternalState(mapper, "typeConverter", typeConverter);
		mapper.start();

		assertMapperColumn(mapper, "featureCode", "featurecode");
		assertMapperColumn(mapper, "serialNumber", "serialnumber");
		assertMapperColumn(mapper, "clientId", "clientid");
		assertMapperColumn(mapper, "capacity", "capacity");
		assertMapperColumn(mapper, "reservationTime", "reservationtime");
		assertMapperColumn(mapper, "mode", "mode");
		assertMapperColumn(mapper, "type", "type");
		assertMapperColumn(mapper, "endDate", "enddate");
		assertMapperColumn(mapper, "fileName", "filename");

		assertMapperColumnConverter(mapper, "mode", modeConverter);
		assertMapperColumnConverter(mapper, "type", typeConverter);
	}
}
