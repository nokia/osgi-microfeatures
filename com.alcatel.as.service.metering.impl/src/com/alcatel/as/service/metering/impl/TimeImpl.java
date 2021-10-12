// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

public class TimeImpl implements Time {
  public long currentTimeMillis() {
    return System.currentTimeMillis();
  }
}
