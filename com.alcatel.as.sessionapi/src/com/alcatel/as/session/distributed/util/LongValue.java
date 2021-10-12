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
 * An AttributeValue that wraps a long.
 */
public class LongValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x13;

  private long value;
  private String toS;

  public LongValue (){
  }

  public LongValue (long value){
    this.value = value;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public long getValue (){
    return value;
  }

  public void writeExternal (ObjectOutput out) throws IOException {
    out.writeLong (value);
  }

  public void readExternal (ObjectInput in) throws IOException{
    value = in.readLong ();
  }

  public String toString (){
    synchronized (this){
      if (toS == null)
        toS = "LongValue["+value+"]";
    }
    return toS;
  }

  public boolean equals (Object o){
    if (o instanceof LongValue){
      return (((LongValue)o).value == this.value);
    }
    return false;
  }

  public int hashCode (){
    return (int)(value ^ (value >>> 32));
  }
}
