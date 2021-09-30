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
