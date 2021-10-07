/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.setting;

import static com.nsn.ood.cls.model.internal.test.SettingTestUtil.setting;
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
import com.nsn.ood.cls.core.db.creator.SettingCreator;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;


/**
 * @author marynows
 * 
 */
public class QuerySettingsTest {
	private static final Conditions CONDITIONS = ConditionsBuilder.create().build();
	private static final ConditionsMapper MAPPER = new ConditionsMapper();

	@Test
	public void testInitialization() throws Exception {
		final QuerySettings query = new QuerySettings(CONDITIONS, MAPPER, null);
		assertEquals("select * from cls.settings", getInternalState(query, String.class, ConditionsQuery.class));
	}

	@Test
	public void testHandleRow() throws Exception {
		final SettingCreator settingCreatorMock = createMock(SettingCreator.class);
		final ResultSet resultSetMock = createMock(ResultSet.class);

		expect(settingCreatorMock.createSetting(resultSetMock)).andReturn(setting(null, 54));

		replayAll();
		final QuerySettings query = new QuerySettings(CONDITIONS, MAPPER, settingCreatorMock);
		assertEquals(setting(null, 54), query.handleRow(resultSetMock));
		verifyAll();
	}
}
