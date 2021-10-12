// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.admin.diameter.beareragg;

import com.alcatel.as.management.platform.ConfigManager;
import com.nextenso.proxylet.admin.BearerAggregator;
import com.nextenso.proxylet.admin.diameter.DiameterBearer;

public class DiameterBearerAggregator extends BearerAggregator
{
  public DiameterBearerAggregator() throws Exception
  {
    super(new DiameterBearer(), "diameteragent", "diameteragent.proxylets");
  }

  @Override 
  public void bindConfigManager(ConfigManager mgr)
  {
    super.bindConfigManager(mgr);
  }
}
