// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.util;


/**
 * @internal
 * @deprecated
 * An AttributeValue that wraps Boolean.FALSE.
 */
public class FalseValue extends BooleanValue {

  public static final int ATT_VALUE_ID = 0x17;

  public static final FalseValue INSTANCE = new FalseValue ();

  /**
   * Constructor for Externalizable.
   * <br/>Not for use outside serialization. Use FalseValue.INSTANCE instead.
   */
  public FalseValue (){
  }

  public boolean booleanValue (){
    return false;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public String toString (){
    return Boolean.FALSE.toString();
  }
}
