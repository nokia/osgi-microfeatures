package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.TypeNumber;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

/**
 * The MM-Content-Type AVP wrapper.
 */
public class MmContentType {

	private TypeNumber _typeNumber = null;
	private String _additionalTypeInformation = null;
	private Long _contentSize = null;
	private List<AdditionalContentInformation> _additionalContentInformations = new ArrayList<AdditionalContentInformation>();

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public MmContentType(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getTypeNumberAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTypeNumber(TypeNumber.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getAdditionalTypeInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setContentSize(Long.valueOf(Unsigned32Format.getUnsigned32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getContentSizeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setAdditionalTypeInformation(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getAdditionalContentInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				addAdditionalContentInformation(new AdditionalContentInformation(searchedAvp, version));
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
		DiameterAVPDefinition def = ChargingUtils.getMmContentTypeAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (getTypeNumber() != null) {
			def = ChargingUtils.getTypeNumberAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getTypeNumber().getValue()), false);
				l.add(avp);
			}
		}

		if (getAdditionalTypeInformation() != null) {
			def = ChargingUtils.getAdditionalTypeInformationAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getAdditionalTypeInformation()), false);
				l.add(avp);
			}
		}

		if (getContentSize() != null) {
			def = ChargingUtils.getContentSizeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Unsigned32Format.toUnsigned32(getContentSize()), false);
				l.add(avp);
			}
		}

		Iterable<AdditionalContentInformation> infos = getAdditionalContentInformations();
		if (infos.iterator().hasNext()) {
			def = ChargingUtils.getAdditionalContentInformationAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (AdditionalContentInformation info : infos) {
					avp.addValue(info.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the Type-Number.
	 * 
	 * @param number The number.
	 */
	public void setTypeNumber(TypeNumber number) {
		_typeNumber = number;
	}

	/**
	 * Gets the Type-Number.
	 * 
	 * @return The number.
	 */
	public TypeNumber getTypeNumber() {
		return _typeNumber;
	}

	/**
	 * Sets the Additional-Type-Information.
	 * 
	 * @param information The information.
	 */
	public void setAdditionalTypeInformation(String information) {
		_additionalTypeInformation = information;
	}

	/**
	 * Gets the Additional-Type-Information.
	 * 
	 * @return The information.
	 */
	public String getAdditionalTypeInformation() {
		return _additionalTypeInformation;
	}

	/**
	 * Sets the Content-Size.
	 * 
	 * @param size The size.
	 */
	public void setContentSize(Long size) {
		_contentSize = size;
	}

	/**
	 * Gets the Content-Size.
	 * 
	 * @return The size.
	 */
	public Long getContentSize() {
		return _contentSize;
	}

	/**
	 * Sets the Additional-Content-Information.
	 * 
	 * @param information The information.
	 */
	public void addAdditionalContentInformation(AdditionalContentInformation information) {
		if (information != null) {
			_additionalContentInformations.add(information);
		}
	}

	/**
	 * Gets the Additional-Content-Information.
	 * 
	 * @return The information list.
	 */
	public Iterable<AdditionalContentInformation> getAdditionalContentInformations() {
		return _additionalContentInformations;
	}

}
