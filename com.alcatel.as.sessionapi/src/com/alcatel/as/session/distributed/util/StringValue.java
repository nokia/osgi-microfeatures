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
 * An AttributeValue that wraps a String.
 */
public class StringValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x15;

  private String value;

  public StringValue (){
  }

  public StringValue (String value){
    if (value == null)
      throw new NullPointerException ("Invalid value (null)");
    this.value = value;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public String getValue (){
    return value;
  }

  public void writeExternal (ObjectOutput out) throws IOException {
    out.writeUTF (value);
  }

  public void readExternal (ObjectInput in) throws IOException{
    value = in.readUTF ();
  }

  public String toString (){
    return "StringValue["+value+"]";
  }

  public boolean equals (Object o){
    if (o instanceof StringValue){
      return ((StringValue)o).value.equals (this.value);
    }
    return false;
  }

  public int hashCode (){
    return value.hashCode ();
  }
}
