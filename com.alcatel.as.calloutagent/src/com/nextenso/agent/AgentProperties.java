package com.nextenso.agent;

import com.alcatel_lucent.as.management.annotation.config.*;

@Config(name = "agent", rootSnmpName = "alcatel.srd.a5350.CalloutAgent", rootOid = { 637, 71, 6, 110 },
        section = "General Parameters")
public interface AgentProperties {
  
  @MSelectProperty(title = "Protocols", range = "",//value will be provided automatically by deployed containers
                   help = "Deprecated: The list of protocols that Agents will handle.", oid = 101,
                   snmpName = "Protocols", dynamic = false)
  public static final String MUX_HANDLERS = "agent.muxhandlers";
  
  @StringProperty(title = "Protocols running in the agent's main thread",
                  defval = "",
                  help = "Enter the list of protocols (capitalized, space separated) running in the agent main thread. By default, all protocols are handled by a separate thread.",
                  oid = 102, snmpName = "ProtocolsRunningInTheAgentSMainThread", dynamic = false)
  public static final String ASYNC_HANDLERS = "agent.asyncHandlers";
  
  @IntProperty(min = 1, max = 100, title = "Number of Queue readers for the asynchronous events",
               help = "Specifies the number of readers that consume the queue of the asynchronous events.",
               oid = 127, snmpName = "NumberOfQueueReadersForTheAsynchronousEvents", required = true,
               dynamic = false, section = "Asynchronous Events", defval = 3)
  public static final String ASYNCHRONOUS_READERS = "pxlet.container.Qreaders";
  
  @IntProperty(min = 0,
      max = 40000,
      title = "Max size of elastic container instances",
      help = "When the load increase, some containers which support elasticity may be dynamically instantiated at runtime. This parameter defines the max number of elastic containers the JVM will be able to create. 0 means that the current number of processors is used as the max number of elastic containers.",
      oid = 135, snmpName = "GracefulShutdownTimeout", required = false, dynamic = false, defval = 1)
  public static final String AGENT_ELASTICITY = "agent.elasticity";
  
  @BooleanProperty(
      title = "High availability.",
      help = "HA support. If this property is enabled, then the callout agent will use the distributed session manager in order to provide the ring id in the IO handler mux identifacation header.",
      required = false, dynamic = false, defval = true)
  public static final String AGENT_HA = "agent.ha";
  
  @BooleanProperty(
	      title = "Dump stack traces on unexpected exit.",
	      help = "Dump a stacktrace when System.exit is called unexpectedly.",
	      required = false, dynamic = true, defval = true)
  public static final String DUMP_STACKTRACE = "agent.dumpStackTraceOnUnexpectedExit";
  
  @BooleanProperty(
	      title = "Use legacy Jndi Configuration.",
	      help = "Sets this property to true if the configuration must be registered in JNDi.",
	      required = false, dynamic = false, defval = true)
  public static final String JNDI_CONFIG = "agent.jndi";
}
