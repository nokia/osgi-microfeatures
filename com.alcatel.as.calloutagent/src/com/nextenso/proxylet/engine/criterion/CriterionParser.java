// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

import com.nextenso.proxylet.admin.CriterionValue;

public interface CriterionParser {
  public Criterion parseCriterionValue(CriterionValue value) throws CriterionException;
}
