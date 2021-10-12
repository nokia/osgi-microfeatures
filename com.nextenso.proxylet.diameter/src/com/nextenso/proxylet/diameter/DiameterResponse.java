// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.diameter;

/**
 * This interface encapsulates a Diameter response.
 */
public interface DiameterResponse
		extends DiameterMessage {
	
	/**
	 * The Attribute storing the UNABLE_TO_DELIVER_CAUSE when CJDI triggers an UNABLE_TO_DELIVER.
	 */
	public static final Object ATTR_UNABLE_TO_DELIVER_CAUSE = new Object (){
			@Override
			public String toString(){return "ATTR_UNABLE_TO_DELIVER_CAUSE";}
		};
	/**
	 * An explanation for the UNABLE_TO_DELIVER response.
	 * The error message is included in the response Error Message AVP.
	 */
	public static enum UNABLE_TO_DELIVER_CAUSE {
		/**
		 * The request was forwarded to a peer, but the connection to this peer was closed.
		 */
		ROUTE_CLOSED ("Route Closed"),
		/**
		 * No route found.
		 */
		NO_ROUTE ("No Route"),
		/**
		 * The request was forwarded to a peer, but no response was received.
		 */
		TIMEOUT ("Timeout"),
		/**
		 * The request is not proxiable.
		 */
		NOT_PROXIABLE ("Not Proxiable");
		
		private String _errMsg;
		private UNABLE_TO_DELIVER_CAUSE (String errMsg){
			_errMsg = errMsg;
		}
		public String errorMessage (){
			return _errMsg;
		}
	};
	
	/**
	 * Gets the result code stored in the Result-Code AVP.
	 * 
	 * @return The result code.
	 * @see com.nextenso.proxylet.diameter.util.DiameterBaseConstants values
	 *      defined in RFC 3588.
	 */
	public long getResultCode();

	/**
	 * Sets the result code stored in the Result-Code AVP.
	 * 
	 * @param code The result code.
	 * @see com.nextenso.proxylet.diameter.util.DiameterBaseConstants values
	 *      defined in RFC 3588.
	 */
	public void setResultCode(long code);

	/**
	 * Indicates if the E flag is set (See RFC 3588 paragraph 3).
	 * 
	 * @return true if the E flag is set, false otherwise
	 */
	public boolean hasErrorFlag();

	/**
	 * Sets the E flag.
	 * 
	 * @param flag true to set the flag, false to remove it.
	 */
	public void setErrorFlag(boolean flag);

	/**
	 * Gets the associated request.
	 * 
	 * @return The associated request.
	 */
	public DiameterRequest getRequest();

	/**
	 * Sets the Origin-Host AVP. <br/>
	 * The Origin-Host is set to the value defined for the local Diameter Peer.
	 */
	public void setOriginHostAVP();

	/**
	 * Sets the Origin-Realm AVP. <br/>
	 * The Origin-Realm is set to the value defined for the local Diameter Peer.
	 */
	public void setOriginRealmAVP();
}
