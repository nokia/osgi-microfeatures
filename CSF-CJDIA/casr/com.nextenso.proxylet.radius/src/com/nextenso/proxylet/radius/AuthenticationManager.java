package com.nextenso.proxylet.radius;

import java.util.List;

/**
 * The Authentication Manager.
 * In order to get a manager, define an osgi service dependency on it:
 * <pre>
 * @Reference
 * void bindAuthenticationManager(AuthenticationManager manager) { ... }
 * </pre>
 */
public interface AuthenticationManager extends List<AuthenticationRule> {
			
	/**
	 * Gets the rule associated to the IP address.
	 * 
	 * @param ip The IP address.
	 * @return The rule or null if not found
	 */
	public AuthenticationRule getRule(int ip);
}
