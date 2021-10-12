// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MonconfPropertiesBuilder {
  private final List<MonconfProperties> _properties = new ArrayList<>();

  public void addProperties(MonconfProperties properties) {
    _properties.add(properties);    
  }
  
  public void write() throws Exception {
    for (MonconfProperties props : _properties) {
      props.write();
      
      // If there is a diff between the generated file and the existing file, then abort everything.
      File generated = props.getPropertyFile();
      String generatedName = generated.getName();
      File previousGenerated = new File("monconf" + File.separator + generatedName);  
      if (! previousGenerated.exists()) {
        throw new IllegalStateException("You must copy the generated file " 
          + generated.getPath() + " to ./monconf/" + generatedName);
      }
      
      byte[] generatedBytes = Files.readAllBytes(Paths.get(generated.getPath()));
      byte[] previousGeneratedBytes = Files.readAllBytes(Paths.get(previousGenerated.getPath()));
      if (! Arrays.equals(generatedBytes, previousGeneratedBytes)) {
        throw new IllegalStateException("The generated monconf file " + generated + 
          " does not match previously generated file " + previousGenerated);
      }
      
      // cleanup the generated file
      generated.delete();
    }
  }  
}
