// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.http.agent.impl;

@SuppressWarnings("serial")
public class HttpMessageException extends RuntimeException {
    public HttpMessageException(String msg) {
      super(msg);
    }
    
    public HttpMessageException(Throwable cause) {
      super(cause);
    }
    
    public HttpMessageException(String msg, Throwable cause) {
      super(msg, cause);
    }
}
