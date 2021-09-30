package com.alcatel.as.service.metering2.util;

import com.alcatel.as.service.metering2.Monitorable;

public interface MonitorableIterator<T> {
    
    T next (Monitorable monitorable, T ctx);
    
}
