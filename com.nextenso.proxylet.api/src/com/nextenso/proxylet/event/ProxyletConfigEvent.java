package com.nextenso.proxylet.event;

import java.util.EventObject;
import com.nextenso.proxylet.ProxyletConfig;

/**
 * A ProxyletConfigEvent wraps changes in a ProxyletConfig.
 * <p/>
 * It is passed to the ProxyletConfigListeners when a change occurs.
 */
public class ProxyletConfigEvent
		extends EventObject {

	/**
	 * <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = 1L;

	private String[] _parameters;

	/**
	 * Constructs a new ProxyletConfigEvent.
	 * 
	 * @param src the updated ProxyletConfig Object.
	 * @param parameters the names of the parameters that were modified.
	 */
	public ProxyletConfigEvent(ProxyletConfig src, String[] parameters) {
		super(src);
		_parameters = parameters;
	}

	/**
	 * Gets the updated ProxyletConfig.
	 * 
	 * @return the ProxyletConfig.
	 */
	public ProxyletConfig getProxyletConfig() {
		Object src = getSource();
		return (src instanceof ProxyletConfig) ? (ProxyletConfig) src : null;
	}

	/**
	 * Gets the names of the parameters that have been modified.
	 * 
	 * @return the names of the modified parameters.
	 */
	public String[] getChangedParameters() {
		return _parameters;
	}

}
