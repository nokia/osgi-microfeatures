package com.alcatel.as.util.http;

import java.util.*;

/**
 * 
 * Some utilities to manipulate Authorization Header.
 */
public class AuthorizationHeader {
	protected String _type, _username;
	protected AuthorizationHeader (String type, String username){
		_type = type;
		_username = username;
	}
	
	public String getType (){ return _type; }
	public String getUsername (){ return _username; }
	public boolean isOfType (String type){ return _type.equals (type);}
}