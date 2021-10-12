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
 * An AttributeValue that wraps an array of Strings.
 */
public class StringArrayValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x18;

  private String[] value;

  public StringArrayValue (){
  }

  public StringArrayValue (String[] value){
    if (value == null)
      throw new NullPointerException ("Invalid value (null)");
    this.value = value;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public String[] getValue (){
    return value;
  }

  public void writeExternal (ObjectOutput out) throws IOException {
    out.writeInt (value.length);
    for (int i=0; i<value.length; i++)
      out.writeUTF (value[i]);
  }

  public void readExternal (ObjectInput in) throws IOException{
    value = new String[in.readInt ()];
    for (int i=0; i<value.length; i++)
      value[i] = in.readUTF ();
  }

  public String toString (){
    StringBuilder buff = new StringBuilder();
    buff.append ("StringArrayValue[");
    String SEP = "";
    for (int i=0; i<value.length; i++){
      buff.append (SEP).append ("value#").append (String.valueOf (i)).append ("=\"").append (value[i]).append ('"');
      SEP = ", ";
    }
    buff.append (']');
    return buff.toString ();
  }

  public boolean equals (Object o){
    if (o instanceof StringArrayValue){
      String[] o_value = ((StringArrayValue)o).value;
      if (value.length != o_value.length)
        return false;
      for (int i=0; i<value.length; i++)
        if (value[i].equals (o_value[i]) == false)
          return false;
      return true;
    }
    return false;
  }

  public int hashCode (){
    int hash = 0;
    for (int i=0; i<value.length; i++)
      hash += value[i].hashCode () ^ (i+1);
    return hash;
  }
}
