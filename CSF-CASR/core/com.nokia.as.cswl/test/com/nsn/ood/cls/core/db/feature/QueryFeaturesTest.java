/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.feature;

import static com.nsn.ood.cls.model.test.FeatureTestUtil.feature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featuresList;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.getInternalState;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.db.ConditionsQuery;
import com.nsn.ood.cls.core.db.creator.FeatureCreator;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.gen.features.Feature;


/**
 * @author marynows
 * 
 */
public class QueryFeaturesTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testInitialization() throws Exception {
		final QueryFeatures query = new QueryFeatures(CONDITIONS, MAPPER, null);
		assertEquals("select * from cls.reservations", getInternalState(query, String.class, ConditionsQuery.class));
	}

	@Test
	public void testHandleRow() throws Exception {
		final Feature f1_a1 = feature(1234L);
		final Feature f1_a2 = feature(1234L);
		final Feature f2_a1 = feature(2345L);
		final Feature f2_a2 = feature(2345L);
		final Feature f2_a3 = feature(2345L);

		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1)),// feature 1 -> allocation 1
				featuresList(f1_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f1_a2, f1_a1)),// feature 1 -> allocation 2
				featuresList(f1_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f2_a1, f2_a1)),// feature 2 -> allocation 1
				featuresList(f1_a1, f2_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f1_a2, f1_a1),// feature 1 -> allocation 2
				Pair.of(f2_a1, f2_a1)),// feature 2 -> allocation 1
				featuresList(f1_a1, f2_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f2_a1, f2_a1),// feature 2 -> allocation 1
				Pair.of(f2_a2, f2_a1)),// feature 2 -> allocation 2
				featuresList(f1_a1, f2_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f1_a2, f1_a1),// feature 1 -> allocation 2
				Pair.of(f2_a1, f2_a1),// feature 2 -> allocation 1
				Pair.of(f2_a2, f2_a1)),// feature 2 -> allocation 2
				featuresList(f1_a1, f2_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f2_a1, f2_a1),// feature 2 -> allocation 1
				Pair.of(f2_a2, f2_a1),// feature 2 -> allocation 2
				Pair.of(f1_a2, f1_a1)),// feature 1 -> allocation 2
				featuresList(f1_a1, f2_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f2_a1, f2_a1),// feature 2 -> allocation 1
				Pair.of(f1_a2, f1_a1),// feature 1 -> allocation 2
				Pair.of(f2_a2, f2_a1)),// feature 2 -> allocation 2
				featuresList(f1_a1, f2_a1));
		testHandleRow(Arrays.asList(//
				Pair.of(f2_a1, f2_a1),// feature 2 -> allocation 1
				Pair.of(f2_a2, f2_a1),// feature 2 -> allocation 2
				Pair.of(f1_a1, f1_a1),// feature 1 -> allocation 1
				Pair.of(f2_a3, f2_a1)),// feature 2 -> allocation 3
				featuresList(f2_a1, f1_a1));
	}

	public void testHandleRow(final List<Pair<Feature, Feature>> featureRelations, final List<Feature> expectedFeatures)
			throws Exception {
		final FeatureCreator featureCreatorMock = createMock(FeatureCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		for (final Pair<Feature, Feature> pair : featureRelations) {
			expect(featureCreatorMock.createFeature(resultSetMock)).andReturn(pair.getLeft());
			featureCreatorMock.addAllocation(same(pair.getRight()), eq(resultSetMock));
		}

		replayAll();
		final QueryFeatures query = new QueryFeatures(CONDITIONS, MAPPER, featureCreatorMock);
		for (int i = 0; i < featureRelations.size(); i++) {
			query.handleRow(resultSetMock);
		}
		assertEquals(expectedFeatures, query.getList());
		verifyAll();
	}
}
