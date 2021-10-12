// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.http;

import com.alcatel.as.management.platform.ConfigManager;
import com.nextenso.proxylet.admin.BearerAggregator;

public class HttpBearerAggregator extends BearerAggregator {
  public HttpBearerAggregator() throws Exception {
    super(new HttpBearer(), "httpagent", "httpagent.proxylets");
  }
  
  @Override
  public void bindConfigManager(ConfigManager mgr) {
    // super.bindConfigManager(mgr); deprecated - do nothing
  }
}
