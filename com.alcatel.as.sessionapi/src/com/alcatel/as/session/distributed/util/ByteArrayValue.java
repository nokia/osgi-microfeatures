package com.alcatel.as.session.distributed.util;

import java.io.*;

/**
 * @internal
 * @deprecated
 * An AttributeValue that wraps a byte array.
 */
public class ByteArrayValue implements AttributeValue {

  public static final int ATT_VALUE_ID = 0x11;

  private byte[] bytes;
  private int off, len;

  public ByteArrayValue (){
  }

  public ByteArrayValue (byte[] bytes, boolean copy){
    this (bytes, 0, bytes.length, copy);
  }

  public ByteArrayValue (byte[] bytes, int off, int len, boolean copy){
    if (copy){
      this.off = 0;
      this.bytes = new byte[len];
      System.arraycopy (bytes, off, this.bytes, 0, len);
    } else {
      this.off = off;
      this.bytes = bytes;
    }
    this.len = len;
  }

  public int getAttributeValueId (){
    return ATT_VALUE_ID;
  }

  public int getOffset (){
    return off;
  }

  public int getLength (){
    return len;
  }

  public byte[] getValue (boolean copy){
    if (copy){
      byte[] data = new byte[len];
      System.arraycopy (bytes, off, data, 0, len);
      return data;
    }
    return bytes;
  }

  public boolean trim (){
    if (off == 0 && len == bytes.length)
      return false;
    byte[] data = new byte[len];
    System.arraycopy (bytes, off, data, 0, len);
    bytes = data;
    off = 0;
    return true;
  }

  public void writeExternal (ObjectOutput out) throws IOException {
    out.writeInt (len);
    out.write (bytes, off, len);
  }

  public void readExternal (ObjectInput in) throws IOException{
    off = 0;
    len = in.readInt ();
    bytes = new byte[len];
    int read = 0;
    while (read < len) {
      int tmp = in.read (bytes, read, len-read);
      if (tmp == -1)
        throw new IOException ("EOF reached");
      read += tmp;
    }
  }

  public boolean equals (Object o){
    if (o instanceof ByteArrayValue){
      ByteArrayValue other = (ByteArrayValue)o;
      if (len != other.len)
        return false;
      for (int i=0; i<len; i++)
        if (bytes[off+i] != other.bytes[other.off+i])
          return false;
      return true;
    }
    return false;
  }

  public int hashCode (){
    int code = 0;
    for (int i=0; i<len; i++)
      code += ((int) bytes[off+i]) ^ i;
    return code;
  }
}
