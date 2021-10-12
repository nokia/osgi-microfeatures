// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.http;

import java.util.*;

/**
 * 
 * A DigestAuthorizationHeader.
 */
public class DigestAuthorizationHeader extends AuthorizationHeader {
	public static final String TYPE_BASIC = "Digest";
	protected String _realm, _nonce, _uri, _qop, _nc, _cnonce, _response, _opaque;
	private DigestAuthorizationHeader (String username){
		super (TYPE_BASIC, username);
	}
	public static boolean isDigest (String value){
		return value != null && value.trim ().startsWith ("Digest ");
	}
	public static DigestAuthorizationHeader parse (String value){
		if (!isDigest (value)) throw new IllegalArgumentException ("Not a Digest Authorization Header");
		value = value.trim ();
		if (value.length () == "Digest ".length ()) throw new IllegalArgumentException ("Missing credentials");
		return new DigestAuthorizationHeader (parseUsername (value));
	}
	
	private static String parseUsername (String auth){
		try{
			int start = auth.indexOf ("username");
			if (start == -1){
				start = auth.toLowerCase ().indexOf ("username");
				if (start == -1)
					return null;
			}
	
			start = trim (auth, start+8);
			if (start == -1)
				return null;
			char c = auth.charAt (start);
			if (c != '=')
				return null;
			start = trim (auth, start+1);
			if (start == -1)
				return null;
	
			c = auth.charAt (start);
			boolean hasSep = (c == '"' || c == '\'');
			char sep = hasSep ? c : ',';
	
			if (hasSep){
				start++;
				if (start == auth.length ())
					return null;
			}

			int end = auth.indexOf (sep, start);
			if (end == -1)
				end = auth.length ();
	
			if (hasSep)
				return auth.substring (start, end);
			return auth.substring (start, end).trim ();
		}catch(Throwable t){
			return null;
		}
	}
	private static int trim (String s, int from){
		while (from < s.length ()){
			if (s.charAt (from) != ' ')
				return from;
			from++;
		}
		return -1;
	}
}