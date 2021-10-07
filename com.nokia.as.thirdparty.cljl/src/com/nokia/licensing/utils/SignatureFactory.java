package com.nokia.licensing.utils;

import java.security.NoSuchProviderException;

import javax.xml.crypto.dsig.XMLSignatureFactory;

import com.nokia.licensing.interfaces.LicenseException;


/**
 *
 * @author bogacz
 *
 */
public interface SignatureFactory {
    /**
     * 
     * @param factory
     *            type e.g. DOM
     * @param provider
     *            implementation class
     * @return XMLSignatureFactory
     * @throws NoSuchProviderException
     */
    public XMLSignatureFactory getSignatureFactory(String type, String provider)
            throws NoSuchProviderException, LicenseException;
}
