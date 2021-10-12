// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nextenso.proxylet.engine.criterion;

/**
* This a utility class that wraps a criterion, a name and a description.
* The Criterion object cannot store a name or a description because of the
* optimization process (which does not create a new Object for each new Criterion).
*/

public class CriterionWrapper {
  
  private String name, description;
  private Criterion criterion;
  
  public CriterionWrapper() {
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getName() {
    return this.name;
  }
  
  public void setDescription(String desc) {
    this.description = desc;
  }
  
  public String getDescription() {
    return this.description;
  }
  
  public void setCriterion(Criterion criterion) {
    this.criterion = criterion;
  }
  
  public Criterion getCriterion() {
    return criterion;
  }
}
