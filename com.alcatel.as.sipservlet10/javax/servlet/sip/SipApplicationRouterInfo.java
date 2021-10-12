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

/**
 * @author  christophe
 */
public class SipApplicationRouterInfo {
    String _NextApplicationName;
    String _Route;
    Serializable _StateInfo;
    String _SubscriberURI;
    SipApplicationRoutingRegion _SipApplicationRoutingRegion;
    SipRouteModifier _SipRouterModifier;
    public SipApplicationRouterInfo(String nextApplicationName,
            String subscriberURI, String route, SipRouteModifier mod,SipApplicationRoutingRegion region,
            Serializable stateIn) {
        _NextApplicationName=nextApplicationName;
        _SubscriberURI=subscriberURI;
        _Route=route;
        _SipRouterModifier=mod;
        _SipApplicationRoutingRegion=region;
        _StateInfo=stateIn;
    }

    public String getNextApplicationName() {
        return _NextApplicationName;

    }

    public String getRoute() {
        return _Route;

    }

    public Serializable getStateInfo() {
        return _StateInfo;

    }

    public String getSubscriberURI() {
        return _SubscriberURI;

    }

    public SipApplicationRoutingRegion getRoutingRegion() {
        return _SipApplicationRoutingRegion;

    }

    public SipRouteModifier getRouteModifier() {
        return _SipRouterModifier;
    }
    public String toString() {
        StringBuffer buffer=new StringBuffer("RouterInfo:");
        buffer.append(_NextApplicationName);
        buffer.append(",");
        buffer.append(_SubscriberURI);
        buffer.append(",");
        buffer.append(_SipRouterModifier);
        buffer.append(",");
        buffer.append(_SipApplicationRoutingRegion);
        buffer.append(",");
        buffer.append(_StateInfo);
        return buffer.toString();
    }
}
