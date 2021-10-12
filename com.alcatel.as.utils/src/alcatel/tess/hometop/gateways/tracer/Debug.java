// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.tracer;

class Debug {
  final static boolean enabled = false;
  
  static void p(Object from, String method, String s) {
    if (enabled) {
      String cname;
      Class clazz;
      
      if (from instanceof Class) {
        clazz = ((Class) from);
      } else {
        clazz = from.getClass();
      }
      
      cname = clazz.getName();
      int lastDot = cname.lastIndexOf(".");
      
      if (lastDot != -1) {
        cname = cname.substring(lastDot + 1);
      }
      
      TracerBox.out.println("[" + cname + "." + method + "] " + s);
    }
  }
}
