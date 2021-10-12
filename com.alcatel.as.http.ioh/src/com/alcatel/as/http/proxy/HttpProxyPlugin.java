// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.http.proxy;

import com.alcatel.as.http.parser.HttpMessage;

public interface HttpProxyPlugin {

    public HttpMessage handle (ClientContext ctx, HttpMessage message);
    
}
