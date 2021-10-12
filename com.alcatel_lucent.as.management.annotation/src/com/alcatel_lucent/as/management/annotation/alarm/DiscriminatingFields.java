// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.alarm ;

/**
 * Alarm discriminating fields
 */
public interface DiscriminatingFields {
  /** Discriminating field: source host */
  public static final int HOST = 1 ;
  /** Discriminating field: source instance */
  public static final int INSTANCE = 2 ;
  /** Discriminating field: user information 1 */
  public static final int USER1 = 4 ;
  /** Discriminating field: user information 2 */
  public static final int USER2 = 8 ;
  /** Discriminating field: default setting */
  public static final int DEFAULT = 3 ;
  /** Discriminating field: Maximum valid value */
  public static final int MAXVAL = HOST | INSTANCE | USER1 | USER2 ;
}
