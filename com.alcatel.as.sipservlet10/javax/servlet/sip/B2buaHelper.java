// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package javax.servlet.sip;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author christophe
 * 
 */
public interface B2buaHelper {
    SipServletRequest createRequest(SipServletRequest origRequest,
            boolean linked, Map<String, Set<String>> headerMap) throws IllegalArgumentException;

    SipServletRequest createRequest(SipSession session,
            SipServletRequest origRequest, Map<String, Set<String>> headerMap)  throws IllegalArgumentException;

    SipServletResponse createResponseToOriginalRequest(SipSession session,
            int status, String reasonPhrase) throws IllegalArgumentException,IllegalStateException;

    SipSession getLinkedSession(SipSession session) throws IllegalArgumentException;

    SipServletRequest getLinkedSipServletRequest(SipServletRequest req);

    List<SipServletMessage> getPendingMessages(SipSession session, UAMode mode) throws IllegalArgumentException;

    void linkSipSessions(SipSession session1, SipSession session2) throws IllegalArgumentException,NullPointerException;

    void unlinkSipSessions(SipSession session) throws IllegalArgumentException;
}
