package com.nextenso.diameter.agent.engine;

import com.nextenso.proxylet.Proxylet;
import com.nextenso.proxylet.engine.Context;
import com.nextenso.proxylet.engine.ProxyletChain;
import com.nextenso.proxylet.engine.criterion.Criterion;

public class DiameterProxyletChain
		extends ProxyletChain {

	public DiameterProxyletChain(Context ctx) {
		super(ctx, -1);
	}

	@Override
	public Proxylet nextProxylet(ProxyletStateTracker target) {
		int state = target.getProxyletState();
		int index = (state & INDEX_MASK);
		Proxylet[] proxylets = getProxylets();
		if (state != index)
			// locked
			return proxylets[index];
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

	private boolean match(int index, ProxyletStateTracker target) {
		return (getCriterion(index).match(target) == Criterion.TRUE);
	}

}
