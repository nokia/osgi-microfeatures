package com.nextenso.proxylet.engine.criterion;

import com.nextenso.proxylet.admin.CriterionValue;

public interface CriterionParser {
  public Criterion parseCriterionValue(CriterionValue value) throws CriterionException;
}
