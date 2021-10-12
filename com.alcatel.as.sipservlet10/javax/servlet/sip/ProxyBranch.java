// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package javax.servlet.sip;

import java.util.List;

/**
 * @author christophe
 *
 */
public interface ProxyBranch {

    void setProxyBranchTimeout(int i);
    void cancel() throws IllegalStateException;
    int getProxyBranchTimeout();
    SipURI getRecordRouteURI();
    void setOutboundInterface(SipURI uri);
    SipServletResponse getResponse();
    SipServletRequest getRequest();
    boolean isStarted();
    List<ProxyBranch> getRecursedProxyBranches();
    Proxy getProxy();
}
