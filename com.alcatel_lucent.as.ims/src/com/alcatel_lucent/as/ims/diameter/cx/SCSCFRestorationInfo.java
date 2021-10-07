package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The SCSCF-Restoration-Information AVP wrapper.
 * 
 * @since 3GPP 29.299 v8.3
 */

public class SCSCFRestorationInfo
		extends RestorationInfo {

	private String _username;
	private List<RestorationInfo> _restorationInfos;
	private final static Version VERSION_8_5 = new Version(8, 5);

	/**
	 * Constructor for this class.
	 * 
	 * @param groupedData The data of the grouped avp.
	 * @param version The Cx version.
	 */
	public SCSCFRestorationInfo(byte[] groupedData, Version version) {
		super(groupedData, version);
		if (groupedData == null || version == null) {
			return;
		}

		if (VERSION_8_5.compareTo(version) < 0) {
			return;
		}

		DiameterAVP avp = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_USER_NAME, groupedData, false);
		if (avp != null) {
			setUsername(UTF8StringFormat.getUtf8String(avp.getValue()));
		}

		DiameterAVPDefinition def = CxUtils.getRestorationInfoAVP(version);
		avp = GroupedFormat.getDiameterAVP(def, groupedData, false);
		if (avp == null) {
			return;
		}

		List<RestorationInfo> infos = new ArrayList<RestorationInfo>();
		for (int i = 0; i < avp.getValueSize(); i++) {
			RestorationInfo info = new RestorationInfo(avp.getValue(i), version);
			infos.add(info);
		}
		setRestorationInfos(infos);

	}

	/**
	 * Sets the username.
	 * 
	 * @param username The username.
	 */
	public void setUsername(String username) {
		_username = username;
	}

	/**
	 * Gets the username.
	 * 
	 * @return The username.
	 */
	public String getUsername() {
		return _username;
	}

	/**
	 * Sets the restorationInfos.
	 * 
	 * @param restorationInfos The restorationInfos.
	 * @since 3GPP 29.299 v8.5
	 */
	public void setRestorationInfos(List<RestorationInfo> restorationInfos) {
		if (restorationInfos != null) {
			_restorationInfos = new ArrayList<RestorationInfo>();
			_restorationInfos.addAll(restorationInfos);
		} else {
			_restorationInfos = null;
		}
	}

	/**
	 * Gets the restorationInfos.
	 * 
	 * @return The restorationInfos.
	 * @since 3GPP 29.299 v8.5
	 */
	public List<RestorationInfo> getRestorationInfos() {
		if (_restorationInfos == null) {
			return null;
		}
		
		List<RestorationInfo> res = new ArrayList<RestorationInfo>();
		res.addAll(_restorationInfos);
		return res;
	}

}
