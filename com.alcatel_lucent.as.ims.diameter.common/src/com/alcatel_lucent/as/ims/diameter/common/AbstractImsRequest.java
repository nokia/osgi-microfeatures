package com.alcatel_lucent.as.ims.diameter.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.ImsRequest;
import com.alcatel_lucent.as.ims.diameter.SupportedFeatures;
import com.alcatel_lucent.as.ims.diameter.cx.CxUtils;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientRequest;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The Ims Request.
 */
public abstract class AbstractImsRequest
		extends ImsMessage
		implements ImsRequest {

	private DiameterClientRequest _request;

	/**
	 * Constructor for this class.
	 * 
	 * @param request The diameter request.
	 * @param version The 3GPP version
	 */
	protected AbstractImsRequest(DiameterClientRequest request, Version version) {
		super(version);
		_request = request;
	}

	/**
	 * Gets the associated request.
	 * 
	 * @return The request.
	 */
	public DiameterClientRequest getRequest() {
		return _request;
	}

	/**
	 * Adds an AVP.
	 * 
	 * @param avp The AVP to be added.
	 */
	public void addAvp(DiameterAVP avp) {
		synchronized (getRequest()) {
			DiameterAVP a = getRequest().getDiameterAVP(avp.getDiameterAVPDefinition());
			if (a == null) {
				getRequest().addDiameterAVP(avp);
			} else {
				a.addValue(avp.getValue(), false);
			}
		}
	}

	/**
	 * Adds the supported features.
	 * 
	 * @param feature The features to be added.
	 */
	public void addSupportedFeature(SupportedFeatures feature) {
		DiameterAVPDefinition def = CxUtils.getSupportedFeaturesAVP(getVersion());
		if (def == null) {
			return;
		}
		synchronized (getRequest()) {
			DiameterAVP avp = getRequest().getDiameterAVP(def);
			if (avp == null) {
				avp = new DiameterAVP(def);
				getRequest().addDiameterAVP(avp);
			}

			avp.addValue(feature.toAvp(getVersion()).getValue(), true);
		}
	}

	/**
	 * Gets the list of supported features.
	 * 
	 * @return the list of supported features.
	 */
	public List<SupportedFeatures> getSupportedFeatures() {
		ArrayList<SupportedFeatures> res = new ArrayList<SupportedFeatures>();
		DiameterAVPDefinition def = CxUtils.getSupportedFeaturesAVP(getVersion());
		if (def == null) {
			return res;
		}

		synchronized (getRequest()) {
			// Retrieve supported features AVP
			DiameterAVP avp = getRequest().getDiameterAVP(def);
			if (avp == null) {
				return res;
			}
			List avps = GroupedFormat.getGroupedAVPs(avp.getValue(), false);
			for (int i = 0; i < avps.size(); i++) {
				avp = (DiameterAVP) avps.get(i);
				res.add(new SupportedFeatures(avp.getValue(), getVersion()));
			}
		}
		return res;
	}

	/**
	 * Sets the User Private ID.
	 * 
	 * @param id The User Private ID.
	 */
	public void setUserName(String id) {
		setUTF8StringAVP(id, DiameterBaseConstants.AVP_USER_NAME);
	}

	/**
	 * Sets the User Public ID.
	 * 
	 * @param id The User Public ID.
	 */
	public abstract void setPublicUserID(String id);

	/**
	 * Sets the S-CSCF of this request. Mapped to the Server-Name diameter AVP.
	 * 
	 * @param scscfName The S-CSCF name.
	 */
	public abstract void setServerName(String scscfName);

	/**
	 * Adds the AVP.
	 * 
	 * @param data The AVP content.
	 * @param def The AVP definition.
	 * @param copy True if the data must be copied into the AVP.
	 */
	protected void addAVP(byte[] data, DiameterAVPDefinition def, boolean copy) {
		if (data == null || def == null) {
			return;
		}
		DiameterAVP avp = getRequest().getDiameterAVP(def);
		if (avp == null) {
			avp = new DiameterAVP(def);
			getRequest().addDiameterAVP(avp);
		}
		avp.addValue(data, copy);
	}

	/**
	 * Sets the AVP value.
	 * 
	 * @param avp The avp to set (replace existing avp).
	 */
	protected void setAVP(DiameterAVP avp) {
		if (avp == null) {
			return;
		}
		setAVP(avp.getValue(), avp.getDiameterAVPDefinition(), true);
	}

	/**
	 * Sets the AVP value.
	 * 
	 * @param data The AVP content.
	 * @param def The AVP definition.
	 * @param copy True if the data must be copied into the AVP.
	 */
	protected void setAVP(byte[] data, DiameterAVPDefinition def, boolean copy) {
		if (def == null) {
			return;
		}
		if (data == null) {
			getRequest().removeDiameterAVP(def);
			return;
		}

		DiameterAVP avp = getRequest().getDiameterAVP(def);

		if (avp == null) {
			avp = new DiameterAVP(def);
			getRequest().addDiameterAVP(avp);
		}

		avp.setValue(data, copy);
	}

	/**
	 * Sets the Date AVP content.
	 * 
	 * @param timestamp The data.
	 * @param def The AVP definition
	 */
	protected void setTimeAVP(Date timestamp, DiameterAVPDefinition def) {
		setAVP(TimeFormat.toTime(timestamp.getTime()), def, false);
	}

	/**
	 * Sets the UTF8 AVP content.
	 * 
	 * @param data The UTF8 data.
	 * @param def The AVP definition
	 */
	protected void setUTF8StringAVP(String data, DiameterAVPDefinition def) {
		setAVP(UTF8StringFormat.toUtf8String(data), def, false);
	}

	/**
	 * Sets the Enumerated AVP content.
	 * 
	 * @param data The data.
	 * @param def The AVP definition
	 */
	protected void setEnumeratedAVP(int data, DiameterAVPDefinition def) {
		setAVP(EnumeratedFormat.toEnumerated(data), def, false);
	}

	/**
	 * Sets the Unsigned32 AVP content.
	 * 
	 * @param data The data.
	 * @param def The AVP definition
	 */
	protected void setUnsigned32AVP(long data, DiameterAVPDefinition def) {
		setAVP(Unsigned32Format.toUnsigned32(data), def, false);
	}

	/**
	 * Sets the Octet AVP content.
	 * 
	 * @param data The data.
	 * @param def The AVP definition
	 */
	protected void setOctetStringAVP(byte[] data, DiameterAVPDefinition def, boolean copy) {
		setAVP(data, def, copy);
	}

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

	public DiameterAVP getAvp(DiameterAVPDefinition def) {
		if (def == null) {
			return null;
		}
		return getRequest().getDiameterAVP(def);
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
		res.append("\nDiameterRequest=").append(getRequest().toString());
		return res.toString();
	}
}
