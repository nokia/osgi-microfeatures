// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MonconfProperties {
  private final String _module;
  private final String _pid;
  private final String _agent;
  private final List<MonconfProperty> _properties = new ArrayList<>();

  public MonconfProperties(String pid, String legacyModule, String legacyAgent) {
    _pid = pid;
    _module = legacyModule;
    _agent = legacyAgent;
  }
  
  public void addProperty(MonconfProperty property) {
    _properties.add(property);
  }
  
  public File getPropertyFile() {
    String dir = System.getProperty("java.io.tmpdir") + File.separator + "monconf";
    File dirFile = new File(dir);
    dirFile.mkdirs();
    return new File(dir, "Properties." + _pid);
  }

  public void write() throws Exception {
    File pidFile = getPropertyFile();
    try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(pidFile)))) {
      out.printf("#Automatically generated by the MBD annotation scanner. Do not edit manually.%n");
      out.printf("%-10s %s%n", "MODULE", _module);
      out.printf("%-10s %s%n", "PID", _pid);
      if (_agent != null) out.printf("%-10s %s%n", "AGENT", _agent);

      for (MonconfProperty props : _properties) {
        out.println();
        props.write(out);
      }
    }
  }
}
