package com.alcatel.as.util.http;

/**
 * 
 * Some utilities to manipulate Authorization Header.
 */
public class AuthorizationUtils {
    
	/**
	 * Parses an Authorization header.
	 * @param auth the header value
	 * @param throwExceptionIfInvalid indicates the preference : null or exception upon invalid input
	 * @return the parsed result
	 **/
	public static AuthorizationHeader parseAuthorizationHeader (String auth, boolean throwExceptionIfInvalid){
		if (auth == null){
			if (throwExceptionIfInvalid) throw new NullPointerException ("Value is null");
			return null;
		}
		try{
			if (BasicAuthorizationHeader.isBasic (auth)) return BasicAuthorizationHeader.parse (auth);
			if (DigestAuthorizationHeader.isDigest (auth)) return DigestAuthorizationHeader.parse (auth);
		}catch(RuntimeException e){
			if (throwExceptionIfInvalid) throw e;
			return null;
		}
		if (throwExceptionIfInvalid) throw new IllegalArgumentException ("Unknown Authorization type");
		return null;
	}
}