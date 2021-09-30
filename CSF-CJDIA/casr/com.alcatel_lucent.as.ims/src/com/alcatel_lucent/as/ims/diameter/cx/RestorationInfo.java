package com.alcatel_lucent.as.ims.diameter.cx;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The Restoration-Information AVP wrapper.
 * 
 * @since 3GPP 29.299 v8.5
 */
public class RestorationInfo {

	private byte[] _path;
	private byte[] _contact;
	private SubscriptionInformation _subscriptionInfo;

	/**
	 * 
	 * Constructor for this class.
	 * 
	 * @param groupedData The data of the grouped avp.
	 * @param version The Cx version.
	 */
	public RestorationInfo(byte[] groupedData, Version version) {
		if (groupedData == null || version == null) {
			return;
		}
		DiameterAVPDefinition def = CxUtils.getPathAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (avp != null) {
				setPath(avp.getValue());
			}
		}
		def = CxUtils.getContactAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (avp != null) {
				setContact(avp.getValue());
			}
		}
		def = CxUtils.getSubscriptionInfoAVP(version);
		if (def != null) {
			DiameterAVP avp = GroupedFormat.getDiameterAVP(def, groupedData, false);
			if (avp != null) {
				setSubscriptionInfo(new SubscriptionInformation(avp.getValue(), version));
			}
		}
	}

	/**
	 * Creates a grouped Restoration-Information AVP.
	 * 
	 * @param version The version of the 3GPP 29.229 document.
	 * @return The AVP or null if not possible.
	 */
	public DiameterAVP toAvp(Version version) {
		DiameterAVPDefinition def = CxUtils.getRestorationInfoAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		def = CxUtils.getPathAVP(version);
		if (def != null) {
			byte[] value = getPath();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, true);
				l.add(avp);
			}
		}

		def = CxUtils.getContactAVP(version);
		if (def != null) {
			byte[] value = getContact();
			if (value != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, true);
				l.add(avp);
			}
		}

		if (getSubscriptionInfo() != null) {
			DiameterAVP avp = getSubscriptionInfo().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the path.
	 * 
	 * @param path The path.
	 */
	public void setPath(byte[] path) {
		_path = copyArray(path);
	}

	/**
	 * Gets the path.
	 * 
	 * @return The path.
	 */
	public byte[] getPath() {
		return copyArray(_path);
	}

	/**
	 * Sets the contact.
	 * 
	 * @param contact The contact.
	 */
	public void setContact(byte[] contact) {
		_contact = copyArray(contact);
	}

	/**
	 * Gets the contact.
	 * 
	 * @return The contact.
	 */
	public byte[] getContact() {
		return copyArray(_contact);
	}

	/**
	 * Sets the subscriptionInfo.
	 * 
	 * @param subscriptionInfo The subscriptionInfo.
	 */
	public void setSubscriptionInfo(SubscriptionInformation subscriptionInfo) {
		_subscriptionInfo = subscriptionInfo;
	}

	/**
	 * Gets the subscriptionInfo.
	 * 
	 * @return The subscriptionInfo.
	 */
	public SubscriptionInformation getSubscriptionInfo() {
		return _subscriptionInfo;
	}

	private byte[] copyArray(byte[] src) {
		if (src == null) {
			return null;
		}

		int len = src.length;
		byte[] res = new byte[len];
		System.arraycopy(src, 0, res, 0, len);
		return res;
	}

}
