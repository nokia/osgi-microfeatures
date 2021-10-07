package com.nokia.licensing.utils;

import java.security.NoSuchProviderException;
import java.security.Provider;

import javax.xml.crypto.dsig.XMLSignatureFactory;

import com.nokia.licensing.interfaces.LicenseException;
import com.nokia.licensing.logging.LicenseLogger;


/**
 *
 * @author bogacz
 *
 */
public class SignatureFactoryImpl implements SignatureFactory {

    private static final String EXCEPTION_TEXT = "Could not determine xml digsig provider";

    /**
     * {@inheritDoc}
     */
    @Override
    public XMLSignatureFactory getSignatureFactory(final String type, final String provider)
            throws NoSuchProviderException, LicenseException {

        if (provider == null) {
            return XMLSignatureFactory.getInstance(type);
        } else {
            final Provider providerInstance = getProvider(provider);
            return XMLSignatureFactory.getInstance(type, providerInstance);
        }
    }

    /**
     * Instantiates provider using reflection
     * 
     * @param provider
     * @return Provider of XML digital signature factory
     * @throws NoSuchProviderException
     * @throws LicenseException
     */
    private Provider getProvider(final String provider) throws LicenseException {
        final String methodName = "provider";
        try {
            return (Provider) Class.forName(provider).newInstance();
        } catch (final InstantiationException e) {
            LicenseLogger.getInstance().error(SignatureFactoryImpl.class.getName(), methodName, e.getMessage(), e);
            throw new LicenseException(EXCEPTION_TEXT);
        } catch (final IllegalAccessException e) {
            LicenseLogger.getInstance().error(SignatureFactoryImpl.class.getName(), methodName, e.getMessage(), e);
            throw new LicenseException(EXCEPTION_TEXT);
        } catch (final ClassNotFoundException e) {
            LicenseLogger.getInstance().error(SignatureFactoryImpl.class.getName(), methodName, e.getMessage(), e);
            throw new LicenseException(EXCEPTION_TEXT);
        }
    }
}
