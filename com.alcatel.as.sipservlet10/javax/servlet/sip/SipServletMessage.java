// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package javax.servlet.sip;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Locale;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipSession;

public interface SipServletMessage
{

    public abstract Address getFrom();

    public abstract Address getTo();

    public abstract String getMethod();

    public abstract String getProtocol();

    public abstract String getHeader(String s);

    public abstract ListIterator getHeaders(String s);

    public abstract Iterator getHeaderNames();

    public abstract void setHeader(String s, String s1);

    public abstract void addHeader(String s, String s1);

    public abstract void removeHeader(String s);

    public abstract Address getAddressHeader(String s)
        throws ServletParseException;

    public abstract ListIterator getAddressHeaders(String s)
        throws ServletParseException;

    public abstract void setAddressHeader(String s, Address address);

    public abstract void addAddressHeader(String s, Address address, boolean flag);

    public abstract String getCallId();

    public abstract int getExpires();

    public abstract void setExpires(int i);

    public abstract String getCharacterEncoding();

    public abstract void setCharacterEncoding(String s)
        throws UnsupportedEncodingException;

    public abstract int getContentLength();

    public abstract String getContentType();

    public abstract byte[] getRawContent()
        throws IOException;

    public abstract Object getContent()
        throws IOException, UnsupportedEncodingException;

    public abstract void setContent(Object obj, String s)
        throws UnsupportedEncodingException;

    public abstract void setContentLength(int i);

    public abstract void setContentType(String s);

    public abstract Object getAttribute(String s);

    public abstract Enumeration getAttributeNames();

    public abstract void setAttribute(String s, Object obj);

    public abstract SipSession getSession();

    public abstract SipSession getSession(boolean flag);

    public abstract SipApplicationSession getApplicationSession();

    public abstract SipApplicationSession getApplicationSession(boolean flag);

    public abstract Locale getAcceptLanguage();

    public abstract Iterator getAcceptLanguages();

    public abstract void setAcceptLanguage(Locale locale);

    public abstract void addAcceptLanguage(Locale locale);

    public abstract void setContentLanguage(Locale locale);

    public abstract Locale getContentLanguage();

    public abstract void send()
        throws IOException;

    public abstract boolean isSecure();

    public abstract boolean isCommitted();

    public abstract String getRemoteUser();

    public abstract boolean isUserInRole(String s);

    public abstract Principal getUserPrincipal();

    public abstract String getLocalAddr();

    public abstract int getLocalPort();

    public abstract String getRemoteAddr();

    public abstract int getRemotePort();

    public abstract String getTransport();
    // JSR289 
    void setParameterableHeader(String name, Parameterable param)  throws IllegalArgumentException;
    Parameterable getParameterableHeader(String name) throws ServletParseException;
    ListIterator<Parameterable> getParameterableHeaders(String name)    throws ServletParseException;
    void addParameterableHeader(String name,Parameterable param,boolean first) throws IllegalArgumentException;
}

