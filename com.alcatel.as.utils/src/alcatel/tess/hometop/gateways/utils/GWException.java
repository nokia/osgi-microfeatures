// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

import java.io.IOException;

import alcatel.tess.hometop.gateways.http.HttpUtils;

/**
 * This exception may be used in a gateway implementation.
 * It manage exception chaining (as in jdk 1.4)
 */
public class GWException extends NestedException {
  
  public GWException() {
    super();
  }
  
  public GWException(Throwable rootCause) {
    super(rootCause.getMessage());
  }
  
  public GWException(String debugMessage) {
    super(debugMessage);
  }
  
  public GWException(String debugMessage, Throwable rootCause) {
    super(debugMessage, rootCause);
  }
  
  public GWException(String debugMessage, int httpErrorCode) {
    super(debugMessage);
    this.httpErrorCode = httpErrorCode;
  }
  
  public GWException(String debugMessage, Throwable rootCause, int httpErrorCode) {
    super(debugMessage, rootCause);
    this.httpErrorCode = httpErrorCode;
  }
  
  public int getHttpErrorCode() {
    return (httpErrorCode);
  }
  
  public String getHttpErrorString() {
    return (HttpUtils.getHttpReason(httpErrorCode));
  }
  
  public static void main(String args[]) {
    try {
      try {
        if (true) {
          throw new IOException("could not connect to web server");
        }
      } catch (IOException e) {
        throw new GWException("could not retrieve wml file", e, 503);
      }
    } catch (GWException e) {
      e.printStackTrace();
      System.out.println();
      System.out.println("http error = " + e.getHttpErrorString());
    }
  }
  
  protected int httpErrorCode;
}
