package com.nextenso.agent;

public class MuxIdentityPayload {
    /**
     * Unique Id retrieved from SessionManager. We'll send this id when sending mux identification
     * to stacks.
     */
    final String sessionMngrRingId;
    
    /**
     * Topology view ID retrieved from SessionManager
     */
    final Integer viewId;
    
    /**
     * Topology view payload retrieved from SessionManager
     */
    final byte[] viewPayload;
    
    public MuxIdentityPayload(String sessionMngrRingId, Integer viewId, byte[] viewPayload) {
      this.sessionMngrRingId = sessionMngrRingId;
      this.viewId = viewId;
      this.viewPayload = viewPayload;
    }
}
