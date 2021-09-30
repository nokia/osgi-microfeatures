package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PocUserRoleInfoUnits;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The PoC-User-Role AVP wrapper.
 */
public class PocUserRole {

	private String _pocUserRoleIds = null;
	private PocUserRoleInfoUnits _pocUserRoleInfoUnits = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public PocUserRole(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getPocUserRoleIdsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocUserRoleIds(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getPocUserRoleInfoUnitsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocUserRoleInfoUnits(PocUserRoleInfoUnits.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getPocUserRoleAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = ChargingUtils.getPocUserRoleIdsAVP(version);
		if (def != null && getPocUserRoleIds() != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(UTF8StringFormat.toUtf8String(getPocUserRoleIds()), false);
			l.add(avp);
		}

		def = ChargingUtils.getPocUserRoleInfoUnitsAVP(version);
		if (def != null && getPocUserRoleInfoUnits() != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(EnumeratedFormat.toEnumerated(getPocUserRoleInfoUnits().getValue()), false);
			l.add(avp);
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the PoC-User-Role-Ids.
	 * 
	 * @param ids The ids.
	 */
	public void setPocUserRoleIds(String ids) {
		_pocUserRoleIds = ids;
	}

	/**
	 * Gets the PoC-User-Role-Ids.
	 * 
	 * @return The ids.
	 */
	public String getPocUserRoleIds() {
		return _pocUserRoleIds;
	}

	/**
	 * Sets the PoC-User-Role-Info-Units.
	 * 
	 * @param units The units.
	 */
	public void setPocUserRoleInfoUnits(PocUserRoleInfoUnits units) {
		_pocUserRoleInfoUnits = units;
	}

	/**
	 * Gets the PoC-User-Role-Info-Units.
	 * 
	 * @return The units.
	 */
	public PocUserRoleInfoUnits getPocUserRoleInfoUnits() {
		return _pocUserRoleInfoUnits;
	}

}
