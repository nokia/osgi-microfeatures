/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.operation;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.settingsList;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;

import com.nsn.ood.cls.core.db.UpdateExecutor;
import com.nsn.ood.cls.core.db.setting.UpdateSettings;
import com.nsn.ood.cls.core.operation.exception.UpdateException;
import com.nsn.ood.cls.model.internal.Setting;


/**
 * @author marynows
 * 
 */
public class SettingUpdateOperationTest extends SettingUpdateOperation {
	private static final List<Setting> SETTINGS = settingsList(setting(null, 33L));
	private static final UpdateSettings UPDATE_STUB = new UpdateSettings(null) {
		@Override
		public int getIndex() {
			return 5;
		};
	};

	private List<Setting> capturedSettings;

	@Override
	protected UpdateSettings createSettingsUpdate(final List<Setting> settings) {
		this.capturedSettings = settings;
		super.createSettingsUpdate(settings);
		return UPDATE_STUB;
	}

	@Test
	public void testUpdateSettings() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(same(UPDATE_STUB));

		replayAll();
		setInternalState(this, updateExecutorMock);
		updateSettings(SETTINGS);
		verifyAll();

		assertEquals(SETTINGS, this.capturedSettings);
	}

	@Test
	public void testUpdateSettingsAndExpectException() throws Exception {
		final UpdateExecutor updateExecutorMock = createMock(UpdateExecutor.class);

		updateExecutorMock.execute(same(UPDATE_STUB));
		expectLastCall().andThrow(new SQLException("message"));

		replayAll();
		setInternalState(this, updateExecutorMock);
		try {
			updateSettings(SETTINGS);
			fail();
		} catch (final UpdateException e) {
			assertEquals("message", e.getMessage());
			assertEquals(5, e.getIndex());
		}
		verifyAll();

		assertEquals(SETTINGS, this.capturedSettings);
	}
}
