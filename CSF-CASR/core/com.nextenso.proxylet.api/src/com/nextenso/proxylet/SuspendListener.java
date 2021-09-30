package com.nextenso.proxylet;

/**
 * The Suspend Listener interface.
 */
public interface SuspendListener {

	/**
	 * Called when the associated data has been suspended but not resumed in the
	 * specified delay
	 * 
	 * @param data The data.
	 * @see ProxyletData#setSuspendListener(SuspendListener, long)
	 */
	public void notResumedData(ProxyletData data);
}
