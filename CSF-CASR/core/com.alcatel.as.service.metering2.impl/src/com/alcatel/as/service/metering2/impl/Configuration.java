package com.alcatel.as.service.metering2.impl;

import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.MonconfVisibility;

/**
 * Metering Service configuration.
 */
@Config(name="com.alcatel.as.service.metering2.impl.MeteringServiceImpl", section="Metering Service", monconfModule="CalloutAgent")
public class Configuration {

  /**
   * Derived meters configuration.
   */
  @FileDataProperty(title="Derived Meters", dynamic=true, required=true, fileData="derivedMeters.txt",
		  help="This property contains the list of derived meters (rate, max value, ...) to create at startup.")
  public final static String DERIVED = "meters.derived";

  /**
   * Shutdown meters configuration.
   */
  @FileDataProperty(title="Shutdown Meters", dynamic=true, required=true, fileData="shutdownMeters.txt",
		  help="This property contains the list of meters to check upon shutdown.")
  public final static String SHUTDOWN = "meters.shutdown";
  
  /**
   * Remote meters configuration. This property is used by the remote meters implementation (IOHandlerImpl).
   * TODO: move this property into the IOHAndlerImpl and create a specific pid for the remote meters.
   */
  @FileDataProperty(title="Remote Meters", dynamic=true, required=true, fileData="remoteMeters.txt",
		  help="This property contains the list of remote meters to fetch locally.")
  public final static String REMOTE = "meters.remote";
  
  /**
   * Flag telling if the metering servlet should be enabled or not.
   */
  @BooleanProperty(
      title = "Metering Servlet.",
      help = "Enables the Http Metering Servlet.",
      required = false, dynamic = false, defval = false)
  public static final String ENABLE_SERVLET = "meters.servlet";

}
