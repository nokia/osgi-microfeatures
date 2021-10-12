// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.radius.agent.impl;

import java.io.EOFException;
import java.io.IOException;

import com.nextenso.proxylet.radius.RadiusAttribute;
import com.nextenso.proxylet.radius.RadiusMessage;
import com.nextenso.proxylet.radius.RadiusUtils;
import com.nextenso.proxylet.radius.VendorSpecificAttribute;
import com.nextenso.proxylet.radius.auth.CHAPPasswordAttribute;
import com.nextenso.proxylet.radius.ExtendedTypeAttribute;
import com.nextenso.proxylet.radius.ExtendedVendorSpecificAttribute;

public class RadiusInputStream {

	public static void readAttributes(RadiusMessageFacade message, byte[] in, int offset, int length)
		throws IOException {
		int len = length;
		int off=offset;
		ExtendedTypeAttribute longExtTypeAttrInFlight = null;
		while (true) {
			if (len == 0) {
				break;
			}
			if (len < 2) {
				throw new EOFException("EOF while parsing Attribute");
			}
			
			int type = in[off] & 0xFF;
			int attLen = in[off + 1] & 0xFF;
			boolean unknownAtt= false;
			len -= attLen;
			if (len < 0) {
				throw new EOFException("EOF while parsing Attribute");
			}
			switch (type) {
				case RadiusUtils.USER_PASSWORD:
					if (longExtTypeAttrInFlight != null) throw new IOException ("Missing LongExtendedTypeAttribute subsequent value");
					if (!(message instanceof AccessRequestFacade)) {
						throw new IOException("Unexpected User-Password Attribute");
					}
					if (attLen < 2) throw new IOException ("Invalid User-Password Attribute : too short");
					AccessRequestFacade request = (AccessRequestFacade) message;
					request.setEncodedPassword(in, off + 2, attLen - 2);
					break;
				case RadiusUtils.CHAP_PASSWORD:
					if (longExtTypeAttrInFlight != null) throw new IOException ("Missing LongExtendedTypeAttribute subsequent value");
					if (!(message instanceof AccessRequestFacade)) {
						throw new IOException("Unexpected CHAP-Password Attribute");
					}
					if (attLen < 3) throw new IOException ("Invalid CHAP-Password Attribute : too short");
					CHAPPasswordAttribute chapAttr = (CHAPPasswordAttribute) message.getRadiusAttribute(type);
					if (chapAttr == null) {
						unknownAtt = true;
						chapAttr = new CHAPPasswordAttribute(in[off + 2] & 0xFF);
					}
					// CHAPPassword is single valued
					chapAttr.setValue(in, off + 3, attLen - 3, true);
					if (unknownAtt) {
						message.addRadiusAttribute(chapAttr);
					}
					break;
				case RadiusUtils.VENDOR_SPECIFIC:
					if (longExtTypeAttrInFlight != null) throw new IOException ("Missing LongExtendedTypeAttribute subsequent value");
					int vendorId = getInt32(in, off + 2);
					VendorSpecificAttribute vendorAttr = message.getVendorSpecificAttribute(vendorId);
					if (vendorAttr == null) {
						unknownAtt = true;
						vendorAttr = new VendorSpecificAttribute(vendorId);
					}

					// System.out.println("AttLen [" + attLen + "] off [" + off + "]");
					int vendorOffset = 6; /* Type:1 / Length:1 / VendorId:4 */
					while (vendorOffset < attLen) {
						int size = in[off + vendorOffset + 1] & 0xFF;
						// System.out.println("[" + cpt++ + "] current Size [" + size + "] OffSet [" + offset + "]");
						vendorAttr.addValue(in, off + vendorOffset, size, true);
						vendorOffset += size;
					} /* while */

					if (unknownAtt) {
						message.addVendorSpecificAttribute(vendorAttr);
					}

					break;
				case ExtendedTypeAttribute.Extended_Type_1:
				case ExtendedTypeAttribute.Extended_Type_2:
				case ExtendedTypeAttribute.Extended_Type_3:
				case ExtendedTypeAttribute.Extended_Type_4:
					if (longExtTypeAttrInFlight != null) throw new IOException ("Missing LongExtendedTypeAttribute subsequent value");
					if (attLen < 4) throw new IOException ("Invalid ExtendedTypeAttribute : too short"); // we dont accept blank value, as per rfc
					int extType = in[off + 2] & 0xFF;
					if (extType == RadiusUtils.VENDOR_SPECIFIC){
						if (attLen < 9) throw new IOException ("Invalid ExtendedVendorSpecificAttribute : too short"); // we dont accept blank value, as per rfc
						vendorId = getInt32(in, off + 3);
						int evsType = in[off+7] & 0xFF;
						ExtendedVendorSpecificAttribute attr = message.getExtendedVendorSpecificAttribute (type, vendorId, evsType);
						if (attr == null) {
							unknownAtt= true;
							attr = new ExtendedVendorSpecificAttribute(type, vendorId, evsType);
						}
						attr.addValue(in, off + 8, attLen - 8, true);
						if (unknownAtt)  {
							message.addRadiusAttribute(attr);
						}
					} else {
						ExtendedTypeAttribute attr = message.getExtendedTypeAttribute (type, extType);
						if (attr == null) {
							unknownAtt= true;
							attr = new ExtendedTypeAttribute(type, extType);
						}
						attr.addValue(in, off + 3, attLen - 3, true);
						if (unknownAtt)  {
							message.addRadiusAttribute(attr);
						}
					}
					break;
				case ExtendedTypeAttribute.Long_Extended_Type_1:
				case ExtendedTypeAttribute.Long_Extended_Type_2:
					if (attLen < 5) throw new IOException ("Invalid LongExtendedTypeAttribute : too short"); // we dont accept blank value, as per rfc
					extType = in[off + 2] & 0xFF;
					int flags = in[off + 3] & 0xFF;
					boolean hasM = ((flags & 0x80) == 0x80);
					if (hasM && attLen != 255)
						throw new IOException ("Invalid LongExtendedTypeAttribute : invalid M block size");
					if (longExtTypeAttrInFlight == null){
						// first block
						if (extType == RadiusUtils.VENDOR_SPECIFIC){
							if (attLen < 9) throw new IOException ("Invalid ExtendedVendorSpecificAttribute : too short"); // we dont accept blank value, as per rfc
							vendorId = getInt32(in, off + 4);
							int evsType = in[off+8] & 0xFF;
							ExtendedVendorSpecificAttribute attr = message.getExtendedVendorSpecificAttribute (type, vendorId, evsType);
							if (attr == null) {
								unknownAtt= true;
								attr = new ExtendedVendorSpecificAttribute(type, vendorId, evsType);
							}
							attr.addValue(in, off + 9, attLen - 9, true);
							if (unknownAtt)  {
								message.addRadiusAttribute(attr);
							}
							if (hasM)
							    longExtTypeAttrInFlight = attr;
						} else {
							ExtendedTypeAttribute attr = message.getExtendedTypeAttribute (type, extType);
							if (attr == null) {
								unknownAtt= true;
								attr = new ExtendedTypeAttribute(type, extType);
							}
							attr.addValue(in, off + 4, attLen - 4, true);
							if (unknownAtt)  {
								message.addRadiusAttribute(attr);
							}
							if (hasM)
							    longExtTypeAttrInFlight = attr;
						}
					} else {
						byte[] value = longExtTypeAttrInFlight.removeValue (longExtTypeAttrInFlight.getValueSize () - 1);
						byte[] newValue = new byte[value.length + attLen - 4];
						System.arraycopy (value, 0, newValue, 0, value.length);
						System.arraycopy (in, off+4, newValue, value.length, attLen - 4);
						longExtTypeAttrInFlight.addValue (newValue, false);
					}
					if (!hasM)
						longExtTypeAttrInFlight = null;
					break;
				case RadiusUtils.MESSAGE_AUTHENTICATOR:
					if (longExtTypeAttrInFlight != null) throw new IOException ("Missing LongExtendedTypeAttribute subsequent value");
					if (attLen != 18) throw new IOException("Invalid Message-Authenticator Attribute : too short");
					if (message.getRadiusAttribute (type) != null)
						throw new IOException("Duplicate Message-Authenticator Attribute");
					message.setMessageAuthenticatorOffset (off + 2 - offset);
					// let default behavior proceed
				default:
					// usual attribute
					if (longExtTypeAttrInFlight != null) throw new IOException ("Missing LongExtendedTypeAttribute subsequent value");
					if (attLen < 2) throw new IOException ("Invalid Attribute (type="+type+") : too short");
					RadiusAttribute attr = message.getRadiusAttribute(type);
					if (attr == null) {
						unknownAtt= true;
						attr = new RadiusAttribute(type);
					}
					attr.addValue(in, off + 2, attLen - 2, true);
					if (unknownAtt)  {
						message.addRadiusAttribute(attr);
					}
					break;
			}
			off += attLen;
		}
	}

	public static int lastIndexOfAttribute(int attribute, byte[] in, int offset, int length)
		throws IOException {
		int len = length;
		int off=offset;
		int index = -1;
		while (true) {
			if (len == 0) {
				break;
			}
			if (len < 2) {
				throw new EOFException("EOF while parsing Attribute");
			}
			int type = in[off] & 0xFF;
			int attLen = in[off + 1] & 0xFF;
			len -= attLen;
			if (len < 0) {
				throw new EOFException("EOF while parsing Attribute");
			}
			if (type == attribute) {
				index = off;
			}
			off += attLen;
		}
		return index;
	}

	private static final int getInt32(byte[] in, int off) {
		int i = in[off] << 24;
		i |= (in[off + 1] & 0xFF) << 16;
		i |= (in[off + 2] & 0xFF) << 8;
		i |= (in[off + 3] & 0xFF);
		return i;
	}

}
