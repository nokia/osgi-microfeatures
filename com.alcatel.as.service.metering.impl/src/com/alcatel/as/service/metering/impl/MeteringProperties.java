// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metering.impl;

import com.alcatel_lucent.as.management.annotation.config.*;

@Config(name="metering", rootSnmpName="alcatel.srd.a5350.CalloutAgent", rootOid={637, 71, 6, 110}, section="Metering Service/Legacy", monconfModule="CalloutAgent")
public interface MeteringProperties {

  @IntProperty(min=0, max=2147483647, title="Metering sampler period", help="Defines the interval in seconds used by the metering sampler thread. The sampler thread will wakeup every Nseconds and will calculate and log meter statistics", oid=106, snmpName="MeteringSamplerPeriod", required=false, dynamic=true, defval=10)
  public static final String METERING_DELAY = "metering.delay";

  @IntProperty(min=-1, max=2147483647, title="Metering sampler threshold", help="You can define here a threshold used to avoid logging unrelevant metering statistics value.Only statistics metering values which are greater or equals to the specified treshold will bedisplayed.", oid=107, snmpName="MeteringSamplerThreshold", required=false, dynamic=true, defval=0)
  public static final String METERING_THRESHOLD = "metering.threshold";

  @BooleanProperty(title="Metering accumulated statistics calculation", help="You can set this property to true if you want to let the metering service calculate accumulated statistics (statistics calculated from the jvm start).", oid=134, snmpName="MeteringCalculateAccum", required=false, dynamic=false, defval=false)
  public static final String METERING_CALCULATEACCUM = "metering.calculateAccum";
}
