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
