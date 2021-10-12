// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.http;

import org.w3c.dom.Node;

import com.nextenso.proxylet.admin.Bearer;
import com.nextenso.proxylet.admin.Protocol;

public class WapBearer extends HttpBearer {
  
  /**
    Builds a new WAP Bearer.
  */
  public WapBearer() {
    super(Protocol.WAP);
  }
  
  public WapBearer(Node node) {
    super(node);
  }
  
  @Override
  public Bearer newBearer(Node node) {
    return new WapBearer(node);
  }
  
}
