package com.nextenso.proxylet.radius;

/**
 * The authentication rule.
 */
public interface AuthenticationRule {

	/**
	 * Gets the password.
	 * 
	 * @return The password not encrypted.
	 */
	public byte[] getPassword();

	/**
	 * Indicates whether the ip matches the rule.
	 * 
	 * @param ip The IP address to be checked.
	 * @return true if the rule is applicable for the IP address.
	 */
	public boolean match(int ip);

	/**
	 * Indicates if the accounting request must be authenticated if the IP matches
	 * the rule.
	 * 
	 * <BR>It allows to have trusted IP or trusted domains.
	 * 
	 * @return true if the authentication is required.
	 */
	public boolean requiresAuthentication();
}
