// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.shutdown;

/**
 * An interface to any contributor to the graceful shutdown mechanism
 * 
 */
public interface Shutdownable {

    /**
     * Called by the shutdown service when a shutdown sequence was launched.
     * @param shutdown the shutdown sequence : call it back to notify that it may resume
     */
    void shutdown(Shutdown shutdown);
}
