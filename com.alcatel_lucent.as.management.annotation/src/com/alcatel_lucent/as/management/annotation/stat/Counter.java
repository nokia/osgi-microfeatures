// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.stat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a method which returns a webadmin statistic counter, which always increments.
 * The method must return an int.
 * The annotation can also be applied on a String class field, which identifies a well-known StatProvider 
 * (used to map an existing Monitorable Meter to Snmp).
 * 
 * <h3>Usage Examples</h3>
 * 
 * <p> Here is a sample showing how an application may define a Counter:
 * <blockquote>
 * <pre>
 * &#64Stat(rootSnmpName = "alcatel.srd.a5350.MyClass", rootOid = { 637, 71, 6, 3050 })
 * public class MyClass
 * {
 *     &#64;Counter(snmpName="MySnmpShortName", oid=101, desc="MyCounter1 Description")
 *     public int getMyCounter()
 *     {
 *         return 1;
 *     }
 *     ...
 * </pre>
 * </blockquote>
 * <p> Here is the above sample defining the same Counter whose all instances values should be averaged :
 * <blockquote>
 * <pre>
 * &#64Stat(rootSnmpName = "alcatel.srd.a5350.MyClass", rootOid = { 637, 71, 6, 3050 }, consolidation = ConsolidationMode.AVERAGE )
 * public class MyClass
 * {
 *     &#64;Counter(snmpName="MySnmpShortName", oid=101, desc="MyCounter1 Description")
 *     public int getMyCounter()
 *     {
 *         return 1;
 *     }
 *     ...
 * </pre>
 * </blockquote>
 * @see Stat
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface Counter {
  /**
   * The sub snmp name used when exposing the statistic counters to snmp. If defined, then the
   * {@link Stat} annotation must be defined and must contain rootSnmp/rootOid attributes
   * 
   * @return The sub snmp name used when exposing the statistic counters to snmp
   * @see Stat#rootSnmpName()
   * @see Stat#rootOid()
   */
  String snmpName() default "";
  
  /**
   * The sub snmp oid used when exposing the statistic counters to snmp. If defined, then the
   * {@link Stat} annotation must be defined and must contain rootSnmp/rootOid attributes
   * 
   * @return The sub snmp name used when exposing the statistic counters to snmp
   * @see Stat#rootSnmpName()
   * @see Stat#rootOid()
   */
  int oid() default -1;
  
  /**
   * The counter index.
   * Optional for backward compatibility but actually mandatory!
   * @return The counter index
   */
  int index() default -1;
  
  /**
   * The counter description.
   * @return The counter description
   */
  String desc();
  
  /**
   * Returns the default value if the counter value returned by a getter method is negative or if the meter
   * associated to a class field name is not available.
   */
  int defaultValue() default 0;
  
  /**
   * The consolidation mode is optional and defines the operation type which should
   * be applied on all instance values for this counter. When present, the (Blueprint) GUI will display
   * the consolidated value in a additional column. 
   * @return The consolidation mode
   */
  ConsolidationMode consolidation() default ConsolidationMode.SUM;
}
