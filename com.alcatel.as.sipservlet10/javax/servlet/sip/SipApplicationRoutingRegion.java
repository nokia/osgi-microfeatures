// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package javax.servlet.sip;

/**
 * @author  christophe
 */
public final class SipApplicationRoutingRegion implements Comparable<String> {
    public final static SipApplicationRoutingRegion NEUTRAL_REGION=new SipApplicationRoutingRegion("NEUTRAL",SipApplicationRoutingRegionType.NEUTRAL);
    public final static SipApplicationRoutingRegion ORIGINATING_REGION=new SipApplicationRoutingRegion("ORIGINATING",SipApplicationRoutingRegionType.ORIGINATING);
    public final static SipApplicationRoutingRegion TERMINATING_REGION=new SipApplicationRoutingRegion("TERMINATING",SipApplicationRoutingRegionType.TERMINATING);
    String _label;
    SipApplicationRoutingRegionType _type;
    public SipApplicationRoutingRegion(String label,
            SipApplicationRoutingRegionType type) {
        _label=label;
        _type=type;
    }
    public boolean equals(Object obj) {
        if (obj instanceof SipApplicationRoutingRegion) {
            return ((SipApplicationRoutingRegion)obj).getLabel().equals(_label);
        } return _label.equals(obj);
    }
    /**
     * @return the _label
     */
    public String getLabel() {
        return _label;
    }
    /**
     * @return the _type
     */
    public SipApplicationRoutingRegionType getType() {
        return _type;
    }
    public int compareTo(String o) { 
        return o.compareTo(_label);
    }
    public String toString() {
        return _label;
    }
}
