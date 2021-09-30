/* ========================================== */
/* Copyright (c) 2009 Nokia                   */
/*          All rights reserved.              */
/*          Company Confidential              */
/* ========================================== */

package com.nokia.licensing.utils;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;


/**
 * This class is used to encrypt and decrypt the data
 *
 * @version 1.0
 */
public class LicenseEncrypt {

    /*
     * This method is used to Encrypt the data Arguments :String data to be encrypt return:Arraylist containing
     * encrypted data and the key
     */
    public static ArrayList<byte[]> encryptData(final String data) {
        final ArrayList<byte[]> arrayList = new ArrayList<byte[]>();

        try {
            Cipher desCipher;

            // Create the cipher
            desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");

            final SecretKey desKey = LicenseKeyGenerator.generateKey();

            // Initialize the cipher for encryption
            desCipher.init(Cipher.ENCRYPT_MODE, desKey);

            // Encrypt the cleartext
            final byte[] ciphertext = desCipher.doFinal(data.getBytes());

            arrayList.add(ciphertext);

            final byte[] key = desKey.getEncoded();

            arrayList.add(key);

            return arrayList;
        } catch (final NoSuchAlgorithmException nsae) {

            // TODO Logging needs to be performed
        } catch (final NoSuchPaddingException nspe) {

            // TODO Logging needs to be performed
        } catch (final IllegalBlockSizeException ibze) {

            // TODO Logging needs to be performed
        } finally {
            return arrayList;
        }
    }

    /*
     * This method is used to Encrypt the data Based on the encrypt Key passed value Arguments :String data and
     * EncryptKey to encrypt the data return:byte array containing encrypted data
     */
    public static byte[] encryptData(final String data, final byte[] encryptKey) {
        byte[] ciphertext = null;

        try {
            Cipher desCipher;

            // Create the cipher
            desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");

            final SecretKey desKey = LicenseKeyGenerator.generateKey(encryptKey);

            // Initialize the cipher for encryption
            desCipher.init(Cipher.ENCRYPT_MODE, desKey);

            // Encrypt the cleartext
            ciphertext = desCipher.doFinal(data.getBytes());

            return ciphertext;
        } catch (final NoSuchAlgorithmException nsae) {

            // TODO Logging needs to be performed
        } catch (final NoSuchPaddingException nspe) {

            // TODO Logging needs to be performed
        } catch (final IllegalBlockSizeException ibze) {

            // TODO Logging needs to be performed
        } finally {
            return ciphertext;
        }
    }

    /**
     * This method decrypts the data and returns the cleartext data back to the caller. The first argument is the data
     * to be decrypted and second argument is the encodeKey which is fetched from the database. This encodedKey is then
     * used to generate the SecretKey
     * 
     * @param data
     *            -- Data to be decryoted
     * @param bs
     *            -- The encode key to generate the SecretKey
     * @return String -- Decrypted cleartext data
     * @throws UnsupportedEncodingException
     */
    public static String decryptData(final byte[] ciphertext, final byte[] key) {
        String retValue = "";

        try {
            Cipher desCipher;

            // Create the cipher
            desCipher = Cipher.getInstance("DES/ECB/PKCS5Padding");

            final SecretKey desKey = LicenseKeyGenerator.generateKey(key);

            // Initialize the cipher for encryption
            desCipher.init(Cipher.DECRYPT_MODE, desKey);

            // Encrypt the cleartext
            final byte[] ciphertext1 = desCipher.doFinal(ciphertext);

            retValue = buildString(ciphertext1);

            return retValue;
        } catch (final NoSuchAlgorithmException nsae) {

            // TODO Logging needs to be performed
        } catch (final NoSuchPaddingException nspe) {

            // TODO Logging needs to be performed
        } catch (final IllegalBlockSizeException ibze) {

            // TODO Logging needs to be performed
        } finally {
            return retValue;
        }
    }

    private static String buildString(final byte[] ciphertext) {
        final StringBuffer sb = new StringBuffer();

        for (final byte element : ciphertext) {
            sb.append((char) element);
        }

        return sb.toString();
    }
}
