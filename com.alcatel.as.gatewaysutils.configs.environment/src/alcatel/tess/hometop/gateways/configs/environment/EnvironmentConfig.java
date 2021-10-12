// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.configs.environment;

import com.alcatel_lucent.as.management.annotation.config.*;

/**
 * This interface is used to declare the environment.properties configuration file
 * in the local configuration space of blueprint instances
 */
@Config(name="environment", section="Environment")
public interface EnvironmentConfig {
  @FileDataProperty(title="Environment", required=false, dynamic=false, fileData="environment.properties")
  public static final String ENV_PROPFILE = "environment.properties";
}
