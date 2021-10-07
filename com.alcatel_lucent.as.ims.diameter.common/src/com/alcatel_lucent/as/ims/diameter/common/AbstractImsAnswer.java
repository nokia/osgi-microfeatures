package com.alcatel_lucent.as.ims.diameter.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.ImsAnswer;
import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The IMSResponse Implementation.
 */
public abstract class AbstractImsAnswer
		extends ImsMessage
		implements ImsAnswer {

	private DiameterClientResponse _response;

	/**
	 * Constructor for this class.
	 * 
	 * @param response
	 * @param version
	 */
	protected AbstractImsAnswer(DiameterClientResponse response, Version version) {
		super(version);
		_response = response;
	}

	/**
	 * Gets an AVP according to its definition.
	 * 
	 * @param definition The definition.
	 * @return The AVP or nullif not found.
	 */
	public DiameterAVP getAvp(DiameterAVPDefinition definition) {
		if (definition == null) {
			return null;
		}
		return _response.getDiameterAVP(definition);
	}

	/**
	 * Gets the result code.
	 * 
	 * @return The result code.
	 */
	public long getResultCode() {
		// First try to retrieve result code from Experimental-Result-Code AVP
		DiameterAVP resultAVP = getAvp(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT);
		if (resultAVP == null) {
			return _response.getResultCode();
		}

		DiameterAVP resultCodeAVP = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_EXPERIMENTAL_RESULT_CODE, resultAVP.getValue(), false);
		if (resultCodeAVP != null) {
			return Unsigned32Format.getUnsigned32(resultCodeAVP.getValue(), 0);
		}

		return _response.getResultCode();
	}

	/**
	 * Gets the list of features supported.
	 */
	public List<SupportedFeatures> getSupportedFeatures() {
		DiameterAVPDefinition def = CxUtils.getSupportedFeaturesAVP(getVersion());
		if (def == null) {
			return null;
		}

		synchronized (_response) {
			// Retrieve supported features AVP
			DiameterAVP avp = getAvp(def);

			if (avp == null)
				return null;

			List<SupportedFeatures> supportedFeatures = new ArrayList<SupportedFeatures>(avp.getValueSize());

			// Retrieve the different values of the supported supported features AVP
			for (int i = 0; i < avp.getValueSize(); i++) {
				byte[] data = avp.getValue(i);
				SupportedFeatures feature = new SupportedFeatures(data, getVersion());
				supportedFeatures.add(feature);
			}

			return supportedFeatures;
		}
	}

	/**
	 * Gets the Private User ID parameter of this response. Mapped to the
	 * User-Name diameter AVP.
	 * 
	 * @return The User-Name AVP value.
	 */
	public String getUserName() {
		return getUTF8StringAVP(DiameterBaseConstants.AVP_USER_NAME);
	}

	/**
	 * Gets the Public User ID parameter of this response. Mapped to the
	 * Public-Identity diameter AVP.
	 */
	public abstract String getPublicIdentity();

	/**
	 * Gets the UTF8 content of the AVP.
	 * 
	 * @param def The AVP definition.
	 * @return The value.
	 */
	protected String getUTF8StringAVP(DiameterAVPDefinition def) {
		DiameterAVP avp = getAvp(def);
		if (avp == null) {
			return null;
		}

		return UTF8StringFormat.getUtf8String(avp.getValue());
	}

	/**
	 * Gets the Enumerated content of the AVP.
	 * 
	 * @param def The AVP definition.
	 * @return The value.
	 */
	protected Integer getEnumeratedAVP(DiameterAVPDefinition def) {
		DiameterAVP avp = getAvp(def);
		if (avp == null)
			return null;

		return Integer.valueOf(EnumeratedFormat.getEnumerated(avp.getValue(), 0));
	}

	/**
	 * Gets the Unsigned 32 content of the AVP.
	 * 
	 * @param def The AVP definition.
	 * @return The value.
	 */
	protected Long getUnsigned32AVP(DiameterAVPDefinition def) {
		DiameterAVP avp = getAvp(def);

		if (avp == null)
			return null;

		return Long.valueOf(Unsigned32Format.getUnsigned32(avp.getValue(), 0));
	}

	/**
	 * Gets the Octet content of the AVP.
	 * 
	 * @param def The AVP definition.
	 * @return The value.
	 */
	protected byte[] getOctetStringAVP(DiameterAVPDefinition def) {
		DiameterAVP avp = getAvp(def);
		if (avp == null) {
			return null;
		}

		return avp.getValue();
	}

	/**
	 * Gets the Date content of the AVP.
	 * 
	 * @param def The AVP definition.
	 * @return The value.
	 */
	protected Date getTimeAVP(DiameterAVPDefinition def) {
		DiameterAVP avp = getAvp(def);
		if (avp == null) {
			return null;
		}

		return new Date(TimeFormat.getTime(avp.getValue(), 0));
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder res = new StringBuilder();
		res.append("\nDiameterResponse=").append(getResponse().toString());
		return res.toString();
	}

	/**
	 * Gets the diameter client response.
	 * 
	 * @return The diameter client response.
	 */
	public DiameterClientResponse getResponse() {
		return _response;
	}

}
