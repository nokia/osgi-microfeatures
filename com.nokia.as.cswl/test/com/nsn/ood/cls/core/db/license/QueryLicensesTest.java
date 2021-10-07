/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.license;

import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.getInternalState;

import java.sql.ResultSet;

import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.db.ConditionsQuery;
import com.nsn.ood.cls.core.db.creator.LicenseCreator;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public class QueryLicensesTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testInitialization() throws Exception {
		final QueryLicenses query = new QueryLicenses(CONDITIONS, MAPPER, null);
		assertEquals("select * from cls.stored_licenses", getInternalState(query, String.class, ConditionsQuery.class));
	}

	@Test
	public void testHandle() throws Exception {
		final LicenseCreator licenseCreatorMock = createMock(LicenseCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(licenseCreatorMock.createLicense(resultSetMock)).andReturn(license("123"));

		replayAll();
		final QueryLicenses query = new QueryLicenses(CONDITIONS, MAPPER, licenseCreatorMock);
		assertEquals(license("123"), query.handleRow(resultSetMock));
		verifyAll();
	}
}
