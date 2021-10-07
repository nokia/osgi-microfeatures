/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.createStrictMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;


/**
 * @author marynows
 * 
 */
public class LoggingPreparedStatementTest {
	private static final Calendar CALENDAR = Calendar.getInstance();
	private static final List<String> JAVA_8_EXCLUSIONS = Arrays
			.asList("public default long[] java.sql.Statement.executeLargeBatch() throws java.sql.SQLException",//
					"public default long java.sql.PreparedStatement.executeLargeUpdate() throws java.sql.SQLException",//
					"public default long java.sql.Statement.executeLargeUpdate(java.lang.String,java.lang.String[]) throws java.sql.SQLException",//
					"public default long java.sql.Statement.executeLargeUpdate(java.lang.String,int) throws java.sql.SQLException",//
					"public default long java.sql.Statement.executeLargeUpdate(java.lang.String) throws java.sql.SQLException",//
					"public default long java.sql.Statement.executeLargeUpdate(java.lang.String,int[]) throws java.sql.SQLException",//
					"public default long java.sql.Statement.getLargeMaxRows() throws java.sql.SQLException",//
					"public default long java.sql.Statement.getLargeUpdateCount() throws java.sql.SQLException",//
					"public default void java.sql.Statement.setLargeMaxRows(long) throws java.sql.SQLException",//
					"public default void java.sql.PreparedStatement.setObject(int,java.lang.Object,java.sql.SQLType,int) throws java.sql.SQLException",//
					"public default void java.sql.PreparedStatement.setObject(int,java.lang.Object,java.sql.SQLType) throws java.sql.SQLException");

	@SuppressWarnings("resource")
	@Test
	public void testParameters() throws Exception {
		final PreparedStatement psMock = createStrictMock(PreparedStatement.class);
		final LoggingPreparedStatement lps = new LoggingPreparedStatement(psMock);

		psMock.setBigDecimal(1, new BigDecimal(1));
		psMock.setBoolean(2, true);
		psMock.setByte(3, (byte) 3);
		psMock.setDate(4, new Date(4L));
		psMock.setDate(5, new Date(5L), CALENDAR);
		psMock.setFloat(6, 6F);
		psMock.setInt(7, 7);
		psMock.setLong(8, 8L);
		psMock.setNString(9, "NString");
		psMock.setNull(10, Types.VARCHAR);
		psMock.setNull(11, Types.NUMERIC, "name");
		psMock.setShort(12, (short) 12);
		psMock.setString(13, "String");
		psMock.setTime(14, new Time(14L));
		psMock.setTime(15, new Time(15L), CALENDAR);
		psMock.setTimestamp(16, new Timestamp(16L));
		psMock.setTimestamp(17, new Timestamp(17L), CALENDAR);

		replayAll();
		assertTrue(lps.getParameters().isEmpty());
		lps.setBigDecimal(1, new BigDecimal(1));
		lps.setBoolean(2, true);
		lps.setByte(3, (byte) 3);
		lps.setDate(4, new Date(4L));
		lps.setDate(5, new Date(5L), CALENDAR);
		lps.setFloat(6, 6F);
		lps.setInt(7, 7);
		lps.setLong(8, 8L);
		lps.setNString(9, "NString");
		lps.setNull(10, Types.VARCHAR);
		lps.setNull(11, Types.NUMERIC, "name");
		lps.setShort(12, (short) 12);
		lps.setString(13, "String");
		lps.setTime(14, new Time(14L));
		lps.setTime(15, new Time(15L), CALENDAR);
		lps.setTimestamp(16, new Timestamp(16L));
		lps.setTimestamp(17, new Timestamp(17L), CALENDAR);
		final List<Object> parameters = lps.getParameters();
		assertEquals(Arrays.<Object> asList(new BigDecimal(1), true, (byte) 3, new Date(4L), new Date(5L), 6F, 7, 8L,
				"NString", null, null, (short) 12, "String", new Time(14L), new Time(15L), new Timestamp(16L),
				new Timestamp(17L)), parameters);
		verifyAll();
	}

	@Test
	public void testMethods() throws Exception {
		final List<Method> methods = getPrepareStatementMethods();
		for (int i = 0; i < methods.size(); i++) {
			final Method method = methods.get(i);

			resetAll();
			final PreparedStatement psMock = createMock(PreparedStatement.class);
			final LoggingPreparedStatement lps = new LoggingPreparedStatement(psMock);

			final Class<?>[] parameterTypes = method.getParameterTypes();
			final Object[] arguments = new Object[parameterTypes.length];
			for (int j = 0; j < parameterTypes.length; j++) {
				arguments[j] = createMockForType(parameterTypes[j]);
			}

			final Class<?> returnType = method.getReturnType();
			final Object returnValue = createMockForType(returnType);

			method.invoke(psMock, arguments);
			if (!returnType.equals(Void.TYPE)) {
				expectLastCall().andReturn(returnValue);
			}

			replayAll();
			assertEquals(returnValue, method.invoke(lps, arguments));
			verifyAll();
		}
	}

	private List<Method> getPrepareStatementMethods() {
		final List<Method> methods = new ArrayList<>();
		for (final Method method : PreparedStatement.class.getMethods()) {
			if (!JAVA_8_EXCLUSIONS.contains(method.toString())) {
				methods.add(method);
			}
		}
		return methods;
	}

	private Object createMockForType(final Class<?> clazz) {
		if (clazz.isPrimitive()) {
			if (clazz.equals(Boolean.TYPE)) {
				return true;
			} else if (clazz.equals(Integer.TYPE)) {
				return 1;
			} else if (clazz.equals(Long.TYPE)) {
				return 1L;
			} else if (clazz.equals(Byte.TYPE)) {
				return (byte) 1;
			} else if (clazz.equals(Short.TYPE)) {
				return (short) 1;
			} else if (clazz.equals(Double.TYPE)) {
				return 1.0;
			} else if (clazz.equals(Float.TYPE)) {
				return 1.0F;
			} else if (clazz.equals(Character.TYPE)) {
				return 'c';
			}
		} else if (clazz.isArray()) {
			return Array.newInstance(clazz.getComponentType(), 0);
		} else if (clazz.equals(Class.class)) {
			return LoggingPreparedStatementTest.class;
		} else if (!clazz.equals(Void.TYPE)) {
			return createMock(clazz);
		}
		return null;
	}
}
