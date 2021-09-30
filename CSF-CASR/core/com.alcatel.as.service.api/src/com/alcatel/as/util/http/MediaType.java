package com.alcatel.as.util.http;

import java.util.Iterator;
import java.util.TreeMap;

/**
 * Wraps a media type : see rfc 2616 section 3.7.
 * <p/>
 * Example : text/html; level=1<br/>
 * "text" is the type<br/>
 * "html" is the subtype<br/>
 * level = 1 is a parameter<br/>
 */
public class MediaType {

    protected String type, subtype;
    protected TreeMap<String, String> params = new TreeMap<String, String>();

    public MediaType(String value) {
	if (value == null)
	    throw new NullPointerException("MediaType is null");
	value = value.trim().toLowerCase();
	String[] tokens = value.split(";");
	parseMediaType(tokens[0]);
	for (int i = 1; i < tokens.length; i++)
	    parseParam(tokens[i]);
	if (!isValid())
	    throw new IllegalArgumentException("Invalid MediaType : " + value);
    }

    protected boolean isValid() {
	return (!type.equals("*") && !subtype.equals("*") && params.get("q") == null);
    }

    private void parseMediaType(String value) {
	int i = value.indexOf('/');
	if (i == 0 || i == value.length() - 1)
	    throw new IllegalArgumentException("Invalid MediaType: " + value);
	type = (i == -1 ? value.trim() : value.substring(0, i).trim());
	subtype = (i == -1 ? "*" : value.substring(i + 1).trim());
    }

    private void parseParam(String value) {
	int i = value.indexOf('=');
	if (i == -1 || i == 0 || i == value.length() - 1)
	    throw new IllegalArgumentException("Invalid MediaType parameter: " + value);
	String pname = value.substring(0, i).trim();
	String pvalue = stripDoubleQuotes(value.substring(i + 1));
	params.put(pname, pvalue);
    }

    public String getType() {
	return type;
    }

    public String getSubType() {
	return subtype;
    }

    public String getParameter(String name) {
	name = name.toLowerCase();
	return params.get(name);
    }

    public boolean equals(Object other) {
	if (other == null)
	    return false;
	if (!(other instanceof MediaType))
	    return false;
	return toString().equals(other.toString());
    }

    public int hashCode() {
	return toString().hashCode();
    }

    public String toString() {
	return toString(new StringBuilder()).toString();
    }

    protected StringBuilder toString(StringBuilder sb) {
	sb.append(type).append('/').append(subtype);
	if (params.size() > 0) { // quick check to save cpu
	    Iterator<String> it = params.navigableKeySet().iterator();
	    while (it.hasNext()) {
		String p = it.next();
		sb.append(';').append(p).append('=').append(params.get(p));
	    }
	}
	return sb;
    }
    
    private static String stripDoubleQuotes(String token) {
	if (token == null) {
	    return null;
	}
	// remove the " if any
	token = token.trim();
	if (token.length() >= 2 && token.charAt(0) == '"' && token.charAt(token.length() - 1) == '"') {
	    return token.substring(1, token.length() - 1);
	} else {
	    return token;
	}
    }
}
