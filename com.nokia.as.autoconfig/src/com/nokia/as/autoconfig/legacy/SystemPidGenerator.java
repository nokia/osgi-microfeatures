// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.legacy;

import static com.alcatel.as.util.config.ConfigConstants.COMPONENT_NAME;
import static com.alcatel.as.util.config.ConfigConstants.GROUP_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_ID;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_NAME;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_NAME;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.nokia.as.autoconfig.AutoConfigurator;

import alcatel.tess.hometop.gateways.utils.Log;

public class SystemPidGenerator {
    
    private SystemPidGenerator() { }
    
    public static void addPropsToSystem(Map<String, Object> props) {
        props.entrySet().stream()
             .filter(e -> e.getValue() instanceof String)
             .forEach(e -> System.setProperty(e.getKey(), (String) e.getValue()));
    }
    
    public static Optional<String> getProcessId() {
        String mgmtName = ManagementFactory.getRuntimeMXBean().getName();
        int ndx = mgmtName.indexOf('@');
            
        return ndx > 0 ? Optional.of(mgmtName.substring(0, ndx))
                       : Optional.empty();
    }
    
    public static int getInstanceId(Map<String, Object> props, long salt) {
        return Objects.hash(props.get(PLATFORM_NAME),
                             props.get(GROUP_NAME),
                             props.get(COMPONENT_NAME),
                             props.get(INSTANCE_NAME),
                             salt);
    }

    public static Map<String, Object> generateSystemPidEntry(long salt) {
		Map<String, Object> systemPidProps = new HashMap<>();
		
		String procId = getProcessId().orElse("");
		String instId = "" + getInstanceId(systemPidProps, salt);
        systemPidProps.put(INSTANCE_ID, instId);
		systemPidProps.put(INSTANCE_PID, procId);
		systemPidProps.put(INSTANCE_NAME, "instance");
		Log.getLogger(AutoConfigurator.LOGGER).debug("System pid entry generated, instance_id=%s, instance_pid=%s, instance_name=%s", instId, procId, "instance");
		return systemPidProps;
	}
    
}
