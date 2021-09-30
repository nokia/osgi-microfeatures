package com.nokia.as.autoconfig.transform;

import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_ID;
import static com.alcatel.as.util.config.ConfigConstants.INSTANCE_PID;
import static com.alcatel.as.util.config.ConfigConstants.PLATFORM_NAME;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.alcatel.as.service.log.LogService;
import com.nokia.as.autoconfig.AutoConfigurator;
import com.nokia.as.autoconfig.Utils;
import com.nokia.as.autoconfig.legacy.SystemPidGenerator;

import alcatel.tess.hometop.gateways.utils.Log;

public class InstancePidTransformer implements Function<Map<String, Object>, Map<String, Object>> {
	private LogService logger = Log.getLogger(AutoConfigurator.LOGGER);
	private Long salt;	
	
	public InstancePidTransformer(Supplier<Long> salt) {
        this.salt = salt.get();
    }
	
	@Override
	public Map<String, Object> apply(Map<String, Object> t) {
		return replaceInstancePid(t);
	}
	
	private Map<String, Object> replaceInstancePid(Map<String, Object> props) {
		
		if(!props.containsKey(INSTANCE_PID)) return props;
		if(!"${instance.pid}".equals(props.get(INSTANCE_PID))) return props;		
		
        //set instance_pid
        props.put(INSTANCE_PID, SystemPidGenerator.getProcessId().orElseGet(() -> Utils.getSystemProperty(INSTANCE_PID)));
        logger.debug("INSTANCE_PID => %s", props.get(INSTANCE_PID));
        
        //set instance_id
        if (props.get(INSTANCE_PID) != null && "asr".equals(props.get(PLATFORM_NAME))) {
            props.put(INSTANCE_ID, Integer.toString(SystemPidGenerator.getInstanceId(props, salt)));
            logger.debug("INSTANCE_ID => %s", props.get(INSTANCE_ID));
        }
        return props;
	}
}
