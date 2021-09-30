/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.creator;

import static com.nsn.ood.cls.model.test.FeatureTestUtil.allocation;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.feature;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureOnOff;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.convert.AllocationUsage2IntegerConverter;
import com.nsn.ood.cls.core.convert.FeatureType2IntegerConverter;
import com.nsn.ood.cls.core.convert.Timestamp2DateTimeConverter;
import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Allocation.Usage;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.features.Feature.Type;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class FeatureCreatorTest {
	private static final DateTime END_DATE = new DateTime(2015, 3, 26, 14, 36);

	private Converter<Timestamp, DateTime> timestamp2DateTimeConverter;
	private Converter<Feature.Type, Integer> featureType2IntegerConverter;
	private Converter<Allocation.Usage, Integer> allocationUsage2IntegerConverter;
	private FeatureCreator creator;

	@Before
	public void setUp() throws Exception {
		this.timestamp2DateTimeConverter = createMock(Timestamp2DateTimeConverter.class);
		this.featureType2IntegerConverter = createMock(FeatureType2IntegerConverter.class);
		this.allocationUsage2IntegerConverter = createMock(AllocationUsage2IntegerConverter.class);
		this.creator = new FeatureCreator();
		setInternalState(creator, "timestamp2DateTimeConverter", timestamp2DateTimeConverter);
		setInternalState(creator, "featureType2IntegerConverter", featureType2IntegerConverter);
		setInternalState(creator, "allocationUsage2IntegerConverter", allocationUsage2IntegerConverter);
	}

	@Test
	public void testCreateFeature() throws Exception {
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getLong("featurecode")).andReturn(2345L);
		expect(resultSetMock.getInt("mode")).andReturn(2);
		expect(this.featureType2IntegerConverter.convertFrom(2)).andReturn(Type.CAPACITY);

		replayAll();
		final Feature feature = this.creator.createFeature(resultSetMock);
		verifyAll();

		assertNotNull(feature);
		assertEquals(feature(2345L).withType(Type.CAPACITY), feature);
	}

	@Test
	public void testAddAllocationToCapacityFeature() throws Exception {
		final Feature feature = featureCapacity(2345L, null);

		testAddAllocation(feature, 20L, featureCapacity(2345L, 20L, a(20L)));
		testAddAllocation(feature, 30L, featureCapacity(2345L, 50L, a(20L), a(30L)));
	}

	@Test
	public void testAddAllocationToOnOffFeature() throws Exception {
		final Feature feature = featureOnOff(2345L);

		testAddAllocation(feature, null, featureOnOff(2345L, a(null)));
		testAddAllocation(feature, null, featureOnOff(2345L, a(null), a(null)));
	}

	private void testAddAllocation(final Feature initialFeature, final Long capacity, final Feature expectedFeature)
			throws SQLException {
		resetAll();

		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(resultSetMock.getString("serialnumber")).andReturn("1234567890");
		expect(resultSetMock.getInt("type")).andReturn(2);
		expect(this.allocationUsage2IntegerConverter.convertFrom(2)).andReturn(Usage.POOL);
		expect(resultSetMock.getTimestamp("enddate")).andReturn(new Timestamp(END_DATE.getMillis()));
		expect(this.timestamp2DateTimeConverter.convertTo(new Timestamp(END_DATE.getMillis()))).andReturn(END_DATE);

		if (initialFeature.getType() == Type.CAPACITY) {
			expect(resultSetMock.getLong("capacity")).andReturn(capacity);
		}

		replayAll();
		this.creator.addAllocation(initialFeature, resultSetMock);
		verifyAll();

		assertEquals(expectedFeature, initialFeature);
	}

	private Allocation a(final Long capacity) {
		return allocation(capacity, "1234567890", Usage.POOL, END_DATE);
	}
}
