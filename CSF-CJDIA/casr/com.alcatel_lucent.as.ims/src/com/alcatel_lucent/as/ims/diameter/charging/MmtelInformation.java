package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.SubscriberRole;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The MMTel-Information AVP wrapper.
 */
public class MmtelInformation {

	private List<SupplementaryService> _supplementaryServices = new ArrayList<SupplementaryService>();
	private SubscriberRole _subscriberRole = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MmtelInformation(DiameterAVP avp, Version version) {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MmtelInformation(byte[] data, Version version) {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getSupplementaryServiceAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addSupplementaryServices(new SupplementaryService(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getSubscriberRoleAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSubscriberRole(SubscriberRole.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getMmtelInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		Iterable<SupplementaryService> services = getSupplementaryServices();
		if (services.iterator().hasNext()) {
			def = ChargingUtils.getCalledPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (SupplementaryService service : services) {
					avp.addValue(service.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		if (getSubscriberRole() != null) {
			def = ChargingUtils.getSubscriberRoleAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getSubscriberRole().getValue()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * add a Supplementary-Service.
	 * 
	 * @param service The service.
	 */
	public void addSupplementaryServices(SupplementaryService service) {
		_supplementaryServices.add(service);
	}

	/**
	 * Gets the Supplementary-Service list..
	 * 
	 * @return The services.
	 */
	public Iterable<SupplementaryService> getSupplementaryServices() {
		return _supplementaryServices;
	}

	/**
	 * Sets the Subscriber-Role.
	 * 
	 * @param role The role.
	 */
	public void setSubscriberRole(SubscriberRole role) {
		_subscriberRole = role;
	}

	/**
	 * Gets the Subscriber-Role.
	 * 
	 * @return The role.
	 */
	public SubscriberRole getSubscriberRole() {
		return _subscriberRole;
	}

}
