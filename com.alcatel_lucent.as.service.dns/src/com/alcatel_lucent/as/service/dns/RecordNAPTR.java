package com.alcatel_lucent.as.service.dns;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Name Authority Pointer Record as per RFC 2915.
 * 
 * <b>Example:</b> if a proxy/server support UDP, TCP and TLS transport
 * protocols, then at least three NAPTR entries should be available for it:
 * 
 * <b>IN NAPTR order preference flags service regexp target</b>
 * 
 * IN NAPTR 100 50 "s" "SIP+D2U" "" _sip._udp
 * 
 * IN NAPTR 80 50 "s" "SIP+D2T" "" _sip._tcp
 * 
 * IN NAPTR 50 50 "s" "SIPS+D2T" "" _sips._tcp
 * 
 * <b>[Note]</b> Rules for NAPTR record priorities are:
 * <ul>
 * <li>1/ order (smaller numbers means higher priority)
 * <li>2/ preference (if order are equals), (smaller numbers means higher
 * priority)
 * </ul>
 * The natural order when sorting is : from higher priority to lower priority
 */

public class RecordNAPTR
		extends Record
		implements Comparable<RecordNAPTR> {

	private int _order;
	private int _preference;
	private String _flags;
	private String _service;
	private String _regexp;
	private String _replacement;

	public RecordNAPTR() {
		super(RecordType.NAPTR);
	}

	/**
	 * Creates an NAPTR Record from the given data
	 * 
	 * @param name The record's name.
	 * @param dclass The record's class.
	 * @param ttl The record's TTL.
	 * @param order The record's order.
	 * @param preference The record's preference.
	 * @param flags The record's flags.
	 * @param service The record's service.
	 * @param regexp The record's regexp.
	 * @param replacement The record's replacement.
	 */
	public RecordNAPTR(String name, RecordDClass dclass, long ttl, int order, int preference, String flags, String service, String regexp,
			String replacement) {
		super(RecordType.NAPTR, name, dclass, ttl);
		_order = order;
		_preference = preference;
		_flags = flags;
		_service = service;
		_regexp = regexp;
		_replacement = replacement;
	}

	/**
	 * Creates an NAPTR Record from the given data
	 * 
	 * @param name The record's name.
	 * @param order The record's order.
	 * @param preference The record's preference.
	 * @param flags The record's flags.
	 * @param service The record's service.
	 * @param regexp The record's regexp.
	 * @param replacement The record's replacement.
	 */
	public RecordNAPTR(String name, int order, int preference, String flags, String service, String regexp, String replacement) {
		super(RecordType.NAPTR, name);
		_order = order;
		_preference = preference;
		_flags = flags;
		_service = service;
		_regexp = regexp;
		_replacement = replacement;
	}

	/**
	 * Compares this Record to the specified object.
	 * 
	 * @see com.alcatel_lucent.as.service.dns.Record#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object record) {
		if (!(record instanceof RecordNAPTR)) {
			return false;
		}

		boolean res = super.equals(record);
		if (!res) {
			return false;
		}
		RecordNAPTR r = (RecordNAPTR) record;
		if (r.getOrder() != getOrder() || r.getPreference() != getPreference() || !isStringEquals(r.getFlags(), getFlags())
				|| !isStringEquals(r.getRegexp(), getRegexp()) || !isStringEquals(r.getReplacement(), getReplacement())
				|| !isStringEquals(r.getService(), getService())) {
			return false;
		}

		return true;
	}

	/**
	 * Returns a hash code for this record.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.Record#readExternal(java.io.ObjectInput)
	 */
	@Override
	public void readExternal(ObjectInput in)
		throws IOException, ClassNotFoundException {
		super.readExternal(in);
		_order = in.readInt();
		_preference = in.readInt();
		boolean hasString = in.readBoolean();
		if (hasString) {
			_regexp = in.readUTF();
		}
		hasString = in.readBoolean();
		if (hasString) {
			_replacement = in.readUTF();
		}
		hasString = in.readBoolean();
		if (hasString) {
			_service = in.readUTF();
		}
	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.Record#writeExternal(java.io.ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out)
		throws IOException {
		super.writeExternal(out);
		out.writeInt(_order);
		out.writeInt(_preference);
		out.writeBoolean(_regexp != null);
		if (_regexp != null) {
			out.writeUTF(_regexp);
		}
		out.writeBoolean(_replacement != null);
		if (_replacement != null) {
			out.writeUTF(_replacement);
		}
		out.writeBoolean(_service != null);
		if (_service != null) {
			out.writeUTF(_service);
		}
	}

	/**
	 * Returns a String object representing this Record's value.
	 * 
	 * @see com.alcatel_lucent.as.service.dns.Record#toString()
	 */
	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(super.toString());
		buffer.append(",order=").append(getOrder());
		buffer.append(",flags=").append(getFlags());
		buffer.append(",preference=").append(getPreference());
		buffer.append(",service=").append(getService());
		buffer.append(",replacement=").append(getReplacement());
		buffer.append(",regexp=").append(getRegexp());
		return buffer.toString();
	}

	/**
	 * Gets the NAPTR record order (smaller numbers means higher priority).
	 * 
	 * @return The record's order.
	 */
	public int getOrder() {
		return _order;
	}

	/**
	 * Gets the NAPTR record preference (if order are equals, smaller numbers
	 * means higher priority).
	 * 
	 * @return The record's preference.
	 */
	public int getPreference() {
		return _preference;
	}

	/**
	 * Gets the NAPTR record flags.
	 * 
	 * @return The record's flags.
	 */
	public String getFlags() {
		return _flags;
	}

	/**
	 * Gets the NAPTR record service.
	 * 
	 * @return The record's service.
	 */
	public String getService() {
		return _service;
	}

	/**
	 * Gets the NAPTR record regexp.
	 * 
	 * @return The record's regexp.
	 */
	public String getRegexp() {
		return _regexp;
	}

	/**
	 * Gets the NAPTR record replacement domain name.
	 * 
	 * @return The record's replacement.
	 */
	public String getReplacement() {
		return _replacement;
	}

	public int compareTo(RecordNAPTR record) {
		// if my order is less than its order, I am stronger.
		int diff = getOrder() - record.getOrder();
		if (diff != 0) {
			return diff;
		}

		// if my preference is more than its preference, I am stronger.
		diff = getPreference() - record.getPreference();
		return diff;
	}
}
