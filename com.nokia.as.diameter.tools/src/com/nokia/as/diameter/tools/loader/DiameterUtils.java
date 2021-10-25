// // Copyright 2000-2021 Nokia
// //
// // Licensed under the Apache License 2.0
// // SPDX-License-Identifier: Apache-2.0
// //
//
//

package com.nokia.as.diameter.tools.loader;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.AddressFormat;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.*;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

public class DiameterUtils {
  static final AtomicInteger ID = new AtomicInteger((int) (System.currentTimeMillis() & 0xFFF) << 20);
  
  //static final AtomicInteger ID = new AtomicInteger(0);
  
  public static boolean isNOK(ByteBuffer rcvBuf) {
    return rcvBuf.get(rcvBuf.position() + 5) == (byte) 0xFF && rcvBuf.get(rcvBuf.position() + 6) == (byte) 0xFF
        && rcvBuf.get(rcvBuf.position() + 7) == (byte) 0xFF;
  }

  public static boolean isDPA(ByteBuffer rcvBuf) {
    return rcvBuf.get(rcvBuf.position() + 5) == (byte) 0x00 && rcvBuf.get(rcvBuf.position() + 6) == (byte) 0x01
        && rcvBuf.get(rcvBuf.position() + 7) == (byte) 0x1A && rcvBuf.get(rcvBuf.position() + 4) == (byte) 0x00;
  }
  
  public static byte[] makeCER(String originHost) {
    // make a CER
    String realm = "realm.com";
    originHost = originHost + "." + realm;
    
    List<DiameterAVP> list = new ArrayList<DiameterAVP>();
    DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
    avp.setValue(IdentityFormat.toIdentity(originHost), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
    avp.setValue(IdentityFormat.toIdentity(realm), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
    avp.setValue(Unsigned32Format.toUnsigned32(0), false);
    list.add(avp);

    avp = new DiameterAVP(DiameterBaseConstants.AVP_PRODUCT_NAME);
    avp.setValue(IdentityFormat.toIdentity("LoadTool"), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_HOST_IP_ADDRESS);
    avp.setValue(AddressFormat.toAddress(1, new byte[] { 127, 0, 0, 1 }), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
    avp.setValue(Unsigned32Format.toUnsigned32(DiameterBaseConstants.APPLICATION_RELAY), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
    avp.setValue(Unsigned32Format.toUnsigned32(DiameterBaseConstants.APPLICATION_RELAY), false);
    list.add(avp);
    
    ByteBuffer cerBuffer = createBuffer(true, false, 257, 0, list);
    cerBuffer.flip();
    
    byte[] cer = new byte[cerBuffer.remaining()];
    cerBuffer.get(cer);
    return cer;
  }
  
  public static byte[] makeDPR(String originHost) {
    // make a DPR
    String realm = "realm.com";
    originHost = originHost + "." + realm;
    
    List<DiameterAVP> list = new ArrayList<DiameterAVP>();
    DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
    avp.setValue(IdentityFormat.toIdentity(originHost), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
    avp.setValue(IdentityFormat.toIdentity(realm), false);
    list.add(avp);

    avp = new DiameterAVP(DiameterBaseConstants.AVP_DISCONNECT_CAUSE);
    avp.setValue(Unsigned32Format.toUnsigned32(2), false);
    list.add(avp);
    
    ByteBuffer dprBuffer = createBuffer(true, false, 282, 0, list);
    dprBuffer.flip();
    
    byte[] dpr = new byte[dprBuffer.remaining()];
    dprBuffer.get(dpr);
    return dpr;
  }
  
  public static byte[] makeACR(String originHost, String destinationHost, byte[] acrUserName, int fillbackSize) {
    String realm = "realm.com";
    originHost = originHost + "." + realm;
    destinationHost = destinationHost + "." + realm;
    
    List<DiameterAVP> list = new ArrayList<DiameterAVP>();

    DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_SESSION_ID);
    avp.setValue(IdentityFormat.toIdentity ("0000"), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_HOST);
    avp.setValue(IdentityFormat.toIdentity(originHost), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_ORIGIN_REALM);
    avp.setValue(IdentityFormat.toIdentity(realm), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
    avp.setValue(Unsigned32Format.toUnsigned32(3), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_DESTINATION_REALM);
    avp.setValue(IdentityFormat.toIdentity(realm), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_DESTINATION_HOST);
    avp.setValue(IdentityFormat.toIdentity(destinationHost), false);
    list.add(avp);
    
    avp = new DiameterAVP(DiameterBaseConstants.AVP_USER_NAME);
    avp.setValue(acrUserName, false);
    list.add(avp);

    avp = new DiameterAVP(DiameterBaseConstants.AVP_CLASS);
    int CDLB = (('C' & 0xFF) << 24) | (('D' & 0xFF) << 16) | (('L' & 0xFF) << 8) | ('B' & 0xFF);
    long l = ((long) CDLB) << 32;
    l |= 2130711301L;
    //l |= 111L;
    avp.addValue(Integer64Format.toInteger64(l), false);
    avp.addValue(Integer64Format.toInteger64(1L), false);
    //list.add(avp);

    if (fillbackSize != -1) {
      DiameterAVPDefinition def = new DiameterAVPDefinition("Fill-Back", 4L, 123L, DiameterAVPDefinition.REQUIRED_FLAG,
          DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.OPTIONAL_FLAG, false, Unsigned32Format.INSTANCE);
      
      avp = new DiameterAVP(def);
      avp.setValue(Unsigned32Format.toUnsigned32(fillbackSize), false);
      list.add(avp);
    }
    
    ByteBuffer acrBuffer = createBuffer(true, true, 1, 123, list);
    acrBuffer.flip();
    byte[] ACR = new byte[acrBuffer.remaining()];
    int index = 0;
    while (acrBuffer.hasRemaining()) {
      ACR[index++] = acrBuffer.get();
    }
    return ACR;
  }
  
  public static ByteBuffer fillBuffer(ByteBuffer buffer, DiameterAVP avp) {
    int code = (int) avp.getAVPCode();
    int vendorId = (int) avp.getVendorId();
    if (avp.getValueSize() > 0) {
      for (int i = 0; i < avp.getValueSize(); i++) {
        int flags = avp.getAVPFlags(i);
	boolean hasVendorId = DiameterAVP.vFlagSet(flags);
        byte[] value = avp.getValue(i);
        buffer.putInt(code);
        buffer.put((byte) flags);
        
        int len = 8 + ((hasVendorId) ? 4 : 0) + value.length;
        int pad = value.length % 4;
        if (pad != 0) {
          pad = 4 - pad;
        }
        //System.out.println("fillBuffer: len=" + len + ", avp=" + avp);
        buffer.put((byte) (len >> 16));
        buffer.put((byte) (len >> 8));
        buffer.put((byte) (len & 0xFF));
        
        if (hasVendorId) {
          buffer.putInt(vendorId);
        }
        buffer.put(value);
        for (int p = 0; p < pad; p++) {
          buffer.put((byte) 0);
        }
      }
    } else if (avp.getDiameterAVPDefinition() == null
        || (avp.getDiameterAVPDefinition() != null && avp.getDiameterAVPDefinition().getDiameterAVPFormat() instanceof GroupedFormat)) {
      int flags = avp.getAVPFlags();
      boolean hasVendorId = DiameterAVP.vFlagSet(flags);
      buffer.putInt(code);
      buffer.put((byte) flags);
      int len = 8 + ((hasVendorId) ? 4 : 0);
      buffer.put((byte) (len >> 16));
      buffer.put((byte) (len >> 8));
      buffer.put((byte) (len & 0xFF));
      if (hasVendorId) {
        buffer.putInt(vendorId);
      }
    }
    
    return buffer;
    
  }
  
  // Returns new hopByHop identifier
  public static int changeHopIds(byte[] ACR) {
    int hopByHopId = ID.getAndIncrement();
    setIntValue(hopByHopId, ACR, 12);
    int endToEndId = ID.getAndIncrement();
    setIntValue(endToEndId, ACR, 16);
    return hopByHopId;
  }
  public static void changeSessionId (byte[] acr, int count){
    String s = Integer.toString (count);
    switch (s.length ()){
    case 1 : s = "00"+s; break;
    case 2 : s = "0"+s; break;
    case 3 : break;
    default : s = s.substring (s.length () - 3); break;
    }
    byte[] b = s.getBytes ();
    System.arraycopy (b, 0, acr, 29, 3);
  }
  
  public static byte[] setIntValue(int value, byte[] bytes, int off) {
    bytes[off++] = (byte) (value >> 24);
    bytes[off++] = (byte) (value >> 16);
    bytes[off++] = (byte) (value >> 8);
    bytes[off] = (byte) value;
    return bytes;
  }
  
  public static ByteBuffer createBuffer(boolean isRequest, boolean isProxyable, int commandCode, int applicationId,
                                        List<DiameterAVP> avps) {
    ByteBuffer buffer = ByteBuffer.allocate(1024 * 100);
    // VERSION
    buffer.put((byte) 1);
    
    // length to be filled later (3 bytes)
    
    buffer.putInt(4, commandCode);
    
    byte flags = 0;
    if (isRequest) {
      flags |= 1 << 7;
    }
    if (isProxyable) {
      flags |= 1 << 6;
    }
    buffer.put(4, flags);
    buffer.position(8);
    
    buffer.putInt(applicationId);
    
    int hopByHopId = ID.getAndIncrement();
    buffer.putInt(hopByHopId);
    int endToEndId = ID.getAndIncrement();
    buffer.putInt(endToEndId);
    
    for (DiameterAVP avp : avps) {
      fillBuffer(buffer, avp);
    }
    
    int p = buffer.position();
    
    buffer.position(1);
    buffer.put((byte) (p >> 16));
    buffer.put((byte) (p >> 8));
    buffer.put((byte) (p & 0xFF));
    
    buffer.position(p);
    
    return buffer;
  }
  
  public static int getHopId(ByteBuffer msg) {
    return msg.getInt(msg.position() + 12);
  }
}
