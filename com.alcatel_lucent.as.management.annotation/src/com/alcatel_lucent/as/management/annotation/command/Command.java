// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method which handles a webadmin command.
 * The method must return void and take no parameter.
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> Here is a sample showing how an application may define a command.
 * <blockquote>
 * <pre>
 * &#64;Component(provide={}, properties={  //register instance as an OSGi component
 *                               CommandScopes.COMMAND_SCOPE+"="+CommandScopes.APP_COMMAND_SCOPE,
 *                               ConfigConstants.MODULE_NAME+"=MyClass"})
 * public class MyClass
 * {
 *     &#64;Command(desc="Description of the command")
 *     public void myCommand()
 *     {
 *         //do something smart
 *     }
 *     ...
 * </pre>
 * </blockquote>
 * @see Commands
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.METHOD })
public @interface Command {
  /**
   * The sub snmp name used when exposing the command to snmp. If defined, then the
   * {@link Commands} annotation must be defined and must contain rootSnmp/rootOid attributes
   * 
   * @return The sub snmp name used when exposing the command to snmp
   * @see Commands#rootSnmpName()
   * @see Commands#rootOid()
   */
  String snmpName() default "";
  
  /**
   * The sub snmp oid used when exposing the command to snmp. If defined, then the
   * {@link Commands} annotation must be defined and must contain rootSnmp/rootOid attributes
   * 
   * @return The sub snmp name used when exposing the command to snmp
   * @see Commands#rootSnmpName()
   * @see Commands#rootOid()
   */
  int oid() default -1;
  
  /**
   * The command code.
   * @return The command code
   */
  int code();
  
  /**
   * The command description.
   * @return The command description
   */
  String desc();
}
