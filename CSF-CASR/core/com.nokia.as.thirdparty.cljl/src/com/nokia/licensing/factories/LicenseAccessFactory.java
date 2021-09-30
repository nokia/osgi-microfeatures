/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.factories;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.nokia.licensing.interfaces.LicenseAccess;
import com.nokia.licensing.interfaces.LicenseCancelDataStorage;
import com.nokia.licensing.interfaces.LicenseDataStorage;
import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;


/**
 * This is the Factory class used to fetch the instance of LicenseAccess. Method in the factory accepts Instance
 * DataStorage and CancelStorage Implementation and returns instance of LinceseAccess implementation It is a Singleton
 * implementation.
 *
 * @version 1.0
 */
public class LicenseAccessFactory {
	private static final String ACCESS = "access";
	private static final String ERROR_CODE_SET_TO = "error code set to: ";
	private static final String GET_INSTANCE = "getInstance";
	private static LicenseAccess licenseAccess = null;

	/**
	 * Instantiates the LicenseAccess implementation and returns the instance of LicensesAccess. It also sets the
	 * reference to LicenseDataStorage and LicenseCancelDataStorage
	 * 
	 * @param dataStorage
	 *            -- LicenseDataStorage instance
	 * @param cancelStorage
	 *            -- LicenseCancelDataStorage instance
	 * @return LicenseAccess -- Instance of LicenseAccess Interface
	 * @throws LicenseException
	 */
	public LicenseAccess getInstance(final LicenseDataStorage dataStorage,
			final LicenseCancelDataStorage cancelDataStorage) throws LicenseException {
		LicenseLogger.getInstance().finest(this.getClass().getName(), ACCESS, "getInstance() entered ");

		final String licenseAccessClass = "com.nokia.licensing.impl.LicenseAccessImpl";
		LicenseAccess invoker = null;

		try {
			if (licenseAccess == null) {
				final Constructor<?> constructor = Class.forName(licenseAccessClass).getConstructor(new Class[] {
						LicenseDataStorage.class, LicenseCancelDataStorage.class });

				invoker = (LicenseAccess) (constructor.newInstance(new Object[] {
						dataStorage, cancelDataStorage }));
			}
		} catch (final ClassNotFoundException cnfe) {
			handleException("ClassNotFound Exception", "CLJL123");
		} catch (final InstantiationException ie) {
			handleException("Instantiation Exception", "CLJL125");
		} catch (final IllegalAccessException iae) {
			handleException("IllegalAccess Exception", "CLJL126");
		} catch (final IllegalArgumentException iare) {
			handleException("IllegalArgument Exception", "CLJL124");
		} catch (final InvocationTargetException ite) {
			handleException("InvocationTarget Exception", "CLJL121");
		} catch (final NoSuchMethodException nme) {
			handleException("NoSuchMethod Exception", "CLJL122");
		}
		return invoker;
	}

	private void handleException(final String reasone, final String errorCode) throws LicenseException {
		LicenseLogger.getInstance().error(this.getClass().getName(), ACCESS, reasone);

		final LicenseException ex = new LicenseException(" " + reasone);

		ex.setErrorCode(errorCode);
		LicenseLogger.getInstance().error(this.getClass().getName(), GET_INSTANCE, ERROR_CODE_SET_TO + ex.getErrorCode());

		throw ex;
	}
}
