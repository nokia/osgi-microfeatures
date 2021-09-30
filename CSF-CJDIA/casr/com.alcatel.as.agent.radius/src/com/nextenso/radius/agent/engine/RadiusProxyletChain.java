package com.nextenso.radius.agent.engine;

import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.criterion.Criterion;

public class RadiusProxyletChain
		extends ProxyletChain {

	public RadiusProxyletChain(Context context, int type) {
		super(context, type);
	}

	/**
	 * @see com.nextenso.proxylet.engine.ProxyletChain#nextProxylet(com.nextenso.proxylet.engine.ProxyletChain.ProxyletStateTracker)
	 */
	@Override
	public Proxylet nextProxylet(ProxyletStateTracker target) {
		int state = target.getProxyletState();
		int index = (state & INDEX_MASK);
		Proxylet[] proxylets = getProxylets();
		if (state != index) {
			// locked
			return proxylets[index];
		}
		// unlocked
		for (int k = index; k < proxylets.length; k++) {
			if (match(k, target)) {
				// we lock
				target.setProxyletState(k | LOCK_BIT);
				return proxylets[k];
			}
		}
		target.setProxyletState(proxylets.length);
		return null;
	}

	public boolean match(int i, ProxyletStateTracker target) {
		return (getCriterion(i).match(target) == Criterion.TRUE);
	}
}
