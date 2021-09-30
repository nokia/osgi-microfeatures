package com.alcatel.as.service.log;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Provides a lightweight Log4j wrapper API, without the need for isXXEnabled methods ...
 */
@ProviderType
public interface LogService
{
    public void error(String format, Object... args);

    public void error(String format, Throwable t, Object... args);

    public void warn(String format, Object... args);

    public void warn(String format, Throwable t, Object... args);

    public void info(String format, Object... args);

    public void info(String format, Throwable t, Object... args);

    public void debug(String format, Object... args);

    public void debug(String format, Throwable t, Object... args);
    
    public void trace(String format, Object... args);

    public void trace(String format, Throwable t, Object... args);

    public boolean isDebugEnabled();

    public boolean isInfoEnabled();

    public boolean isTraceEnabled();
}
