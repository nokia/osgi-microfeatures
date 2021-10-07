package com.nsn.ood.cls.util.log;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertSame;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.resetAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.util.log.Loggable.Level;


/**
 * @author marynows
 * 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
		LoggableInterceptor.class, LoggerFactory.class })
public class LoggableInterceptorTest {
	private LoggableInterceptor interceptor;

	@Before
	public void setUp() throws Exception {
		this.interceptor = new LoggableInterceptor();

		mockStatic(LoggerFactory.class);
	}

	@Test
	public void testStandardMethods() throws Throwable {
		testLog("testError", false, new LogMethodMock() {
			@Override
			public void log(final Logger logger, final String message, final Object... arguments) {
				logger.error(message, arguments);
			}
		});
		testLog("testWarn", false, new LogMethodMock() {
			@Override
			public void log(final Logger logger, final String message, final Object... arguments) {
				logger.warn(message, arguments);
			}
		});
		testLog("testInfo", true, new LogMethodMock() {
			@Override
			public void log(final Logger logger, final String message, final Object... arguments) {
				logger.info(message, arguments);
			}
		});
		testLog("testDebug", false, new LogMethodMock() {
			@Override
			public void log(final Logger logger, final String message, final Object... arguments) {
				logger.debug(message, arguments);
			}
		});	
		testLog("testTrace", false, new LogMethodMock() {
			@Override
			public void log(final Logger logger, final String message, final Object... arguments) {
				logger.trace(message, arguments);
			}
		});
	}

	private void testLog(final String methodName, final boolean useDuration, final LogMethodMock logMethodMock)
			throws Throwable {
		resetAll();

		final ProceedingJoinPoint ctxMock = createMock(ProceedingJoinPoint.class);
		final MethodSignature sigMock = createMock(MethodSignature.class);
		final Logger loggerMock = createMock(Logger.class);
		final Object object = new Object();

		expect(ctxMock.getSignature()).andReturn(sigMock);
		expect(sigMock.getMethod()).andReturn(TestLog.class.getMethod(methodName));
		expect(LoggerFactory.getLogger(TestLog.class)).andReturn(loggerMock);
		logMethodMock.log(loggerMock, "{} - entry", methodName);
		expect(ctxMock.proceed()).andReturn(object);
		if (useDuration) {
			logMethodMock.log(loggerMock, eq("{} - exit [{}]"), eq(methodName), isA(Long.class));
		} else {
			logMethodMock.log(loggerMock, "{} - exit", methodName);
		}

		replayAll();
		assertSame(object, this.interceptor.aroundInvoke(ctxMock));
		verifyAll();
	}

	private interface LogMethodMock {
		void log(Logger logger, String message, Object... arguments);
	}

	@Test
	public void testMethodFromBaseClass() throws Throwable {
		final ProceedingJoinPoint ctxMock = createMock(ProceedingJoinPoint.class);
		final MethodSignature sigMock = createMock(MethodSignature.class);
		final Logger loggerMock = createMock(Logger.class);
		final Object object = new Object();

		expect(ctxMock.getSignature()).andReturn(sigMock);
		expect(sigMock.getMethod()).andReturn(DerivedTestLog.class.getMethod("testBase"));
		expect(ctxMock.getTarget()).andReturn(new DerivedTestLog());
		expect(LoggerFactory.getLogger(TestLog.class)).andReturn(loggerMock);
		loggerMock.trace("{} - entry", new Object[] {
			"testBase" });
		expect(ctxMock.proceed()).andReturn(object);
		loggerMock.trace("{} - exit", new Object[] {
			"testBase" });

		replayAll();
		assertSame(object, this.interceptor.aroundInvoke(ctxMock));
		verifyAll();
	}
}

abstract class BaseTestLog {

	public void testBase() {
	};
}

@Loggable
class TestLog extends BaseTestLog {

	@Loggable(Level.ERROR)
	public void testError() {
	}

	@Loggable(Level.WARNING)
	public void testWarn() {
	}

	@Loggable(value = Level.INFO, duration = true)
	public void testInfo() {
	}

	@Loggable(Level.DEBUG)
	public void testDebug() {
	}

	public void testTrace() {
	}
}

class DerivedTestLog extends TestLog {
}
