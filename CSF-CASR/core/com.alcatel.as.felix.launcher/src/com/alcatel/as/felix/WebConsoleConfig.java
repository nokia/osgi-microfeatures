package com.alcatel.as.felix;

import com.alcatel_lucent.as.management.annotation.config.*;

/**
 * Properties definitions for Felix WebConsole
 */
@Config(name="org.apache.felix.http", section="Felix WebConsole Configuration")
public class WebConsoleConfig
{

  @IntProperty(title="Felix WebConsole port number",
      help="Port number used by the Felix WebConsole",
      min=0,
      max=9999,
      defval=9090,
      required=true)
  public final static String WEBCONSOLE_PORT  = "org.osgi.service.http.port";
}
