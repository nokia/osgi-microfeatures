/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumn;
import static com.nsn.ood.cls.core.test.ConditionsMapperTestUtil.assertMapperColumnValue;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Test;

import com.nsn.ood.cls.core.convert.FeatureType2IntegerConverter;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class FeatureConditionsMapperTest {

	@Test
	public void testMapper() throws Exception {
		final FeatureConditionsMapper mapper = new FeatureConditionsMapper();
		mapper.start();

		assertMapperColumn(mapper, "clientId", "clientid");
		assertMapperColumn(mapper, "featureCode", "featurecode");
		assertMapperColumn(mapper, "type", "mode");
	}

	@Test
	public void testType() throws Exception {
		final Converter<Feature.Type, Integer> converterMock = createMock(FeatureType2IntegerConverter.class);

		expect(converterMock.convertTo(Feature.Type.CAPACITY)).andReturn(1);
		expect(converterMock.convertFrom(1)).andReturn(Feature.Type.CAPACITY);

		replayAll();
		final FeatureConditionsMapper mapper = new FeatureConditionsMapper();
		setInternalState(mapper, converterMock);
		mapper.start();
		assertMapperColumnValue(mapper, "type", "capacity", 1);
		verifyAll();
	}
}
