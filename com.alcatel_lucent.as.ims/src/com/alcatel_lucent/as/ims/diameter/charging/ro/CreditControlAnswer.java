// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.charging.ro;

import com.alcatel_lucent.as.ims.diameter.FailedAvp;
import com.alcatel_lucent.as.ims.diameter.ImsAnswer;
import com.alcatel_lucent.as.ims.diameter.charging.CostInformation;
import com.alcatel_lucent.as.ims.diameter.charging.MultipleServicesCreditControl;
import com.alcatel_lucent.as.ims.diameter.charging.RemainingBalance;
import com.alcatel_lucent.as.ims.diameter.charging.ServiceInformation;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CcSessionFailover;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.CreditControlFailureHandling;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.DirectDebitingFailureHandling;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.LowBalanceIndication;
import com.alcatel_lucent.as.ims.diameter.charging.ChargingConstants.RedirectHostUsage;
import com.nextenso.proxylet.diameter.util.URIFormat.URI;

/**
 * 
 * The Credit Control Answer (CCA).
 */
public interface CreditControlAnswer
		extends ImsAnswer {

	/**
	 * Gets the CC-Session-Failover AVP value.
	 * 
	 * @return The value.
	 */
	public CcSessionFailover getCcSessionFailover();

	/**
	 * Gets the list of the Multiple-Services-Credit-Control AVP values.
	 * 
	 * @return The values.
	 */
	public Iterable<MultipleServicesCreditControl> getMultipleServicesCreditControls();

	/**
	 * Gets the Cost-Information AVP value.
	 * 
	 * @return The information.
	 */
	public CostInformation getCostInformation();

	/**
	 * Gets the Low-Balance-Indication AVP value.
	 * 
	 * @return The indication.
	 */
	public LowBalanceIndication getLowBalanceIndication();

	/**
	 * Gets the Remaining-Balance AVP value.
	 * 
	 * @return The value.
	 */
	public RemainingBalance getRemainingBalance();

	/**
	 * Gets the Credit-Control-Failure-Handling AVP value.
	 * 
	 * @return The value.
	 */
	public CreditControlFailureHandling getCreditControlFailureHandling();

	/**
	 * Gets the Direct-Debiting-Failure-Handling AVP value.
	 * 
	 * @return The value.
	 */
	public DirectDebitingFailureHandling getDirectDebitingFailureHandling();

	/**
	 * Gets the list of the Redirect-Host AVP values.
	 * 
	 * @return The values.
	 */
	public Iterable<URI> getRedirectHosts();

	/**
	 * Gets the Redirect-Host-Usage AVP value.
	 * 
	 * @return The value.
	 */
	public RedirectHostUsage getRedirectHostUsage();

	/**
	 * Gets the Redirect-Max-Cache-Time AVP value.
	 * 
	 * @return The value.
	 */
	public Long getRedirectMaxCacheTime();

	/**
	 * Gets the list of the Failed-AVP AVP values.
	 * 
	 * @return The values.
	 */
	public Iterable<FailedAvp> getFailedAvp();

	/**
	 * Gets the Service-Information AVP value.
	 * 
	 * @return The service information..
	 */
	public ServiceInformation getServiceInformation();

}
