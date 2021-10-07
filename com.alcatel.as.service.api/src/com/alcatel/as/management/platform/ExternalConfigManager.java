package com.alcatel.as.management.platform;

import java.util.Map;

import org.json.JSONObject;

import com.alcatel.as.service.metatype.InstanceProperties;

public interface ExternalConfigManager {

	public JSONObject manageExternalProperties(Map<String, InstanceProperties> config);
	
	public boolean isValueValid (String validationClassS, String componentPath, String bundleName, String bundleVersion, 
			String name, Object value) throws Exception;
	
}
