// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a class with some Configuration metadata. Only makes sense when using
 * properties annotation (ASXXProperty).
 * 
 * <h3>Usage Examples</h3>
 * 
 * <blockquote>
 * <pre>
 * &#64Config(rootSnmpName = "alcatel.srd.a5350.MyClass", 
 *         rootOid = { 637, 71, 6, 3050 },
 *         section = "My Property Section Name")
 * public class MyClass
 * {
 *     // see @XXXProperty annotations
 * }
 * </pre>
 * </blockquote>
 * @see IntProperty
 * @see FileDataProperty
 * @see StringProperty
 * @see AddressProperty
 * @see BooleanProperty
 * @see ExternalProperty
 * @see MSelectProperty
 * @see OrderedProperty
 * @see SelectProperty
 */
@Retention(RetentionPolicy.CLASS)
@Target({ java.lang.annotation.ElementType.TYPE })
public @interface Config {
  /**
   * Defines the unique name for this configuration. This name (or <code>PID</code> in OSGi terminology),
   * identifies a unique configuration dictionary, meant to be injected to an OSGi component. For example,
   * in Declarative Service, the name of a Declarative Service component is used as the configuration name used
   * to retrieve the properties from the configuration database.
   * @return The unique configuration name (PID). default = class name on which the annotation is applied on.
   */
  String name() default "";
  
  /**
   * Defines the configuration section, used when displaying the configuration dictionary in the webadmin.
   * Properties (annotated with @ASXXProperty) may override this section.
   * @return the configuration section
   */
  String section() default "";
  
  /**
   * The root snmp name used when exposing the configuration properties to SNMP.
   * @return The root snmp name used when exposing the configuration properties to SNMP.
   */
  String rootSnmpName() default "";
  
  /**
   * The root snmp OID used when exposing the configuration properties to SNMP.
   * @return The root snmp Oid used when exposing the configuration properties to SNMP.
   */
  int[] rootOid() default {};
  
  /**
   * Returns the legacy property module name (this corresponds to the legacy MODULE parameter in the monconf/Properties.XX file).
   */
  public String monconfModule() default "";
  
  /**
   * Returns the legacy AGENT parameter for the old legacy monconf/Properties file.
   */
  public String monconfAgent() default "";
}
