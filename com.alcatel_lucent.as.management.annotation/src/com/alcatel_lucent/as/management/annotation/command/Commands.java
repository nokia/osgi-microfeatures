package com.alcatel_lucent.as.management.annotation.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a class which handles a webadmin commands.
 * This annotation is only used to specify the root SNMP name and OID, 
 * when the commands need to be provided via SNMP
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> Here is a sample showing how an application may define a command.
 * <blockquote>
 * <pre>
 * &#64;Component(provide={}, properties={  //register instance as an OSGi component
 *                               CommandScopes.COMMAND_SCOPE+"="+CommandScopes.APP_COMMAND_SCOPE,
 *                               ConfigConstants.MODULE_NAME+"=MyClass"})
 * &#64;Commands(rootSnmpName="alcatel.srd.a5350.MyClass", rootOid = { 637, 71, 6, 3050 })
 * public class MyClass
 * {
 *     &#64;Command(snmpName="MySnmpShortName", oid=101, desc="Description of the command")
 *     public void myCommand()
 *     {
 *         //do something smart
 *     }
 *     ...
 * </pre>
 * </blockquote>
 * @see Command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.TYPE })
public @interface Commands {
  /**
   * The root snmp name used when exposing the commands to snmp. If defined, then
   * all types of annotated commands must also contain snmp attributes.
   * 
   * @return the root snmp name used when exposing the commands to snmp
   * @see Command#snmpName()
   * @see Command#oid()
   */
  String rootSnmpName() default "";
  
  /**
   * The root snmp oid used when exposing the commands to snmp. If defined, then
   * all types of annotated commands must also contain snmp attributes.
   * 
   * @return the root snmp oid used when exposing the commands to snmp
   * @see Command#snmpName()
   * @see Command#oid()
   */
  int[] rootOid() default {};
}
