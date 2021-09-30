package com.alcatel_lucent.as.agent.web.container;

import java.util.Map;

import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.MSelectProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;

@Config(name = "webagent", section = "Web Agent parameters", rootSnmpName = "alcatel.srd.a5350.WebAgent", rootOid = { 637, 71, 6, 1130 })
public interface AgentProperties {

    @IntProperty(title = "Request Blocking Timeout", 
      min = 0, 
      max = 8640000, 
      defval = -1,
      section = "Buffers",
      dynamic = false, 
      required = true,
      help="This timeout in millis applies to http request receive timeout. When an http fragmented request body arrives in multiple chunks, the request will fail is chunks does not arrive timely. Choose -1 to not use any blocking request timeout, or a posivite valie in millis."
      )
   public static String REQUEST_BLOCKING_TIMEOUT = "webagent.request.blocking.timeout";

   @IntProperty(title = "Session Timeout", 
      min = 0, 
      max = 8640000, 
      defval = 900,
      section = "Sessions",
      dynamic = false, 
      required = true,
      help="The session timeout value in seconds",
      snmpName = "SessionTimeout",
      oid = 5600)
  public static String SESSION_TIMEOUT = "webagent.session.timeout";
  
  @BooleanProperty(title = "High Availability activated",
      defval=false,
      section="Sessions",
      dynamic=false,
      required=true,
      help="Activates the High Availability mode",
      snmpName="HighAvailability",
      oid=5601)
  final static String HIGH_AVAILABILITY = "webagent.high.availability";

  @IntProperty(title = "Request header buffer size",
      min = 1024, 
      max = 65536, 
      defval = 8192, 
      section = "Buffers",
      dynamic = false, 
      required = true,
      help = "Size of the HTTP request header buffer",
      snmpName="RequestHeaderBufferSize",
      oid=5610)
  final static String HEADER_REQBUFSIZE = "webagent.requestHeaderBufferSize";

  @IntProperty(title = "Response header buffer size",
      min = 1024, 
      max = 65536, 
      defval = 8192, 
      section = "Buffers",
      dynamic = false, 
      required = true,
      help = "Size of the HTTP response header buffer",
      snmpName="ResponseHeaderBufferSize",
      oid=5611)
  final static String HEADER_RSPBUFSIZE = "webagent.responseHeaderBufferSize";

  @IntProperty(title = "Output buffer size",
      min = 1024, 
      max = 65536, 
      defval = 32768, 
      section = "Buffers",
      dynamic = false, 
      required = true,
      help = "Size of the output buffer",
      snmpName="OutputBufferSize",
      oid=5612)
  final static String HEADER_OUTPUTBUF = "webagent.outputBufferSize";

  @FileDataProperty(title = "Extra configuration",
      section="Jetty server",
      dynamic=false,
      required=true,
      help="Jetty extra configuration",
      snmpName="ExtraConfiguration",
      oid=5620,
      fileData="jettyExtraConfig.xml")
  final static String EXTRACONF = "webagent.extraConfig";

  /**
   * Web Connector property metadata used to generate the mbeans descriptor.
   */
  @StringProperty(title = "External webapp contexts directory",
      section="Jetty server",
      dynamic=false,
      required=false,
      help="Directory containing external webapp contexts",
      snmpName="ExternalWebappContextsDirectory",
      oid=5621)
  final static String EXTERNAL_CONTEXTDIR= "webagent.externalContextDir";

  @FileDataProperty(title = "override-web.xml path for all webapps",
		  section="Jetty server",
		  dynamic=true,
		  required=false,
		  help="Descriptor used to override web.xml for all webapps. You can specify here a list of webagent xml descritors for each known web app bundle symbolic names.",
		  fileData="webagent.override.descriptor.properties")
  final static String OVERRIDE_DESCRIPTOR= "webagent.override.descriptor";
  
  @StringProperty(title = "OSGi HTTP Service root path",
      section="Jetty server",
      defval="/",
      dynamic=false,
      required=false,
      help="OSGi HTTP Service root path can be equal to '/' or '/myrootpath' (REST apps can be registered on top of HTTP Service)",
      snmpName="OsgiHttpServicePath",
      oid=5622)
  final static String OSGI_HTTP_SERVICE_PATH= "webagent.osgi.http.service.path";
  
  @BooleanProperty(title = "Show available service paths on 404 errors",
		  defval=false,
		  section="Jetty server",
		  dynamic=false,
		  required=true,
		  help="The default 404 page will list available service paths (disable in production for security reasons)",
		  snmpName="ShowServicePathsOn404",
		  oid=5601)
  final static String SHOW_SERVICE_PATHS_ON_404 = "webagent.show.service.paths.on.404";

  public final static String IO_TP = "IO-Threadpool";
  public final static String PROCESSING_TP = "Processing-Threadpool";
  @MSelectProperty(title="Servlet Executor",
      snmpName="ServletExecutor",
      defval=IO_TP,
      required=true,
      dynamic=false,
      range = { IO_TP, PROCESSING_TP },
      oid=5623,
      help="<p>The executor used to run the servlets:"
          + "<ul>"
          + "<li><b>IO Threadpool:</b> The pool used for IO-bound tasks,</li>"
          + "<li><b>Processing Threadpool:</b> The pool used for CPU-bound tasks</li>"
          + "</ul>")
  public final static String EXECUTOR = "webagent.servlet.executor";
  
  @BooleanProperty(title="Web Agent Auto Start Mode", help="set the property to false in case the agent should not accept traffic on start.", required=true, defval=true)			
  public static final String AUTOSTART = "webagent.autostart";

  /**
   * Web Connector property metadata used to generate the mbeans descriptor.
   */
  @StringProperty(title = "Shared webapps resource classpath directories",
      section="Jetty server",
      dynamic=false,
      required=false,
      help="Optional resource classpath directories shared for all webapps. You can drop resource properties in these directories and then the webapps will be able to " +
           " load them using their webapp bundle classloaders. You can specify multiple directories (comma separated)"
      )
  final static String WEBAPP_RESOURCE_DIRS= "webagent.webapp.resource.dirs";
}
