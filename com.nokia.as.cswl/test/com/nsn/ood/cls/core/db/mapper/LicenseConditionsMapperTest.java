/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import static org.powermock.reflect.Whitebox.setInternalState;

import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumn;
import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumnConverter;

import org.junit.Test;

import com.nsn.ood.cls.core.db.util.ConditionsMapper.RangePolicy;


/**
 * @author marynows
 * 
 */
public class LicenseConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final LicenseModeValueConverter modeConverter = new LicenseModeValueConverter();
		final LicenseTypeValueConverter typeConverter = new LicenseTypeValueConverter();

		final LicenseConditionsMapper mapper = new LicenseConditionsMapper();
		setInternalState(mapper, "modeConverter", modeConverter);
		setInternalState(mapper, "typeConverter", typeConverter);
		mapper.start();

		assertMapperColumn(mapper, "serialNumber", "serialnumber");
		assertMapperColumn(mapper, "fileName", "filename");
		assertMapperColumn(mapper, "code", "code");
		assertMapperColumn(mapper, "name", "name");
		assertMapperColumn(mapper, "mode", "mode");
		assertMapperColumn(mapper, "totalCapacity", "total");
		assertMapperColumn(mapper, "usedCapacity", "used");
		assertMapperColumn(mapper, "capacityUnit", "capacityunit");
		assertMapperColumn(mapper, "type", "type");
		assertMapperColumn(mapper, "startDate", "startdate");
		assertMapperColumn(mapper, "endDate", "enddate", RangePolicy.NULLS_LAST);
		assertMapperColumn(mapper, "targetType", "targettype");
		assertMapperColumn(mapper, "targetId", "targetid");
		assertMapperColumn(mapper, "featureCode", "featurecode");
		assertMapperColumn(mapper, "featureName", "featurename");

		assertMapperColumnConverter(mapper, "mode", modeConverter);
		assertMapperColumnConverter(mapper, "type", typeConverter);
	}
}
