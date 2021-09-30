package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.UncompatibleAVPDefinition;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * 
 * The Server-Capabilities AVP wrapper.
 */
public class ServerCapabilities {

	private List<Long> _mandatoryCapabilites = new ArrayList<Long>();
	private List<Long> _optionalCapabilities = new ArrayList<Long>();
	private List<String> _serverNames = new ArrayList<String>();

	/**
	 * Constructor for this class.
	 */
	public ServerCapabilities() {

	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 29.229 document.
	 * @throws UncompatibleAVPDefinition if the avp is not compatible with the
	 *           version.
	 */
	public ServerCapabilities(DiameterAVP avp, Version version)
			throws UncompatibleAVPDefinition {

		DiameterAVPDefinition def = CxUtils.getServerCapabilitiesAVP(version);
		if (!avp.isInstanceOf(def)) {
			throw new UncompatibleAVPDefinition(def, avp.getDiameterAVPDefinition());
		}

		def = CxUtils.getMandatoryCapabilityAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, avp.getValue(), true);
			if (data != null) {
				for (int i = 0; i < data.getValueSize(); i++) {
					byte[] value = data.getValue(i);
					long capability = Unsigned32Format.getUnsigned32(value, 0);
					addMandatoryCapability(Long.valueOf(capability));
				}
			}
		}

		def = CxUtils.getOptionalCapabilityAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, avp.getValue(), true);
			if (data != null) {
				for (int i = 0; i < data.getValueSize(); i++) {
					byte[] value = data.getValue(i);
					long capability = Unsigned32Format.getUnsigned32(value, 0);
					addOptionalCapability(Long.valueOf(capability));
				}
			}
		}

		def = CxUtils.getServerNameAVP(version);
		if (def != null) {
			DiameterAVP data = GroupedFormat.getDiameterAVP(def, avp.getValue(), true);
			if (data != null) {
				for (int i = 0; i < data.getValueSize(); i++) {
					byte[] value = data.getValue(i);
					String name = UTF8StringFormat.getUtf8String(value);
					addServerName(name);
				}
			}
		}

	}

	/**
	 * Creates a grouped Server-Capabilities AVP.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getServerCapabilitiesAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);

		def = CxUtils.getMandatoryCapabilityAVP(version);
		if (def != null && !_mandatoryCapabilites.isEmpty()) {
			DiameterAVP avp = new DiameterAVP(def);
			for (Long capability : _mandatoryCapabilites) {
				avp.addValue(Unsigned32Format.toUnsigned32(capability), false);
			}
			res.addValue(avp.getValue(), false);
		}

		def = CxUtils.getOptionalCapabilityAVP(version);
		if (def != null && !_optionalCapabilities.isEmpty()) {
			DiameterAVP avp = new DiameterAVP(def);
			for (Long capability : _optionalCapabilities) {
				avp.addValue(Unsigned32Format.toUnsigned32(capability), false);
			}
			res.addValue(avp.getValue(), false);
		}

		def = CxUtils.getServerNameAVP(version);
		if (def != null && !_serverNames.isEmpty()) {
			DiameterAVP avp = new DiameterAVP(def);
			for (String server : _serverNames) {
				avp.addValue(UTF8StringFormat.toUtf8String(server), false);
			}
			res.addValue(avp.getValue(), false);
		}

		return res;
	}

	/**
	 * Adds a single determined mandatory capability of an S-CSCF.
	 * 
	 * @param capability The capabilty to be added.
	 */
	public void addMandatoryCapability(Long capability) {
		_mandatoryCapabilites.add(capability);
	}

	/**
	 * Gets the mandatory capabilities of an S-CSCF.
	 * 
	 * @return The list of mandatory capabilities.
	 */
	public List<Long> getMandatoryCapabilities() {
		return _mandatoryCapabilites;
	}

	/**
	 * Adds a single determined optional capability of an S-CSCF.
	 * 
	 * @param capability The capabilty to be added.
	 */
	public void addOptionalCapability(Long capability) {
		_optionalCapabilities.add(capability);
	}

	/**
	 * Gets the optional capability of an S-CSCF.
	 * 
	 * @return The list of optional capabilities.
	 */
	public List<Long> getOptionalCapabilities() {
		return _optionalCapabilities;
	}

	/**
	 * Adds a SIP-URL used to identify a SIP server (e.g. S-CSCF name).
	 * 
	 * @param serverName The server to be added.
	 */
	public void addServerName(String serverName) {
		_serverNames.add(serverName);
	}

	/**
	 * Gets the SIP-URL used to identify a SIP server (e.g. S-CSCF name).
	 * 
	 * @return The list of server names.
	 */
	public List<String> getServerNames() {
		return _serverNames;
	}
}
