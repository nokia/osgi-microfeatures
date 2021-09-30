package com.alcatel.as.service.diagnostics;

import java.io.PrintStream;

/**
 * The ServiceDiagnostics service provides a runtime analysis of OSGi services 
 * and enables tracking leaf missing dependencies in the "forest" of unavailable services.
 *
 * @deprecated Use org.apache.felix.servicediagnostics.ServiceDiagnostics instead
 */
public interface ServiceDiagnostics {
  /** returns a json representation of all OSGi service in the framework */
  String allServices() ;
  /** outputs a json representation of all OSGi service in the framework to the given PrintStream */
  void allServices(PrintStream out) ;
  /** returns merged leaf unvailable services from different ServiceDiagnosticsPlugin implementations */
  String notAvail() ;
  /** outputs merged leaf unvailable services from different ServiceDiagnosticsPlugin implementations 
   * to the given PrintStream */
  void notAvail(PrintStream out) ;
}

