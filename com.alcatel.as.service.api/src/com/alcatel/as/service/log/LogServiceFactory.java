package com.alcatel.as.service.log;

/**
 * Factory service for the LogService
 */
public interface LogServiceFactory
{
    public LogService getLogger(String name);

    public LogService getLogger(Class<?> clazz);
}
