// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

/**
 * This exception is thrown by the mbd parser, in order to interrupt bnd, so it won't generate
 * the target bundle.
 * TODO: check with peter kriens if there is another better way to do this.
 */
@SuppressWarnings("serial")
public class BndPluginError extends Error {
  public BndPluginError(String msg) {
    super(msg);
  }
  
  public BndPluginError(String msg, Throwable cause) {
    super(msg, cause);
  }
}
