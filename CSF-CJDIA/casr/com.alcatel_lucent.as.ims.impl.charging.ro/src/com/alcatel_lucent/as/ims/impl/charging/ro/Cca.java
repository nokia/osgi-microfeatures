package com.alcatel_lucent.as.ims.impl.charging.ro;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.alcatel_lucent.as.ims.Version;
import com.alcatel_lucent.as.ims.diameter.DiameterMissingAVPException;
import com.alcatel_lucent.as.ims.diameter.FailedAvp;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CcSessionFailover;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CreditControlFailureHandling;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.DirectDebitingFailureHandling;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.LowBalanceIndication;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.RedirectHostUsage;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingUtils;
import com.alcatel_lucent.as.ims.diameter.charging.CostInformation;
import com.alcatel_lucent.as.ims.diameter.charging.MultipleServicesCreditControl;
import com.alcatel_lucent.as.ims.diameter.charging.RemainingBalance;
import com.alcatel_lucent.as.ims.diameter.charging.ServiceInformation;
import com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer;
import com.alcatel_lucent.as.ims.diameter.common.AbstractImsAnswer;
import com.nextenso.proxylet.diameter.DiameterAVP;
import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.client.DiameterClientResponse;
import com.nextenso.proxylet.diameter.util.DiameterBaseConstants;
import com.nextenso.proxylet.diameter.util.URIFormat;
import com.nextenso.proxylet.diameter.util.URIFormat.URI;
import com.nextenso.proxylet.diameter.util.Unsigned32Format;

public class Cca
		extends AbstractImsAnswer
		implements CreditControlAnswer {

	private final static Logger LOGGER = Logger.getLogger("3gpp.interfaces.ro.cca");

	protected Cca(DiameterClientResponse response, Version version) {
		super(response, version);
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getCcSessionFailover()
	 */
	public CcSessionFailover getCcSessionFailover() {
		DiameterAVPDefinition def = ChargingUtils.getCcSessionFailoverAVP();
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			CcSessionFailover res = CcSessionFailover.getData(getEnumeratedAVP(def));
			return res;
		}

		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getMultipleServicesCreditControls()
	 */
	public Iterable<MultipleServicesCreditControl> getMultipleServicesCreditControls() {
		DiameterAVPDefinition def = ChargingUtils.getMultipleServiceCreditControlAVP();
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			List<MultipleServicesCreditControl> res = new ArrayList<MultipleServicesCreditControl>();
			for (int i = 0; i < avp.getValueSize(); i++) {
				try {
					MultipleServicesCreditControl mscc = new MultipleServicesCreditControl(avp.getValue(i), getVersion());
					res.add(mscc);
				}
				catch (DiameterMissingAVPException e) {
					LOGGER.warn("missing AVP in Multiple-Service-Credit-Control: " + e);
				}
			}
			return res;
		}
		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getCostInformation()
	 */
	public CostInformation getCostInformation() {
		DiameterAVPDefinition def = ChargingUtils.getCostInformationAVP();
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			CostInformation res = null;
			try {
				res = new CostInformation(avp);
			}
			catch (DiameterMissingAVPException e) {
				LOGGER.warn("missing AVP in Cost-Information: " + e);
			}
			return res;
		}
		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getLowBalanceIndication()
	 */
	public LowBalanceIndication getLowBalanceIndication() {
		DiameterAVPDefinition def = ChargingUtils.getLowBalanceIndicationAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			LowBalanceIndication res = LowBalanceIndication.getData(getEnumeratedAVP(def));
			return res;
		}

		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getRemainingBalance()
	 */
	public RemainingBalance getRemainingBalance() {
		DiameterAVPDefinition def = ChargingUtils.getRemainingBalanceAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			RemainingBalance res = null;
			try {
				res = new RemainingBalance(avp);
			}
			catch (DiameterMissingAVPException e) {
				LOGGER.warn("missing AVP in Remaining-Balance: " + e);
			}
			return res;
		}
		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getCreditControlFailureHandling()
	 */
	public CreditControlFailureHandling getCreditControlFailureHandling() {
		DiameterAVPDefinition def = ChargingUtils.getCreditControlFailureHandlingAVP();
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			CreditControlFailureHandling res = CreditControlFailureHandling.getData(getEnumeratedAVP(def));
			return res;
		}

		return null;
	}

	/**
	 * 
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getDirectDebitingFailureHandling()
	 */
	public DirectDebitingFailureHandling getDirectDebitingFailureHandling() {
		DiameterAVPDefinition def = ChargingUtils.getDirectDebitingFailureHandlingAVP();
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			DirectDebitingFailureHandling res = DirectDebitingFailureHandling.getData(getEnumeratedAVP(def));
			return res;
		}

		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getRedirectHosts()
	 */
	public Iterable<URI> getRedirectHosts() {
		DiameterAVPDefinition def = DiameterBaseConstants.AVP_REDIRECT_HOST;
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			List<URI> res = new ArrayList<URI>();
			for (int i = 0; i < avp.getValueSize(); i++) {
				URI uri = URIFormat.getURI(avp.getValue(i));
				res.add(uri);
			}
			return res;
		}
		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getRedirectHostUsage()
	 */
	public RedirectHostUsage getRedirectHostUsage() {
		DiameterAVPDefinition def = DiameterBaseConstants.AVP_REDIRECT_HOST_USAGE;
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			RedirectHostUsage res = RedirectHostUsage.getData(getEnumeratedAVP(def));
			return res;
		}

		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getRedirectMaxCacheTime()
	 */
	public Long getRedirectMaxCacheTime() {
		DiameterAVPDefinition def = DiameterBaseConstants.AVP_REDIRECT_MAX_CACHE_TIME;
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			Long res = Long.valueOf(Unsigned32Format.getUnsigned32(avp.getValue(), 0));
			return res;
		}

		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getFailedAvp()
	 */
	public Iterable<FailedAvp> getFailedAvp() {
		DiameterAVPDefinition def = DiameterBaseConstants.AVP_FAILED_AVP;
		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			List<FailedAvp> res = new ArrayList<FailedAvp>();
			for (int i = 0; i < avp.getValueSize(); i++) {
				FailedAvp uri = new FailedAvp(avp.getValue(i));
				res.add(uri);
			}
			return res;
		}
		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.charging.ro.CreditControlAnswer#getServiceInformation()
	 */
	public ServiceInformation getServiceInformation() {
		DiameterAVPDefinition def = ChargingUtils.getServiceInformationAVP(getVersion());
		if (def == null) {
			return null;
		}

		DiameterAVP avp = getAvp(def);
		if (avp != null) {
			ServiceInformation res = null;
			try {
				res = new ServiceInformation(avp, getVersion());
			}
			catch (DiameterMissingAVPException e) {
				LOGGER.warn("missing AVP in Service-Information: " + e);
			}
			return res;
		}
		return null;
	}

	/**
	 * @see com.alcatel_lucent.as.ims.diameter.common.AbstractImsAnswer#getPublicIdentity()
	 */
	@Override
	public String getPublicIdentity() {
		// Not used in Ro
		return null;
	}

}
