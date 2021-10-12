// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.management.blueprint;

import java.util.List;
import java.util.ArrayList;

public class ResolutionException extends Exception
{
  List<String> missing; 
  public ResolutionException(String request) 
  {
    super(request);
    missing = new ArrayList<String>();
  }

  public String getRequest() { return super.getMessage(); }

  public ResolutionException addMissingRequirement(String req)
  {
    missing.add(req);
    return this;
  }

  public List<String> getMissingRequirements() { return missing; }
}
