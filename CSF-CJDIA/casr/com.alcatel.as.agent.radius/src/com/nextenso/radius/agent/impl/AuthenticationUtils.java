package com.nextenso.radius.agent.impl;

import java.io.BufferedReader;
import java.io.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.ByteBuffer;
import alcatel.tess.hometop.gateways.utils.ConfigException;

import com.nextenso.proxylet.radius.AuthenticationManager;
import com.nextenso.radius.agent.Utils;

public class AuthenticationUtils {

	private static final Logger LOGGER = Logger.getLogger("agent.radius.authentication");

	private static final Random RANDOM = new Random(System.currentTimeMillis());
	private static final byte[] VOID_16 = new byte[16];

	public static void parseSecrets(String data)
		throws ConfigException {
		List<RadiusSecret> list = new ArrayList<RadiusSecret>();
		try {
			BufferedReader reader = new BufferedReader(new StringReader(data));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("init: secret line to be parsed:[" + line + "]");
				}
				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}
				RadiusSecret secret = new RadiusSecret(line);
				list.add(secret);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("init: Registered secret=" + secret);
				}
			}
			reader.close();
		}
		catch (Throwable t) {
			throw new ConfigException("Invalid client secrets", t);
		}

		AuthenticationManager manager = Utils.getAuthenticationManager();
		manager.addAll(list);
	}


	public static boolean compare(byte[] b1, int off1, byte[] b2, int off2, int len) {
		for (int i = 0; i < len; i++) {
			if (b1[off1 + i] != b2[off2 + i])
				return false;
		}
		return true;
	}

	protected static boolean authenticate(MessageDigest digest, byte[] message, int off, int len, byte[] secret, byte[] authenticator) {
		ByteBuffer buffer = new ByteBuffer(len);

		// append the first line
		buffer.append(message, off, 4);

		// append the specified Authenticator
		buffer.append(authenticator);

		// append the Attributes
		int left = len - 20;
		if (left > 0)
			buffer.append(message, off + 20, left);

		// append the secret
		buffer.append(secret);

		// generate the Authenticator
		byte[] digested = digest(digest, buffer);

		// compare
		return compare(digested, 0, message, off + 4, 16);
	}
	
	public static byte[] digest(MessageDigest digest, ByteBuffer buffer) {
		return digest(digest, buffer.toByteArray(false), 0, buffer.size());
	}

	private static byte[] digest(MessageDigest digest, byte[] data, int off, int len) {
		digest.reset();
		digest.update(data, off, len);
		return digest.digest();
	}

	public static boolean authenticate(MessageDigest digest, byte[] request, int off, int len, byte[] secret) {
			return authenticate(digest, request, off, len, secret, VOID_16);
	}

	// used for Acct requests (rfc 5176)
	public static boolean checkRequestMessageAuthenticator(byte[] message, int off, int len, int msgAuthOffset, String secret) {
		return checkMessageAuthenticator (message, off, len, msgAuthOffset, VOID_16, 0, secret);
	}
	public static boolean checkMessageAuthenticator(byte[] message, int off, int len, int msgAuthOffset, byte[] authenticator, String secret) {
		return checkMessageAuthenticator (message, off, len, msgAuthOffset, authenticator, 0, secret);
	}
	public static boolean checkMessageAuthenticator(byte[] message, int off, int len, int msgAuthOffset, byte[] authenticator, int authenticatorOff, String secret) {
		// we use the rfc 5176/3579 algo to check it since 2869 is not clear
		ByteBuffer buffer = new ByteBuffer(len);

		// append the first line
		buffer.append(message, off, 4);

		// append the authenticator
		buffer.append(authenticator, authenticatorOff, 16);
		
		// append the Attributes but wipe out Message-Authenticator
		int attrsOffet = off + 20;
		buffer.append (message, attrsOffet, msgAuthOffset);
		buffer.append (VOID_16, 0, 16);
		int left = len - 20 - msgAuthOffset - 16;
		if (left > 0){
		    buffer.append(message, attrsOffet + msgAuthOffset + 16, left);
		}
		//System.out.println ("digest :\n"+buffer);
		
		// generate the Authenticator
		byte[] digested = hmacDigest (buffer.toByteArray (true), secret);
		//ByteBuffer b = new ByteBuffer (16); b.append (digested);
		//System.out.println ("digested :\n"+b);
		
		// compare
		return compare(digested, 0, message, attrsOffet + msgAuthOffset, 16);
	}

	public static byte[] getRandomAuthenticator() {
		byte[] bytes = new byte[16];
		synchronized (RANDOM) {
			for (int i = 0; i < 16; i++)
				bytes[i] = (byte) RANDOM.nextInt(256);
		}
		return bytes;
	}

    public static void main (String[] s) throws Exception {
	// test code to test message-authenticator parsing
	// change RadiusMessageFacade to activate it (return true in constructor)
	byte[] bytes = new byte[]{
	    (byte)0x04, (byte)0x59, (byte)0x00, (byte)0xA2, (byte)0x12, (byte)0x61, (byte)0x5B, (byte)0xA2, (byte)0x22, (byte)0x99, (byte)0x51, (byte)0x56, (byte)0x3C, (byte)0x40, (byte)0xFA, (byte)0x6B,
	    (byte)0xD6, (byte)0x5B, (byte)0xF3, (byte)0xD0, (byte)0x2C, (byte)0x13, (byte)0x34, (byte)0x44, (byte)0x32, (byte)0x42, (byte)0x42, (byte)0x38, (byte)0x41, (byte)0x43, (byte)0x2D, (byte)0x30,
	    (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x30, (byte)0x39, (byte)0x38, (byte)0x28, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x2D, (byte)0x06, (byte)0x00,
	    (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x01, (byte)0x07, (byte)0x61, (byte)0x6C, (byte)0x69, (byte)0x63, (byte)0x65, (byte)0x05, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0A,
	    (byte)0x1E, (byte)0x1F, (byte)0x30, (byte)0x30, (byte)0x2D, (byte)0x30, (byte)0x32, (byte)0x2D, (byte)0x36, (byte)0x46, (byte)0x2D, (byte)0x41, (byte)0x41, (byte)0x2D, (byte)0x41, (byte)0x41,
	    (byte)0x2D, (byte)0x41, (byte)0x41, (byte)0x3A, (byte)0x4D, (byte)0x79, (byte)0x20, (byte)0x57, (byte)0x69, (byte)0x72, (byte)0x65, (byte)0x6C, (byte)0x65, (byte)0x73, (byte)0x73, (byte)0x1F,
	    (byte)0x13, (byte)0x30, (byte)0x30, (byte)0x2D, (byte)0x31, (byte)0x43, (byte)0x2D, (byte)0x42, (byte)0x33, (byte)0x2D, (byte)0x41, (byte)0x41, (byte)0x2D, (byte)0x41, (byte)0x41, (byte)0x2D,
	    (byte)0x41, (byte)0x41, (byte)0x3D, (byte)0x06, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x13, (byte)0x4D, (byte)0x18, (byte)0x43, (byte)0x4F, (byte)0x4E, (byte)0x4E, (byte)0x45, (byte)0x43,
	    (byte)0x54, (byte)0x20, (byte)0x34, (byte)0x38, (byte)0x4D, (byte)0x62, (byte)0x70, (byte)0x73, (byte)0x20, (byte)0x38, (byte)0x30, (byte)0x32, (byte)0x2E, (byte)0x31, (byte)0x31, (byte)0x62,
	    (byte)0x50, (byte)0x12, (byte)0x47, (byte)0x93, (byte)0xDD, (byte)0x67, (byte)0xD3, (byte)0xA5, (byte)0x43, (byte)0xCB, (byte)0x0B, (byte)0x48, (byte)0x27, (byte)0x9F, (byte)0xC6, (byte)0x56,
	    (byte)0x34, (byte)0xFE
	};
	
	ByteArrayInputStream in = new ByteArrayInputStream(bytes);
	AccountingRequestFacade req = new AccountingRequestFacade (1, true);
	RadiusInputStream.readAttributes (req, bytes, 20, bytes.length - 20);
	System.out.println (checkRequestMessageAuthenticator (bytes, 0, bytes.length, req.getMessageAuthenticatorOffset (), "secret"));
	ByteArrayOutputStream baos = new ByteArrayOutputStream ();
	req.writeTo (baos);
	req = new AccountingRequestFacade (1, true);
	bytes = baos.toByteArray ();
	RadiusInputStream.readAttributes (req, bytes, 20, bytes.length - 20);
	System.out.println (checkRequestMessageAuthenticator (bytes, 0, bytes.length, req.getMessageAuthenticatorOffset (), "secret"));
    }

    private static final String HMAC_MD5_ALGO = "HmacMD5";
    public static byte[] hmacDigest(byte[] msg, String keyString) {
	return hmacDigest (msg, 0, msg.length, keyString);
    }
    public static byte[] hmacDigest(byte[] msg, int off, int size, String keyString) {
	try {
	    SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), HMAC_MD5_ALGO);
	    Mac mac = Mac.getInstance(HMAC_MD5_ALGO);
	    mac.init(key);
	    mac.update(msg, off, size);
	    byte[] bytes = mac.doFinal ();
	    return bytes;
	} catch (UnsupportedEncodingException e) {
	} catch (InvalidKeyException e) {
	} catch (NoSuchAlgorithmException e) {
	    // java/jre/lib/ext/sunjce_provider.jar not in the classpath
	    throw new RuntimeException ("java/jre/lib/ext/sunjce_provider.jar not in the classpath");
	} // Exception cannot happen
	return null;
    }
}
