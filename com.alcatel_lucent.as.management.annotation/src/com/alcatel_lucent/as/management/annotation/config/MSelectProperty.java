// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a String class field for a property name whose value may contain one of several values
 * chosen from a range of predefined values. 
 *
 * <p> Here is a sample showing a proxylet defining a MSelect Property: 
 * 
 * <blockquote>
 * <pre>
 * &#64Config(section = "My Properties Section Name")
 * public class MyProxylet implements BufferedHttpRequestProxylet
 * {
 *     &#64;MSelectProperty(title="My String Property", required=true, defval="1",
 *                      range={"1", "2", "3"})
 *     public final static String MY_PROPERTY = "MyApplication.myProperty";
 *     
 *     public void init(ProxyletConfig cnf) throws ProxyletException {
 *        String myPropertyValue = cnf.getStringProperty(MY_PROPERTY);
 *     }
 *     ...
 * </pre>
 * </blockquote>
 * 
 * @see Config
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface MSelectProperty {
  /**
   * Returns the property name. If not specified, then the annotation has to be applied on a final String field and 
   * the field constant value is used as the property name.
   */
  public String name() default "";
  
  /**
   * Returns the property HTML help string.
   */
  public String help() default "";
  
  /**
   * Returns the property HTML help file, which must be included in the jar of the annotated class.
   * (for instance: META-INF/helps/myproperty.html)
   */
  public String helpPath() default "";
  
  /**
   * Returns the GUI section name, when the property will be displayed.
   */
  public String section() default "";
  
  /**
   * Returns the property webadmin title.
   */
  public String title();
  
  /**
   * Optional property snmp mapping. The first snmp Oid digit must be >= 100.
   */
  public String snmpName() default "";
  
  /**
   * Optional property snmp mapping. The first snmp Oid digit must be >= 100.
   */
  public int oid() default -1;
  
  /**
   * Is this property required ?
   */
  public boolean required() default false;
  
  /**
   * Is this property dynamic.
   */
  public boolean dynamic() default false;
  
  /**
   * Returns an optional validaton class.
   * @deprecated
   * @see validation()
   */
  public Class<?> valid() default Object.class;
  
  /**
   * Returns an optional validaton class.
   */
  public String validation() default "";
  
  /**
   * Returns default property value as a string.
   * @deprecated
   */
  public String defval() default "";
  
  /**
   * Returns default property values 
   */
  public String[] defvals() default {}; 

  /**
   * Returns the property range
   */
  public String[] range();
  
  /**
   * Returns the property scope, which specifies the area this property must be applie to.
   */
  public Scope scope() default Scope.ANY;
  
  /**
   * Returns the property visibility which specify in which mode the property is displayed.
   */
  public Visibility visibility() default Visibility.BASIC;
  
  /**
   * Returns the legacy property module name (this corresponds to the legacy MODULE parameter in the monconf/Properties.XX file).
   */
  public String legacyModule() default "";
  
  /**
   * Returns the legacy property visbility (PUBLIC/PRIVATE).
   */
  public MonconfVisibility monconfVisibility() default MonconfVisibility.PUBLIC;
}
