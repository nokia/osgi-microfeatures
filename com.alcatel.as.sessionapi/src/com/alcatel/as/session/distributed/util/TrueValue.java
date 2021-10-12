// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.util;


/**
 * @internal
 * @deprecated
 * An AttributeValue that wraps Boolean.TRUE.
 */
public class TrueValue extends BooleanValue {

  public static final int ATT_VALUE_ID = 0x16;

  public static final TrueValue INSTANCE = new TrueValue ();

  /**
   * Constructor for Externalizable.
   * <br/>Not for use outside serialization. Use TrueValue.INSTANCE instead.
   */
  public TrueValue (){
  }

  public boolean booleanValue (){
    return true;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public String toString (){
    return Boolean.TRUE.toString();
  }
}
