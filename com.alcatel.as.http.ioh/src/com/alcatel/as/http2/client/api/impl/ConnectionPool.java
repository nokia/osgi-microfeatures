// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http2.client.api.impl;

import com.alcatel.as.http2.client.Http2Connection;
import com.alcatel.as.http2.client.api.HttpRequest;

import java.util.concurrent.CompletableFuture;

public interface ConnectionPool {

    void get(CompletableFuture<Http2Connection> cf_conn, HttpRequest request);

    enum ClosePolicy {
        CLOSE_FORCIBLY,
        CLOSE_GRACEFULLY,
        CLOSE_GRACEFULLY_SECURE
    }

    void close(ClosePolicy policy);

}
