package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.GroupedFormat;

/**
 * The Service-Information AVP wrapper.
 */
public class ServiceInformation {

	private List<SubscriptionId> _subscriptionIds = new ArrayList<SubscriptionId>();
	private ServiceGenericInformation _sgInformation = null;
	private WlanInformation _wlanInformation = null;
	private MmtelInformation _mmtelInformation = null;
	private SmsInformation _smsInformation = null;
	private MbmsInformation _mbmsInformation = null;
	private PocInformation _pocInformation = null;
	private LcsInformation _lcsInformation = null;
	private MmsInformation _mmsInformation = null;
	private PSInformation _psInformation = null;
	private ImsInformation _imsInformation = null;

	/**
	 * Constructor for this class.
	 */
	public ServiceInformation() {}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public ServiceInformation(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		this(avp.getValue(), version);
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param data The avp data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public ServiceInformation(byte[] data, Version version)
			throws DiameterMissingAVPException {
		if (data == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		DiameterAVPDefinition def = ChargingUtils.getSubscriptionIdAVP();
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addSubscriptionId(new SubscriptionId(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getPsInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPSInformation(new PSInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getWlanInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setWlanInformation(new WlanInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getImsInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setImsInformation(new ImsInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getMmsInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMmsInformation(new MmsInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getLcsInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setLcsInformation(new LcsInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getPocInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPocInformation(new PocInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getMbmsInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMbmsInformation(new MbmsInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getSmsInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSmsInformation(new SmsInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getMmtelInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMmtelInformation(new MmtelInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getServiceGenericInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServiceGenericInformation(new ServiceGenericInformation(searchedAvp, version));
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
		DiameterAVPDefinition def = ChargingUtils.getServiceInformationAVP(version);
		if (def == null) {
			return null;
		}

		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		Iterable<SubscriptionId> ids = getSubscriptionIds();
		if (ids.iterator().hasNext()) {
			def = ChargingUtils.getSubscriptionIdAVP();
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (SubscriptionId id : ids) {
					avp.addValue(id.toAvp().getValue(), false);
				}
				l.add(avp);
			}
		}

		if (getPSInformation() != null) {
			DiameterAVP avp = getPSInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getWlanInformation() != null) {
			DiameterAVP avp = getWlanInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getWlanInformation() != null) {
			DiameterAVP avp = getWlanInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getImsInformation() != null) {
			DiameterAVP avp = getImsInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getMmsInformation() != null) {
			DiameterAVP avp = getMmsInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getLcsInformation() != null) {
			DiameterAVP avp = getLcsInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getPocInformation() != null) {
			DiameterAVP avp = getPocInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getMbmsInformation() != null) {
			DiameterAVP avp = getMbmsInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getSmsInformation() != null) {
			DiameterAVP avp = getSmsInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getMmtelInformation() != null) {
			DiameterAVP avp = getMmtelInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getServiceGenericInformation() != null) {
			DiameterAVP avp = getServiceGenericInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Adds a subcription identifier.
	 * 
	 * @param identifier The identifier to be added.
	 */
	public void addSubscriptionId(SubscriptionId identifier) {
		if (identifier != null) {
			_subscriptionIds.add(identifier);
		}
	}

	/**
	 * Gets the subcription identifiers.
	 * 
	 * @return The subcription identifiers.
	 */
	public Iterable<SubscriptionId> getSubscriptionIds() {
		return _subscriptionIds;
	}

	/**
	 * Gets the PS information.
	 * 
	 * @return The PS information.
	 */
	public PSInformation getPSInformation() {
		return _psInformation;
	}

	/**
	 * Sets the PS information.
	 * 
	 * @param info The PS information.
	 */
	public void setPSInformation(PSInformation info) {
		_psInformation = info;
	}

	/**
	 * Gets the Wlan information.
	 * 
	 * @return The Wlan information.
	 */
	public WlanInformation getWlanInformation() {
		return _wlanInformation;
	}

	/**
	 * Sets the Wlan information.
	 * 
	 * @param info The Wlan information.
	 */
	public void setWlanInformation(WlanInformation info) {
		_wlanInformation = info;
	}

	/**
	 * Gets the Ims information.
	 * 
	 * @return The Ims information.
	 */
	public ImsInformation getImsInformation() {
		return _imsInformation;
	}

	/**
	 * Sets the Ims information.
	 * 
	 * @param info The Ims information.
	 */
	public void setImsInformation(ImsInformation info) {
		_imsInformation = info;
	}

	/**
	 * Gets the Mms information.
	 * 
	 * @return The Mms information.
	 */
	public MmsInformation getMmsInformation() {
		return _mmsInformation;
	}

	/**
	 * Sets the Mms information.
	 * 
	 * @param info The Mms information.
	 */
	public void setMmsInformation(MmsInformation info) {
		_mmsInformation = info;
	}

	/**
	 * Gets the Lcs information.
	 * 
	 * @return The Lcs information.
	 */
	public LcsInformation getLcsInformation() {
		return _lcsInformation;
	}

	/**
	 * Sets the Lcs information.
	 * 
	 * @param info The Lcs information.
	 */
	public void setLcsInformation(LcsInformation info) {
		_lcsInformation = info;
	}

	/**
	 * Gets the Poc information.
	 * 
	 * @return The Poc information.
	 */
	public PocInformation getPocInformation() {
		return _pocInformation;
	}

	/**
	 * Sets the Poc information.
	 * 
	 * @param info The Poc information.
	 */
	public void setPocInformation(PocInformation info) {
		_pocInformation = info;
	}

	/**
	 * Gets the Mbms information.
	 * 
	 * @return The Mbms information.
	 */
	public MbmsInformation getMbmsInformation() {
		return _mbmsInformation;
	}

	/**
	 * Sets the Mbms information.
	 * 
	 * @param info The Mbms information.
	 */
	public void setMbmsInformation(MbmsInformation info) {
		_mbmsInformation = info;
	}

	/**
	 * Gets the Sms information.
	 * 
	 * @return The Sms information.
	 */
	public SmsInformation getSmsInformation() {
		return _smsInformation;
	}

	/**
	 * Sets the Sms information.
	 * 
	 * @param info The Sms information.
	 */
	public void setSmsInformation(SmsInformation info) {
		_smsInformation = info;
	}

	/**
	 * Gets the Mmtel information.
	 * 
	 * @return The Mmtel information.
	 */
	public MmtelInformation getMmtelInformation() {
		return _mmtelInformation;
	}

	/**
	 * Sets the Mmtel information.
	 * 
	 * @param info The Mmtel information.
	 */
	public void setMmtelInformation(MmtelInformation info) {
		_mmtelInformation = info;
	}

	/**
	 * Gets the ServiceGeneric information.
	 * 
	 * @return The ServiceGeneric information.
	 */
	public ServiceGenericInformation getServiceGenericInformation() {
		return _sgInformation;
	}

	/**
	 * Sets the ServiceGeneric information.
	 * 
	 * @param info The ServiceGeneric information.
	 */
	public void setServiceGenericInformation(ServiceGenericInformation info) {
		_sgInformation = info;
	}

}
