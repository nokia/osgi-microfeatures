// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package javax.servlet.sip;

import java.io.Serializable;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipApplicationRoutingRegion;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;


/**
 * @author christophe
 *
 */
    public interface SipSession
    extends Serializable
{

    public abstract long getCreationTime();

    public abstract String getId();

    public abstract long getLastAccessedTime();

    public abstract void invalidate();

    public abstract SipApplicationSession getApplicationSession();

    public abstract String getCallId();

    public abstract Address getLocalParty();

    public abstract Address getRemoteParty();

    public abstract SipServletRequest createRequest(String s);

    public abstract void setHandler(String s)
        throws ServletException;

    public abstract Object getAttribute(String s);

    public abstract Enumeration getAttributeNames();

    public abstract void setAttribute(String s, Object obj);

    public abstract void removeAttribute(String s);
    // JSR289 
    public enum State {
        CONFIRMED,EARLY,INITIAL,TERMINATED
    }
    void setOutboundInterface(SipURI uri);
    boolean isValid();
    URI getSubscriberURI();
    SipApplicationRoutingRegion getRegion();
    SipSession.State getState();
    boolean isOngoingTransaction();

    
}
