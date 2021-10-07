/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import org.junit.Test;

import com.nsn.ood.cls.core.convert.LicenseType2IntegerConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class LicenseTypeValueConverterTest {

	@Test
	public void testType() throws Exception {
		final Converter<License.Type, Integer> converterMock = createMock(LicenseType2IntegerConverter.class);

		expect(converterMock.convertTo(License.Type.POOL)).andReturn(1);
		expect(converterMock.convertFrom(1)).andReturn(License.Type.POOL);

		replayAll();
		final LicenseTypeValueConverter converter = new LicenseTypeValueConverter();
		setInternalState(converter, converterMock);
		assertEquals(Integer.valueOf(1), converter.prepareConvert("pool"));
		assertEquals("pool", converter.handleConvert(1));
		verifyAll();
	}
}
