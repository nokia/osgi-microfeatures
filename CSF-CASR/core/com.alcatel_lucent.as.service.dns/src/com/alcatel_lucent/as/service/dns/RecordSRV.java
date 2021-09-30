package com.alcatel_lucent.as.service.dns;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Server Selection Record as per RFC 2782, it is useful to find hosts running
 * services in a domain.
 * <p>
 * <b>Example:</b> <p/><b>_service._proto.name SRV priority weight port
 * target</b> <p/> _sip._udp SRV 3 100 UDP_PROXY_PORT PROXY_HOST <p/> _sip._tcp
 * SRV 2 100 TCP_PROXY_PORT PROXY_HOST <p/> _sips._tcp SRV 1 100 TLS_PROXY_PORT
 * PROXY_HOST <p/> <b>[Note]</b> Rules for SRV record priorities are:
 * <ul>
 * <li>1/ priority (smaller numbers means higher priority)
 * <li>2/ weight (% repartition, higher % means more weight)
 * </ul>
 * The natural order when sorting is : from higher priority to lower priority
 */

public class RecordSRV
		extends Record
		implements Comparable<RecordSRV> {

	private int _priority;
	private int _weight;
	private int _port;
	private String _target;

	public RecordSRV() {
		super(RecordType.SRV);
	}

	/**
	 * Creates an SRV Record from the given data.
	 * 
	 * @param name The record's name.
	 * @param dclass The record's class.
	 * @param ttl The record's TTL.
	 * @param priority The record's priority.
	 * @param weight The record's weight.
	 * @param port The record's port.
	 * @param target The record's target.
	 */
	public RecordSRV(String name, RecordDClass dclass, long ttl, int priority, int weight, int port, String target) {
		super(RecordType.SRV, name, dclass, ttl);
		_priority = priority;
		_weight = weight;
		_port = port;
		_target = target;
	}

	/**
	 * Creates an SRV Record from the given data.
	 * 
	 * @param name The record's name.
	 * @param priority The record's priority.
	 * @param weight The record's weight.
	 * @param port The record's port.
	 * @param target The record's target.
	 */
	public RecordSRV(String name, int priority, int weight, int port, String target) {
		super(RecordType.SRV, name);
		_priority = priority;
		_weight = weight;
		_port = port;
		_target = target;
	}

	/**
	 * Compares this Record to the specified object.
	 * 
	 * @see com.alcatel_lucent.as.service.dns.Record#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object record) {
		if (!(record instanceof RecordSRV)) {
			return false;
		}

		boolean res = super.equals(record);
		if (!res) {
			return false;
		}

		RecordSRV r = (RecordSRV) record;
		if (getPort() != r.getPort() || getWeight() != r.getWeight() || getPriority() != r.getPriority()) {
			return false;
		}

		if (!isStringEquals(getTarget(), r.getTarget())) {
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
		_port = in.readInt();
		_priority = in.readInt();
		_weight = in.readInt();
		boolean hasTarget = in.readBoolean();
		if (hasTarget) {
			_target = in.readUTF();
		}

	}

	/**
	 * @see com.alcatel_lucent.as.service.dns.Record#writeExternal(java.io.ObjectOutput)
	 */
	@Override
	public void writeExternal(ObjectOutput out)
		throws IOException {
		super.writeExternal(out);
		out.writeInt(_port);
		out.writeInt(_priority);
		out.writeInt(_weight);
		out.writeBoolean(_target != null);
		if (_target != null) {
			out.writeUTF(_target);
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
		buffer.append(", target=").append(getTarget());
		buffer.append(", port=").append(getPort());
		buffer.append(", priority=").append(getPriority());
		buffer.append(", weight=").append(getWeight());
		return buffer.toString();
	}

	/**
	 * Gets the priority (smaller numbers means higher priority).
	 * 
	 * @return The record's priority.
	 */
	public int getPriority() {
		return _priority;
	}

	/**
	 * Gets the weight (% repartition, higher % means more weight).
	 * 
	 * @return The record's weight.
	 */
	public int getWeight() {
		return _weight;
	}

	/**
	 * Gets the port that the service runs on.
	 * 
	 * @return The record's port.
	 */
	public int getPort() {
		return _port;
	}

	/**
	 * Gets the host running the service.
	 * 
	 * @return The record's target.
	 */
	public String getTarget() {
		return _target;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(RecordSRV record) {
		// If my priority is less than its, I am stronger
		int diff = getPriority() - record.getPriority();
		if (diff != 0) {
			return diff;
		}
		
		// if my weigth is more than its, I am stronger
		diff = record.getWeight() - getWeight();
		return diff;
	}
}
