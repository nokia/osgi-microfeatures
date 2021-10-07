package com.alcatel.as.util.sctp;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.sun.jna.Pointer;

/**
 * struct sctp_event_subscribe {
 *	__u8 sctp_data_io_event;
 *	__u8 sctp_association_event;
 *	__u8 sctp_address_event;
 *	__u8 sctp_send_failure_event;
 *	__u8 sctp_peer_error_event;
 *	__u8 sctp_shutdown_event;
 *	__u8 sctp_partial_delivery_event;
 *	__u8 sctp_adaptation_layer_event;
 *	__u8 sctp_authentication_event;
 *	__u8 sctp_sender_dry_event;
 * };	
 */
public class sctp_event_subscribe implements SctpSocketParam {
	
	public boolean sctp_data_io_event;
	public boolean sctp_association_event;
	public boolean sctp_address_event;
	public boolean sctp_send_failure_event;
	public boolean sctp_peer_error_event;
	public boolean sctp_shutdown_event;
	public boolean sctp_partial_delivery_event;
	public boolean sctp_adaptation_layer_event;
	public boolean sctp_authentication_event;
	public boolean sctp_sender_dry_event;
	
	public sctp_event_subscribe() { }

	public sctp_event_subscribe(boolean sctp_data_io_event, boolean sctp_association_event, boolean sctp_address_event,
								boolean sctp_send_failure_event, boolean sctp_peer_error_event, boolean sctp_shutdown_event,
								boolean sctp_partial_delivery_event, boolean sctp_adaptation_layer_event,
								boolean sctp_authentication_event, boolean sctp_sender_dry_event) {
		
		this.sctp_data_io_event = sctp_data_io_event;
		this.sctp_association_event = sctp_association_event;
		this.sctp_address_event = sctp_address_event;
		this.sctp_send_failure_event = sctp_send_failure_event;
		this.sctp_peer_error_event = sctp_peer_error_event;
		this.sctp_shutdown_event = sctp_shutdown_event;
		this.sctp_partial_delivery_event = sctp_partial_delivery_event;
		this.sctp_adaptation_layer_event = sctp_adaptation_layer_event;
		this.sctp_authentication_event = sctp_authentication_event;
		this.sctp_sender_dry_event = sctp_sender_dry_event;
	}

	@Override
	public String toString() {
		return "sctp_event_subscribe [sctp_data_io_event=" + sctp_data_io_event + ", sctp_association_event="
				+ sctp_association_event + ", sctp_address_event=" + sctp_address_event + ", sctp_send_failure_event="
				+ sctp_send_failure_event + ", sctp_peer_error_event=" + sctp_peer_error_event
				+ ", sctp_shutdown_event=" + sctp_shutdown_event + ", sctp_partial_delivery_event="
				+ sctp_partial_delivery_event + ", sctp_adaptation_layer_event=" + sctp_adaptation_layer_event
				+ ", sctp_authentication_event=" + sctp_authentication_event + ", sctp_sender_dry_event="
				+ sctp_sender_dry_event + "]";
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeBoolean(sctp_data_io_event);
		out.writeBoolean(sctp_association_event);
		out.writeBoolean(sctp_address_event);
		out.writeBoolean(sctp_send_failure_event);
		out.writeBoolean(sctp_peer_error_event);
		out.writeBoolean(sctp_shutdown_event);
		out.writeBoolean(sctp_partial_delivery_event);
		out.writeBoolean(sctp_adaptation_layer_event);
		out.writeBoolean(sctp_authentication_event);
		out.writeBoolean(sctp_sender_dry_event);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		sctp_data_io_event = in.readBoolean();
		sctp_association_event = in.readBoolean();
		sctp_address_event = in.readBoolean();
		sctp_send_failure_event = in.readBoolean();
		sctp_peer_error_event = in.readBoolean();
		sctp_shutdown_event = in.readBoolean();
		sctp_partial_delivery_event = in.readBoolean();
		sctp_adaptation_layer_event = in.readBoolean();
		sctp_authentication_event = in.readBoolean();
		sctp_sender_dry_event = in.readBoolean();
	}

	public SctpSocketParam merge(SctpSocketParam other2) {
		if(!(other2 instanceof sctp_event_subscribe)) throw new IllegalArgumentException("Not an sctp_event_subscribe");
		sctp_event_subscribe other = (sctp_event_subscribe) other2;
		return new sctp_event_subscribe(this.sctp_data_io_event || other.sctp_data_io_event,
						this.sctp_association_event || other.sctp_association_event,
						this.sctp_address_event || other.sctp_address_event,
						this.sctp_send_failure_event || other.sctp_send_failure_event,
						this.sctp_peer_error_event || other.sctp_peer_error_event,
						this.sctp_shutdown_event || other.sctp_shutdown_event,
						this.sctp_partial_delivery_event || other.sctp_partial_delivery_event,
						this.sctp_adaptation_layer_event || other.sctp_adaptation_layer_event,
						this.sctp_authentication_event || other.sctp_authentication_event,
						this.sctp_sender_dry_event || other.sctp_sender_dry_event);
	}
	
	public Pointer toJNA(Pointer p) {
		p.setByte(0, sctp_data_io_event ? (byte) 1 : (byte) 0);
		p.setByte(1, sctp_association_event ? (byte) 1 : (byte) 0);
		p.setByte(2, sctp_address_event ? (byte) 1 : (byte) 0);
		p.setByte(3, sctp_send_failure_event ? (byte) 1 : (byte) 0);
		p.setByte(4, sctp_peer_error_event ? (byte) 1 : (byte) 0);
		p.setByte(5, sctp_shutdown_event ? (byte) 1 : (byte) 0);
		p.setByte(6, sctp_partial_delivery_event ? (byte) 1 : (byte) 0);
		p.setByte(7, sctp_adaptation_layer_event ? (byte) 1 : (byte) 0);
		p.setByte(8, sctp_authentication_event ? (byte) 1 : (byte) 0);
		p.setByte(9, sctp_sender_dry_event ? (byte) 1 : (byte) 0);
		return p;
	}
	
	public sctp_event_subscribe fromJNA(Pointer p) {
		sctp_data_io_event = (p.getByte(0) == (byte) 1); 
		sctp_association_event = (p.getByte(1) == (byte) 1); 
		sctp_address_event = (p.getByte(2) == (byte) 1); 
		sctp_send_failure_event = (p.getByte(3) == (byte) 1); 
		sctp_peer_error_event = (p.getByte(4) == (byte) 1); 
		sctp_shutdown_event = (p.getByte(5) == (byte) 1); 
		sctp_partial_delivery_event = (p.getByte(6) == (byte) 1); 
		sctp_adaptation_layer_event = (p.getByte(7) == (byte) 1); 
		sctp_authentication_event = (p.getByte(8) == (byte) 1); 
		sctp_sender_dry_event = (p.getByte(9) == (byte) 1);
		return this;
	}
	
	public int jnaSize() {
		return 10; //10 booleans
	}

}
