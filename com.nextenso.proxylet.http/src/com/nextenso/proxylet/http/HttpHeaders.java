// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.http;

import java.util.Enumeration;

/**
 * This Class encapsulates the headers of a message (request or response).
 *
 */
public interface HttpHeaders extends HttpObject {
  
    /**
     * Returns an Enumeration of all the header names.
     * @return an Enumeration of the header names.
     */
    public Enumeration getHeaderNames ();

    /**
     * Returns the raw value of a header.
     * @param name the header name.
     * @return the header value or <code>null</code> if not set.
     */
    public String getHeader (String name);

    /**
     * Returns the raw value of a header.
     * <br/>Returns the specified default value if the header is not set.
     * @param name the header name.
     * @param def the default value.
     * @return the header value or the default value if not set.
     */
    public String getHeader (String name, String def);
  
    /**
     * Returns an Enumeration of the values of a header.
     * <br/>An empty Enumeration is returned if the header is not set.
     * <br/>The comma ',' is used as the delimiter.
     * @param name the header name.
     * @return an Enumeration of the values.
     */
    public Enumeration getHeaders (String name);
  
    /**
     * Returns the integer value of a header.
     * <br/>Returns the specified default value if the header is not set or if the value is not an int.
     * @param name the header name.
     * @param def the default value.
     * @return the header value or the default value.
     */
    public int getIntHeader (String name, int def);

    /**
     * Returns the date value of a header as a long.
     * <br/>Returns -1L if the header is not set or if the value is not a Date.
     * @param name the header name.
     * @return the header value as a long
     */
    public long getDateHeader (String name);


    /**
     * Sets a header value.
     * @param name the header name.
     * @param val the header value.
     */
    public void setHeader (String name, String val);
  
    /**
     * Sets a header value to a specific Date.
     * @param name the header name.
     * @param date the header value.
     */
    public void setDateHeader (String name, long date);
  
    /**
     * Sets a header value as an int.
     * @param name the header name.
     * @param val the header value.
     */
    public void setIntHeader (String name, int val);
  
    /**
     * Adds a header value using ',' as the delimiter.
     * @param name the header name.
     * @param value the header value to add.
     */
    public void addHeader (String name, String value);
  
    /**
     * Adds a date header value using ',' as the delimiter.
     * @param name the header name.
     * @param date the header value to add.
     */
    public void addDateHeader (String name, long date);
  
    /**
     * Adds an integer header value using ',' as the delimiter.
     * @param name the header name.
     * @param val the header value to add.
     */
    public void addIntHeader (String name, int val);
  
    /**
     * Returns the character encoding specified in the content-type header.
     * @return the character encoding or <code>null</code> if not specified.
     */
    public String getCharacterEncoding ();
  
    /**
     * Returns the value of the content-length header.
     * <br/>Returns -1 if the content-length is not specified.
     * @return	the content-length, or -1 if not specified.
     */
    public int getContentLength ();

    /**
     * Returns the value of the content-type header.
     * <br/>This method strips the parameters to return only the MIME type.
     * @return	the content-type or <code>null</code> if not specified.
     */
    public String getContentType ();
  
    /**
     * Sets the content type of the message.
     * <br/>The content type may include the type of character encoding used, for example, 
     * <code>text/html; charset=ISO-8859-4</code>.
     * @param ct the content-type
     */
    public void setContentType (String ct);

    /**
     * Removes the value of a header.
     * @param name the header name
     */
    public String removeHeader (String name);
  
    /**
     * Removes all the headers AND all the cookies.
     */
    public void removeHeaders ();
  
    /**
     * Adds a Cookie to the message ('cookie' header for a request, 'set-cookie' for a response).
     * @param cookie the cookie to add.
     */
    public void addCookie (HttpCookie cookie);
  
    /**
     * Returns the Cookie with the specified name.
     * <br/>If several cookies have the same name, the first one is returned. In such case,
     * <code>getCookies()</code> should be used to retrieve all of them.
     * @param name the cookie name.
     * @return the cookie or <code>null</code> if no cookie matches the name.
     */
    public HttpCookie getCookie (String name);

    /**
     * Returns an Enumeration of the Cookies contained in the message.
     * @return an Enumeration of all the cookies.
     */
    public Enumeration getCookies ();
  
    /**
     * Returns an Enumeration of the Names of the Cookies contained in the message.
     * @return an Enumeration of the cookies names.
     */
    public Enumeration getCookieNames ();

    /**
     * Removes a Cookie from the message.
     * <br/>Does nothing if no such Cookie was present.
     * @param cookie the cookie to remove.
     */
    public void removeCookie (HttpCookie cookie);
  
    /**
     * Removes all the Cookies from the message.
     */
    public void removeCookies ();
  
    /**
     * Adds a Paramater to a header using ';' as the delimiter.
     * @param headerName the name of the header.
     * @param parameterName the name of the parameter.
     * @param parameterValue the value of the parameter.
     */
    public void addParameter (String headerName, String parameterName, String parameterValue);

}
