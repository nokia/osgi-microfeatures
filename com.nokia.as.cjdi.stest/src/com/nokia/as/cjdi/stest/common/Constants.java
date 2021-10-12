// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.cjdi.stest.common;

import com.nextenso.proxylet.diameter.DiameterAVPDefinition;
import com.nextenso.proxylet.diameter.util.UTF8StringFormat;


public class Constants {
	public static final DiameterAVPDefinition MY_AVP = new DiameterAVPDefinition("JDiameter-Test", 1001L, 1000L, DiameterAVPDefinition.REQUIRED_FLAG,
			DiameterAVPDefinition.REQUIRED_FLAG, DiameterAVPDefinition.FORBIDDEN_FLAG, false, UTF8StringFormat.INSTANCE);

}
