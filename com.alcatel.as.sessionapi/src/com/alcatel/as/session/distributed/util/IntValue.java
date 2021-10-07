package com.alcatel.as.session.distributed.util;
import java.io.*;

/**
 * @internal
 * @deprecated
 * An AttributeValue that wraps an int.
 */
public class IntValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x12;

  private int value;
  private String toS;

  public IntValue (){
  }

  public IntValue (int value){
    this.value = value;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public final int getValue (){
    return value;
  }

  public void writeExternal (ObjectOutput out) throws IOException {
    out.writeInt (value);
  }

  public void readExternal (ObjectInput in) throws IOException{
    value = in.readInt ();
  }

  public String toString (){
    synchronized (this){
      if (toS == null)
        toS = "IntValue["+value+"]";
    }
    return toS;
  }

  public boolean equals (Object o){
    if (o instanceof IntValue){
      return (((IntValue)o).value == this.value);
    }
    return false;
  }

  public int hashCode (){
    return value;
  }
}
