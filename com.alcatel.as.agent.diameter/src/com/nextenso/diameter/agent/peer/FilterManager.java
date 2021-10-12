// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.peer;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.annotation.api.Component;

import com.nextenso.diameter.agent.Utils;
import com.nextenso.proxylet.diameter.DiameterConnectionFilter;
import com.nextenso.proxylet.diameter.DiameterFilterTable;

/**
 * The Filter Manager.
 */
@Component
public class FilterManager implements DiameterFilterTable {		

	private final Collection<DiameterConnectionFilter> _incomingBlackList = new CopyOnWriteArrayList<DiameterConnectionFilter>();
	private final Collection<DiameterConnectionFilter> _incomingWhiteList = new CopyOnWriteArrayList<DiameterConnectionFilter>();

	@Override
	public Collection<DiameterConnectionFilter> getIncomingSocketBlackList() {
		return _incomingBlackList;
	}

	@Override
	public Collection<DiameterConnectionFilter> getIncomingSocketWhiteList() {
		return _incomingWhiteList;
	}

	@Override
	public void applyLists() {
		Utils.applyFilters();
	}

}
