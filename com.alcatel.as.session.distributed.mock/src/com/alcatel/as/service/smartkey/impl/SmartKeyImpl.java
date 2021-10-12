// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.smartkey.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.alcatel.as.session.distributed.smartkey.SmartKey;

public class SmartKeyImpl implements SmartKey, Externalizable {
  private static final long serialVersionUID = 1L;
  
  private String sessionId;
  private int containerId, uid;
  private long[] agentIds;
  
  public SmartKeyImpl() {
  }

  public SmartKeyImpl(String sessionId, int containerId, int uid, long[] agentIds) {
    this.sessionId = sessionId;
    this.containerId = containerId;
    this.uid = uid;
    this.agentIds = agentIds;
  }

  @Override
  public long[] getAgentIds()   {
    return agentIds;
  }

  @Override
  public int getContainerId()   {
    return containerId;
  }

  @Override
  public String getGroupId() {
    int from = sessionId.indexOf('-') +1;
    return sessionId.substring(from, sessionId.indexOf('-', from));
  }

  @Override
  public String getPlatformId() {
    return sessionId.substring(0, sessionId.indexOf('-'));
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  @Override
  public int getUid() {
    return uid;
  }

  @Override
  public String getUserPart() {
    int from = 0;
    for (int i = 0; i < agentIds.length+5; i++) {
      from = sessionId.indexOf('-', from);
      from++;
    }
    if (from > 0) return sessionId.substring(from);
    return null;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeByte(1);  // Version for future usage
    out.writeUTF(sessionId);
    out.writeInt(containerId);
    out.writeInt(uid);
    out.writeInt(agentIds.length);
    for (int i = 0; i < agentIds.length; i++) {
      out.writeLong(agentIds[i]);
    }
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    in.readByte();
    sessionId = in.readUTF();
    containerId = in.readInt();
    uid = in.readInt();
    int nb = in.readInt();
    agentIds = new long[nb];
    for (int i = 0; i < nb; i++) {
      agentIds[i] = in.readLong();
    }
    
  }

  @Override
  public String toString() {
    return "SmartKey [sessionId=" + sessionId + "]";
  }
 
}
