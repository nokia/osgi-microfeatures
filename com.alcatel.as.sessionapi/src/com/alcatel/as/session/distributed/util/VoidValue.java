// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.session.distributed.util;
import java.io.*;

/**
 * @internal
 * @deprecated
 * This value is used to store an empty attribute value.
 */
public class VoidValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x14;

  public static final VoidValue INSTANCE = new VoidValue ();

  /**
   * Constructor for Externalizable.
   * <br/>Not for use outside serialization. Use VoidValue.INSTANCE instead.
   */
  public VoidValue (){
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public void writeExternal (ObjectOutput out) {
  }

  public void readExternal (ObjectInput in) {
  }

  public String toString (){
    return "VoidValue";
  }
}
