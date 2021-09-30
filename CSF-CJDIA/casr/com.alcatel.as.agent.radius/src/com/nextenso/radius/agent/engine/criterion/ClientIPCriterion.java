package com.nextenso.radius.agent.engine.criterion;

import com.nextenso.mux.util.MuxUtils;
import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.radius.RadiusMessage;

public class ClientIPCriterion
		extends Criterion {

	private int _ip;

	private ClientIPCriterion(String value)
			throws CriterionException {
		_ip = MuxUtils.getIPAsInt(value);
		if (_ip == -1) {
			throw new CriterionException(CriterionException.INVALID_IP, value);
		}
	}


	public static Criterion getInstance(String value)
		throws CriterionException {
		return new ClientIPCriterion(value);
	}

	/**
	 * Gets the IP address as an integer.
	 * @return The IP address.
	 */
	protected int getIp() {
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
		int  dataIp = MuxUtils.getIPAsInt(((RadiusMessage) data).getClientAddr());
		return (getIp() == dataIp) ? TRUE : FALSE;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ("[client-ip=" + MuxUtils.getIPAsString(getIp()) + "]");
	}

	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#includes(com.nextenso.proxylet.engine.criterion.Criterion)
	 */
	@Override
	public boolean includes(Criterion c) {
		// (ip=80) implies (ip=80)
		if (c instanceof ClientIPCriterion) {
			return (getIp() == ((ClientIPCriterion) c).getIp());
		}
		return super.includes(c);
	}

	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#excludes(com.nextenso.proxylet.engine.criterion.Criterion)
	 */
	@Override
	public boolean excludes(Criterion c) {
		// (ip=80) rejects (ip=8080)
		if (c instanceof ClientIPCriterion) {
			return (getIp() != ((ClientIPCriterion) c).getIp());
		}
		return super.excludes(c);
	}

	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getIp();
	}


	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof ClientIPCriterion) {
			return (getIp() == ((ClientIPCriterion) o).getIp());
		}
		return false;
	}

	/**
	 * @see com.nextenso.proxylet.engine.criterion.Criterion#getDepth()
	 */
	@Override
	public int getDepth() {
		return 1;
	}
}
