// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.utils;

public class LexerException extends NestedException {
  public LexerException() {
    super();
  }
  
  public LexerException(String s) {
    super(s);
  }
  
  public LexerException(String s, Throwable t) {
    super(s, t);
  }
}
