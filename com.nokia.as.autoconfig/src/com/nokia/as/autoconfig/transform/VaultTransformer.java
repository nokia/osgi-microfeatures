// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.autoconfig.transform;

import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.alcatel.as.service.log.LogService;
import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LogicalResponse;
import com.nokia.as.autoconfig.Utils;

public class VaultTransformer implements Function<Map<String, Object>, Map<String, Object>> {
	
	private static final String REGEX = "\\$vault(\\{([^}]+?)\\})";
	private static final Pattern PTN = Pattern.compile(REGEX);
	private Vault vault;
	private boolean enabled = false;
	private LogService logger;	
	
	public VaultTransformer(LogService logger) {
		this.logger = logger;
		this.vault = null;
		
		final String address = System.getenv("VAULT_ADDR");
		final String token = System.getenv("VAULT_TOKEN");
		logger.debug("VaultTransformer ADDR=%s TOKEN=%s", address, token);
		
		if(address == null || token == null) return;
		this.enabled = true;
		try {
			VaultConfig config = new VaultConfig()
			        .address(address)
			        .token(token)
			        .build();
			this.vault = new Vault(config, 1);
			logger.debug("VaultTransformer init successful");
		} catch (VaultException e) {
			enabled = false;
			logger.warn("Unable to initialize Vault transformer", e);
		}
	}

	@Override
	public Map<String, Object> apply(Map<String, Object> t) {
		if(!enabled) return t;
		return replaceVaultProperties(t);
	}
	
	private Map<String, Object> replaceVaultProperties(Map<String, Object> map) {
		return map.entrySet().stream()
				  .map(e -> replaceVaultProperty(e))
				  .collect(Collectors.toMap(e -> e.getKey(), 
											e -> e.getValue()));
	}
	
	private Map.Entry<String, Object> replaceVaultProperty(Map.Entry<String, Object> e) {
		String key = e.getKey();
		if(!(e.getValue() instanceof String)) return e;
		String value = String.valueOf(e.getValue());
		
		Matcher m = PTN.matcher(value);
  	    StringBuffer buf = new StringBuffer();
  	    
  	    //$vault{<first>}
  	    while(m.find()) {
  	        String val = m.group(2); //first look in system properties
  	        logger.debug("VaultTransformer matched %s", val);
  	        String[] split = String.valueOf(val).split(":");
  	        if(split.length == 2) {
				try {
					logger.debug("Searching %s, value %s", split[0], split[1]);
					
					LogicalResponse read = vault.logical().read(split[0]);
					logger.debug("Status %d: %s", read.getRestResponse().getStatus(), new String(read.getRestResponse().getBody()));
					String vaultValue = read.getData().get(split[1]);
					logger.debug("VaultTransformer VALUE -> %s", vaultValue);
					if(vaultValue != null) {
						m.appendReplacement(buf, Matcher.quoteReplacement(vaultValue));
					}
				} catch(VaultException ex) {
					logger.warn("Could not find %s, value %s in Vault", split[0], split[1]);
				}
  	        } else {
  	            m.appendReplacement(buf, Matcher.quoteReplacement(m.group()));
  	        }
  	    }
  	    m.appendTail(buf);
	    return new AbstractMap.SimpleEntry<String, Object>(key, buf.toString());
	}

}
