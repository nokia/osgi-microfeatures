// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package javax.servlet.sip;

import javax.servlet.sip.Rel100Exception;
import javax.servlet.sip.SipServletRequest;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;

// Referenced classes of package javax.servlet.sip:
//            SipServletMessage, Rel100Exception, SipServletRequest, Proxy

public interface SipServletResponse
    extends ServletResponse, SipServletMessage
{

    public abstract SipServletRequest getRequest();

    public abstract int getStatus();

    public abstract void setStatus(int i);

    public abstract void setStatus(int i, String s);

    public abstract String getReasonPhrase();

    public abstract ServletOutputStream getOutputStream()
        throws IOException;

    public abstract PrintWriter getWriter()
        throws IOException;

    public abstract Proxy getProxy();

    public abstract void sendReliably()
        throws Rel100Exception;

    public abstract void send()
        throws IOException;

    public abstract SipServletRequest createAck();

    public static final int SC_TRYING = 100;
    public static final int SC_RINGING = 180;
    public static final int SC_CALL_BEING_FORWARDED = 181;
    public static final int SC_CALL_QUEUED = 182;
    public static final int SC_SESSION_PROGRESS = 183;
    public static final int SC_OK = 200;
    public static final int SC_ACCEPTED = 202;
    public static final int SC_MULTIPLE_CHOICES = 300;
    public static final int SC_MOVED_PERMANENTLY = 301;
    public static final int SC_MOVED_TEMPORARILY = 302;
    public static final int SC_USE_PROXY = 305;
    public static final int SC_ALTERNATIVE_SERVICE = 380;
    public static final int SC_BAD_REQUEST = 400;
    public static final int SC_UNAUTHORIZED = 401;
    public static final int SC_PAYMENT_REQUIRED = 402;
    public static final int SC_FORBIDDEN = 403;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_METHOD_NOT_ALLOWED = 405;
    public static final int SC_NOT_ACCEPTABLE = 406;
    public static final int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
    public static final int SC_REQUEST_TIMEOUT = 408;
    public static final int SC_GONE = 410;
    public static final int SC_REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int SC_REQUEST_URI_TOO_LONG = 414;
    public static final int SC_UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int SC_UNSUPPORTED_URI_SCHEME = 416;
    public static final int SC_BAD_EXTENSION = 420;
    public static final int SC_EXTENSION_REQUIRED = 421;
    public static final int SC_INTERVAL_TOO_BRIEF = 423;
    public static final int SC_TEMPORARLY_UNAVAILABLE = 480;
    public static final int SC_CALL_LEG_DONE = 481;
    public static final int SC_LOOP_DETECTED = 482;
    public static final int SC_TOO_MANY_HOPS = 483;
    public static final int SC_ADDRESS_INCOMPLETE = 484;
    public static final int SC_AMBIGUOUS = 485;
    public static final int SC_BUSY_HERE = 486;
    public static final int SC_REQUEST_TERMINATED = 487;
    public static final int SC_NOT_ACCEPTABLE_HERE = 488;
    public static final int SC_REQUEST_PENDING = 491;
    public static final int SC_UNDECIPHERABLE = 493;
    public static final int SC_SERVER_INTERNAL_ERROR = 500;
    public static final int SC_NOT_IMPLEMENTED = 501;
    public static final int SC_BAD_GATEWAY = 502;
    public static final int SC_SERVICE_UNAVAILABLE = 503;
    public static final int SC_SERVER_TIMEOUT = 504;
    public static final int SC_VERSION_NOT_SUPPORTED = 505;
    public static final int SC_MESSAGE_TOO_LARGE = 513;
    public static final int SC_BUSY_EVERYWHERE = 600;
    public static final int SC_DECLINE = 603;
    public static final int SC_DOES_NOT_EXIT_ANYWHERE = 604;
    public static final int SC_NOT_ACCEPTABLE_ANYWHERE = 606;
    SipServletRequest createPrack() throws IllegalStateException,Rel100Exception;
}

