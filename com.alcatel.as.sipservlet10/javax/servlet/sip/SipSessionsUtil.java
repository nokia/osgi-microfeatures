// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package javax.servlet.sip;

public interface SipSessionsUtil {
    SipApplicationSession getApplicationSessionById(java.lang.String applicationSessionId);
    SipApplicationSession getApplicationSessionKey(java.lang.String key,boolean create);
    SipSession getCorrespondingSipSession(SipSession session, java.lang.String headerName);
}
