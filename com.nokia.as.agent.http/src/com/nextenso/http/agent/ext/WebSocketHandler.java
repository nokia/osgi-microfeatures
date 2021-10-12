// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.ext;

import java.nio.ByteBuffer;

import com.nextenso.proxylet.http.HttpRequest;

public interface WebSocketHandler
{

  void onFrame(HttpRequest request, ByteBuffer ... buffers);

}
