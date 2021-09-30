/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.assertLicensedFeature;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.sql.ResultSet;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.model.internal.LicensedFeature;


/**
 * @author marynows
 * 
 */
public class LicensedFeatureCreatorTest {
	private LicensedFeatureCreator creator;

	@Before
	public void setUp() throws Exception {
		this.creator = new LicensedFeatureCreator();
	}

	@Test
	public void testCreateLicensedFeature() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getLong("featurecode")).andReturn(1234L);
		expect(resultSetMock.getString("featurename")).andReturn("name");
		expect(resultSetMock.getString("capacityunit")).andReturn("unit");
		expect(resultSetMock.getString("targettype")).andReturn("type");
		expect(resultSetMock.getLong("total")).andReturn(777L);
		expect(resultSetMock.getLong("used")).andReturn(444L);
		expect(resultSetMock.getLong("remaining")).andReturn(333L);

		replayAll();
		final LicensedFeature licensedFeature = this.creator.createLicensedFeature(resultSetMock);
		verifyAll();

		assertNotNull(licensedFeature);
		assertLicensedFeature(licensedFeature, 1234L, "name", "unit", "type", 777L, 444L, 333L);
	}
}
