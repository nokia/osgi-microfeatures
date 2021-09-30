/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.service;

import static com.nsn.ood.cls.core.test.LicenseFileTestUtil.licenseFile;
import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.storedLicense;
import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.storedLicensesList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.feature;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.featuresList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.model.test.ViolationErrorTestUtil.violationError;
import static com.nsn.ood.cls.util.test.AnnotationTestUtil.assertAnnotation;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.audit.AuditLog;
import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.model.LicenseFile;
import com.nsn.ood.cls.core.operation.FeatureLockOperation;
import com.nsn.ood.cls.core.operation.FeatureLockOperation.LockException;
import com.nsn.ood.cls.core.operation.LicenseDbCancelOperation;
import com.nsn.ood.cls.core.operation.LicenseDbInstallOperation;
import com.nsn.ood.cls.core.operation.LicenseFileCancelOperation;
import com.nsn.ood.cls.core.operation.LicenseFileExportOperation;
import com.nsn.ood.cls.core.operation.LicenseFileExportOperation.ExportException;
import com.nsn.ood.cls.core.operation.LicenseFileInstallOperation;
import com.nsn.ood.cls.core.operation.LicenseRetrieveOperation;
import com.nsn.ood.cls.core.operation.StoredLicenseRetrieveOperation;
import com.nsn.ood.cls.core.operation.exception.CancelException;
import com.nsn.ood.cls.core.operation.exception.InstallException;
import com.nsn.ood.cls.core.operation.exception.RetrieveException;
import com.nsn.ood.cls.core.operation.util.LicenseErrorType;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.model.metadata.MetaDataList;


/**
 * @author marynows
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(fullyQualifiedNames = {
		"com.nsn.ood.cls.core.operation.util.ReservationsUtils.CalculationException" },
		value = {
				LockException.class, ExportException.class })
public class LicensesServiceTest {
	private LicenseRetrieveOperation licenseRetrieveOperationMock;
	private StoredLicenseRetrieveOperation storedLicenseRetrieveOperationMock;
	private LicenseFileInstallOperation licenseFileInstallOperationMock;
	private LicenseFileCancelOperation licenseFileCancelOperationMock;
	private LicenseFileExportOperation licenseFileExportOperationMock;
	private LicenseDbInstallOperation licenseDbInstallOperationMock;
	private LicenseDbCancelOperation licenseDbCancelOperationMock;
	private FeatureLockOperation featureLockOperationMock;
	private ServiceExceptionFactory serviceExceptionFactoryMock;
	private LicensesService service;

	@Before
	public void setUp() throws Exception {
		this.licenseRetrieveOperationMock = createMock(LicenseRetrieveOperation.class);
		this.storedLicenseRetrieveOperationMock = createMock(StoredLicenseRetrieveOperation.class);
		this.licenseFileInstallOperationMock = createMock(LicenseFileInstallOperation.class);
		this.licenseFileCancelOperationMock = createMock(LicenseFileCancelOperation.class);
		this.licenseFileExportOperationMock = createMock(LicenseFileExportOperation.class);
		this.licenseDbInstallOperationMock = createMock(LicenseDbInstallOperation.class);
		this.licenseDbCancelOperationMock = createMock(LicenseDbCancelOperation.class);
		this.featureLockOperationMock = createMock(FeatureLockOperation.class);
		this.serviceExceptionFactoryMock = createMock(ServiceExceptionFactory.class);

		this.service = new LicensesService();
		setInternalState(this.service, this.licenseRetrieveOperationMock, this.storedLicenseRetrieveOperationMock,
				this.licenseFileInstallOperationMock, this.licenseFileCancelOperationMock,
				this.licenseFileExportOperationMock, this.licenseDbInstallOperationMock,
				this.licenseDbCancelOperationMock, this.featureLockOperationMock, this.serviceExceptionFactoryMock);
	}

	@Test
	public void testGetLicenses() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final MetaDataList<License> licenses = new MetaDataList<>(licensesList(license("123")), metaData());

		expect(this.licenseRetrieveOperationMock.getList(conditionsMock)).andReturn(licenses);

		replayAll();
		assertEquals(licenses, this.service.getLicenses(conditionsMock));
		verifyAll();
	}

	@Test
	public void testGetLicensesWithException() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licenseRetrieveOperationMock.getList(conditionsMock)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m")))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.getLicenses(conditionsMock);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetStoredLicenses() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final MetaDataList<StoredLicense> licenses = new MetaDataList<>(storedLicensesList(storedLicense("123")),
				metaData());

		expect(this.storedLicenseRetrieveOperationMock.getList(conditionsMock)).andReturn(licenses);

		replayAll();
		assertEquals(licenses, this.service.getStoredLicenses(conditionsMock));
		verifyAll();
	}

	@Test
	public void testGetStoredLicensesWithException() throws Exception {
		final Conditions conditionsMock = createMock(Conditions.class);
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.storedLicenseRetrieveOperationMock.getList(conditionsMock)).andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m")))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.getStoredLicenses(conditionsMock);
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testGetStoredLicenseFilterValues() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();

		expect(this.storedLicenseRetrieveOperationMock.getFilterValues("filterName", conditions))
				.andReturn(Arrays.asList("1", "2"));

		replayAll();
		assertEquals(Arrays.asList("1", "2"), this.service.getStoredLicenseFilterValues("filterName"));
		verifyAll();
	}

	@Test
	public void testGetStoredLicenseFilterValuesWithException() throws Exception {
		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().build();
		final RetrieveException exceptionMock = createMock(RetrieveException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.storedLicenseRetrieveOperationMock.getFilterValues("filterName", conditions))
				.andThrow(exceptionMock);
		expect(exceptionMock.getError()).andReturn(violationError("m"));
		expect(this.serviceExceptionFactoryMock.violation(exceptionMock, violationError("m")))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.getStoredLicenseFilterValues("filterName");
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testInstall() throws Exception {
		final License license = license("123").withFeatures(featuresList(feature(1001L, null)));

		expect(this.licenseFileInstallOperationMock.install(licenseFile("fileName", "licenseContent")))
				.andReturn(license);
		this.featureLockOperationMock.lock(1001L);
		this.licenseDbInstallOperationMock.install(license);

		replayAll();
		assertEquals(license, this.service.install(licenseFile("fileName", "licenseContent")));
		verifyAll();
	}

	@Test
	public void testInstallWithLockException() throws Exception {
		final License license = license("123").withFeatures(featuresList(feature(1001L, null)));

		final LockException exceptionMock = createMock(LockException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licenseFileInstallOperationMock.install(licenseFile("fileName", "licenseContent")))
				.andReturn(license);
		this.featureLockOperationMock.lock(1001L);
		expectLastCall().andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, exceptionMock))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.install(licenseFile("fileName", "licenseContent"));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testInstallWithInstallException() throws Exception {
		testInstallWithInstallException(LicenseErrorType.CLJL, ErrorCode.CLJL_LICENSE_INSTALL_FAIL);
		testInstallWithInstallException(LicenseErrorType.VERIFICATION, ErrorCode.LICENSE_VERIFICATION_FAIL);
		testInstallWithInstallException(LicenseErrorType.DB, ErrorCode.LICENSE_INSTALL_FAIL);
	}

	private void testInstallWithInstallException(final LicenseErrorType errorType, final ErrorCode expectedErrorCode)
			throws Exception {
		resetAll();

		final InstallException exceptionMock = createMock(InstallException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licenseFileInstallOperationMock.install(licenseFile("fileName", "licenseContent")))
				.andThrow(exceptionMock);
		expect(exceptionMock.getErrorType()).andReturn(errorType);
		expect(exceptionMock.getCljlErrorCode()).andReturn("cljlCode");
		expect(this.serviceExceptionFactoryMock.license(expectedErrorCode, exceptionMock, "cljlCode",
				new License().withFileName("fileName").withTargets(null).withFeatures(null)))
						.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.install(licenseFile("fileName", "licenseContent"));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testCancel() throws Exception {
		final License canceledLicense = license("234").withFeatures(featuresList(feature(2002L, null)));

		expect(this.licenseFileCancelOperationMock.cancel(license("123"))).andReturn(canceledLicense);
		this.featureLockOperationMock.lock(2002L);
		this.licenseDbCancelOperationMock.cancel(canceledLicense);

		replayAll();
		assertEquals(canceledLicense, this.service.cancel(license("123")));
		verifyAll();
	}

	@Test
	public void testCancelWithLockException() throws Exception {
		final License canceledLicense = license("234").withFeatures(featuresList(feature(2002L, null)));

		final LockException exceptionMock = createMock(LockException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licenseFileCancelOperationMock.cancel(license("123"))).andReturn(canceledLicense);
		this.featureLockOperationMock.lock(2002L);
		expectLastCall().andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.error(ErrorCode.CONCURRENT_ACTIONS_FAIL, exceptionMock))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.cancel(license("123"));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testCancelWithCancelException() throws Exception {
		testCancelWithCancelException(LicenseErrorType.CLJL, ErrorCode.CLJL_LICENSE_CANCEL_FAIL);
		testCancelWithCancelException(LicenseErrorType.DB, ErrorCode.LICENSE_CANCEL_FAIL);
	}

	private void testCancelWithCancelException(final LicenseErrorType errorType, final ErrorCode expectedErrorCode)
			throws Exception {
		resetAll();

		final CancelException exceptionMock = createMock(CancelException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licenseFileCancelOperationMock.cancel(license("123"))).andThrow(exceptionMock);
		expect(exceptionMock.getErrorType()).andReturn(errorType);
		expect(exceptionMock.getCljlErrorCode()).andReturn("cljlCode");
		expect(this.serviceExceptionFactoryMock.license(expectedErrorCode, exceptionMock, "cljlCode", license("123")))
				.andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.cancel(license("123"));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testExport() throws Exception {
		expect(this.licenseFileExportOperationMock.export(license("123"))).andReturn(licenseFile("123"));

		replayAll();
		assertEquals(licenseFile("123"), this.service.export(license("123")));
		verifyAll();
	}

	@Test
	public void testExportWithException() throws Exception {
		final ExportException exceptionMock = createMock(ExportException.class);
		final ServiceException serviceExceptionMock = createMock(ServiceException.class);

		expect(this.licenseFileExportOperationMock.export(license("123"))).andThrow(exceptionMock);
		expect(this.serviceExceptionFactoryMock.license(ErrorCode.LICENSE_EXPORT_FAIL, exceptionMock, null,
				license("123"))).andReturn(serviceExceptionMock);

		replayAll();
		try {
			this.service.export(license("123"));
			fail();
		} catch (final ServiceException e) {
			assertEquals(serviceExceptionMock, e);
		}
		verifyAll();
	}

	@Test
	public void testAnnotation() throws Exception {
		assertAnnotation(LicensesService.class.getMethod("install", LicenseFile.class), AuditLog.class);
		assertAnnotation(LicensesService.class.getMethod("cancel", License.class), AuditLog.class);
	}
}
