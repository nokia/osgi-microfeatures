// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.agent.event;

public interface AsynchronousEventListener {
  public void asynchronousEvent(Object data, int type);
}
