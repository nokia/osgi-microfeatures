// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

public class CriterionException extends Exception {
  
  public static final int UNKNOWN_REFERENCE = 10;
  public static final int INVALID_LIST = 11;
  public static final int INVALID_SYNTAX = 20;
  public static final int INVALID_REGEXP = 100;
  public static final int INVALID_IP = 110;
  public static final int INVALID_HOST = 120;
  public static final int INVALID_PORT = 130;
  public static final int INVALID_PATH = 140;
  public static final int INVALID_HEADER = 150;
  public static final int INVALID_DAY = 200;
  public static final int INVALID_MONTH = 210;
  public static final int INVALID_DATE = 220;
  public static final int INVALID_TIME = 230;
  public static final int INVALID_SESSION_ATTR = 300;
  public static final int INVALID_MESSAGE_ATTR = 310;
  public static final int INVALID_ALPHABET = 1010;
  public static final int INVALID_CLASS = 1020;
  public static final int INVALID_TYPE = 1030;
  public static final int INVALID_SIDE = 1040;
  public static final int INVALID_APPLICATION = 1050;
  
  private int id = 0;
  
  public CriterionException(int id, String msg) {
    super(msg);
    this.id = id;
  }
  
  public int getErrorId() {
    return id;
  }
}
