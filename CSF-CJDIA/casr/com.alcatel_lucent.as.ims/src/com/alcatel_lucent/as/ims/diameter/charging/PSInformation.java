package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.Address;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.DynamicAddressFlag;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.PdpContextType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ServingNodeType;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.ThreeGppPdpType;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.TimeFormat;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The PS-Information AVP wrapper.
 */
public class PSInformation {

	private byte[] _3gppChargingId = null;
	private ThreeGppPdpType _3gppPdpType = null;
	private Address _pdpAddress = null;
	private DynamicAddressFlag _dynamicAddressFlag = null;
	private String _3gppGprsNegociatedQosProfile = null;
	private Address _sgsnAddress = null;
	private Address _ggsnAddress = null;
	private Address _cgAddress = null;
	private ServingNodeType _servingNodeType = null;
	private String _3gppImsiMccMnc = null;
	private String _3gppGgsnMccMnc = null;
	private byte[] _3gppNsapi = null;
	private String _calledStationId = null;
	private String _3gppSessionStopIindicator = null;
	private String _3gppSelectionMode = null;
	private String _3gppChargingCharacteristics = null;
	private String _3gppSgsnMccMnc = null;
	private byte[] _3gppMsTimezone = null;
	private List<String> _chargingRuleBaseNames = new ArrayList<String>();
	private byte[] _3gppUserLocationInfo = null;
	private String _3gpp2Bsid = null;
	private byte[] _3gppRatType = null;
	private PsFurnishChargingInformation _psFurnishChargingInformation = null;
	private PdpContextType _pdpContextType = null;
	private OfflineCharging _offlineCharging = null;
	private List<TrafficDataVolumes> _trafficDataVolumes = new ArrayList<TrafficDataVolumes>();
	private List<ServiceDataContainer> _serviceDataContainers = new ArrayList<ServiceDataContainer>();
	private UserEquipmentInfo _userEquipmentInfo = null;
	private TerminalInformation _terminalInformation = null;
	private Date _startTime = null;
	private Date _stopTime = null;
	private Integer _changeCondition = null;
	private Integer _diagnostics = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 * @exception DiameterMissingAVPException if a mandatory AVP is missing.
	 */
	public PSInformation(DiameterAVP avp, Version version)
			throws DiameterMissingAVPException {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] data = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.get3gppChargingIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppChargingId(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.get3gppPdpTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppPdpType(ThreeGppPdpType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getPdpAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPdpAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getDynamicAddressFlagAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDynamicAddressFlag(DynamicAddressFlag.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.get3gppGprsNegociatedQosProfileAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppGprsNegociatedQosProfile(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getSgsnAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setSgsnAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getGgsnAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setGgsnAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getCgAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setCgAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getServingNodeTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setServingNodeType(ServingNodeType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.get3gppImsiMccMncAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppImsiMccMnc(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppGgsnMccMncAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppGgsnMccMnc(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppNsapiAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppNsapi(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getCalledStationIdAVP();
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setCalledStationId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppSessionStopIndicatorAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppSessionStopIindicator(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppChargingCharacteristicsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppChargingCharacteristics(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppSelectionModeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppSelectionMode(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppSgsnMccMncAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppSgsnMccMnc(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppMsTimeZoneAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppMsTimezone(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getChargingRuleBaseNameAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addChargingRuleBaseNames(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
			}
		}

		def = ChargingUtils.get3gppUserLocationInfoAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppUserLocationInfo(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.get3gpp2BsidAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gpp2Bsid(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.get3gppRatTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				set3gppRatType(searchedAvp.getValue());
			}
		}

		def = ChargingUtils.getPsFurnishChargingInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPsFurnishChargingInformation(new PsFurnishChargingInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getPdpContextTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setPdpContextType(PdpContextType.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getOfflineChargingAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setOfflineCharging(new OfflineCharging(searchedAvp, version));
			}
		}

		def = ChargingUtils.getTrafficDataVolumesAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addTrafficDataVolumes(new TrafficDataVolumes(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getServiceDataContainerAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addServiceDataContainer(new ServiceDataContainer(searchedAvp.getValue(i), version));
				}
			}
		}

		def = ChargingUtils.getUserEquipmentInfoAVP();
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setUserEquipmentInfo(new UserEquipmentInfo(searchedAvp, version));
			}
		}

		def = ChargingUtils.getTerminalInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setTerminalInformation(new TerminalInformation(searchedAvp, version));
			}
		}

		def = ChargingUtils.getStartTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setStartTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getStopTimeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setStopTime(new Date(TimeFormat.getTime(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getChangeConditionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setChangeCondition(Integer.valueOf(Integer32Format.getInteger32(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getDiagnosticsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, data, false);
			if (searchedAvp != null) {
				setDiagnostics(Integer.valueOf(Integer32Format.getInteger32(searchedAvp.getValue(), 0)));
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
		DiameterAVPDefinition def = ChargingUtils.getPsInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> l = new ArrayList<DiameterAVP>();

		if (get3gppChargingId() != null) {
			def = ChargingUtils.get3gppChargingIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(get3gppChargingId(), false);
				l.add(avp);
			}
		}

		if (get3gppPdpType() != null) {
			def = ChargingUtils.get3gppPdpTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(get3gppPdpType().getValue()), false);
				l.add(avp);
			}
		}

		if (getPdpAddress() != null) {
			def = ChargingUtils.getPdpAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getPdpAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getDynamicAddressFlag() != null) {
			def = ChargingUtils.getDynamicAddressFlagAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getDynamicAddressFlag().getValue()), false);
				l.add(avp);
			}
		}

		if (get3gppGprsNegociatedQosProfile() != null) {
			def = ChargingUtils.get3gppPdpTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppGprsNegociatedQosProfile()), false);
				l.add(avp);
			}
		}

		if (getSgsnAddress() != null) {
			def = ChargingUtils.getSgsnAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getSgsnAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getGgsnAddress() != null) {
			def = ChargingUtils.getGgsnAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getGgsnAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getCgAddress() != null) {
			def = ChargingUtils.getCgAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getCgAddress().getAvpValue(), false);
				l.add(avp);
			}
		}

		if (getServingNodeType() != null) {
			def = ChargingUtils.getServingNodeTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getServingNodeType().getValue()), false);
				l.add(avp);
			}
		}

		if (get3gppImsiMccMnc() != null) {
			def = ChargingUtils.get3gppImsiMccMncAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppImsiMccMnc()), false);
				l.add(avp);
			}
		}

		if (get3gppGgsnMccMnc() != null) {
			def = ChargingUtils.get3gppGgsnMccMncAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppGgsnMccMnc()), false);
				l.add(avp);
			}
		}

		if (get3gppNsapi() != null) {
			def = ChargingUtils.get3gppNsapiAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(get3gppNsapi(), false);
				l.add(avp);
			}
		}

		if (getCalledStationId() != null) {
			def = ChargingUtils.getCalledStationIdAVP();
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getCalledStationId()), false);
				l.add(avp);
			}
		}

		if (get3gppSessionStopIindicator() != null) {
			def = ChargingUtils.get3gppSessionStopIndicatorAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppSessionStopIindicator()), false);
				l.add(avp);
			}
		}

		if (get3gppSelectionMode() != null) {
			def = ChargingUtils.get3gppSelectionModeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppSelectionMode()), false);
				l.add(avp);
			}
		}

		if (get3gppChargingCharacteristics() != null) {
			def = ChargingUtils.get3gppChargingCharacteristicsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppChargingCharacteristics()), false);
				l.add(avp);
			}
		}

		if (get3gppSgsnMccMnc() != null) {
			def = ChargingUtils.get3gppSgsnMccMncAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gppSgsnMccMnc()), false);
				l.add(avp);
			}
		}

		if (get3gppMsTimezone() != null) {
			def = ChargingUtils.get3gppMsTimeZoneAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(get3gppMsTimezone(), false);
				l.add(avp);
			}
		}

		Iterable<String> names = getChargingRuleBaseNames();
		if (names.iterator().hasNext()) {
			def = ChargingUtils.getChargingRuleBaseNameAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String name : names) {
					avp.addValue(UTF8StringFormat.toUtf8String(name), false);
				}
				l.add(avp);
			}
		}

		if (get3gppUserLocationInfo() != null) {
			def = ChargingUtils.get3gppUserLocationInfoAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(get3gppUserLocationInfo(), false);
				l.add(avp);
			}
		}

		if (get3gpp2Bsid() != null) {
			def = ChargingUtils.get3gpp2BsidAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(get3gpp2Bsid()), false);
				l.add(avp);
			}
		}

		if (get3gppRatType() != null) {
			def = ChargingUtils.get3gppRatTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(get3gppRatType(), false);
				l.add(avp);
			}
		}

		if (getPsFurnishChargingInformation() != null) {
			DiameterAVP avp = getPsFurnishChargingInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getPdpContextType() != null) {
			def = ChargingUtils.getPdpContextTypeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getPdpContextType().getValue()), false);
				l.add(avp);
			}
		}

		if (getOfflineCharging() != null) {
			DiameterAVP avp = getOfflineCharging().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		Iterable<TrafficDataVolumes> volumes = getTrafficDataVolumes();
		if (volumes.iterator().hasNext()) {
			def = ChargingUtils.getTrafficDataVolumesAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (TrafficDataVolumes volume : volumes) {
					avp.addValue(volume.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		Iterable<ServiceDataContainer> containers = getServiceDataContainers();
		if (containers.iterator().hasNext()) {
			def = ChargingUtils.getServiceDataContainerAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (ServiceDataContainer container : containers) {
					avp.addValue(container.toAvp(version).getValue(), false);
				}
				l.add(avp);
			}
		}

		if (getUserEquipmentInfo() != null) {
			DiameterAVP avp = getUserEquipmentInfo().toAvp();
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getTerminalInformation() != null) {
			DiameterAVP avp = getTerminalInformation().toAvp(version);
			if (avp != null) {
				l.add(avp);
			}
		}

		if (getStartTime() != null) {
			def = ChargingUtils.getStartTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getStartTime().getTime()), false);
				l.add(avp);
			}
		}

		if (getStopTime() != null) {
			def = ChargingUtils.getStopTimeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(TimeFormat.toTime(getStopTime().getTime()), false);
				l.add(avp);
			}
		}

		if (getChangeCondition() != null) {
			def = ChargingUtils.getChangeConditionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Integer32Format.toInteger32(getChangeCondition()), false);
				l.add(avp);
			}
		}

		if (getDiagnostics() != null) {
			def = ChargingUtils.getDiagnosticsAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(Integer32Format.toInteger32(getDiagnostics()), false);
				l.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(l), false);
		return res;
	}

	/**
	 * Sets the 3GPP-Charging-Id.
	 * 
	 * @param id The id.
	 */
	public void set3gppChargingId(byte[] id) {
		_3gppChargingId = ChargingUtils.copyArray(id);
	}

	/**
	 * Gets the 3GPP-Charging-Id.
	 * 
	 * @return The id.
	 */
	public byte[] get3gppChargingId() {
		return ChargingUtils.copyArray(_3gppChargingId);
	}

	/**
	 * Sets the 3GPP-Pdp-Type.
	 * 
	 * @param type The type.
	 */
	public void set3gppPdpType(ThreeGppPdpType type) {
		_3gppPdpType = type;
	}

	/**
	 * Gets the 3GPP-Pdp-Type.
	 * 
	 * @return The type.
	 */
	public ThreeGppPdpType get3gppPdpType() {
		return _3gppPdpType;
	}

	/**
	 * Sets the PDP-Address.
	 * 
	 * @param address The address.
	 */
	public void setPdpAddress(Address address) {
		_pdpAddress = address;
	}

	/**
	 * Gets the PDP-Address.
	 * 
	 * @return The address.
	 */
	public Address getPdpAddress() {
		return _pdpAddress;
	}

	/**
	 * Sets the Dynamic-Address-Flag.
	 * 
	 * @param flag The flag.
	 */
	public void setDynamicAddressFlag(DynamicAddressFlag flag) {
		_dynamicAddressFlag = flag;
	}

	/**
	 * Gets the Dynamic-Address-Flag.
	 * 
	 * @return The flag.
	 */
	public DynamicAddressFlag getDynamicAddressFlag() {
		return _dynamicAddressFlag;
	}

	/**
	 * Sets the 3GPP-Gprs-Negociated-QoS-Profile.
	 * 
	 * @param profile The profile.
	 */
	public void set3gppGprsNegociatedQosProfile(String profile) {
		this._3gppGprsNegociatedQosProfile = profile;
	}

	/**
	 * Gets the 3GPP-Gprs-Negociated-QoS-Profile.
	 * 
	 * @return The profile.
	 */
	public String get3gppGprsNegociatedQosProfile() {
		return _3gppGprsNegociatedQosProfile;
	}

	/**
	 * Sets the SGSN-Address.
	 * 
	 * @param address The address.
	 */
	public void setSgsnAddress(Address address) {
		_sgsnAddress = address;
	}

	/**
	 * Gets the SGSN-Address.
	 * 
	 * @return The address.
	 */
	public Address getSgsnAddress() {
		return _sgsnAddress;
	}

	/**
	 * Sets the GGSN-Address.
	 * 
	 * @param address The address.
	 */
	public void setGgsnAddress(Address address) {
		_ggsnAddress = address;
	}

	/**
	 * Gets the GGSN-Address.
	 * 
	 * @return The address.
	 */
	public Address getGgsnAddress() {
		return _ggsnAddress;
	}

	/**
	 * Sets the CG-Address.
	 * 
	 * @param address The address.
	 */
	public void setCgAddress(Address address) {
		_cgAddress = address;
	}

	/**
	 * Gets the CG-Address.
	 * 
	 * @return The address.
	 */
	public Address getCgAddress() {
		return _cgAddress;
	}

	/**
	 * Sets the Serving-Node-Type.
	 * 
	 * @param type The type.
	 */
	public void setServingNodeType(ServingNodeType type) {
		_servingNodeType = type;
	}

	/**
	 * Gets the Serving-Node-Type.
	 * 
	 * @return The type.
	 */
	public ServingNodeType getServingNodeType() {
		return _servingNodeType;
	}

	/**
	 * Sets the 3GPP-IMSI-MCC-MNC.
	 * 
	 * @param value The value.
	 */
	public void set3gppImsiMccMnc(String value) {
		_3gppImsiMccMnc = value;
	}

	/**
	 * Gets the 3GPP-IMSI-MCC-MNC.
	 * 
	 * @return The value.
	 */
	public String get3gppImsiMccMnc() {
		return _3gppImsiMccMnc;
	}

	/**
	 * Sets the 3GPP-GGSN-MCC-MNC.
	 * 
	 * @param value The value.
	 */
	public void set3gppGgsnMccMnc(String value) {
		_3gppGgsnMccMnc = value;
	}

	/**
	 * Gets the 3GPP-GGSN-MCC-MNC.
	 * 
	 * @return The value.
	 */
	public String get3gppGgsnMccMnc() {
		return _3gppGgsnMccMnc;
	}

	/**
	 * Sets the 3GPP-NSAPI.
	 * 
	 * @param value The value.
	 */
	public void set3gppNsapi(byte[] value) {
		_3gppNsapi = ChargingUtils.copyArray(value);
	}

	/**
	 * Gets the 3GPP-NSAPI.
	 * 
	 * @return The value.
	 */
	public byte[] get3gppNsapi() {
		return ChargingUtils.copyArray(_3gppNsapi);
	}

	/**
	 * Sets the Called-Station-Id.
	 * 
	 * @param id The id.
	 */
	public void setCalledStationId(String id) {
		_calledStationId = id;
	}

	/**
	 * Gets the Called-Station-Id.
	 * 
	 * @return The id.
	 */
	public String getCalledStationId() {
		return _calledStationId;
	}

	/**
	 * Sets the 3GPP-Session-Stop-Indicator.
	 * 
	 * @param indicator The indicator.
	 */
	public void set3gppSessionStopIindicator(String indicator) {
		_3gppSessionStopIindicator = indicator;
	}

	/**
	 * Gets the 3GPP-Session-Stop-Indicator.
	 * 
	 * @return The indicator.
	 */
	public String get3gppSessionStopIindicator() {
		return _3gppSessionStopIindicator;
	}

	/**
	 * Sets the 3GPP-Selection-Mode.
	 * 
	 * @param mode The mode.
	 */
	public void set3gppSelectionMode(String mode) {
		_3gppSelectionMode = mode;
	}

	/**
	 * Gets the 3GPP-Selection-Mode.
	 * 
	 * @return The mode.
	 */
	public String get3gppSelectionMode() {
		return _3gppSelectionMode;
	}

	/**
	 * Sets the 3GPP-Charging-Characteristics.
	 * 
	 * @param characteristics The characteristics.
	 */
	public void set3gppChargingCharacteristics(String characteristics) {
		_3gppChargingCharacteristics = characteristics;
	}

	/**
	 * Gets the 3GPP-Charging-Characteristics.
	 * 
	 * @return The characteristics.
	 */
	public String get3gppChargingCharacteristics() {
		return _3gppChargingCharacteristics;
	}

	/**
	 * Sets the 3GPP-SGSN-MCC-MNC.
	 * 
	 * @param value The value.
	 */
	public void set3gppSgsnMccMnc(String value) {
		_3gppSgsnMccMnc = value;
	}

	/**
	 * Gets the 3GPP-SGSN-MCC-MNC.
	 * 
	 * @return The value.
	 */
	public String get3gppSgsnMccMnc() {
		return _3gppSgsnMccMnc;
	}

	/**
	 * Sets the 3GPP-MS-TimeZone.
	 * 
	 * @param timezone The timezone.
	 */
	public void set3gppMsTimezone(byte[] timezone) {
		_3gppMsTimezone = ChargingUtils.copyArray(timezone);
	}

	/**
	 * Gets the 3GPP-MS-TimeZone.
	 * 
	 * @return The timezone.
	 */
	public byte[] get3gppMsTimezone() {
		return ChargingUtils.copyArray(_3gppMsTimezone);
	}

	/**
	 * Adds a Charging-Rule-Base-Name.
	 * 
	 * @param name The name to be added.
	 */
	public void addChargingRuleBaseNames(String name) {
		if (name != null) {
			_chargingRuleBaseNames.add(name);
		}
	}

	/**
	 * Gets the Charging-Rule-Base-Name list.
	 * 
	 * @return The names.
	 */
	public Iterable<String> getChargingRuleBaseNames() {
		return _chargingRuleBaseNames;
	}

	/**
	 * Sets the 3GPP-User-Location-Info.
	 * 
	 * @param info The info.
	 */
	public void set3gppUserLocationInfo(byte[] info) {
		_3gppUserLocationInfo = ChargingUtils.copyArray(info);
	}

	/**
	 * Gets the 3GPP-User-Location-Info.
	 * 
	 * @return The info.
	 */
	public byte[] get3gppUserLocationInfo() {
		return ChargingUtils.copyArray(_3gppUserLocationInfo);
	}

	/**
	 * Sets the 3GPP2-BSID.
	 * 
	 * @param id The id.
	 */
	public void set3gpp2Bsid(String id) {
		_3gpp2Bsid = id;
	}

	/**
	 * Gets the 3GPP2-BSID.
	 * 
	 * @return The id.
	 */
	public String get3gpp2Bsid() {
		return _3gpp2Bsid;
	}

	/**
	 * Sets the 3GPP-RAT-Type.
	 * 
	 * @param type The type.
	 */
	public void set3gppRatType(byte[] type) {
		_3gppRatType = ChargingUtils.copyArray(type);
	}

	/**
	 * Gets the 3GPP-RAT-Type.
	 * 
	 * @return The type.
	 */
	public byte[] get3gppRatType() {
		return ChargingUtils.copyArray(_3gppRatType);
	}

	/**
	 * Sets the PS-Furnish-Charging-Information.
	 * 
	 * @param information The information.
	 */
	public void setPsFurnishChargingInformation(PsFurnishChargingInformation information) {
		_psFurnishChargingInformation = information;
	}

	/**
	 * Gets the PS-Furnish-Charging-Information.
	 * 
	 * @return The information.
	 */
	public PsFurnishChargingInformation getPsFurnishChargingInformation() {
		return _psFurnishChargingInformation;
	}

	/**
	 * Sets the PDP-Context-Type.
	 * 
	 * @param type The type.
	 */
	public void setPdpContextType(PdpContextType type) {
		_pdpContextType = type;
	}

	/**
	 * Gets the PDP-Context-Type.
	 * 
	 * @return The type.
	 */
	public PdpContextType getPdpContextType() {
		return _pdpContextType;
	}

	/**
	 * Sets the Offline-Charging.
	 * 
	 * @param offlineCharging The offline charging.
	 */
	public void setOfflineCharging(OfflineCharging offlineCharging) {
		_offlineCharging = offlineCharging;
	}

	/**
	 * Gets the Offline-Charging.
	 * 
	 * @return The offline charging.
	 */
	public OfflineCharging getOfflineCharging() {
		return _offlineCharging;
	}

	/**
	 * Adds a Traffic-Data-Volumes.
	 * 
	 * @param volumes The volumes.
	 */
	public void addTrafficDataVolumes(TrafficDataVolumes volumes) {
		_trafficDataVolumes.add(volumes);
	}

	/**
	 * Gets the Traffic-Data-Volumes list.
	 * 
	 * @return The volumes.
	 */
	public Iterable<TrafficDataVolumes> getTrafficDataVolumes() {
		return _trafficDataVolumes;
	}

	/**
	 * Adds a Service-Data-Container.
	 * 
	 * @param container The container.
	 */
	public void addServiceDataContainer(ServiceDataContainer container) {
		_serviceDataContainers.add(container);
	}

	/**
	 * Gets the Service-Data-Container list.
	 * 
	 * @return The containers.
	 */
	public List<ServiceDataContainer> getServiceDataContainers() {
		return _serviceDataContainers;
	}

	/**
	 * Sets the User-Equipment-Info.
	 * 
	 * @param info The info.
	 */
	public void setUserEquipmentInfo(UserEquipmentInfo info) {
		_userEquipmentInfo = info;
	}

	/**
	 * Gets the User-Equipment-Info.
	 * 
	 * @return The info.
	 */
	public UserEquipmentInfo getUserEquipmentInfo() {
		return _userEquipmentInfo;
	}

	/**
	 * Sets the Terminal-Information.
	 * 
	 * @param iInformation The iInformation.
	 */
	public void setTerminalInformation(TerminalInformation iInformation) {
		_terminalInformation = iInformation;
	}

	/**
	 * Gets the _terminalInformation.
	 * 
	 * @return The iInformation.
	 */
	public TerminalInformation getTerminalInformation() {
		return _terminalInformation;
	}

	/**
	 * Sets the Start-Time.
	 * 
	 * @param time The time.
	 */
	public void setStartTime(Date time) {
		if (time == null) {
			_startTime = null;
		} else {
			_startTime = (Date) time.clone();
		}
	}

	/**
	 * Gets the Start-Time.
	 * 
	 * @return The time.
	 */
	public Date getStartTime() {
		if (_startTime == null) {
			return null;
		}
		return (Date) _startTime.clone();
	}

	/**
	 * Sets the Stop-Time.
	 * 
	 * @param time The time.
	 */
	public void setStopTime(Date time) {
		if (time == null) {
			_stopTime = null;
		} else {
			_stopTime = (Date) time.clone();
		}
	}

	/**
	 * Gets the Stop-Time.
	 * 
	 * @return The time.
	 */
	public Date getStopTime() {
		if (_stopTime == null) {
			return null;
		}
		return (Date) _stopTime.clone();
	}

	/**
	 * Sets the Change-Condition.
	 * 
	 * @param condition The condition.
	 */
	public void setChangeCondition(Integer condition) {
		_changeCondition = condition;
	}

	/**
	 * Gets the Change-Condition.
	 * 
	 * @return The condition.
	 */
	public Integer getChangeCondition() {
		return _changeCondition;
	}

	/**
	 * Sets the Diagnostics.
	 * 
	 * @param diagnostics The diagnostics.
	 */
	public void setDiagnostics(Integer diagnostics) {
		_diagnostics = diagnostics;
	}

	/**
	 * Gets the Diagnostics.
	 * 
	 * @return The diagnostics.
	 */
	public Integer getDiagnostics() {
		return _diagnostics;
	}

}
