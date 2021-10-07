package com.nextenso.diameter.agent.peer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.diameter.agent.DiameterProperties;
import com.nextenso.diameter.agent.impl.DiameterRequestFacade;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterApplication;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.diameter.DiameterPeer;
import com.nextenso.proxylet.diameter.DiameterPeerTable;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.GroupedFormat;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

public class Capabilities {

	private List<DiameterApplication> _applications = new ArrayList<DiameterApplication>();

	private Set<Long> _authenticationApplicationIds = new HashSet<Long>();
	private Set<Long> _accountingApplicationIds = new HashSet<Long>();
	private Set<Long> _supportedVendorIds = new HashSet<Long>();
	private List<DiameterApplication> _vendorIdSupportedApplications = new ArrayList<DiameterApplication>();

	private boolean _isRelay = false;
	private DiameterAVP _authAppsAVP, _acctAppsAVP, _vendorAppsAVP, _vendorSupportedAVP;

	private static final Logger LOGGER = Logger.getLogger("agent.diameter.capabilities");
	private static final DiameterApplication RELAY_ACCT_APPLICATION = new DiameterApplication(DiameterBaseConstants.APPLICATION_RELAY, 0, false);
	private static final DiameterApplication RELAY_AUTH_APPLICATION = new DiameterApplication(DiameterBaseConstants.APPLICATION_RELAY, 0, true);

	public void setRelay(boolean isRelay) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("setRelay: relay=" + isRelay);
		}

		_isRelay = isRelay;
		if (_isRelay) {
			_applications.clear();
			_applications.add(RELAY_ACCT_APPLICATION);
			_applications.add(RELAY_AUTH_APPLICATION);
		}
	}

	public boolean isRelay() {
		return _isRelay;
	}

	public void setSupportedApplications(List<DiameterApplication> applications) {
		_applications.clear();
		_applications.addAll(applications);
		setRelay(false);
	}

	public List<DiameterApplication> getSupportedApplications() {
		return _applications;
	}

	public void update() {
		_authenticationApplicationIds.clear();
		_accountingApplicationIds.clear();
		_supportedVendorIds.clear();
		_vendorIdSupportedApplications.clear();

		_acctAppsAVP = null;
		_vendorSupportedAVP = null;
		_vendorAppsAVP = null;

		for (DiameterApplication app : _applications) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("updateCapabilities: appli=" + app);
			}
			long vendorId = app.getVendorId();

			if (vendorId > 0) {
				_supportedVendorIds.add(vendorId);

				if (app.getApplicationId() != 0) {
					_vendorIdSupportedApplications.add(app);
					if (_vendorAppsAVP == null) {
						_vendorAppsAVP = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
					}

					List<DiameterAVP> list = new ArrayList<DiameterAVP>();
					// Add the Vendor_id AVP
					DiameterAVP avp = new DiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID);
					avp.addValue(Unsigned32Format.toUnsigned32(app.getVendorId()), false);
					list.add(avp);

					// Add the acct or auth application id AVP
					avp = (app.isAuthentication()) ? new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID) : new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
					avp.addValue(Unsigned32Format.toUnsigned32(app.getApplicationId()), false);
					list.add(avp);

					_vendorAppsAVP.addValue(GroupedFormat.toGroupedAVP(list), false);
				}

			} else {
				if (app.isAuthentication()) {
					_authenticationApplicationIds.add(app.getApplicationId());
				} else {
					_accountingApplicationIds.add(app.getApplicationId());
				}
			}

		}

		if (!_authenticationApplicationIds.isEmpty()) {
			_authAppsAVP = new DiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
			for (long value : _authenticationApplicationIds) {
				_authAppsAVP.addValue(Unsigned32Format.toUnsigned32(value), false);
			}
		}

		if (!_accountingApplicationIds.isEmpty()) {
			_acctAppsAVP = new DiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
			for (long value : _accountingApplicationIds) {
				_acctAppsAVP.addValue(Unsigned32Format.toUnsigned32(value), false);
			}
		}

		if (!_supportedVendorIds.isEmpty()) {
			_vendorSupportedAVP = new DiameterAVP(DiameterBaseConstants.AVP_SUPPORTED_VENDOR_ID);
			for (long vendorId : _supportedVendorIds) {
				_vendorSupportedAVP.addValue(Unsigned32Format.toUnsigned32(vendorId), false);
			}
		}

		//  send CER to all peers
		DiameterPeerTable table = Utils.getTableManager();
		List<DiameterPeer> localPeers = table.getLocalDiameterPeers();
		for (DiameterPeer localPeer : localPeers) {
			List<DiameterPeer> peers = table.getDiameterPeers(localPeer);
			for (DiameterPeer peer : peers) {
				if (peer instanceof RemotePeer) {
					RemotePeer rPeer = (RemotePeer) peer;
					PeerSocket socket = rPeer.getStateMachine ().getSocket ();
					if (socket == null) continue;
					DiameterRequestFacade cer = Utils.createCER(rPeer, socket.getLocalInetSocketAddresses ());
					rPeer.sendMessage(cer);
				}
			}
		}
	}

	public boolean isCompliantMessage(DiameterMessage message) {
		if (_isRelay) {
			return true;
		}

		if (DiameterProperties.checkAppAdvert () == false &&
		    (message.getDiameterApplication () != 0 ||
		     message.getDiameterCommand () != 257)) // do the check for CER/CEA
			return true;

		DiameterAVP avp = message.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID);
		if (avp != null) {
			for (int i = 0; i < avp.getValueSize(); i++) {
				long appId = Unsigned32Format.getUnsigned32(avp.getValue(i), 0);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("isCompliantMessage: check accounting app=" + appId + " in " + _accountingApplicationIds);
				}
				if (appId == DiameterBaseConstants.APPLICATION_RELAY) {
					return true;
				}
				if (_accountingApplicationIds.contains(Long.valueOf(appId))) {
					return true;
				}
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("isCompliantMessage: not found");
				}
			}
		}

		avp = message.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID);
		if (avp != null) {
			for (int i = 0; i < avp.getValueSize(); i++) {
				long appId = Unsigned32Format.getUnsigned32(avp.getValue(i), 0);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("isCompliantMessage: check authentication app=" + appId + " in " + _authenticationApplicationIds);
				}

				if (appId == DiameterBaseConstants.APPLICATION_RELAY) {
					return true;
				}
				if (_authenticationApplicationIds.contains(appId)) {
					return true;
				}
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("isCompliantMessage: not found");
				}
			}
		}

		if (_vendorIdSupportedApplications.isEmpty()) {
			return false;
		}

		avp = message.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_SPECIFIC_APPLICATION_ID);
		if (avp != null) {
			for (int i = 0; i < avp.getValueSize(); i++) {
				DiameterAVP vendorIdAVP = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_VENDOR_ID, avp.getValue(i), false);
				DiameterAVP applicationIdAVP = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_ACCT_APPLICATION_ID, avp.getValue(i), false);
				boolean acct = (applicationIdAVP != null);
				if (!acct) {
					applicationIdAVP = GroupedFormat.getDiameterAVP(DiameterBaseConstants.AVP_AUTH_APPLICATION_ID, avp.getValue(i), false);
				}

				if (vendorIdAVP != null && applicationIdAVP != null && vendorIdAVP.getValueSize() > 0 && applicationIdAVP.getValueSize() > 0) {
					long vendorId = Unsigned32Format.getUnsigned32(vendorIdAVP.getValue(0), 0);
					long applicationId = Unsigned32Format.getUnsigned32(applicationIdAVP.getValue(0), 0);
					for (DiameterApplication app : _vendorIdSupportedApplications) {
						if (vendorId == app.getVendorId() && applicationId == app.getApplicationId() && acct == app.isAccounting()) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public void fillCapabilitiesMessage(DiameterMessage message) {
		if (_authAppsAVP != null) {
			message.addDiameterAVP((DiameterAVP) _authAppsAVP.clone());
		}
		if (_acctAppsAVP != null) {
			message.addDiameterAVP((DiameterAVP) _acctAppsAVP.clone());
		}
		if (_vendorSupportedAVP != null) {
			message.addDiameterAVP((DiameterAVP) _vendorSupportedAVP.clone());
		}
		if (_vendorAppsAVP != null) {
			message.addDiameterAVP((DiameterAVP) _vendorAppsAVP.clone());
		}
	}

	public Collection<Long> getSupportedVendorIds() {
		return _supportedVendorIds;
	}

	public List<DiameterApplication> getVendorSpecificApplications() {
		return _vendorIdSupportedApplications;
	}

	public Collection<Long> getAuthApplications() {
		return _authenticationApplicationIds;
	}

	public Collection<Long> getAcctApplications() {
		return _accountingApplicationIds;
	}

}
