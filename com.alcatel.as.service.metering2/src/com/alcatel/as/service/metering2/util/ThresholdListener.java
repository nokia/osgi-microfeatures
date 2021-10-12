// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.util;

import com.alcatel.as.service.metering2.Meter;

public interface ThresholdListener<C> {
  public C above(long threshold, Meter meter, C ctx);
  
  public C below(long threshold, Meter meter, C ctx);
}
