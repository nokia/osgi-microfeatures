/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service;

import static com.nsn.ood.cls.core.test.ClientTagTestUtil.assertClientTag;
import static com.nsn.ood.cls.core.test.ClientTagTestUtil.clientTag;
import static com.nsn.ood.cls.core.test.ObjectWithTagTestUtil.clientWithTag;
import static com.nsn.ood.cls.model.test.ClientTestUtil.client;
import static com.nsn.ood.cls.model.test.FeatureErrorTestUtil.featureError;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.allocation;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featureCapacity;
import static com.nsn.ood.cls.model.test.FeatureTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Triple;
import org.easymock.Capture;
import org.easymock.IExpectationSetters;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.Filter;
import com.nsn.ood.cls.core.condition.Filter.Type;
import com.nsn.ood.cls.core.model.FeaturesWithTag;
import com.nsn.ood.cls.core.operation.ClientCreateOperation;
import com.nsn.ood.cls.core.operation.ClientRetrieveOperation;
import com.nsn.ood.cls.core.operation.ClientUpdateOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation.LockException;
import com.nsn.ood.cls.core.operation.FeatureReleaseOperation;
import com.nsn.ood.cls.core.operation.FeatureReleaseOperation.ReleaseException;
import com.nsn.ood.cls.core.operation.FeatureReservationOperation;
import com.nsn.ood.cls.core.operation.FeatureReservationOperation.ReservationException;
import com.nsn.ood.cls.core.operation.FeatureReservationOperation.ReservationResult;
import com.nsn.ood.cls.core.operation.FeatureRetrieveOperation;
import com.nsn.ood.cls.core.operation.UpdateCapacityOperation;
import com.nsn.ood.cls.core.operation.exception.CreateException;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.core.operation.util.ReservationErrorType;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Allocation.Usage;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.util.MapBuilder;


/**
 * @author marynows
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		LockException.class, ReservationException.class })
public class FeaturesServiceTest {
	private static final String CLIENT_ID = "12345";

	private FeatureLockOperation featureLockOperationMock;
	private ClientCreateOperation clientCreateOperationMock;
	private FeatureReservationOperation featureReservationOperationMock;
	private ClientUpdateOperation clientUpdateOperationMock;
	private ClientRetrieveOperation clientRetrieveOperationMock;
	private FeatureReleaseOperation featureReleaseOperationMock;
	private FeatureRetrieveOperation featureRetrieveOperationMock;
	private UpdateCapacityOperation updateCapacityOperationMock;
	private ErrorExceptionFactory errorExceptionFactoryMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private FeaturesService service;

	@Before
	public void setUp() throws Exception {
		this.featureLockOperationMock = createMock(FeatureLockOperation.class);
		this.clientCreateOperationMock = createMock(ClientCreateOperation.class);
		this.featureReservationOperationMock = createMock(FeatureReservationOperation.class);
		this.clientUpdateOperationMock = createMock(ClientUpdateOperation.class);
		this.clientRetrieveOperationMock = createMock(ClientRetrieveOperation.class);
		this.featureReleaseOperationMock = createMock(FeatureReleaseOperation.class);
		this.featureRetrieveOperationMock = createMock(FeatureRetrieveOperation.class);
		this.updateCapacityOperationMock = createMock(UpdateCapacityOperation.class);
		this.errorExceptionFactoryMock = createMock(ErrorExceptionFactory.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);

		this.service = new FeaturesService();
		setInternalState(this.service, this.featureLockOperationMock, this.clientCreateOperationMock,
				this.featureReservationOperationMock, this.clientUpdateOperationMock, this.clientRetrieveOperationMock,
				this.featureReleaseOperationMock, this.featureRetrieveOperationMock, this.updateCapacityOperationMock,
				this.errorExceptionFactoryMock, this.serviceExceptionFactoryMock);
	}

	@Test
	public void testReserveCapacity() throws Exception {
		testReserveCapacity(MapBuilder.linkedMap(featureCapacity(2345L, 10L), true).build());
		testReserveCapacity(MapBuilder.linkedMap(featureCapacity(2345L, 10L), false).build());
		testReserveCapacity(MapBuilder.linkedMap(featureCapacity(2345L, 10L), true)//
				.put(featureCapacity(3456L, 20L), true)//
				.put(featureCapacity(4567L, 30L), true).build());
		testReserveCapacity(MapBuilder.linkedMap(featureCapacity(2345L, 10L), true)//
				.put(featureCapacity(3456L, 20L), false)//
				.put(featureCapacity(4567L, 30L), true).build());
		testReserveCapacity(MapBuilder.linkedMap(featureCapacity(2345L, 0L), true).build());
		testReserveCapacity(MapBuilder.linkedMap(featureCapacity(2345L, 0L), false).build());
	}

	private void testReserveCapacity(final Map<Feature, Boolean> featuresMap) throws Exception {
		resetAll();

		final List<Long> featureCodes = new ArrayList<>();
		final List<Long> featureCodesToUpdate = new ArrayList<>();
		for (final Entry<Feature, Boolean> entry : featuresMap.entrySet()) {
			final Long featureCode = entry.getKey().getFeatureCode();
			featureCodes.add(featureCode);
			if (entry.getValue()) {
				featureCodesToUpdate.add(featureCode);
			}
		}

		expect(this.featureLockOperationMock.lock(featureCodes)).andReturn(featureCodes);
		expect(this.clientCreateOperationMock.createIfNotExist(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));

		final List<Feature> featuresWithAllocations = new ArrayList<>();
		for (final Entry<Feature, Boolean> entry : featuresMap.entrySet()) {
			final Feature feature = entry.getKey();
			final Feature featureWithAllocation = featureCapacity(feature.getFeatureCode(), feature.getCapacity(),
					allocationForFeature(feature));
			expect(this.featureReservationOperationMock.reserveCapacity(client(CLIENT_ID), feature))
					.andReturn(new TestReservationResult(entry.getValue(), featureWithAllocation));
			featuresWithAllocations.add(featureWithAllocation);
		}

		// if (!featureCodesToUpdate.isEmpty()) {
		// this.updateCapacityOperationMock.updateCapacity(featureCodesToUpdate);
		// }

		expect(this.clientUpdateOperationMock.updateExpirationTime(clientWithTag(client(CLIENT_ID), clientTag())))
				.andReturn(clientTag());

		replayAll();
		final FeaturesWithTag result = this.service.reserveCapacity(CLIENT_ID, new ArrayList<>(featuresMap.keySet()));
		verifyAll();

		assertNotNull(result);
		assertClientTag(result.getClientTag());
		assertEquals(featuresWithAllocations, result.getObject());
	}

	@Test
	public void testReserveCapacityAndExpectExpirationTimeUpdateFail() throws Exception {
		final UpdateException exceptionMock = createMock(UpdateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.featureLockOperationMock.lock(Arrays.asList(2345L))).andReturn(Arrays.asList(2345L));
		expect(this.clientCreateOperationMock.createIfNotExist(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureReservationOperationMock.reserveCapacity(client(CLIENT_ID), featureCapacity(2345L, 10L)))
				.andReturn(new TestReservationResult(true, featureCapacity(2345L, 10L, allocation())));
		// this.updateCapacityOperationMock.updateCapacity(Arrays.asList(2345L));
		expect(this.clientUpdateOperationMock.updateExpirationTime(clientWithTag(client(CLIENT_ID), clientTag())))
				.andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.client(ErrorCode.CANNOT_UPDATE_KEEP_ALIVE, exceptionMock,
				client(CLIENT_ID))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.reserveCapacity(CLIENT_ID, featuresList(featureCapacity(2345L, 10L)));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	@Ignore("Not neede - capacity update is disabled during reservation (it's handled internally by reservation)")
	public void testReserveCapacityAndExpectUpdateCapacityFail() throws Exception {
		final UpdateException exceptionMock = createMock(UpdateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.featureLockOperationMock.lock(Arrays.asList(2345L))).andReturn(Arrays.asList(2345L));
		expect(this.clientCreateOperationMock.createIfNotExist(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureReservationOperationMock.reserveCapacity(client(CLIENT_ID), featureCapacity(2345L, 10L)))
				.andReturn(new TestReservationResult(true, featureCapacity(2345L, 10L, allocation())));
		this.updateCapacityOperationMock.updateCapacity(Arrays.asList(2345L));
		expectLastCall().andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.CAPACITY_UPDATE_FAIL, exceptionMock))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.reserveCapacity(CLIENT_ID, featuresList(featureCapacity(2345L, 10L)));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveCapacityAndExpectReservationFail() throws Exception {
		final List<Feature> features = featuresList(featureCapacity(2345L, 10L), featureCapacity(3456L, 20L),
				featureCapacity(4567L, 30L), featureCapacity(5678L, 40L));

		testReserveCapacityAndExpectReservationFail(features, 1, ReservationErrorType.CAPACITY,
				ErrorCode.NOT_ENOUGH_CAPACITY, featureError(2345L, 10L, 5L));
		testReserveCapacityAndExpectReservationFail(features, 2, ReservationErrorType.RELEASE,
				ErrorCode.CANNOT_RELEASE_CAPACITY, featureError(3456L, 20L, null));
		testReserveCapacityAndExpectReservationFail(features, 3, ReservationErrorType.ON_OFF,
				ErrorCode.ON_OFF_LICENSE_MISSING, featureError(4567L));
		testReserveCapacityAndExpectReservationFail(features, 4, null, ErrorCode.NOT_ENOUGH_CAPACITY,
				featureError(5678L, 40L, 5L));
	}

	private void testReserveCapacityAndExpectReservationFail(final List<Feature> features, final int failAtFeature,
			final ReservationErrorType errorType, final ErrorCode expectedErrorCode,
			final FeatureError expectedFeatureError) throws Exception {
		resetAll();

		final ReservationException exceptionMock = createMock(ReservationException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);
		final ErrorException errorExceptionMock = createMock(ErrorException.class);

		final List<Long> featureCodes = getFeatureCodes(features);
		expect(this.featureLockOperationMock.lock(featureCodes)).andReturn(featureCodes);
		expect(this.clientCreateOperationMock.createIfNotExist(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));

		for (int i = 0; i < features.size(); i++) {
			final IExpectationSetters<ReservationResult> expectation = expect(
					this.featureReservationOperationMock.reserveCapacity(client(CLIENT_ID), features.get(i)));
			if (i == (failAtFeature - 1)) {
				expectation.andThrow(exceptionMock);
				expect(exceptionMock.getErrorType()).andReturn(errorType);
				expect(exceptionMock.getError()).andReturn(expectedFeatureError);
				expect(this.errorExceptionFactoryMock.feature(expectedErrorCode, exceptionMock, expectedFeatureError))
						.andReturn(errorExceptionMock);
			} else {
				expectation.andReturn(new TestReservationResult(true, features.get(i)));
			}
		}

		expect(this.serviceExceptionFactoryMock.exceptions(Arrays.asList(errorExceptionMock)))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.reserveCapacity(CLIENT_ID, features);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveCapacityAndExpectMultipleReservationFail() throws Exception {
		testReserveCapacityAndExpectMultipleReservationFails(
				featuresList(//
						featureCapacity(2345L, 10L), //
						featureCapacity(3456L, 20L), //
						featureCapacity(4567L, 30L), //
						featureCapacity(5678L, 40L)),
				Arrays.asList(//
						Triple.of(featureError(3456L, 20L, 5L), ReservationErrorType.CAPACITY,
								ErrorCode.NOT_ENOUGH_CAPACITY), //
						Triple.of(featureError(5678L, 50L), ReservationErrorType.RELEASE,
								ErrorCode.CANNOT_RELEASE_CAPACITY)));

		testReserveCapacityAndExpectMultipleReservationFails(
				featuresList(//
						featureCapacity(2345L, 10L), //
						featureCapacity(3456L, 20L), //
						featureCapacity(4567L, 30L), //
						featureCapacity(5678L, 40L)),
				Arrays.asList(//
						Triple.of(featureError(2345L, 10L, 5L), ReservationErrorType.CAPACITY,
								ErrorCode.NOT_ENOUGH_CAPACITY), //
						Triple.of(featureError(3456L), ReservationErrorType.ON_OFF, ErrorCode.ON_OFF_LICENSE_MISSING), //
						Triple.of(featureError(4567L, 50L), ReservationErrorType.RELEASE,
								ErrorCode.CANNOT_RELEASE_CAPACITY)));
	}

	private void testReserveCapacityAndExpectMultipleReservationFails(final List<Feature> features,
			final List<Triple<FeatureError, ReservationErrorType, ErrorCode>> expectedErrors)
					throws LockException, CreateException, ReservationException, UnknownErrorException {
		resetAll();

		final ReservationException exceptionMock = createMock(ReservationException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);
		final ErrorException errorExceptionMock = createMock(ErrorException.class);

		final List<Long> featureCodes = getFeatureCodes(features);
		expect(this.featureLockOperationMock.lock(featureCodes)).andReturn(featureCodes);
		expect(this.clientCreateOperationMock.createIfNotExist(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));

		final Map<Long, Triple<FeatureError, ReservationErrorType, ErrorCode>> errorsMap = new HashMap<>();
		for (final Triple<FeatureError, ReservationErrorType, ErrorCode> error : expectedErrors) {
			errorsMap.put(error.getLeft().getFeatureCode(), error);
		}

		final List<ErrorException> errorExceptionMocks = new ArrayList<>();
		for (final Feature feature : features) {
			final IExpectationSetters<ReservationResult> expectation = expect(
					this.featureReservationOperationMock.reserveCapacity(client(CLIENT_ID), feature));
			if (errorsMap.containsKey(feature.getFeatureCode())) {
				expectation.andThrow(exceptionMock);

				final Triple<FeatureError, ReservationErrorType, ErrorCode> error = errorsMap
						.get(feature.getFeatureCode());
				expect(exceptionMock.getErrorType()).andReturn(error.getMiddle());
				expect(exceptionMock.getError()).andReturn(error.getLeft());
				expect(this.errorExceptionFactoryMock.feature(error.getRight(), exceptionMock, error.getLeft()))
						.andReturn(errorExceptionMock);
				errorExceptionMocks.add(errorExceptionMock);
			} else {
				expectation.andReturn(new TestReservationResult(true, feature));
			}
		}

		expect(this.serviceExceptionFactoryMock.exceptions(errorExceptionMocks)).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.reserveCapacity(CLIENT_ID, features);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	private List<Long> getFeatureCodes(final List<Feature> features) {
		final List<Long> featureCodes = new ArrayList<>();
		for (final Feature feature : features) {
			featureCodes.add(feature.getFeatureCode());
		}
		return featureCodes;
	}

	@Test
	public void testReserveCapacityAndExpectClientCreationFail() throws Exception {
		final CreateException exceptionMock = createMock(CreateException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.featureLockOperationMock.lock(Arrays.asList(2345L))).andReturn(Arrays.asList(2345L));
		expect(this.clientCreateOperationMock.createIfNotExist(CLIENT_ID)).andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.client(ErrorCode.CANNOT_RESERVE_CLIENT_ID, exceptionMock,
				client(CLIENT_ID))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.reserveCapacity(CLIENT_ID, featuresList(featureCapacity(2345L, 10L)));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReserveCapacityAndExpectLockFail() throws Exception {
		final LockException exceptionMock = createMock(LockException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.featureLockOperationMock.lock(Arrays.asList(2345L))).andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, exceptionMock))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.reserveCapacity(CLIENT_ID, featuresList(featureCapacity(2345L, 10L)));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	private Allocation allocationForFeature(final Feature feature) {
		return allocation(feature.getCapacity(), String.valueOf(feature.getFeatureCode()), Usage.FLOATING_POOL,
				new DateTime(2015, 1, 26, 13, 59));
	}

	@Test
	public void testReleaseCapacity() throws Exception {
		testReleaseCapacity(Arrays.asList(1234L), true);
		testReleaseCapacity(Arrays.asList(1234L, 2345L), false);
	}

	private void testReleaseCapacity(final List<Long> featureCodes, final boolean force) throws Exception {
		resetAll();

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureLockOperationMock.lock(featureCodes)).andReturn(featureCodes);
		for (final Long featureCode : featureCodes) {
			this.featureReleaseOperationMock.release(client(CLIENT_ID), featureCode.longValue(), force);
		}
		this.updateCapacityOperationMock.updateCapacity(featureCodes);

		replayAll();
		this.service.releaseCapacity(CLIENT_ID, featureCodes, force);
		verifyAll();
	}

	@Test
	public void testReleaseCapacityAndExpectLockFail() throws Exception {
		final LockException exceptionMock = createMock(LockException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureLockOperationMock.lock(Arrays.asList(1234L))).andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, exceptionMock))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.releaseCapacity(CLIENT_ID, Arrays.asList(1234L), false);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReleaseCapacityAndExpectReleasingFail() throws Exception {
		final List<Long> featureCodes = Arrays.asList(1234L, 2345L, 3456L);

		testReleaseCapacityAndExpectReleasingFail(featureCodes, 1, true);
		testReleaseCapacityAndExpectReleasingFail(featureCodes, 2, false);
		testReleaseCapacityAndExpectReleasingFail(featureCodes, 3, true);
	}

	private void testReleaseCapacityAndExpectReleasingFail(final List<Long> featureCodes, final int failAtFeature,
			final boolean force) throws Exception {
		resetAll();

		final ReleaseException exceptionMock = createMock(ReleaseException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureLockOperationMock.lock(featureCodes)).andReturn(featureCodes);

		for (int i = 0; i < (failAtFeature - 1); i++) {
			this.featureReleaseOperationMock.release(client(CLIENT_ID), featureCodes.get(i).longValue(), force);
		}
		this.featureReleaseOperationMock.release(client(CLIENT_ID), featureCodes.get(failAtFeature - 1).longValue(),
				force);
		expectLastCall().andThrow(exceptionMock);

		expect(exceptionMock.getError()).andReturn(featureError(1234L, 20L, null));
		expect(this.serviceExceptionFactoryMock.feature(ErrorCode.CANNOT_RELEASE_CAPACITY, exceptionMock,
				featureError(1234L, 20L, null))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.releaseCapacity(CLIENT_ID, featureCodes, force);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReleaseCapacityForAllFeatures() throws Exception {
		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureLockOperationMock.lockForClient(client(CLIENT_ID))).andReturn(Arrays.asList(1234L, 2345L));
		this.featureReleaseOperationMock.releaseAll(client(CLIENT_ID), true);
		this.updateCapacityOperationMock.updateCapacity(Arrays.asList(1234L, 2345L));

		replayAll();
		this.service.releaseCapacity(CLIENT_ID, new ArrayList<Long>(), true);
		verifyAll();
	}

	@Test
	public void testReleaseCapacityForAllFeaturesWhenNoFeatures() throws Exception {
		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureLockOperationMock.lockForClient(client(CLIENT_ID)))
				.andReturn(Collections.<Long> emptyList());

		replayAll();
		this.service.releaseCapacity(CLIENT_ID, new ArrayList<Long>(), true);
		verifyAll();
	}

	@Test
	public void testReleaseCapacityForAllFeaturesAndExpectLockFail() throws Exception {
		final LockException exceptionMock = createMock(LockException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		this.featureLockOperationMock.lockForClient(client(CLIENT_ID));
		expectLastCall().andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, exceptionMock))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.releaseCapacity(CLIENT_ID, new ArrayList<Long>(), true);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReleaseCapacityForAllFeaturesAndExpectReleasingFail() throws Exception {
		final ReleaseException exceptionMock = createMock(ReleaseException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		expect(this.featureLockOperationMock.lockForClient(client(CLIENT_ID))).andReturn(Arrays.asList(1234L));
		this.featureReleaseOperationMock.releaseAll(client(CLIENT_ID), false);
		expectLastCall().andThrow(exceptionMock);

		expect(exceptionMock.getError()).andReturn(null);
		expect(this.serviceExceptionFactoryMock.feature(ErrorCode.CANNOT_RELEASE_CAPACITY, exceptionMock, null))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.releaseCapacity(CLIENT_ID, new ArrayList<Long>(), false);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testReleaseCapacityWhenNoSuchClient() throws Exception {
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID)).andReturn(null);
		expect(this.serviceExceptionFactoryMock.clientNotFound(client(CLIENT_ID))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.releaseCapacity(CLIENT_ID, new ArrayList<Long>(), false);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetFeatures() throws Exception {
		final MetaDataList<Feature> features = new MetaDataList<>(featuresList(featureCapacity(2345L, 10L)),
				metaData());
		final Conditions conditionsMock = createMock(Conditions.class);
		final Capture<Filter> capturedFilter = new Capture<>();

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		conditionsMock.addFilter(capture(capturedFilter));
		expect(this.featureRetrieveOperationMock.getList(conditionsMock)).andReturn(features);

		replayAll();
		assertEquals(features, this.service.getFeatures(CLIENT_ID, conditionsMock));
		verifyAll();

		assertFilter(capturedFilter.getValue(), "clientId", CLIENT_ID);
	}

	@Test
	public void testGetFeaturesAndExpectError() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final Capture<Filter> capturedFilter = new Capture<>();
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID))
				.andReturn(clientWithTag(client(CLIENT_ID), clientTag()));
		conditionsMock.addFilter(capture(capturedFilter));
		expect(this.featureRetrieveOperationMock.getList(conditionsMock)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m")))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.getFeatures(CLIENT_ID, conditionsMock);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();

		assertFilter(capturedFilter.getValue(), "clientId", CLIENT_ID);
	}

	private void assertFilter(final Filter filter, final String expectedName, final String expectedValue) {
		assertEquals(expectedName, filter.name());
		assertEquals(expectedValue, filter.value());
		assertEquals(Type.EQUAL, filter.type());
	}

	@Test
	public void testGetFeaturesWhenNoSuchclient() throws Exception {
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.clientRetrieveOperationMock.getClient(CLIENT_ID)).andReturn(null);
		expect(this.serviceExceptionFactoryMock.clientNotFound(client(CLIENT_ID))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.getFeatures(CLIENT_ID, null);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	private final class TestReservationResult implements ReservationResult {
		private final boolean updated;
		private final Feature feature;

		public TestReservationResult(final boolean updated, final Feature feature) {
			this.updated = updated;
			this.feature = feature;
		}

		@Override
		public boolean isUpdated() {
			return this.updated;
		}

		@Override
		public Feature getFeature() {
			return this.feature;
		}
	}
}
