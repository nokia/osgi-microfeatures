package com.alcatel.as.session.distributed.util;
import java.io.*;

/**
 * @internal
 * @deprecated
 * An AttributeValue that wraps null.
 */
public class NullValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x10;

  public static final NullValue INSTANCE = new NullValue ();

  /**
   * Constructor for Externalizable.
   * <br/>Not for use outside serialization. Use NullValue.INSTANCE instead.
   */
  public NullValue (){
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public void writeExternal (ObjectOutput out) {
  }

  public void readExternal (ObjectInput in) {
  }

  public String toString (){
    return "NullValue";
  }
}
