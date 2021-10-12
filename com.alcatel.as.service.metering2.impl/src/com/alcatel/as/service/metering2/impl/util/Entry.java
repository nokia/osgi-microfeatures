// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.impl.util;

import com.alcatel.as.service.concurrent.SerialExecutor;
import com.alcatel.as.service.metering2.MeteringService;
import com.alcatel.as.service.metering2.util.MeteringRegistry;

public interface Entry {
	public void start (MeteringRegistry reg, MeteringService srv, SerialExecutor serial); //maybe called many times
	public void stop ();
}
