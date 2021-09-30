package com.alcatel_lucent.as.management.annotation.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a String class field for a property whose type is a String. The default value is 
 * stored in the annotated class bundle
 *
 * <p> Here is a sample showing a proxylet may defining a File Data Property: 
 * 
 * <blockquote>
 * <pre>
 * &#64Config(section = "My Properties Section Name")
 * public class MyProxylet implements BufferedHttpRequestProxylet
 * {
 *     &#64;FileDataroperty(title="My String Property", required=true, fileData="META-INF/MyDefaultValue.xml")
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
public @interface FileDataProperty {
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
   * Returns the file data containing the default value for this property.
   */
  public String fileData();
  
  /**
   * Returns the property scope, which specifies the area this property must be applie to.
   */
  public Scope scope() default Scope.ANY;
  
  /**
   * Returns the property visibility which specify in which mode the property is displayed.
   */
  public Visibility visibility() default Visibility.BASIC;

  /**
   * Returns the optional URL of an external legacy editor.
   */
  public String displayPage() default "";
  
  /**
   * Returns the optional URL of an external blueprint editor.
   */
  public String blueprintEditor() default "";
  
  /**
   * Returns the legacy property visbility (PUBLIC/PRIVATE).
   */
  public MonconfVisibility monconfVisibility() default MonconfVisibility.PUBLIC;
}
