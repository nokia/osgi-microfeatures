/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.utils;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


/**
 * @version 1.0
 *
 */
public class LicenseKeyGenerator {

    /**
     * This method generates a SecretKey (Symmetric Key) based on DES Algorithm. This key is used for encryption of
     * information
     * 
     * @return
     */
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        try {
            final KeyGenerator keygen = KeyGenerator.getInstance("DES");
            final SecretKey desKey = keygen.generateKey();

            return desKey;
        } catch (final NoSuchAlgorithmException nsae) {

            // Logging to be implemented
            throw nsae;
        }
    }

    /**
     * This method accepts a key information in an encoded format and generates a SecretKey instance based on the
     * encoded instance.
     * 
     * @param encodedFormat
     * @return SecretKey -- SecretKey instance from the encoded data
     */
    public static SecretKey generateKey(final byte[] encodedFormat) throws NoSuchAlgorithmException {
        final SecretKey desKey = new SecretKeySpec(encodedFormat, "DES");

        return desKey;
    }
}
