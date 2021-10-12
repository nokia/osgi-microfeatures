// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine;

import java.util.Map;

import com.nextenso.proxylet.engine.criterion.CriterionParser;
import com.nextenso.proxylet.admin.Bearer;

public interface DeployerDescriptor {
  String getProtocol();
  
  CriterionParser getParser();
  
  Bearer.Factory getBearerFactory();
  
  String getProxyletsConfiguration();
  
  /**
   * returns a map of expected interfaces for each chain type
   * for instance in http: "request-chain" : {BufferedHttpRequestProxylet.class, BufferedHttpRequestPushlet.class}
   */
  Map<String, Class[]> getBindings();
}
