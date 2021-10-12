// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.ims.diameter.sh;

import java.util.Date;

/** 
 * The Subscribe Notifications Answer (SNA) 
 */
public interface SubscribeNotificationsAnswer extends ShAnswer {
	/**
	 * Gets the requested data.
	 * 
	 * @return The requested data.
	 */
	public byte[] getUserData();

	/**
	 * Gets the Expiry-Time.
	 * @return The Expiry-Time or null if not found.
	 */
	public Date getExpiryTime() ;
	
}
