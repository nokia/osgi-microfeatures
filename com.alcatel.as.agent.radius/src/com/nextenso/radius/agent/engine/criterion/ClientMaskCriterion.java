package com.nextenso.radius.agent.engine.criterion;

import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.FalseCriterion;
import com.nextenso.proxylet.engine.criterion.TrueCriterion;
import com.nextenso.proxylet.radius.RadiusMessage;

public class ClientMaskCriterion
		extends Criterion {

	private int _ip;

	private ClientMaskCriterion(int ip) {
		_ip = ip;
	}


	public static Criterion getInstance(String value)
		throws CriterionException {
		if (value.equals("255.255.255.255")) {
			return TrueCriterion.getInstance();
		}
		int ip = MuxUtils.getIPAsInt(value);
		if (ip == -1) {
			throw new CriterionException(CriterionException.INVALID_IP, value);
		}
		if (ip == 0) {
			return FalseCriterion.getInstance();
		}
		return new ClientMaskCriterion(ip);
	}


	private  int getIp() {
		return _ip;
	}

	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#match(com.nextenso.proxylet.ProxyletData)
	 */
	@Override
	public int match(ProxyletData data) {
		if (! (data instanceof RadiusMessage)) {
			return FALSE;
		}
		String value = ((RadiusMessage) data).getClientAddr();
		return (match(MuxUtils.getIPAsInt(value))) ? TRUE : FALSE;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ("[client-mask=" + MuxUtils.getIPAsString(getIp()) + "]");
	}
	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#includes(com.nextenso.proxylet.engine.criterion.Criterion)
	 */
	@Override
	public boolean includes(Criterion c) {
		// (mask=11) implies (mask=111)
		if (c instanceof ClientMaskCriterion) {
			return ((ClientMaskCriterion) c).match(getIp());
		}
		return super.includes(c);
	}

	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#excludes(com.nextenso.proxylet.engine.criterion.Criterion)
	 */
	@Override
	public boolean excludes(Criterion c) {
		// (mask=10) rejects (mask=01)
		if (c instanceof ClientMaskCriterion) {
			int ipc = ((ClientMaskCriterion) c).getIp();
			return ((ipc & getIp()) == 0);
		}
		return super.excludes(c);
	}

	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ClientMaskCriterion) {
			return (getIp() == ((ClientMaskCriterion) o).getIp());
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getIp();
	}

	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#getDepth()
	 */
	@Override
	public int getDepth() {
		return 1;
	}
	

	protected boolean match(int value) {
		return ((value & getIp()) == value);
	}

}
