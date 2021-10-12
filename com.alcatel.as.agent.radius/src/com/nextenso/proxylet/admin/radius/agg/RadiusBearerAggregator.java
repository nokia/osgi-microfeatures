// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.radius.agg;

import com.alcatel.as.management.platform.ConfigManager;
import com.nextenso.proxylet.admin.BearerAggregator;
import com.nextenso.proxylet.admin.radius.RadiusBearer;

public class RadiusBearerAggregator extends BearerAggregator
{
  public RadiusBearerAggregator() throws Exception
  {
    super(new RadiusBearer(), "radiusagent", "radiusagent.proxylets");
  }

  public void bindConfigManager(ConfigManager mgr)
  {
    super.bindConfigManager(mgr);
  }
}
