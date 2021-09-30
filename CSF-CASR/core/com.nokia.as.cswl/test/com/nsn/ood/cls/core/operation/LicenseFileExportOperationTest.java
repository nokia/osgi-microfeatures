/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.core.test.LicenseFileTestUtil.assertLicenseFile;
import static com.nsn.ood.cls.core.test.LicenseFileTestUtil.licenseFile;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.nsn.ood.cls.core.db.QueryExecutor;
import com.nsn.ood.cls.core.db.license.QueryLicenseFile;
import com.nsn.ood.cls.core.model.LicenseFile;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		LicenseFileExportOperation.class, FileUtils.class })
public class LicenseFileExportOperationTest extends LicenseFileExportOperation {
	private String capturedSerialNumber;

	@Override
	protected QueryLicenseFile createQueryLicenseFile(final String serialNumber) {
		this.capturedSerialNumber = serialNumber;
		super.createQueryLicenseFile(serialNumber);

		return new QueryLicenseFile(null) {
			@Override
			public LicenseFile getValue() {
				if ("nofile".equals(serialNumber)) {
					return null;
				}
				return licenseFile("12345", "name", "path");
			};
		};
	}

	@Before
	public void setUp() throws Exception {
		mockStatic(FileUtils.class);
	}

	@Test
	public void testExport() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(isA(QueryLicenseFile.class));
		expect(FileUtils.readFileToString(anyObject(File.class))).andReturn("content");

		replayAll();
		setInternalState(this, queryExecutorMock);
		final LicenseFile result = export(license("12345"));
		verifyAll();

		assertLicenseFile(result, "12345", "name", "content");
		assertEquals("12345", this.capturedSerialNumber);
	}

	@Test
	public void testExportWithIOException() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);
		final IOException exception = new IOException("message");

		queryExecutorMock.execute(isA(QueryLicenseFile.class));
		expect(FileUtils.readFileToString(anyObject(File.class))).andThrow(exception);

		replayAll();
		setInternalState(this, queryExecutorMock);
		try {
			export(license("12345"));
			fail();
		} catch (final ExportException e) {
			assertEquals("message", e.getMessage());
			assertEquals(exception, e.getCause());
		}
		verifyAll();

		assertEquals("12345", this.capturedSerialNumber);
	}

	@Test
	public void testExportWithSQLExceptionAndNoLicenseFile() throws Exception {
		final QueryExecutor queryExecutorMock = createMock(QueryExecutor.class);

		queryExecutorMock.execute(isA(QueryLicenseFile.class));
		expectLastCall().andThrow(new SQLException());

		replayAll();
		setInternalState(this, queryExecutorMock);
		try {
			export(license("nofile"));
			fail();
		} catch (final ExportException e) {
			assertEquals("License does not exist", e.getMessage());
		}
		verifyAll();

		assertEquals("nofile", this.capturedSerialNumber);
	}
}
