// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.concurrent;

import java.util.concurrent.ScheduledExecutorService;

/**
 * A PlatformExecutor, used to schedule timers in a given platform executor.
 * @deprecated use {@link TimerService} service instead of this class.
 */
@Deprecated
public interface PlatformExecutorCompatibility extends ScheduledExecutorService {
  
}
