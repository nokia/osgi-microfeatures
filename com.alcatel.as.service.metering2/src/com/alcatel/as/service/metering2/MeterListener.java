// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2;

/**
 * A Meter listener is called by timer-based (periodic) or changed-based {@link MonitoringJob}.
 * @param <C> the type of listener context which can be passed when creating a Monitoring job using 
 */
public interface MeterListener<C> {
  /**
   * Callback for notification of a {@link Meter} change
   * @param meter The meter that has changed
   * @param context the context that has been passed when creating the corresponding monitoring job
   * @return the listener context, which will be updated in the monitoring job, or null
   */
  C updated(Meter meter, C context);
}
