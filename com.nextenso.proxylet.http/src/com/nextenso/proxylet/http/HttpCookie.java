
package com.nextenso.proxylet.http;

import java.io.IOException;
import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;

/**
 * Utility class that wraps a Cookie.
 * <p/>Cookies can be sent by a client (header 'Cookie') or by a server (header 'Set-Cookie').
 */
public class HttpCookie implements Cloneable, Externalizable {

    private String name;
    private String value;
    private String comment;
    private String domain;
    private String path;
    private boolean secure = false;
    private long expDate = Long.MAX_VALUE;
    private int version = 0; // default to version '0' (Netscape)

    /**
     * Creates a new <code>HttpCookie</code> instance.
     * This public empty constructor is only used when processing
     * serialization.
     *
     */
    public HttpCookie() {}
    	
    /**
     * Constructs a cookie with a specified name and value.
     *
     * <p>The name must conform to RFC 2109. That means it can contain 
     * only ASCII alphanumeric characters and cannot contain commas, 
     * semicolons, or white space or begin with a $ character. The cookie's
     * name cannot be changed after creation.
     *
     * <p>The value can be anything the server chooses to send. Its
     * value is probably of interest only to the server. The cookie's
     * value can be changed after creation with the
     * <code>setValue</code> method.
     *
     * <p>By default, cookies are created according to the Netscape
     * cookie specification. The version can be changed with the 
     * <code>setVersion</code> method.
     *
     *
     * @param name 			a <code>String</code> specifying the name of the cookie
     *
     * @param value			a <code>String</code> specifying the value of the cookie
     *
     * @throws IllegalArgumentException	if the cookie name contains illegal characters
     *					(for example, a comma, space, or semicolon)
     *					or it is one of the tokens reserved for use
     *					by the cookie protocol
     * @see #setValue
     * @see #setVersion
     *
     */

    public HttpCookie(String name, String value) {
	checkName(name);
	this.name = name;
	this.value = value;
    }


    /**
     *
     * Specifies a comment that describes a cookie's purpose.
     * The comment is useful if the browser presents the cookie 
     * to the user. Comments
     * are not supported by Netscape Version 0 cookies.
     *
     * @param purpose		a <code>String</code> specifying the comment 
     *				to display to the user
     *
     * @see #getComment
     *
     */

    public void setComment(String purpose) {
	comment = purpose;
    }
    
    
    

    /**
     * Returns the comment describing the purpose of this cookie, or
     * <code>null</code> if the cookie has no comment.
     *
     * @return			a <code>String</code> containing the comment,
     *				or <code>null</code> if none
     *
     * @see #setComment
     *
     */ 

    public String getComment() {
	return comment;
    }
    
    
    


    /**
     *
     * Specifies the domain within which this cookie should be presented.
     *
     * <p>The form of the domain name is specified by RFC 2109. A domain
     * name begins with a dot (<code>.foo.com</code>) and means that
     * the cookie is visible to servers in a specified Domain Name System
     * (DNS) zone (for example, <code>www.foo.com</code>, but not 
     * <code>a.b.foo.com</code>). By default, cookies are only returned
     * to the server that sent them.
     *
     *
     * @param pattern		a <code>String</code> containing the domain name
     *				within which this cookie is visible;
     *				form is according to RFC 2109
     *
     * @see #getDomain
     *
     */

    public void setDomain(String pattern) {
	domain = (pattern != null)? pattern.toLowerCase() : null;	// IE allegedly needs this
    }
    
    
    
    

    /**
     * Returns the domain name set for this cookie. The form of 
     * the domain name is set by RFC 2109.
     *
     * @return			a <code>String</code> containing the domain name
     *
     * @see #setDomain
     *
     */ 

    public String getDomain() {
	return domain;
    }




    /**
     * Sets the maximum age of the cookie in seconds.
     * The value can be positive or negative.
     * It requires that the localhost clock should be correctly set.
     * The special value Integer.MAX_VALUE blanks out the maxAge and expiration date.
     *
     * @see #getMaxAge
     *
     */

    public void setMaxAge(int expiry) {
        if (expiry == Integer.MAX_VALUE){
                expDate = Long.MAX_VALUE;
                return;
        }

	if (expiry == -1) {
	  //
	  // This cookies is not  persistent and will expire 
	  // when the session will be close.
	  //
	  expDate = Long.MAX_VALUE;
	  return;
	}

	if (expiry < 0) {
	  // 
	  // The offset corresponds to a date prior to 1970, 
	  // and this cookies has expired.
	  //
	  expiry = 0; // 1970, 00:00:00 GMT
	}

        long now = System.currentTimeMillis ();
        expDate = now + expiry*1000;
    }




    /**
     * Returns the maximum age of the cookie, specified in seconds,
     * It requires that the localhost clock should be correctly set.
     * The special value Integer.MAX_VALUE means that the value is not set.
     *
     * @see #setMaxAge
     */

    public int getMaxAge() {
        if (expDate == Long.MAX_VALUE)
                return Integer.MAX_VALUE;

	if (expDate < 0) {
	  //
	  // The date is prior to 1970: consider that the max age is reached!
	  //
	  return (0);
	}

        long now = System.currentTimeMillis ();
	long maxAge = (expDate-now)/1000L;
	
	//
	// Avoid negative integer cast if maxAge is upper than Integer.MAX_VALUE-1.
	//
	if (maxAge > Integer.MAX_VALUE-1) {
	  maxAge = (long) Integer.MAX_VALUE-1;
	}

	return ((int) maxAge);
    }
    
    
    

    /**
     * Specifies a path for the cookie
     * to which the client should return the cookie.
     *
     * <p>The cookie is visible to all the pages in the directory
     * you specify, and all the pages in that directory's subdirectories. 
     * A cookie's path must include the servlet that set the cookie,
     * for example, <i>/catalog</i>, which makes the cookie
     * visible to all directories on the server under <i>/catalog</i>.
     *
     * <p>Consult RFC 2109 (available on the Internet) for more
     * information on setting path names for cookies.
     *
     *
     * @param uri		a <code>String</code> specifying a path
     *
     *
     * @see #getPath
     *
     */

    public void setPath(String uri) {
	path = uri;
    }




    /**
     * Returns the path on the server 
     * to which the browser returns this cookie. The
     * cookie is visible to all subpaths on the server.
     *
     *
     * @return		a <code>String</code> specifying a path that contains
     *			a servlet name, for example, <i>/catalog</i>
     *
     * @see #setPath
     *
     */ 

    public String getPath() {
	return path;
    }





    /**
     * Indicates to the browser whether the cookie should only be sent
     * using a secure protocol, such as HTTPS or SSL.
     *
     * <p>The default value is <code>false</code>.
     *
     * @param flag	if <code>true</code>, sends the cookie from the browser
     *			to the server using only when using a secure protocol;
     *			if <code>false</code>, sent on any protocol
     *
     * @see #getSecure
     *
     */
 
    public void setSecure(boolean flag) {
	secure = flag;
    }




    /**
     * Returns <code>true</code> if the browser is sending cookies
     * only over a secure protocol, or <code>false</code> if the
     * browser can send cookies using any protocol.
     *
     * @return		<code>true</code> if the browser uses a secure protocol;
     * 			 otherwise, <code>true</code>
     *
     * @see #setSecure
     *
     */

    public boolean getSecure() {
	return secure;
    }





    /**
     * Returns the name of the cookie. The name cannot be changed after
     * creation.
     *
     * @return		a <code>String</code> specifying the cookie's name
     *
     */

    public String getName() {
	return name;
    }





    /**
     *
     * Assigns a new value to a cookie after the cookie is created.
     * If you use a binary value, you may want to use BASE64 encoding.
     *
     * <p>With Version 0 cookies, values should not contain white 
     * space, brackets, parentheses, equals signs, commas,
     * double quotes, slashes, question marks, at signs, colons,
     * and semicolons. Empty values may not behave the same way
     * on all browsers.
     *
     * @param newValue		a <code>String</code> specifying the new value 
     *
     *
     * @see #getValue
     *
     */

    public void setValue(String newValue) {
	value = newValue;
    }




    /**
     * Returns the value of the cookie.
     *
     * @return			a <code>String</code> containing the cookie's
     *				present value
     *
     * @see #setValue
     *
     */

    public String getValue() {
	return value;
    }




    /**
     * Returns the version of the protocol this cookie complies 
     * with. Version 1 complies with RFC 2109, 
     * and version 0 complies with the original
     * cookie specification drafted by Netscape. Cookies provided
     * by a browser use and identify the browser's cookie version.
     * 
     *
     * @return			0 if the cookie complies with the
     *				original Netscape specification; 1
     *				if the cookie complies with RFC 2109
     *
     * @see #setVersion
     *
     */

    public int getVersion() {
	return version;
    }




    /**
     * Sets the version of the cookie protocol this cookie complies
     * with. Version 0 complies with the original Netscape cookie
     * specification. Version 1 complies with RFC 2109.
     *
     * <p>Since RFC 2109 is still somewhat new, consider
     * version 1 as experimental; do not use it yet on production sites.
     *
     *
     * @param v			0 if the cookie should comply with 
     *				the original Netscape specification;
     *				1 if the cookie should comply with RFC 2109
     *
     * @see #getVersion
     *
     */

    public void setVersion(int v) {
	version = v;
    }
    
    /**
    * Returns the expiration date.
    * The special value Long.MAX_VALUE means that it is not set.
    */
    public long getExpirationDate() {
        return expDate;
    }
    
    /**
    * Sets the expiration date.
    * The special value Long.MAX_VALUE blanks out the maxAge and expiration date.
    */
    public void setExpirationDate(long date) {
      expDate = date;
    }
    

    /**
     * The <code>readExternal</code> method reconstruct an HttpCookie instance
     * from an input stream.
     *
     * @param in an <code>ObjectInput</code> value
     * @exception IOException if an error occurs
     */
    public void readExternal (ObjectInput in) throws IOException, ClassNotFoundException {
        name = (String) in.readObject ();
	value = (String) in.readObject ();
	comment = (String) in.readObject ();
	domain = (String) in.readObject ();
	path = (String) in.readObject ();
	secure = in.readBoolean ();
	expDate = in.readLong ();
	version = in.readInt ();
    }

    /**
     * The <code>writeExternal</code> method writes this HttpCookie
     * to  an output stream.
     *
     * @param out an <code>ObjectOutput</code> value
     * @exception IOException if an error occurs
     */
    public void writeExternal (ObjectOutput out) throws IOException {
        out.writeObject (name);
	out.writeObject (value);
	out.writeObject (comment);
	out.writeObject (domain);
	out.writeObject (path);
	out.writeBoolean (secure);
	out.writeLong (expDate);
	out.writeInt (version);
    }

    // Note -- disabled for now to allow full Netscape compatibility
    // from RFC 2068, token special case characters
    // 
    // private static final String tspecials = "()<>@,;:\\\"/[]?={} \t";

    private static final String tspecials = ",;";
    
    
    

    /*
     * Tests a string and returns true if the string counts as a 
     * reserved token in the Java language.
     * 
     * @param value		the <code>String</code> to be tested
     *
     * @return			<code>true</code> if the <code>String</code> is
     *				a reserved token; <code>false</code>
     *				if it is not			
     */

    private static boolean isToken(String value) {
	int len = value.length();

	for (int i = 0; i < len; i++) {
	    char c = value.charAt(i);

	    if (c < 0x20 || c >= 0x7f || tspecials.indexOf(c) != -1)
		return false;
	}
	return true;
    }

    private static void checkName(String name){
        if (!isToken(name)
		|| name.equalsIgnoreCase("Comment")
		|| name.equalsIgnoreCase("Discard")
		|| name.equalsIgnoreCase("Domain")
		|| name.equalsIgnoreCase("Expires")
		|| name.equalsIgnoreCase("Max-Age")
		|| name.equalsIgnoreCase("Path")
		|| name.equalsIgnoreCase("Secure")
		|| name.equalsIgnoreCase("Version")
	    )
            throw new IllegalArgumentException("Invalid Cookie Name");
    }

    /**
     *
     * Overrides the standard <code>java.lang.Object.clone</code> 
     * method to return a copy of this cookie.
     *		
     *
     */

    public Object clone() {
	try {
	    return super.clone();
	} catch (CloneNotSupportedException e) {
	    throw new RuntimeException(e.getMessage());
	}
    }

    /**
     *
     * Displays this cookie as a string
     *
     *
     */

    public String toString() {
        StringBuffer buf = new StringBuffer();
	buf.append ("HttpCookie [name=").append (name).append (", value=").append (value);
	buf.append (", comment=").append (comment).append (", domain=");
	buf.append (domain).append (", path=").append (path);
	buf.append (", secure=").append (secure);
      
	buf.append (", expDate=");
	String expDateStr = null;

	if (expDate != Long.MAX_VALUE) {
	    try {
	        java.util.Date dt = new java.util.Date (expDate);
		expDateStr = dt.toString ();
	    } catch (Throwable t) {
		expDateStr = null;
	    }
	}
	
	buf.append (expDateStr);
	buf.append (", version=").append (version);
	buf.append (']');
	return (buf.toString ());
    }
}

