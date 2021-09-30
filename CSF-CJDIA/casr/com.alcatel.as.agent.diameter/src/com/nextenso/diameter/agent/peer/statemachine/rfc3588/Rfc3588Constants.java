package com.nextenso.diameter.agent.peer.statemachine.rfc3588;


public class Rfc3588Constants {
	
	public enum Event {
		START("START"),
		TIMEOUT_CONN("TIMEOUT_CONN"),
		TIMEOUT_CLOSE("TIMEOUT_CLOSE"),
		SEND_MESSAGE("SEND_MESSAGE"),
		STOP("STOP"),
		WIN_ELECTION("WIN_ELECTION"),
	
		R_CONN_CER("R_CONN_CER"),
		R_PEER_DISC("R_PEER_DISC"),
		R_RCV_CER("R_RCV_CER"),
		R_RCV_CEA("R_RCV_CEA"),
		R_RCV_DPR("R_RCV_DPR"),
		R_RCV_DPA("R_RCV_DPA"),
		R_RCV_DWR("R_RCV_DWR"),
		R_RCV_DWA("R_RCV_DWA"),
		R_RCV_MESSAGE("R_RCV_MESSAGE"),

		I_RCV_CONN_ACK("I_RCV_CONN_ACK"),
		I_RCV_CONN_NACK("I_RCV_CONN_NACK"),
		I_RCV_NON_CEA("I_RCV_NON_CEA"),
		I_PEER_DISC("I_PEER_DISC"),
		I_RCV_CEA("I_RCV_CEA"),
		I_RCV_CER("I_RCV_CER"),
		I_RCV_DPR("I_RCV_DPR"),
		I_RCV_DPA("I_RCV_DPA"),
		I_RCV_DWR("I_RCV_DWR"),
		I_RCV_DWA("I_RCV_DWA"),
		I_RCV_MESSAGE("I_RCV_MESSAGE"),
		
		TIMEOUT("TIMEOUT"),

		CLOSE("CLOSE");
	

		private String _name = null;

		private Event(String name) {
			_name = name;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return _name;
		}

	}




}
