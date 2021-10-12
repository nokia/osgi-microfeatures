// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering2.util;

import com.alcatel.as.service.metering2.Monitorable;

public interface MonitorableIterator<T> {
    
    T next (Monitorable monitorable, T ctx);
    
}
