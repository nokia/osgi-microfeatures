// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.diameter.lb.impl.router;

import com.alcatel.as.diameter.lb.*;
import java.nio.ByteBuffer;
import java.io.*;
import org.apache.log4j.Logger;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.util.*;

public class ImsiDecoder {

    public static final Logger LOGGER = Logger.getLogger ("as.diameter.lb.imsi");

    private static final Base64.Decoder B64Decoder = Base64.getDecoder ();

    private static SecretKeySpec[] KEYS = new SecretKeySpec[16];

    public static final int AVP_USERNAME = 1;
    public static final int APP_ID_EAP = 5;
    public static final int COMMAND_CODE_EAP = 268;

    public static final void initKeys (String keys) {
	if (keys == null) return;
	try{
	    SecretKeySpec[] tmp = new SecretKeySpec[16];
	    BufferedReader reader = new BufferedReader(new StringReader(keys));
	    String line = null;
	    int index = 0;
	    while ((line = reader.readLine ()) != null){
		line = line.trim ();
		if (line.length () == 0 || line.startsWith ("#")) continue;
		byte[] key_as_binary = new byte[line.length () / 2];
		for (int i = 0; i<key_as_binary.length; i++){
		    int v = Integer.parseInt (line.substring (i*2, i*2+2), 16);
		    key_as_binary[i] = (byte)v;
		}
		SecretKeySpec key_as_keyspec = new SecretKeySpec(key_as_binary, "AES");
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug ("ImsiDecoder : loaded key #"+index+" "+line);
		tmp[index] = key_as_keyspec;
		index++;
	    }
	    KEYS = tmp; // swap upon success
	}catch(Exception e){
	    LOGGER.error ("ImsiDecoder : Failed to load encryption keys", e);
	}
    }

    public static final boolean isUsernameAvpCode (int avpCode){
	return avpCode == AVP_USERNAME;
    }

    public static final boolean isEapApplication (DiameterMessage msg){
	return (msg.getApplicationID () == APP_ID_EAP &&
		msg.getCommandCode () == COMMAND_CODE_EAP);
    }

    public static String getUsername (DiameterMessage message, byte[] data, int off, int len) {
	return getUsername (message, data, off, len, false);
    }
    // isTest is set to true in main() tests
    private static String getUsername (DiameterMessage message, byte[] data, int off, int len, boolean isTest) {
	// look for @ and strip the domain
	int len_orig = len;
	for (int i = off; i<(off+len); i++){
	    if (data[i] == (byte) '@'){
		len = i - off;
	    }
	}

	if (isTest) // no message instanciated
	    return getImsi (data, off, len, len_orig);

	if (isEapApplication (message))
	    return getImsi (data, off, len, len_orig);
	
	String imsi = new String (data, off, len, DiameterUtils.ASCII);
	if (LOGGER.isDebugEnabled ())
	    LOGGER.debug ("ImsiDecoder : plain imsi : data= "+dumpHex (data, off, len_orig)+" imsi="+imsi);
	return imsi;
    }

    private static String getImsi (byte[] data, int off, int len, int len_orig){
	try{
	    if (len < 2) throw new IllegalArgumentException ("Encoded IMSI too short : "+len);
	    
	    switch (data[off]){
	    case (byte)'0':
	    case (byte)'6':
		String imsi = new String (data, off+1, len-1, DiameterUtils.ASCII);
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug ("ImsiDecoder : decoded plain imsi : data= "+dumpHex (data, off, len_orig)+" imsi="+imsi);
		return imsi;
	    case (byte)'2':
	    case (byte)'7':
		imsi = decodeImsi (data, off+1, len-1);
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug ("ImsiDecoder : decoded Pseudonym : data= "+dumpHex (data, off, len_orig)+" imsi="+imsi);
		return imsi;
	    case (byte)'4':
	    case (byte)'8':
		imsi = decodeImsi (data, off+1, len-1);
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug ("ImsiDecoder : decoded FastReauth : data= "+dumpHex (data, off, len_orig)+" imsi="+imsi);
		return imsi;
	    default:
		imsi = new String (data, off, len, DiameterUtils.ASCII);
		if (LOGGER.isDebugEnabled ())
		    LOGGER.debug ("ImsiDecoder : plain imsi : data= "+dumpHex (data, off, len_orig)+" imsi="+imsi);
		return imsi;
	    }
	}catch(Exception e){
	    if (LOGGER.isInfoEnabled ())
		LOGGER.info ("ImsiDecoder : failed to decode imsi : "+dumpHex (data, off, len_orig), e);
	    return new String (data, off, len, DiameterUtils.ASCII); // return raw data
	}
    }
    
    private static String decodeImsi(byte [] encrypted_imsi, int off, int len) throws Exception {
        byte[] encrypted_imsi_padded = new byte[len + 2];
        System.arraycopy(encrypted_imsi, off, encrypted_imsi_padded, 2, len);
        encrypted_imsi_padded[0] = (byte) 'A';
        encrypted_imsi_padded[1] = (byte) 'A';

        byte[] encrypted_imsi_as_binary = B64Decoder.decode(encrypted_imsi_padded);

        int key_index = encrypted_imsi_as_binary[1];

        // TODO ? refactor into long-lived variable
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");

        cipher.init(Cipher.DECRYPT_MODE, KEYS[key_index]);

        byte[] imsi_as_binary = cipher
                .doFinal(encrypted_imsi_as_binary, 2, 16);

        String imsi = uncompress_imsi(imsi_as_binary);

        return imsi;
    }
    public static String uncompress_imsi(byte[] compressed_imsi) {
	if ((compressed_imsi[0] & 0xF0) != 0xF0 ||
	    compressed_imsi.length < 8)
	    throw new IllegalArgumentException ("Invalid compressed Imsi : "+dumpHex (compressed_imsi));
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i != 0)
                buffer.append((char) ('0' + ((compressed_imsi[i] >> 4) & 0x0f)));
            buffer.append((char) ('0' + (compressed_imsi[i] & 0x0f)));
        }

        return buffer.toString();
    }


    private static String dumpHex (byte[] data){
	return dumpHex (data, 0, data.length);
    }
    private static String dumpHex (byte[] data, int off, int len){
	StringBuilder s = new StringBuilder ();
	for (int i = off; i<(off+len); i++){
	    String tmp = Integer.toHexString (data[i] & 0xFF);
	    if (tmp.length () == 0) s.append ('0');
	    s.append (tmp);
	}
	return s.toString ();
    }

    public static void main (String[] s) throws Exception {
	
	String[] encrypted_imsis = {
	    "7Cu6uT8FQuLBlfJ4k8puVca",  // key_index=0x0
	    "7+u6uT8FQuLBlfJ4k8puVca",  // key_index=0xF
	    "8Ba7mHCzGnn4DMTdUd+OI3a",
	    "7Cqq+qhzSuhBVoOaiT8yRRB@nokia.com",
	    "2CtSWT1lI0iU+vEGAao7yZD",
	    "4AXxIxCh5PqL+3FR7dBp4Wm",
	    "7DkjSZ625ELHi/HYUmTEKg2@nono.com",
	    "8D9BbfMgJjDxZcl9WeQpf+G"
        };
        int[] key_indexes = {0, 0xf, 0, 0, 0, 0, 0, 0};
        String[] imsis = {"111111111111111", "111111111111111", "111111111111111", "111111111111111",
			  "111111111111234", "111111111111234", "111111111111111", "111111111111111"};
	
	for (int i = 0; i < key_indexes.length; i++) {

	    StringBuilder keys = new StringBuilder ();
	    for (int k = 0; k<16; k++){
		if (k == key_indexes[i])
		    keys.append ("00112233445566778899aabbccddeeff\n");
		else
		    keys.append ("00112233445566778899aabbccdd0000\n");
	    }
	    initKeys (keys.toString ());
	    
	    byte[] encrypted_data = encrypted_imsis[i].getBytes();
	    String imsi           = getUsername(null, encrypted_data, 0, encrypted_data.length, true);

	    if (!imsi.equals(imsis[i])){
		System.out.println ("KO !!!! for test #"+i);
		System.exit (0);
	    }
	}
	System.out.println ("OK !");
    }
}
