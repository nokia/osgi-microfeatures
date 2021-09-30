package com.alcatel.as.service.reporter.api ;

import org.apache.felix.service.command.CommandProcessor;

/**
 * Core reporter definitions.
 * Examples:
 * <pre>
   // register counters
   _bundleContext.registerService(Object.class.getName(), new Counters(), new Hashtable() {{
     put(ConfigConstants.MODULE_NAME, sourceId);
     put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COUNTER_SCOPE);
     put(CommandScopes.COMMAND_FUNCTION, new String[] { "counterOne", "counterTwo" });
   }});

   // register commands
   _bundleContext.registerService(Object.class.getName(), new Commands(), new Hashtable() {{
     put(ConfigConstants.MODULE_NAME, sourceId);
     put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COMMAND_SCOPE);
     put(CommandScopes.COMMAND_FUNCTION, new String[] { "commandOne", "commandTwo" });
   }});
 * </pre>
 *
 * The "sourceId" identifier represents the "path" to the sub-component, 
 * such as "container/application/servlet". For instance: "http/MyApp/MyProxylet".
 * It should be ommitted only for the core instance itself (typically the callout agent)
 */
public interface CommandScopes {

  /**
   * Command scope key.
   * The key to be used for the command scope value when registering a command.
   */
  public static final String COMMAND_SCOPE = CommandProcessor.COMMAND_SCOPE;

  /**
   * Module name to use when defining counters or commands associated with the core component
   * itself (and not a sub-component)
   */
  public static final String CORE_COMPONENT = "Core" ;

  /**
   * Command function key.
   * The key to be used for the command function value when registering a command.
   * The corresponding value is an array of String, containing the method names usable for the given scope. 
   */
  public static final String COMMAND_FUNCTION = CommandProcessor.COMMAND_FUNCTION;


  /** 
   * Command scope: Application commands.
   * An application which supports application commands needs to create unique methods for
   * the commands it supports and register them with this command scope. The signature
   * of these methods must be "String myCommand (String params)" where params is the parameter
   * received from the management entity which initiated the command execution. This method
   * returns a String which can be whatever the command implementation sees fit to return
   */
  public static final String APP_COMMAND_SCOPE = "asr.agent.commands" ;

  /** 
   * Command scope: Application counters.
   * An application which supports counters needs to create unique methods to report counter
   * values to management entities and register them with this command scope. The signature
   * of these methods must be "String myCommand()". The command must return a JSON string
   * representing its current counter values. Counters from several counter handlers are
   * merged by the reporter in a single JSON array
   */
  public static final String APP_COUNTER_SCOPE = "asr.agent.counters" ;

  /** 
   * Command scope: Property update handlers.
   * An application which supports runtime updates of its property values needs to create unique 
   * methods through which it will be notified when its properties are updated and register them 
   * with this command scope. The signature of these methods must be "void myCommand (String param)".
   * The parameter string provided in argument is implementation dependent but is typically a list
   * of updated property PIDs 
   * @deprecated This property is not used anymore.
   */
  public static final String APP_PROPERTY_SCOPE = "asr.agent.properties" ;

  /** 
   * Command scope: Shutdown command handlers.
   * An application which needs to perform specific actions when instructed to shutdown by a management
   * entity needs to create unique methods to handle shutdown requests and register them with this 
   * command scope. The signature of these methods must be "void myCommand()".
   * When the reporter receives a shutdown order, it checks if there is at least one registered
   * shutdown handler. If not, it immediately executes a System.exit() to shutdown the JVM right
   * away. If on the other hand one or more shutdown handlers are defined, they are called all at
   * once. The monitor does not wait for the completion of the shutdown handler executions and it is
   * the responsibility of the application to shutdown the JVM when it is appropriate.
   * @deprecated This property is not used anymore.
   */
  public static final String APP_SHUTDOWN_SCOPE = "asr.agent.shutdown" ;

  /**
   * Topic used when sending commands using the EventAdmin API.
   * For example:
   * eventAdmin.sendEvent(new Event(
                   COMMAND_TOPIC,
                   (Map) new Properties() {
                       {
                           put(COMMAND_TARGET, "targetApplication/targetGroup/targetComponent/targetInstance/targetModule");
                           put(COMMAND_CODE, "targetCommand");
                       }
                   }));
   */
  public static final String COMMAND_TOPIC = "as/reporter/command" ;

  /**
   * Used to identify the target instance
   * when sending commands using the EventAdmin API.
   * For example:
   * eventAdmin.sendEvent(new Event(
                   COMMAND_TOPIC,
                   (Map) new Properties() {
                       {
                           put(COMMAND_TARGET, "targetApplication/targetGroup/targetComponent/targetInstance/targetModule");
                           put(COMMAND_CODE, "targetCommand");
                       }
                   }));
   */
  public static final String COMMAND_TARGET = "asr.command.target" ;

  /**
   * Used to identify the target command code 
   * when sending commands using the EventAdmin API.
   * For example:
   * eventAdmin.sendEvent(new Event(
                   COMMAND_TOPIC,
                   (Map) new Properties() {
                       {
                           put(COMMAND_TARGET, "targetApplication/targetGroup/targetComponent/targetInstance/targetModule");
                           put(COMMAND_CODE, "targetCommand");
                       }
                   }));
   */
  public static final String COMMAND_CODE = "asr.command.code" ;
}
