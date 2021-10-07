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

import com.nsn.ood.cls.core.convert.LicenseMode2IntegerConverter;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class LicenseModeValueConverterTest {

	@Test
	public void testMode() throws Exception {
		final Converter<License.Mode, Integer> converterMock = createMock(LicenseMode2IntegerConverter.class);

		expect(converterMock.convertTo(License.Mode.CAPACITY)).andReturn(1);
		expect(converterMock.convertFrom(1)).andReturn(License.Mode.CAPACITY);

		replayAll();
		final LicenseModeValueConverter converter = new LicenseModeValueConverter();
		setInternalState(converter, converterMock);
		assertEquals(Integer.valueOf(1), converter.prepareConvert("capacity"));
		assertEquals("capacity", converter.handleConvert(1));
		verifyAll();
	}
}
