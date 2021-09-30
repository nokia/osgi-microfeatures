package com.alcatel.as.session.distributed.util;

import java.io.*;

/**
 * @internal
 * @deprecated
 * An AttributeValue that wraps an int array.
 */
public class IntArrayValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x20;

  private int[] ints;

  public IntArrayValue (){
  }

  public IntArrayValue (int[] ints){
    this.ints = ints;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public final int[] getValue (){
    return ints;
  }

  public void writeExternal (ObjectOutput out) throws IOException {
    out.writeInt (ints.length);
    for (int i=0; i<ints.length; i++)
      out.writeInt (ints[i]);
  }

  public void readExternal (ObjectInput in) throws IOException{
    ints = new int[in.readInt ()];
    for (int i=0; i<ints.length; i++)
      ints[i] = in.readInt ();
  }

  public boolean equals (Object o){
    if (o instanceof IntArrayValue){
      IntArrayValue other = (IntArrayValue)o;
      if (ints.length != other.ints.length)
        return false;
      for (int i=0; i<ints.length; i++)
        if (ints[i] != other.ints[i])
          return false;
      return true;
    }
    return false;
  }

  public int hashCode (){
    int code = 0;
    for (int i=0; i<ints.length; i++)
      code += ints[i] ^ i;
    return code;
  }
}
