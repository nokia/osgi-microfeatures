// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.log;

/**
 * Factory service for the LogService
 */
public interface LogServiceFactory
{
    public LogService getLogger(String name);

    public LogService getLogger(Class<?> clazz);
}
