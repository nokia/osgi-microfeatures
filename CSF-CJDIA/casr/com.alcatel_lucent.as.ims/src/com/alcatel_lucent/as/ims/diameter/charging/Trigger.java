package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.TriggerType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The Trigger AVP wrapper.
 */
public class Trigger {

	private List<TriggerType> _triggerTypes = new ArrayList<TriggerType>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public Trigger(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();
		DiameterAVPDefinition def = ChargingUtils.getTriggerTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addTriggerType(TriggerType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(i), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getTriggerAVP(version);
		if (def == null) {
			return null;
		}
		
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		Iterable<TriggerType> types = getTriggerTypes();
		if (types.iterator().hasNext()) {
			def = ChargingUtils.getUsedServiceUnitAVP();
			DiameterAVP avp = new DiameterAVP(def);
			for (TriggerType type : types) {
				avp.addValue(EnumeratedFormat.toEnumerated(type.getValue()), false);
			}
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Adds a Trigger-Type.
	 * 
	 * @param type The type.
	 */
	public void addTriggerType(TriggerType type) {
		if (type != null) {
			_triggerTypes.add(type);
		}
	}

	/**
	 * Gets the Trigger-Type list.
	 * 
	 * @return The types.
	 */
	public Iterable<TriggerType> getTriggerTypes() {
		return _triggerTypes;
	}

}
