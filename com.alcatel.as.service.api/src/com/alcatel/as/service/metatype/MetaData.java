// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.service.metatype;

import java.util.Map;

/**
 * Accessor interface for configuration and administration meta data of a bundle
 */
public interface MetaData 
{
  /**
   * Gets the name of the bundle this MetaData came from
   */
  String getBundleName();

  /**
   * Gets the symbolic name of the bundle this MetaData came from
   */
  String getBundleSymbolicName();

  /**
   * Gets the version of the bundle this MetaData came from
   */
  String getBundleVersion();

  /**
   * Returns the list of configuration properties descriptors for one bundle 
   * mapped by configuration pid
   */
  Map<String, PropertiesDescriptor> getProperties();
  /**
   * Returns the list of counter descriptors for one bundle 
   * mapped by counter full name (class.method)
   */
  Map<String, CounterDescriptor> getCounters();
  /**
   * Returns the list of alarm descriptors for one bundle
   * mapped by alarm full name (source.name)
   */
  Map<String, AlarmDescriptor> getAlarms();
  /**
   * Returns the list of command descriptors for one bundle
   * mapped by command full name (class.method)
   */
  Map<String, CommandDescriptor> getCommands();
}
