package com.nextenso.proxylet.radius;

import com.nextenso.proxylet.Proxylet;

/**
 * This is a markup interface for radius proxylets. <br/>
 * It does not define any specific method. It defines the returned codes of the
 * <code>accept</code> method that all subinterfaces define.
 */
public interface RadiusProxylet
		extends Proxylet {

	/**
	 * Possible returned code of method <code>accept</code>: the proxylet accepts
	 * the message and will not block while processing it.
	 */
	public static final int ACCEPT = 1;

	/**
	 * Possible returned code of method <code>accept</code>: the proxylet accepts
	 * the message but may block while processing it.
	 */
	public static final int ACCEPT_MAY_BLOCK = 2;

	/**
	 * Possible returned code of method <code>accept</code>: the proxylet does not
	 * want to process the message.
	 */
	public static final int IGNORE = 3;

	/**
	 * The Engine will return to the first proxylet.
	 */
	public static final int FIRST_PROXYLET = 1;

	/**
	 * The proxy will call the same proxylet again.
	 */
	public static final int SAME_PROXYLET = 2;

	/**
	 * The Engine will call the next proxylet.
	 */
	public static final int NEXT_PROXYLET = 3;

	/**
	 * The Engine will not call any other proxylet.
	 */
	public static final int LAST_PROXYLET = 4;
	
	/**
	 * The engine will suspend to processing of the current proxylet chain.
	 */
	public static final int SUSPEND = 5;

	/**
	 * The Engine will respond to the client and the response will go through the
	 * response proxylets.
	 */
	public static final int RESPOND_FIRST_PROXYLET = 100;

	/**
	 * The Engine will respond to the client and the response will NOT go through
	 * the response proxylets.
	 */
	public static final int RESPOND_LAST_PROXYLET = 101;

}
