package com.nextenso.proxylet.diameter.util;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;

/**
 * The Grouped AVP Format.
 * <p>
 * See RFC 3588 paragraph 4.4 for information.
 */
public class GroupedFormat
		extends DiameterAVPFormat {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The single static instance.
	 */
	public static final GroupedFormat INSTANCE = new GroupedFormat();

	protected GroupedFormat() {}

	/**
	 * @see com.nextenso.proxylet.diameter.util.DiameterAVPFormat#toString(byte[],
	 *      int, int, int)
	 */
	@Override
	public String toString(byte[] data, int off, int len, int level) {
		StringBuilder s = new StringBuilder("GroupedAVP={");
		List list = getGroupedAVPs(data, off, len, false);
		String sep = "\n\t";
		for (int i = 0; i < level; i++) {
			sep += "  ";
		}
		String newSep = sep;
		for (int i = 0; i < list.size(); i++) {
			DiameterAVP avp = (DiameterAVP) list.get(i);
			s.append(newSep).append("AVP#").append(String.valueOf(i)).append('=').append(avp.toString(level + 1));
			newSep = "," + sep;
		}
		s.append('}');
		return s.toString();
	}

	/**
	 * Decodes a Grouped AVP value. <br>
	 * The returned list contains DiameterAVP objects.
	 * 
	 * @param data The data to decode. May be null.
	 * @param copy Specifies if the returned DiameterAVPs may reference the data
	 *          directly.
	 * @return A list of DiameterAVPs. Or null if data is null.
	 */
	public static List getGroupedAVPs(byte[] data, boolean copy) {
		if (data == null) return null;
		return getGroupedAVPs(data, 0, data.length, copy);
	}

	/**
	 * Looks up a DiameterAVP in a Grouped AVP value.
	 * 
	 * @param definition The Diameter AVP definition to use.
	 * @param data The data to parse.
	 * @param copy Specifies if the returned DiameterAVP may reference the data
	 *          directly.
	 * @return The DiameterAVP or <code>null</code> if it was not found.
	 */
	public static DiameterAVP getDiameterAVP(DiameterAVPDefinition definition, byte[] data, boolean copy) {
		return getDiameterAVP(definition, data, 0, data.length, copy);
	}

	/**
	 * Looks up a DiameterAVP in a Grouped AVP value.
	 * 
	 * @param definition The DiameterAVP definition to use.
	 * @param data The value to parse.
	 * @param offset The offset in the provided data.
	 * @param length The length of the provided data.
	 * @param copy specifies if the returned DiameterAVP may reference the data
	 *          directly.
	 * @return the DiameterAVP or <code>null</code> if it was not found
	 */
	public static DiameterAVP getDiameterAVP(DiameterAVPDefinition definition, byte[] data, int offset, int length, boolean copy) {
		int off = offset;
		int len = length;

		DiameterAVP avp = null;
		while (len > 0) {
			if (len < 8) throw new IndexOutOfBoundsException ();
			long code = Unsigned32Format.getUnsigned32(data, off);
			int flags = data[off + 4] & 0xFF;
			int dataLength = (data[off + 5] & 0xFF) << 16;
			dataLength |= (data[off + 6] & 0xFF) << 8;
			dataLength |= data[off + 7] & 0xFF;
			off += 8;
			len -= 8;
			dataLength -= 8;
			long vendorId = 0L;
			if ((flags & DiameterAVP.V_FLAG) == DiameterAVP.V_FLAG) {
				if (len < 4) {
					throw new IndexOutOfBoundsException();
				}
				vendorId = Unsigned32Format.getUnsigned32(data, off);
				off += 4;
				len -= 4;
				dataLength -= 4;
			}
			if (dataLength < 0) {
				//LOGGER.warn("data length too small -> cannot read more data in the grouped AVP");
				break;
			}
			if (dataLength > len) {
				throw new IndexOutOfBoundsException();
			}
			if (code == definition.getAVPCode() && vendorId == definition.getVendorId()) {
				if (avp == null) {
					avp = new DiameterAVP(definition);
				}
				avp.addValue(data, off, dataLength, flags, copy);
			}
			off += dataLength;
			len -= dataLength;
			// pad
			int pad = dataLength % 4;
			if (pad > 0) {
				pad = 4 - pad;
				off += pad;
				len -= pad;
			}
		}
		return avp;
	}

	/**
	 * Decodes a Grouped AVP value. <br>
	 * The returned list contains DiameterAVP objects.
	 * 
	 * @param data The data to decode.
	 * @param offset The offset in the provided data.
	 * @param length The length of the provided data.
	 * @param copy Specifies if the returned DiameterAVPs may reference the data
	 *          directly.
	 * @return A list of DiameterAVPs.
	 */
	public static List getGroupedAVPs(byte[] data, int offset, int length, boolean copy) {
		int off = offset;
		int len = length;
		List<DiameterAVP> list = new ArrayList<DiameterAVP>();
		while (len > 0) {
			if (len < 8) throw new IndexOutOfBoundsException();
			long code = Unsigned32Format.getUnsigned32(data, off);
			int flags = data[off + 4] & 0xFF;
			int dataLength = (data[off + 5] & 0xFF) << 16;
			dataLength |= (data[off + 6] & 0xFF) << 8;
			dataLength |= data[off + 7] & 0xFF;
			off += 8;
			len -= 8;
			dataLength -= 8;
			long vendorId = 0L;
			if ((flags & DiameterAVP.V_FLAG) == DiameterAVP.V_FLAG) {
				if (len < 4) {
					throw new IndexOutOfBoundsException();
				}
				vendorId = Unsigned32Format.getUnsigned32(data, off);
				off += 4;
				len -= 4;
				dataLength -= 4;
			}
			if (dataLength < 0) {
				break;
			}
			if (dataLength > len) {
				throw new IndexOutOfBoundsException();
			}
			DiameterAVP avp = null;
			for (DiameterAVP tmp : list) {
				if (tmp.getAVPCode() == code && tmp.getVendorId() == vendorId) {
					avp = tmp;
					break;
				}
			}
			if (avp == null) {
				avp = new DiameterAVP(code, vendorId, flags);
				list.add(avp);
			}
			avp.addValue(data, off, dataLength, flags, copy);
			off += dataLength;
			len -= dataLength;
			// pad
			int pad = dataLength % 4;
			if (pad > 0) {
				pad = 4 - pad;
				off += pad;
				len -= pad;
			}
		}
		return list;
	}

	/**
	 * Encodes into a GroupedAVP value.
	 * 
	 * @param destination The destination array where the encoded value should be
	 *          placed.
	 * @param destinationOffset The offset in the destination array.
	 * @param avps The list of DiameterAVPs to encode in the GroupedAVP value.
	 * @return The length of the encoded value.
	 */
	public static int toGroupedAVP(byte[] destination, int destinationOffset, List avps) {
		byte[] value = toGroupedAVP(avps);
		System.arraycopy(value, 0, destination, destinationOffset, value.length);
		return value.length;
	}

	/**
	 * Encodes into a GroupedAVP value.
	 * 
	 * @param avps The list of DiameterAVPs to encode in the GroupedAVP value.
	 * @return The encoded value.
	 */
	public static byte[] toGroupedAVP(List avps) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (DiameterAVP avp : (List<DiameterAVP>) avps) {
			int code = (int) avp.getAVPCode();

			int nbValues = avp.getValueSize();
			if (nbValues > 0) {

				for (int k = 0; k < nbValues; k++) {
					int len = 8;
					int flags = avp.getAVPFlags(k);
					boolean vFlag = DiameterAVP.vFlagSet(flags);
					if (vFlag) {
						len += 4;
					}

					out.write(code >> 24);
					out.write(code >> 16);
					out.write(code >> 8);
					out.write(code);

					out.write(flags);

					byte[] data = avp.getValue(k);
					len += data.length;
					out.write(len >> 16);
					out.write(len >> 8);
					out.write(len);

					if (vFlag) {
						int vendorId = (int) avp.getVendorId();
						out.write(vendorId >> 24);
						out.write(vendorId >> 16);
						out.write(vendorId >> 8);
						out.write(vendorId);
					}

					out.write(data, 0, data.length);
					// pad
					int pad = data.length % 4;
					if (pad > 0) {
						pad = 4 - pad;
						for (int x = 0; x < pad; x++) {
							out.write(0);
						}
					}
				}
			} else {
				out.write(code >> 24);
				out.write(code >> 16);
				out.write(code >> 8);
				out.write(code);

				out.write(avp.getAVPFlags());

				boolean vFlag = avp.vFlagSet();
				int len = 8;
				if (vFlag) {
					len += 4;
				}

				out.write(len >> 16);
				out.write(len >> 8);
				out.write(len);

				if (vFlag) {
					int vendorId = (int) avp.getVendorId();
					out.write(vendorId >> 24);
					out.write(vendorId >> 16);
					out.write(vendorId >> 8);
					out.write(vendorId);
				}
			}
		}

		return out.toByteArray();
	}

	public static void main (String[] s) {
		DiameterAVP avp1 = new DiameterAVP (DiameterBaseConstants.AVP_ACCOUNTING_SESSION_ID);
		avp1.addValue (UTF8StringFormat.toUtf8String ("id"), false);
		DiameterAVP avp2 = new DiameterAVP (DiameterBaseConstants.AVP_ACCOUNTING_SUB_SESSION_ID);
		avp2.addValue (UTF8StringFormat.toUtf8String ("sub-id"), false);		
		List<DiameterAVP> list = new ArrayList<DiameterAVP> ();
		list.add (avp1);
		list.add (avp2);
		byte[] b = toGroupedAVP (list);

		System.out.println (getGroupedAVPs (new byte[0], true));
		System.out.println (getDiameterAVP (DiameterBaseConstants.AVP_ACCOUNTING_SESSION_ID, new byte[0], true));
		System.out.println (getGroupedAVPs (b, true));
		System.out.println (getDiameterAVP (DiameterBaseConstants.AVP_ACCOUNTING_SESSION_ID, b, true));
		//System.out.println (getGroupedAVPs (new byte[4], true));
		System.out.println (getGroupedAVPs (b, 0, b.length -3, true));
	}

	@Override
	public byte[] encode(Object value) throws IllegalArgumentException {
		if(value instanceof List<?>) {
			return toGroupedAVP((List<?>) value);
		} else {
			throw new IllegalArgumentException("cannot encode value "
					+ "of type " + value.getClass() + ". "
					+ "A List of DiameterAVP is expected");
		}
	}
}
