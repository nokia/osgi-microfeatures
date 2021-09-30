package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ClassIdentifier;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Message-Class AVP wrapper.
 */
public class MessageClass {

	private ClassIdentifier _classIdentifier = null;
	private String _tokenText = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MessageClass(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getClassIdentifierAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setClassIdentifier(ClassIdentifier.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getTokenTextAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTokenText(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

	}

	/**
	 * Creates a grouped AVP.
	 * 
	 * @param version The version of the 3GPP 32.299 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = ChargingUtils.getOriginatorAddressAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getClassIdentifier() != null) {
			def = ChargingUtils.getClassIdentifierAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getClassIdentifier().getValue()), false);
				l.add(avp);
			}
		}

		if (getTokenText() != null) {
			def = ChargingUtils.getTokenTextAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getTokenText()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Class-Identifier.
	 * 
	 * @param identifier The identifier.
	 */
	public void setClassIdentifier(ClassIdentifier identifier) {
		_classIdentifier = identifier;
	}

	/**
	 * Gets the Class-Identifier.
	 * 
	 * @return The identifier.
	 */
	public ClassIdentifier getClassIdentifier() {
		return _classIdentifier;
	}

	/**
	 * Sets the Token-Text.
	 * 
	 * @param text The text.
	 */
	public void setTokenText(String text) {
		_tokenText = text;
	}

	/**
	 * Gets the Token-Text.
	 * 
	 * @return The text.
	 */
	public String getTokenText() {
		return _tokenText;
	}
}
