/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import static com.nsn.ood.cls.model.internal.test.LicensedFeatureTestUtil.licensedFeature;
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
import com.nsn.ood.cls.core.db.creator.LicensedFeatureCreator;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public class QueryLicensedFeaturesTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testInitialization() throws Exception {
		final QueryLicensedFeatures query = new QueryLicensedFeatures(CONDITIONS, MAPPER, null);
		assertEquals("select * from cls.features", getInternalState(query, String.class, ConditionsQuery.class));
	}

	@Test
	public void testHandleRow() throws Exception {
		final LicensedFeatureCreator licensedFeatureCreatorMock = createMock(LicensedFeatureCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(licensedFeatureCreatorMock.createLicensedFeature(resultSetMock)).andReturn(licensedFeature(12L));

		replayAll();
		final QueryLicensedFeatures query = new QueryLicensedFeatures(CONDITIONS, MAPPER, licensedFeatureCreatorMock);
		assertEquals(licensedFeature(12L), query.handleRow(resultSetMock));
		verifyAll();
	}
}
