package com.alcatel.as.util.http;

/**
 * 
 * A BasicAuthorizationHeader.
 */
public class BasicAuthorizationHeader extends AuthorizationHeader {
	public static final String TYPE_BASIC = "Basic";
	protected String _password;
	public BasicAuthorizationHeader (String username, String password){
		super (TYPE_BASIC, username);
		_password = password;
	}
	public static boolean isBasic (String value){
		return value != null && value.trim ().startsWith ("Basic ");
	}
	public static BasicAuthorizationHeader parse (String value){
		if (!isBasic (value)) throw new IllegalArgumentException ("Not a Basic Authorization Header");
		value = value.trim ();
		if (value.length () == "Basic ".length ()) throw new IllegalArgumentException ("Missing credentials");
		value = value.substring ("Basic ".length ()).trim ();
		value = uudecode (value);
		int i = value.indexOf (':');
		if (i == -1) throw new IllegalArgumentException ("Invalid credentials");
		if (i == 0) {
			if (value.length () == 1) return new BasicAuthorizationHeader ("", "");
			else return new BasicAuthorizationHeader ("", value.substring (1));
		}else{
			if (value.length () == i+1) return new BasicAuthorizationHeader (value.substring (0, i), "");
			else return new BasicAuthorizationHeader (value.substring (0, i), value.substring (i+1));
		}
	}

	/** the base64 characters **/
	private static final int pr2six[] = {
		64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,
		64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,62,64,64,64,63,
		52,53,54,55,56,57,58,59,60,61,64,64,64,64,64,64,64,0,1,2,3,4,5,6,7,8,9,
		10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,64,64,64,64,64,64,26,27,
		28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,
		64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,
		64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,
		64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,
		64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,
		64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,64,
		64,64,64,64,64,64,64,64,64,64,64,64,64
	};
	private static String uudecode (String base64string) {
		StringBuilder ret = new StringBuilder ();
    
		while ((base64string.length () % 4) != 0)
			base64string += "=";           // that should be safe.
		int i = 0;
		int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
		while (i < base64string.length () && pr2six[base64string.charAt (i)] <= 63) {
			c1 = pr2six[base64string.charAt (i)];
			c2 = pr2six[base64string.charAt (i+1)];
			c3 = pr2six[base64string.charAt (i+2)];
			c4 = pr2six[base64string.charAt (i+3)];
			ret.append ((char)(c1 << 2 | c2 >> 4));
			ret.append ((char)((c2 << 4 | c3 >> 2) % 256));
			ret.append ((char)((c3 << 6 | c4) % 256));      
			i += 4;
		}    
    
		if (c3 > 63)
			ret.setLength (ret.length () - 2);
		else if (c4 > 63)
			ret.setLength (ret.length () - 1);
		return ret.toString ();
	}
}