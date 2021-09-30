package com.nextenso.mux;

/**
 * MuxHeader version 6 is same as the version 0. This new version is introduced
 * to differentiate the MuxHandler calls between TCP and SCTP protocols. Since
 * the flags is encoded in one byte we don't have enough bits to add SCTP
 * protocol. Hence we use the version 6 switch to say SCTP but the protocol mask
 * will be set to TCP.
 * <p/>
 * The fields are in the order:<br/>
 * <ul>
 * <li>the flags (1 byte)
 * <li>the length (2 bytes)
 * <li>the sessionId (8 bytes)
 * <li>the channelId (4 bytes)
 * </ul>
 * <br/>
 * The length is not part of the Header. The length is directly determined by
 * the size of the data sent along with the Header.
 */
public class MuxHeaderV6
		extends MuxHeaderV0 {

	/**
	 * Constructor for this class.
	 */
	public MuxHeaderV6() {}

	/**
	 * @see com.nextenso.mux.MuxHeader#getVersion()
	 */
	@Override
	public int getVersion() {
		return 6;
	}

}
