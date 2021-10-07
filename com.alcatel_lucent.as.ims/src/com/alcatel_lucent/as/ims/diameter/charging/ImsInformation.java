package com.alcatel_lucent.as.ims.diameter.charging;

import java.util.ArrayList;
import java.util.List;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.Address;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.NodeFunctionality;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.RoleOfNode;
import com.alcatel_lucent.as.ims.diameter.cx.ServerCapabilities;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.EnumeratedFormat;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Integer32Format;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;

/**
 * The IMS-Information AVP wrapper.
 */
public class ImsInformation {

	private static final int DEFAULT_CAUSE_CODE = Integer.MIN_VALUE;

	private EventType _eventType = null;
	private RoleOfNode _roleOfNode = null;
	private NodeFunctionality _nodeFunctionality = null;
	private String _userSessionId = null;
	private List<String> _callingPartyAddresses = new ArrayList<String>();
	private String _calledPartyAddress = null;
	private List<String> _calledAssertedIdentities = new ArrayList<String>();
	private String _numberPortabilityRoutingInformation = null;
	private String _carrierSelectRoutingInformation = null;
	private String _alternateChargedPartyAddress = null;
	private String _requestedPartyAddress = null;
	private List<String> _associatedUris = new ArrayList<String>();
	private TimeStamps _timestamps;
	private List<ApplicationServerInformation> _applicationServerInformations = new ArrayList<ApplicationServerInformation>();
	private List<InterOperatorIdentifier> _interOperatorIdentifiers = new ArrayList<InterOperatorIdentifier>();
	private String _imsChargingIdentifier = null;
	private List<String> _sdpSessionDescriptions = new ArrayList<String>();
	private List<SdpMediaComponent> _sdpMediaComponents = new ArrayList<SdpMediaComponent>();
	private Address _servedPartyIpAddress = null;
	private ServerCapabilities _serverCapabilities = null;
	private TrunkGroupId _trunkGroupId = null;
	private byte[] _bearerService = null;
	private String _serviceId = null;
	private List<ServiceSpecificInfo> _serviceSpecificInfos = new ArrayList<ServiceSpecificInfo>();
	private List<MessageBody> _messageBodies = new ArrayList<MessageBody>();
	private int _causeCode = DEFAULT_CAUSE_CODE;
	private byte[] _accessNetworkInformation;
	private List<EarlyMediaDescription> _earlyMediaDescriptions = new ArrayList<EarlyMediaDescription>();
	private String _imsCommunicationServiceIdentifier = null;

	/**
	 * Constructor for this class.
	 * 
	 * @param role The node functionality.Ar
	 */
	public ImsInformation(NodeFunctionality role) {
		_nodeFunctionality = role;
	}

	/**
	 * Constructor for this class.
	 * 
	 * @param avp The avp which contains the data.
	 * @param version The version of the 3GPP 32.299 document.
	 */
	public ImsInformation(DiameterAVP avp, Version version) {
		if (avp == null || version == null) {
			throw new IllegalArgumentException("null parameter");
		}
		byte[] informationData = avp.getValue();

		DiameterAVPDefinition def = ChargingUtils.getEventTypeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setEventType(new EventType(searchedAvp, version));
			}
		}

		def = ChargingUtils.getRoleOfNodeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setRoleOfNode(RoleOfNode.getData(EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0)));
			}
		}

		def = ChargingUtils.getUserSessionIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setUserSessionId(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getCallingPartyAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addCallingPartyAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
			}
		}

		def = ChargingUtils.getCalledPartyAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setCalledPartyAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getCalledAssertedIdentityAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addCalledAssertedIdentity(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
			}
		}

		def = ChargingUtils.getNumberPortabilityRoutingInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setNumberPortabilityRoutingInformation(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getCarrierSelectRoutingInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setCarrierSelectRoutingInformation(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getAlternateChargedPartyAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setAlternateChargedPartyAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getRequestedPartyAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setRequestedPartyAddress(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getAssociatedUriAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					addAssociatedUri(UTF8StringFormat.getUtf8String(searchedAvp.getValue(i)));
				}
			}
		}

		def = ChargingUtils.getTimeStampsAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setTimeStamps(new TimeStamps(searchedAvp, version));
			}
		}

		def = ChargingUtils.getApplicationServerInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					ApplicationServerInformation info = new ApplicationServerInformation(searchedAvp.getValue(i), version);
					addApplicationServerInformation(info);
				}
			}
		}

		def = ChargingUtils.getInterOperatorIdentifierAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					InterOperatorIdentifier id = new InterOperatorIdentifier(searchedAvp.getValue(i), version);
					addInterOperatorIdentifier(id);
				}
			}
		}

		def = ChargingUtils.getImsChargingIdentifierAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setImsChargingIdentifier(UTF8StringFormat.getUtf8String(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getSdpSessionDescriptionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					String desc = UTF8StringFormat.getUtf8String(searchedAvp.getValue(i));
					addSdpSessionDescription(desc);
				}
			}
		}

		def = ChargingUtils.getSdpMediaComponentAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					SdpMediaComponent component = new SdpMediaComponent(searchedAvp.getValue(i), version);
					addSdpMediaComponent(component);
				}
			}
		}

		def = ChargingUtils.getServedPartyIpAddressAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				setServedPartyIpAddress(new Address(searchedAvp.getValue()));
			}
		}

		def = ChargingUtils.getServerCapabilitiesAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				ServerCapabilities capabilities = new ServerCapabilities(searchedAvp, ChargingUtils.getVersion29229(version));
				setServerCapabilities(capabilities);
			}
		}

		def = ChargingUtils.getTrunkGroupIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				TrunkGroupId id = new TrunkGroupId(searchedAvp, version);
				setTrunkGroupId(id);
			}
		}

		def = ChargingUtils.getBearerServiceAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				byte[] service = searchedAvp.getValue();
				setBearerService(service);
			}
		}

		def = ChargingUtils.getServiceIdAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				String id = UTF8StringFormat.getUtf8String(searchedAvp.getValue());
				setServiceId(id);
			}
		}

		def = ChargingUtils.getServiceSpecificInfoAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					ServiceSpecificInfo info = new ServiceSpecificInfo(searchedAvp.getValue(i), version);
					addServiceSpecificInfo(info);
				}
			}
		}

		def = ChargingUtils.getMessageBodyAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					MessageBody body = new MessageBody(searchedAvp.getValue(i), version);
					addMessageBody(body);
				}
			}
		}

		def = ChargingUtils.getCauseCodeAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				if (def.getDiameterAVPFormat() == Integer32Format.INSTANCE) {
					int code = Integer32Format.getInteger32(searchedAvp.getValue(), 0);
					setCauseCode(code);
				} else if (def.getDiameterAVPFormat() == EnumeratedFormat.INSTANCE) {
					int code = EnumeratedFormat.getEnumerated(searchedAvp.getValue(), 0);
					setCauseCode(code);
				}
			}
		}

		def = ChargingUtils.getAccessNetworkInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				byte[] info = searchedAvp.getValue();
				setAccessNetworkInformation(info);
			}
		}

		def = ChargingUtils.getEarlyMediaDescriptionAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				for (int i = 0; i < searchedAvp.getValueSize(); i++) {
					EarlyMediaDescription description = new EarlyMediaDescription(searchedAvp.getValue(i), version);
					addEarlyMediaDescription(description);
				}
			}
		}

		def = ChargingUtils.getAccessNetworkInformationAVP(version);
		if (def != null) {
			DiameterAVP searchedAvp = GroupedFormat.getDiameterAVP(def, informationData, false);
			if (searchedAvp != null) {
				String id = UTF8StringFormat.getUtf8String(searchedAvp.getValue());
				setImsCommunicationServiceIdentifier(id);
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
		DiameterAVPDefinition def = ChargingUtils.getImsInformationAVP(version);
		if (def == null) {
			return null;
		}
		DiameterAVP res = new DiameterAVP(def);
		List<DiameterAVP> avps = new ArrayList<DiameterAVP>();

		if (getEventType() != null) {
			DiameterAVP avp = getEventType().toAvp(version);
			if (avp != null) {
				avps.add(avp);
			}
		}

		if (getRoleOfNode() != null) {
			def = ChargingUtils.getRoleOfNodeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(EnumeratedFormat.toEnumerated(getRoleOfNode().getValue()), false);
				avps.add(avp);
			}
		}

		def = ChargingUtils.getNodeFunctionalityAVP(version);
		if (def != null) {
			DiameterAVP avp = new DiameterAVP(def);
			avp.setValue(EnumeratedFormat.toEnumerated(getNodeFunctionality().getValue()), false);
			avps.add(avp);
		}

		if (getUserSessionId() != null) {
			def = ChargingUtils.getUserSessionIdAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getUserSessionId()), false);
				avps.add(avp);
			}
		}

		Iterable<String> addresses = getCallingPartyAddresses();
		if (addresses.iterator().hasNext()) {
			def = ChargingUtils.getCalledPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String address : addresses) {
					avp.addValue(UTF8StringFormat.toUtf8String(address), false);
				}
				avps.add(avp);
			}
		}

		if (getCalledPartyAddress() != null) {
			def = ChargingUtils.getCalledPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getCalledPartyAddress()), false);
				avps.add(avp);
			}
		}

		Iterable<String> identities = getCalledAssertedIdentities();
		if (identities.iterator().hasNext()) {
			def = ChargingUtils.getCalledAssertedIdentityAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String id : identities) {
					avp.addValue(UTF8StringFormat.toUtf8String(id), false);
				}
				avps.add(avp);
			}
		}

		if (getNumberPortabilityRoutingInformation() != null) {
			def = ChargingUtils.getNumberPortabilityRoutingInformationAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getNumberPortabilityRoutingInformation()), false);
				avps.add(avp);
			}
		}

		if (getCarrierSelectRoutingInformation() != null) {
			def = ChargingUtils.getCarrierSelectRoutingInformationAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getCarrierSelectRoutingInformation()), false);
				avps.add(avp);
			}
		}

		if (getAlternateChargedPartyAddress() != null) {
			def = ChargingUtils.getAlternateChargedPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getAlternateChargedPartyAddress()), false);
				avps.add(avp);
			}
		}

		if (getRequestedPartyAddress() != null) {
			def = ChargingUtils.getRequestedPartyAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getRequestedPartyAddress()), false);
				avps.add(avp);
			}
		}

		Iterable<String> uris = getAssociatedUris();
		if (uris.iterator().hasNext()) {
			def = ChargingUtils.getAssociatedUriAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String uri : uris) {
					avp.addValue(UTF8StringFormat.toUtf8String(uri), false);
				}
				avps.add(avp);
			}
		}

		if (getTimeStamps() != null) {
			DiameterAVP avp = getTimeStamps().toAvp(version);
			if (avp != null) {
				avps.add(avp);
			}
		}

		Iterable<ApplicationServerInformation> infos = getApplicationServerInformations();
		if (infos.iterator().hasNext()) {
			def = ChargingUtils.getApplicationServerInformationAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (ApplicationServerInformation uri : infos) {
					avp.addValue(uri.toAvp(version).getValue(), false);
				}
				avps.add(avp);
			}
		}

		Iterable<InterOperatorIdentifier> ids = getInterOperatorIdentifiers();
		if (ids.iterator().hasNext()) {
			def = ChargingUtils.getInterOperatorIdentifierAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (InterOperatorIdentifier id : ids) {
					avp.addValue(id.toAvp(version).getValue(), false);
				}
				avps.add(avp);
			}
		}

		if (getImsChargingIdentifier() != null) {
			def = ChargingUtils.getImsChargingIdentifierAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getImsChargingIdentifier()), false);
				avps.add(avp);
			}
		}

		Iterable<String> descs = getSdpSessionDescriptions();
		if (descs.iterator().hasNext()) {
			def = ChargingUtils.getSdpSessionDescriptionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (String desc : descs) {
					avp.addValue(UTF8StringFormat.toUtf8String(desc), false);
				}
				avps.add(avp);
			}
		}

		Iterable<SdpMediaComponent> components = getSdpMediaComponents();
		if (components.iterator().hasNext()) {
			def = ChargingUtils.getSdpSessionDescriptionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (SdpMediaComponent component : components) {
					avp.addValue(component.toAvp(version).getValue(), false);
				}
				avps.add(avp);
			}
		}

		if (getServedPartyIpAddress() != null) {
			def = ChargingUtils.getServedPartyIpAddressAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(getServedPartyIpAddress().getAvpValue(), false);
				avps.add(avp);
			}
		}

		if (getServerCapabilities() != null) {
			Version cxVersion = ChargingUtils.getVersion29229(version);
			def = ChargingUtils.getServerCapabilitiesAVP(cxVersion);
			if (def != null) {
				DiameterAVP avp = getServerCapabilities().toAvp(cxVersion);
				if (avp != null) {
					avps.add(avp);
				}
			}
		}

		if (getTrunkGroupId() != null) {
			DiameterAVP avp = getTrunkGroupId().toAvp(version);
			if (avp != null) {
				avps.add(avp);
			}
		}

		byte[] value = getBearerService();
		if (value != null) {
			def = ChargingUtils.getBearerServiceAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				avps.add(avp);
			}
		}

		Iterable<ServiceSpecificInfo> services = getServiceSpecificInfos();
		if (services.iterator().hasNext()) {
			def = ChargingUtils.getServiceSpecificInfoAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (ServiceSpecificInfo service : services) {
					avp.addValue(service.toAvp(version).getValue(), false);
				}
				avps.add(avp);
			}
		}

		Iterable<MessageBody> bodies = getMessageBodies();
		if (bodies.iterator().hasNext()) {
			def = ChargingUtils.getServiceSpecificInfoAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (MessageBody body : bodies) {
					avp.addValue(body.toAvp(version).getValue(), false);
				}
				avps.add(avp);
			}
		}

		if (getCauseCode() != DEFAULT_CAUSE_CODE) {
			def = ChargingUtils.getCauseCodeAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				if (def.getDiameterAVPFormat() == Integer32Format.INSTANCE) {
					avp.setValue(Integer32Format.toInteger32(getCauseCode()), false);
				} else if (def.getDiameterAVPFormat() == EnumeratedFormat.INSTANCE) {
					avp.setValue(EnumeratedFormat.toEnumerated(getCauseCode()), false);
				}
				avps.add(avp);
			}
		}

		value = getAccessNetworkInformation();
		if (value != null) {
			def = ChargingUtils.getAccessNetworkInformationAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(value, false);
				avps.add(avp);
			}
		}

		Iterable<EarlyMediaDescription> descriptions = getEarlyMediaDescriptions();
		if (descriptions.iterator().hasNext()) {
			def = ChargingUtils.getEarlyMediaDescriptionAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				for (EarlyMediaDescription desc : descriptions) {
					avp.addValue(desc.toAvp(version).getValue(), false);
				}
				avps.add(avp);
			}
		}

		if (getImsCommunicationServiceIdentifier() != null) {
			def = ChargingUtils.getImsCommunicationServiceIdentifierAVP(version);
			if (def != null) {
				DiameterAVP avp = new DiameterAVP(def);
				avp.setValue(UTF8StringFormat.toUtf8String(getImsCommunicationServiceIdentifier()), false);
				avps.add(avp);
			}
		}

		res.setValue(GroupedFormat.toGroupedAVP(avps), false);
		return res;
	}

	/**
	 * Gets the event type (mapped to the Event-Type AVP).
	 * 
	 * @return The event type.
	 */
	public EventType getEventType() {
		return _eventType;
	}

	/**
	 * Sets the event type.
	 * 
	 * @param type The event type.
	 */
	public void setEventType(EventType type) {
		_eventType = type;
	}

	/**
	 * Gets the role of node (mapped to Role-Of-Node AVP).
	 * 
	 * @return The role of node.
	 */
	public RoleOfNode getRoleOfNode() {
		return _roleOfNode;
	}

	/**
	 * Sets the role of node (mapped to Role-Of-Node AVP).
	 * 
	 * @param role The role of node.
	 */
	public void setRoleOfNode(RoleOfNode role) {
		_roleOfNode = role;
	}

	/**
	 * Gets the node functionality (mapped to Node-Functionality AVP).
	 * 
	 * @return The node functionality.
	 */
	public NodeFunctionality getNodeFunctionality() {
		return _nodeFunctionality;
	}

	/**
	 * Gets the user session identifier (mapped to User-Session-Id AVP).
	 * 
	 * @return The user session identifier.
	 */
	public String getUserSessionId() {
		return _userSessionId;
	}

	/**
	 * Sets the user session identifier .
	 * 
	 * @param id The user session identifier.
	 */
	public void setUserSessionId(String id) {
		_userSessionId = id;
	}

	/**
	 * Gets the calling party addresses (mapped to Calling-Party-Address AVP).
	 * 
	 * @return The addresses.
	 */
	public Iterable<String> getCallingPartyAddresses() {
		return _callingPartyAddresses;
	}

	/**
	 * Adds a calling party address.
	 * 
	 * @param address The address to be added
	 */
	public void addCallingPartyAddress(String address) {
		_callingPartyAddresses.add(address);
	}

	/**
	 * Gets the called party address ( mapped to Called-Party-Address AVP).
	 * 
	 * @return The address.
	 */
	public String getCalledPartyAddress() {
		return _calledPartyAddress;
	}

	/**
	 * Sets the called party address.
	 * 
	 * @param address The address.
	 */
	public void setCalledPartyAddress(String address) {
		_calledPartyAddress = address;
	}

	/**
	 * Gets the called asserted identities (mapped to Called-Asserted-Identities
	 * AVP).
	 * 
	 * @return The identities.
	 */
	public Iterable<String> getCalledAssertedIdentities() {
		return _calledAssertedIdentities;
	}

	/**
	 * Adds the called asserted identity
	 * 
	 * @param identity The identity to be added
	 */
	public void addCalledAssertedIdentity(String identity) {
		_calledAssertedIdentities.add(identity);
	}

	/**
	 * Gets the number portability routing information (mapped to
	 * Number-Portability-Routing-Information AVP).
	 * 
	 * @return The information.
	 */
	public String getNumberPortabilityRoutingInformation() {
		return _numberPortabilityRoutingInformation;
	}

	/**
	 * Sets the number portability routing information.
	 * 
	 * @param information The information.
	 */
	public void setNumberPortabilityRoutingInformation(String information) {
		_numberPortabilityRoutingInformation = information;
	}

	/**
	 * Gets the carrier select routing information (mapped to
	 * Carrier-Select-Routing-Information AVP).
	 * 
	 * @return The information.
	 */
	public String getCarrierSelectRoutingInformation() {
		return _carrierSelectRoutingInformation;
	}

	/**
	 * Sets the carrier select routing information.
	 * 
	 * @param information The information.
	 */
	public void setCarrierSelectRoutingInformation(String information) {
		_carrierSelectRoutingInformation = information;
	}

	/**
	 * Gets the alternate charged party address (mapped to
	 * Alternate-Charged-Party-Address AVP).
	 * 
	 * @return The address.
	 */
	public String getAlternateChargedPartyAddress() {
		return _alternateChargedPartyAddress;
	}

	/**
	 * Sets the requested party address.
	 * 
	 * @param address The address.
	 */
	public void setAlternateChargedPartyAddress(String address) {
		_alternateChargedPartyAddress = address;
	}

	/**
	 * Gets the requested party address (mapped to Requested-Party-Address AVP).
	 * 
	 * @return The address.
	 */
	public String getRequestedPartyAddress() {
		return _requestedPartyAddress;
	}

	/**
	 * Sets the requested party address.
	 * 
	 * @param address The address.
	 */
	public void setRequestedPartyAddress(String address) {
		_requestedPartyAddress = address;
	}

	/**
	 * Gets the associated uris (mapped to Associated-Uri AVP).
	 * 
	 * @return The uris.
	 */
	public Iterable<String> getAssociatedUris() {
		return _associatedUris;
	}

	/**
	 * Adds a associated uri.
	 * 
	 * @param uri The uri to be added
	 */
	public void addAssociatedUri(String uri) {
		_associatedUris.add(uri);
	}

	/**
	 * Gets the timestamps (mapped to Time-Stamps AVP).
	 * 
	 * @return The timestamps.
	 */
	public TimeStamps getTimeStamps() {
		return _timestamps;
	}

	/**
	 * Sets the timestamps.
	 * 
	 * @param timeStamps The timestamps.
	 */
	public void setTimeStamps(TimeStamps timeStamps) {
		_timestamps = timeStamps;
	}

	/**
	 * Gets the application server information list (mapped to
	 * Application-Server-Information AVP).
	 * 
	 * @return The application server information list.
	 */
	public Iterable<ApplicationServerInformation> getApplicationServerInformations() {
		return _applicationServerInformations;
	}

	/**
	 * Adds an application server information.
	 * 
	 * @param information the information to be added.
	 */
	public void addApplicationServerInformation(ApplicationServerInformation information) {
		_applicationServerInformations.add(information);
	}

	/**
	 * Gets the inter operator identifiers (mapped to Inter-Operator-Identifier
	 * AVP).
	 * 
	 * @return The inter operator identifiers.
	 */
	public Iterable<InterOperatorIdentifier> getInterOperatorIdentifiers() {
		return _interOperatorIdentifiers;
	}

	/**
	 * Adds an inter operator identifier.
	 * 
	 * @param id The identifier to be added.
	 */
	public void addInterOperatorIdentifier(InterOperatorIdentifier id) {
		_interOperatorIdentifiers.add(id);
	}

	/**
	 * Gets the IMS charging identifier (mapped to Ims-Charging-Identifier).
	 * 
	 * @return The identifier.
	 */
	public String getImsChargingIdentifier() {
		return _imsChargingIdentifier;
	}

	/**
	 * Sets the IMS charging identifier.
	 * 
	 * @param id The identifier.
	 */
	public void setImsChargingIdentifier(String id) {
		_imsChargingIdentifier = id;
	}

	/**
	 * Gets the SDP session descriptions (mapped to SDP-Session-Description AVP).
	 * 
	 * @return The descriptions.
	 */
	public Iterable<String> getSdpSessionDescriptions() {
		return _sdpSessionDescriptions;
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
	 * Gets the SDP Media Component (mapped to SDP-Media-Component AVP).
	 * 
	 * @return The components.
	 */
	public Iterable<SdpMediaComponent> getSdpMediaComponents() {
		return _sdpMediaComponents;
	}

	/**
	 * Adds a SDP Media Component.
	 * 
	 * @param component The component.
	 */
	public void addSdpMediaComponent(SdpMediaComponent component) {
		_sdpMediaComponents.add(component);
	}

	/**
	 * Gets the Served-Party-Ip-Address.
	 * 
	 * @return The address.
	 */
	public Address getServedPartyIpAddress() {
		return _servedPartyIpAddress;
	}

	/**
	 * Sets the Served-Party-Ip-Address.
	 * 
	 * @param address The address.
	 */
	public void setServedPartyIpAddress(Address address) {
		_servedPartyIpAddress = address;
	}

	/**
	 * Gets the server capabilities (mapped to Server-Capabilities AVP).
	 * 
	 * @return The capabilities.
	 */
	public ServerCapabilities getServerCapabilities() {
		return _serverCapabilities;
	}

	/**
	 * Sets the server capabilities
	 * 
	 * @param capabilities The capabilities.
	 */
	public void setServerCapabilities(ServerCapabilities capabilities) {
		_serverCapabilities = capabilities;
	}

	/**
	 * Gets the thrunk group identifier (mapped to Trunk-Group-IdAVP).
	 * 
	 * @return The identifier.
	 */
	public TrunkGroupId getTrunkGroupId() {
		return _trunkGroupId;
	}

	/**
	 * Sets the thrunk group identifier.
	 * 
	 * @param id The identifier.
	 */
	public void setTrunkGroupId(TrunkGroupId id) {
		_trunkGroupId = id;
	}

	/**
	 * Gets the bearer service (mapped to the Bearer-Service AVP).
	 * 
	 * @return The service.
	 */
	public byte[] getBearerService() {
		return copyArray(_bearerService);
	}

	/**
	 * Sets the bearer service.
	 * 
	 * @param service The service.
	 */
	public void setBearerService(byte[] service) {
		_bearerService = copyArray(service);
	}

	/**
	 * Gets the service identifier (mapped to the Service-Id AVP);
	 * 
	 * @return The identifier.
	 */
	public String getServiceId() {
		return _serviceId;
	}

	/**
	 * Sets the service identifier.
	 * 
	 * @param id The identifier.
	 */
	public void setServiceId(String id) {
		_serviceId = id;
	}

	/**
	 * Gets the service specific infos (mapped to Service-Specific-Info AVP).
	 * 
	 * @return The infos.
	 */
	public Iterable<ServiceSpecificInfo> getServiceSpecificInfos() {
		return _serviceSpecificInfos;
	}

	/**
	 * Adds a service specific info
	 * 
	 * @param info The info to be added.
	 */
	public void addServiceSpecificInfo(ServiceSpecificInfo info) {
		_serviceSpecificInfos.add(info);
	}

	/**
	 * Gets the message bodies (mapped to Message-Body AVP).
	 * 
	 * @return The infos.
	 */
	public Iterable<MessageBody> getMessageBodies() {
		return _messageBodies;
	}

	/**
	 * Adds a message body
	 * 
	 * @param body The body to be added.
	 */
	public void addMessageBody(MessageBody body) {
		_messageBodies.add(body);
	}

	/**
	 * Gets the cause code (mapped to the Cause-Code AVP).
	 * 
	 * @return The cause code.
	 */
	public int getCauseCode() {
		return _causeCode;
	}

	/**
	 * Sets the cause code.
	 * 
	 * @param code The cause code.
	 */
	public void setCauseCode(int code) {
		_causeCode = code;
	}

	/**
	 * Gets the access network information (mapped to the
	 * Access-Network-Information AVP).
	 * 
	 * @return The information.
	 */
	public byte[] getAccessNetworkInformation() {
		return copyArray(_accessNetworkInformation);
	}

	/**
	 * Sets the bearer service.
	 * 
	 * @param information The information.
	 */
	public void setAccessNetworkInformation(byte[] information) {
		_accessNetworkInformation = copyArray(information);
	}

	/**
	 * Gets the early media description (mapped to Early-Media-Description AVP).
	 * 
	 * @return The descriptions.
	 */
	public Iterable<EarlyMediaDescription> getEarlyMediaDescriptions() {
		return _earlyMediaDescriptions;
	}

	/**
	 * Adds a early media description.
	 * 
	 * @param description The description to be added.
	 */
	public void addEarlyMediaDescription(EarlyMediaDescription description) {
		_earlyMediaDescriptions.add(description);
	}

	/**
	 * Gets the IMS communication service identifier (mapped to the
	 * IMS-Communication-Service-Identifier AVP);
	 * 
	 * @return The identifier.
	 */
	public String getImsCommunicationServiceIdentifier() {
		return _imsCommunicationServiceIdentifier;
	}

	/**
	 * Sets the IMS communication service identifier.
	 * 
	 * @param id The identifier.
	 */
	public void setImsCommunicationServiceIdentifier(String id) {
		_imsCommunicationServiceIdentifier = id;
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