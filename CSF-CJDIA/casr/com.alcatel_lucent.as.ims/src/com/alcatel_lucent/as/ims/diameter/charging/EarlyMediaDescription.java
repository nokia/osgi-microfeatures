package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The Early-Media-Component AVP wrapper.
 */
public class EarlyMediaDescription {

	private SdpTimeStamps _sdpTimeStamps = null;
	private List<SdpMediaComponent> _sdpMediaComponents = new ArrayList<SdpMediaComponent>();
	private List<String> _sdpSessionDescriptions = new ArrayList<String>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public EarlyMediaDescription(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public EarlyMediaDescription(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getSdpTimestampsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSdpTimeStamps(new SdpTimeStamps(searchedAvp, version));
			}
		}

		def = ChargingUtils.getSdpMediaComponentAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addSdpMediaComponent(new SdpMediaComponent(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getSdpSessionDescriptionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addSdpSessionDescription(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
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
		DiameterAVPDefinition def = ChargingUtils.getEarlyMediaDescriptionAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);

		List<DiameterAVP> l = new ArrayList<DiameterAVP>();
		if (getSdpTimeStamps() != null) {
			DiameterAVP avp = getSdpTimeStamps().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		Iterable<SdpMediaComponent> components = getSdpMediaComponents();
		if (components.iterator().hasNext()) {
			def = ChargingUtils.getSdpMediaComponentAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (SdpMediaComponent component : components) {
					avp.addValue(component.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		Iterable<String> descriptions = getSdpSessionDescriptions();
		if (descriptions.iterator().hasNext()) {
			def = ChargingUtils.getSdpSessionDescriptionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String desc : descriptions) {
					avp.addValue(UTF8StringFormat.toUtf8String(desc), false);
				}
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;

	}

	/**
	 * Adds a SDP session description.
	 * 
	 * @param description The description.
	 */
	public void addSdpSessionDescription(String description) {
		_sdpSessionDescriptions.add(description);
	}

	/**
	 * Gets the SDP session descriptions (mapped to the SDP-Session-Description
	 * AVP).
	 * 
	 * @return The descriptions.
	 */
	public Iterable<String> getSdpSessionDescriptions() {
		return _sdpSessionDescriptions;
	}

	/**
	 * Sets the SDP timestamps.
	 * 
	 * @param timestamps The timestamps.
	 */
	public void setSdpTimeStamps(SdpTimeStamps timestamps) {
		_sdpTimeStamps = timestamps;
	}

	/**
	 * Gets the SDP timestamps (mapped to the SDP-TimeStamps AVP);.
	 * 
	 * @return The timestamps.
	 */
	public SdpTimeStamps getSdpTimeStamps() {
		return _sdpTimeStamps;
	}

	/**
	 * Adds a SDP media component.
	 * 
	 * @param component The component to be added.
	 */
	public void addSdpMediaComponent(SdpMediaComponent component) {
		_sdpMediaComponents.add(component);
	}

	/**
	 * Gets the SDP media component (mapped to the SDP-Media-Component AVP).
	 * 
	 * @return The descriptions.
	 */
	public Iterable<SdpMediaComponent> getSdpMediaComponents() {
		return _sdpMediaComponents;
	}

}
