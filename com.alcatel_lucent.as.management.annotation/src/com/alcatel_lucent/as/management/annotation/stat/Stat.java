package com.alcatel_lucent.as.management.annotation.stat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a class for defining general statistic informations like snmp root Oid. 
 * This annotation only makes sense when some {@link Counter} or {@link Gauge} statistic annotation are used.
 * @see Counter
 * @see Gauge 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ java.lang.annotation.ElementType.TYPE })
public @interface Stat {
  /**
   * The root snmp name used when exposing the statistic counters to snmp. If defined, then
   * all types of annotated counters must also contains snmp attributes.
   * 
   * @return the root snmp name used when exposing the statistic counters to snmp
   * @see Gauge#snmpName()
   * @see Gauge#oid()
   * @see Counter#snmpName()
   * @see Counter#oid()
   */
  String rootSnmpName() default "";
  
  /**
   * The root oid used when exposing the statistic counters to snmp. If defined, then
   * all types of annotated counters must also contains snmp attributes.
   * 
   * @return the root oid used when exposing the statistic counters to snmp
   * @see Gauge#snmpName()
   * @see Gauge#oid()
   * @see Counter#snmpName()
   * @see Counter#oid()
   */
  int[] rootOid() default {};
}
