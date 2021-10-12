// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.FileRepairSupported;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.Mbms2g3gIndicator;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MbmsServiceType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.MbmsUserServiceType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The MBMS-Information AVP wrapper.
 */
public class MbmsInformation {

	private byte[] _tmgi = null;
	private MbmsServiceType _mbmsServiceType = null;
	private MbmsUserServiceType _mbmsUserServiceType = null;
	private FileRepairSupported _fileRepairSupported = null;
	private String _requiredMbmsBearerCapabilities = null;
	private Mbms2g3gIndicator _mbms2g3gIndicator = null;
	private String _rai = null;
	private List<byte[]> _mbmsServiceAreas = new ArrayList<byte[]>();
	private byte[] _mbmsSessionIdentity = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MbmsInformation(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}

		byte[] data = avp.getValue();
		DiameterAVPDefinition def = ChargingUtils.getTmgiAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTmgi(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getMbmsServiceTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMbmsServiceType(MbmsServiceType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getMbmsUserServiceTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMbmsUserServiceType(MbmsUserServiceType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getFileRepairSupportedAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setFileRepairSupported(FileRepairSupported.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getRequiredMbmsBearerCapabilitiesAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setRequiredMbmsBearerCapabilities(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getMbms2g3gIndicatorAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMbms2g3gIndicator(Mbms2g3gIndicator.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getRaiAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setRai(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getMbmsServiceAreaAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addMbmsServiceArea(searchedAvp.getValue(i));
				}
			}
		}

		def = ChargingUtils.getMbmsSessionIdentityAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setMbmsSessionIdentity(searchedAvp.getValue());
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
		DiameterAVPDefinition def = ChargingUtils.getMbmsInformationAVP(version);
		if (def == null) {
			return null;
		}
		
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getTmgi() != null) {
			def = ChargingUtils.getTmgiAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getTmgi(), false);
				l.add(avp);
			}
		}

		if (getMbmsServiceType() != null) {
			def = ChargingUtils.getMbmsServiceTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getMbmsServiceType().getValue()), false);
				l.add(avp);
			}
		}

		if (getMbmsUserServiceType() != null) {
			def = ChargingUtils.getMbmsUserServiceTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getMbmsUserServiceType().getValue()), false);
				l.add(avp);
			}
		}

		if (getRequiredMbmsBearerCapabilities() != null) {
			def = ChargingUtils.getRequiredMbmsBearerCapabilitiesAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getRequiredMbmsBearerCapabilities()), false);
				l.add(avp);
			}
		}

		if (getMbms2g3gIndicator() != null) {
			def = ChargingUtils.getMbms2g3gIndicatorAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getMbms2g3gIndicator().getValue()), false);
				l.add(avp);
			}
		}

		if (getRai() != null) {
			def = ChargingUtils.getRaiAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getRai()), false);
				l.add(avp);
			}
		}

		Iterable<byte[]> areas = getMbmsServiceAreas();
		if (areas.iterator().hasNext()) {
			def = ChargingUtils.getMbmsServiceAreaAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (byte[] area : areas) {
					avp.addValue(area, false);
				}
				l.add(avp);
			}
		}

		if (getMbmsSessionIdentity() != null) {
			def = ChargingUtils.getMbmsSessionIdentityAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getMbmsSessionIdentity(), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the TMGI.
	 * 
	 * @param identity The identity.
	 */
	public void setTmgi(byte[] identity) {
		_tmgi = ChargingUtils.copyArray(identity);
	}

	/**
	 * Gets the TMGI.
	 * 
	 * @return The identity.
	 */
	public byte[] getTmgi() {
		return ChargingUtils.copyArray(_tmgi);
	}

	/**
	 * Sets the MBMS-Service-Type.
	 * 
	 * @param type The type.
	 */
	public void setMbmsServiceType(MbmsServiceType type) {
		_mbmsServiceType = type;
	}

	/**
	 * Gets the MBMS-Service-Type.
	 * 
	 * @return The TMGI.
	 */
	public MbmsServiceType getMbmsServiceType() {
		return _mbmsServiceType;
	}

	/**
	 * Sets the MBMS-User-Service-Type.
	 * 
	 * @param type The type.
	 */
	public void setMbmsUserServiceType(MbmsUserServiceType type) {
		_mbmsUserServiceType = type;
	}

	/**
	 * Gets the MBMS-User-Service-Type.
	 * 
	 * @return The type.
	 */
	public MbmsUserServiceType getMbmsUserServiceType() {
		return _mbmsUserServiceType;
	}

	/**
	 * Sets the File-Repair-Supported.
	 * 
	 * @param support The support.
	 */
	public void setFileRepairSupported(FileRepairSupported support) {
		_fileRepairSupported = support;
	}

	/**
	 * Gets the File-Repair-Supported.
	 * 
	 * @return The support.
	 */
	public FileRepairSupported getFileRepairSupported() {
		return _fileRepairSupported;
	}

	/**
	 * Sets the Required-MBMS-Bearer-Capabilities.
	 * 
	 * @param capabilities The capabilities.
	 */
	public void setRequiredMbmsBearerCapabilities(String capabilities) {
		_requiredMbmsBearerCapabilities = capabilities;
	}

	/**
	 * Gets the Required-MBMS-Bearer-Capabilities.
	 * 
	 * @return The capabilities.
	 */
	public String getRequiredMbmsBearerCapabilities() {
		return _requiredMbmsBearerCapabilities;
	}

	/**
	 * Sets the MBMS-2G-3G-Indicator.
	 * 
	 * @param indicator The indicator.
	 */
	public void setMbms2g3gIndicator(Mbms2g3gIndicator indicator) {
		_mbms2g3gIndicator = indicator;
	}

	/**
	 * Gets the MBMS-2G-3G-Indicator.
	 * 
	 * @return The indicator.
	 */
	public Mbms2g3gIndicator getMbms2g3gIndicator() {
		return _mbms2g3gIndicator;
	}

	/**
	 * Sets the RAI.
	 * 
	 * @param identity The identity.
	 */
	public void setRai(String identity) {
		_rai = identity;
	}

	/**
	 * Gets the RAI.
	 * 
	 * @return The identity.
	 */
	public String getRai() {
		return _rai;
	}

	/**
	 * Adds a MBMS-Service-Area..
	 * 
	 * @param area The area.
	 */
	public void addMbmsServiceArea(byte[] area) {
		if (area != null) {
			_mbmsServiceAreas.add(ChargingUtils.copyArray(area));
		}
	}

	/**
	 * Gets the MBMS-Service-Area list.
	 * 
	 * @return The areas.
	 */
	public Iterable<byte[]> getMbmsServiceAreas() {
		return _mbmsServiceAreas;
	}

	/**
	 * Sets the MBMS-Session-Identity.
	 * 
	 * @param identity The identity.
	 */
	public void setMbmsSessionIdentity(byte[] identity) {
		_mbmsSessionIdentity = ChargingUtils.copyArray(identity);
	}

	/**
	 * Gets the MBMS-Session-Identity.
	 * 
	 * @return The identity.
	 */
	public byte[] getMbmsSessionIdentity() {
		return ChargingUtils.copyArray(_mbmsSessionIdentity);
	}

}
