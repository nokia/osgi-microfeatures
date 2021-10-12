// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.diameter.agent.engine.criterion;

import com.nextenso.proxylet.ProxyletData;
import com.nextenso.proxylet.diameter.DiameterMessage;
import com.nextenso.proxylet.engine.criterion.Criterion;
import com.nextenso.proxylet.engine.criterion.CriterionException;
import com.nextenso.proxylet.engine.criterion.TrueCriterion;

public class ApplicationCriterion
		extends Criterion {

	private long _applicationId;

	private ApplicationCriterion(String id)
			throws NumberFormatException, CriterionException {
		this(Long.parseLong(id));
	}

	public ApplicationCriterion(long id)
			throws CriterionException {
		if (id <= 0) {
			throw new CriterionException(CriterionException.INVALID_APPLICATION, String.valueOf(id));
		}
		_applicationId = id;
	}

	protected long getApplicationId() {
		return _applicationId;
	}

	@Override
	public int match(ProxyletData data) {
		if (data instanceof DiameterMessage) {
			return match(((DiameterMessage) data).getDiameterApplication());
		}
		return FALSE;
	}

	public int match(long id) {
		return (id == _applicationId) ? TRUE : FALSE;
	}

	@Override
	public String toString() {
		return ("[application=" + _applicationId + "]");
	}

	public static Criterion getInstance(String id)
		throws CriterionException {
		if ("*".equals(id)) {
			return TrueCriterion.getInstance();
		}

		try {
			return new ApplicationCriterion(id);
		}
		catch (NumberFormatException e) {
			throw new CriterionException(CriterionException.INVALID_APPLICATION, id);
		}
	}

	@Override
	public boolean includes(Criterion c) {
		// (id=80) implies (id=80)
		if (c instanceof ApplicationCriterion) {
			return (_applicationId == ((ApplicationCriterion) c).getApplicationId());
		}
		return super.includes(c);
	}

	@Override
	public boolean excludes(Criterion c) {
		// (id=80) rejects (id=8080)
		if (c instanceof ApplicationCriterion) {
			return (_applicationId != ((ApplicationCriterion) c).getApplicationId());
		}
		return super.excludes(c);
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ApplicationCriterion) {
			return (_applicationId == ((ApplicationCriterion) o).getApplicationId());
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Long.valueOf(_applicationId).hashCode();
	}

	@Override
	public int getDepth() {
		return 1;
	}
}
