// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel_lucent.as.management.bnd;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.osgi.Annotation;
import aQute.bnd.osgi.ClassDataCollector;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.service.reporter.Reporter;

import com.alcatel_lucent.as.management.annotation.alarm.Alarm;
import com.alcatel_lucent.as.management.annotation.command.Commands;
import com.alcatel_lucent.as.management.annotation.command.Command;
import com.alcatel_lucent.as.management.annotation.config.AddressProperty;
import com.alcatel_lucent.as.management.annotation.config.BooleanProperty;
import com.alcatel_lucent.as.management.annotation.config.Config;
import com.alcatel_lucent.as.management.annotation.config.ExternalProperty;
import com.alcatel_lucent.as.management.annotation.config.FileDataProperty;
import com.alcatel_lucent.as.management.annotation.config.IntProperty;
import com.alcatel_lucent.as.management.annotation.config.MSelectProperty;
import com.alcatel_lucent.as.management.annotation.config.OrderedProperty;
import com.alcatel_lucent.as.management.annotation.config.SelectProperty;
import com.alcatel_lucent.as.management.annotation.config.StringProperty;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Gauge;
import com.alcatel_lucent.as.management.annotation.stat.Stat;

/**
 * This is the scanner which does all the annotation parsing on a given class.
 * To start the parsing, just invoke the parseClassFileWithCollector and finish methods.
 * Once parsed, the corresponding component descriptors can be built using the "print" method.
 */
@SuppressWarnings({ "unused", "serial" })
public class AnnotationCollector extends ClassDataCollector {
  final static String A_ALARM = Alarm.class.getName();
  final static String A_COUNTER = Counter.class.getName();
  final static String A_COMMAND = Command.class.getName();
  final static String A_COMMANDS = Commands.class.getName();
  final static String A_GAUGE = Gauge.class.getName();
  final static String A_ADDRESS_PROPERTY = AddressProperty.class.getName();
  final static String A_BOOLEAN_PROPERTY = BooleanProperty.class.getName();
  final static String A_CONFIG = Config.class.getName();
  final static String A_STAT = Stat.class.getName();
  final static String A_EXTTERNAL_PROPERTY = ExternalProperty.class.getName();
  final static String A_INT_PROPERTY = IntProperty.class.getName();
  final static String A_MSELCT_PROPERTY = MSelectProperty.class.getName();
  final static String A_ORDERED_PROPERTY = OrderedProperty.class.getName();
  final static String A_SELECT_PROPERTY = SelectProperty.class.getName();
  final static String A_STRING_PROPERTY = StringProperty.class.getName();
  final static String A_FILEDATA_PROPERTY = FileDataProperty.class.getName();
  
  private Reporter _reporter;
  private String _className;
  private String[] _interfaces;
  private boolean _isField;
  private String _field;
  private String _method;
  private String _descriptor;
  private Set<String> _methods = new HashSet<String>();
  private Set<String> _dependencyNames = new HashSet<String>();
  private MBDBean _mbd = new MBDBean();
  private boolean _mbdAnnotationParsed;
  private String _strFieldConstantValue;
  private final static Map<String, PropertyBean.Type> _propertyTypes = new HashMap<String, PropertyBean.Type>() {
    {
      put(A_ADDRESS_PROPERTY, PropertyBean.Type.ADDRESS);
      put(A_BOOLEAN_PROPERTY, PropertyBean.Type.BOOLEAN);
      put(A_EXTTERNAL_PROPERTY, PropertyBean.Type.EXTERNAL);
      put(A_INT_PROPERTY, PropertyBean.Type.INTEGER);
      put(A_MSELCT_PROPERTY, PropertyBean.Type.MSELECT);
      put(A_ORDERED_PROPERTY, PropertyBean.Type.ORDERED);
      put(A_SELECT_PROPERTY, PropertyBean.Type.SELECT);
      put(A_STRING_PROPERTY, PropertyBean.Type.STRING);
      put(A_FILEDATA_PROPERTY, PropertyBean.Type.FILEDATA);
    }
  };
  
  /**
   * Makes a new Collector for parsing a given class.
   * @param reporter the object used to report logs.
   */
  public AnnotationCollector(Reporter reporter) {
    _reporter = reporter;
    _mbd.reporter(reporter);
  }
  
  /**
   * Returns the log reporter.
   * @return the log reporter.
   */
  public Reporter getReporter() {
    return _reporter;
  }
  
  /**
   * Parses the name of the class.
   * @param access the class access
   * @param name the class name (package are "/" separated).
   */
  @Override
  public void classBegin(int access, TypeRef ref) {
    _className = ref.getFQN();
    _reporter.trace("class name: " + _className);
    _mbd.fullClassName(_className);
  }
  
  /**
   * Parses the implemented interfaces ("/" separated).
   */
  @Override
  public void implementsInterfaces(TypeRef[] interfaces) {
    _interfaces = new String[interfaces.length];
    for (int i = 0; i < interfaces.length; i++) {
      _interfaces[i] = interfaces[i].getFQN();
    }
    _reporter.trace("implements: %s", Arrays.toString(_interfaces));
  }
  
  /**
   * Parses a method. Always invoked BEFORE eventual method annotation.
   */
  @Override
  public void method(Clazz.MethodDef method) {
    if (method.isConstructor()) {
      return;
    }
        _reporter.trace("Parsed method %s, descriptor=%s", method.getName(), method.getDescriptor());
    _isField = false;
    _method = method.getName();
    _descriptor = method.getDescriptor().toString();
    _methods.add(_method + _descriptor);
  }
  
  /**
   * Parses a field. Always invoked BEFORE eventual field annotation
   */
  @Override
  public void field(Clazz.FieldDef field) {
        _reporter.trace("Parsed field %s, descriptor=%s", field.getName(), field.getDescriptor());
    _isField = true;
    _field = field.getName();
    _descriptor = field.getDescriptor().toString();
  }
  
  /**
   * Parses a String constant value. Always invoked AFTER eventual field annotation.
   * @deprecated method: now the constant value is provided using the "constant" callback.
   */
  public void stringConstantValue(String constantValue) {
    if (constantValue != null) {
      _reporter.trace("Parsed string constant value: %s", constantValue);
    }
    _strFieldConstantValue = constantValue;
  }
  
  /**
   * Parses an integer constant value. Always invoked AFTER eventual field annotation.
   * @deprecated method: now the constant value is provided using the "constant" callback.
   */
  public void integerConstantValue(Integer constantValue) {
    _reporter.trace("Parsed int constant value: %d", constantValue.intValue());
    _strFieldConstantValue = constantValue.toString();
  }
  
  /**
   * Parses a String constant value. Always invoked AFTER eventual field annotation
   */
  public void constant(Object constantValue) {
    if (constantValue != null) {
      _reporter.trace("Parsed constant value: %s", constantValue);
      _strFieldConstantValue = constantValue.toString();
    }
  }
  
  /** 
   * An annotation has been parsed. Always invoked AFTER the "method"/"field"/"classBegin" callbacks. 
   */
  @Override
  public void annotation(Annotation annotation) {
    _reporter.trace("Parsing annotation: %s", Utils.getName(annotation));
    boolean foundAnnotations = false;
    if (Utils.getName(annotation).equals(A_ALARM)) {
      _mbd.alarm(annotation, _strFieldConstantValue);
      foundAnnotations = true;
    } else if (Utils.getName(annotation).equals(A_STAT)) {
      _mbd.stat(annotation);
      foundAnnotations = true;
    } else if (Utils.getName(annotation).equals(A_COMMANDS)) {
      _mbd.commands(annotation);
      foundAnnotations = true;
    } else if (Utils.getName(annotation).equals(A_COMMAND)) {
      _mbd.command(annotation, _method);
      foundAnnotations = true;
    } else if (Utils.getName(annotation).equals(A_COUNTER)) {
      if (Utils.checkDescriptor(_descriptor, Utils.TSTRING)) {
        _mbd.counterField(annotation, CounterBean.Type.COUNTER, _field, _strFieldConstantValue);  
      } else if (Utils.checkDescriptor(_descriptor, Utils.COUNTER_METHOD)) {
        _mbd.counterMethod(annotation, CounterBean.Type.COUNTER, _method);
      } else {
        throw new IllegalArgumentException("Annotation " + Utils.getName(annotation)
          + " must be applied either on a void method which returns an int, or on a class field of String or int type");
      }
      foundAnnotations = true;
    } else if (Utils.getName(annotation).equals(A_GAUGE)) {
      if (Utils.checkDescriptor(_descriptor, Utils.TSTRING)) {
        _mbd.counterField(annotation, CounterBean.Type.GAUGE, _field, _strFieldConstantValue);
      } else if (Utils.checkDescriptor(_descriptor, Utils.COUNTER_METHOD)) {
        _mbd.counterMethod(annotation, CounterBean.Type.GAUGE, _method);
      } else {
        throw new IllegalArgumentException("Annotation " + Utils.getName(annotation)
          + " must be applied either on a void method which returns an int, or on a class field of String or int type");
      }
      foundAnnotations = true;
    } else if (Utils.getName(annotation).equals(A_CONFIG)) {
      _mbd.config(annotation);
      foundAnnotations = true;
    } else {
      PropertyBean.Type ptype = _propertyTypes.get(Utils.getName(annotation));
      if (ptype != null) {
        String propertyName = getPropertyName(annotation);
        _mbd.property(annotation, ptype, _field, propertyName);
        foundAnnotations = true;
      }
    }
    
    // keep track of found annotations (we'll do validation only if we have found some of our annotations).
    _mbdAnnotationParsed |= foundAnnotations;
        
    if (foundAnnotations) {
    	_strFieldConstantValue = null; // reset field value (we have consumed it).
    }
  }
  
  /**
   * Finishes up the class parsing. This method must be called once the parseClassFileWithCollector method has returned.
   * @return true if some annotations have been parsed, false if not.
   */
  public boolean finish() {
    if (_mbdAnnotationParsed) {
      _mbd.validate();
    }
    _reporter.trace("\n");
    return _mbdAnnotationParsed;
  }

  /**
   * Says if there is something to be printed. If not, the caller should not call the
   * print method to avoid writing an empty .mbd file.
   * @return true if printable else false
   */
  public boolean isPrintable() {
    return _mbd.isPrintable();
  }
  
  /**
   * Writes the generated component descriptor in the given print writer.
   * The first line must be the service (@Service or AspectService).
   * @param pw the writer where the component descriptor will be written.
   */
  public void print(PrintWriter pw) {
    _mbd.print(pw);
  }
  
  /**
   * Writes the generated component descriptor in the given monconf builder.
   * @param monconf the builder object used to create the legacy monconf file.
   */
  public void generateMonconf(MonconfPropertiesBuilder monconf) {
    _mbd.print(monconf);
  }

  private String getPropertyName(Annotation annotation) {
    String propertyName = null;
    
    // If the annotation provides a 'name' attribute, return it.
    // Else if the annotation is applied on a method name, then derive the property name from the method name
    // using Dependency Manager property type convention.
    // Else if the annotation is applied on a final static string field, then return the default value.
    propertyName = Utils.get(annotation, "name", null);
    
    if (propertyName == null) {    	
      if (_method != null) {
    	  propertyName = derivePropertyNameFromMethodName(_method);
      } else if (_strFieldConstantValue != null && _strFieldConstantValue.trim().length() > 0) {
    	  if (!Utils.checkDescriptor(_descriptor, Utils.TSTRING)) {
    		  throw new IllegalArgumentException("Annotation " + Utils.getName(annotation)
    		  + " must be applied on a final static String field");
    	  }
    	  propertyName = _strFieldConstantValue;
      } else {
    	  throw new IllegalArgumentException("Annotation " + Utils.getName(annotation)
    	  + " must provide a \"name\" attribute or must be applied on a method name or on final field whose value corresponds to the property name");    	  
      }
    }
    return propertyName;
  }

  /**
   * Derive the property name from a method name on which an annotation is applied.
   * We use dependency manager convention:
   * 
   */
  private String derivePropertyNameFromMethodName(String method) {	
	  // First, strip java bean prefix (like getFoo/isFoo -> foo)
	  method = stripJavabeanPrefix(method);
	  
	  // Next, convert camel case (like fooBar -> foo.bar)
	  method = convertMethodNameUsingUsingCamelCaseConvention(method);
	  
	  // Finally, convert standard OSGI metatype conventions (like foo_bar -> foo.bar, or foo__bar -> foo_bar
	  method = convertMethodNameUsingMetaTypeConvention(method);

	  return method;	  
  }
  
  // strip java bean prefix (like getFoo -> foo or isFoo -> foo)
  private String stripJavabeanPrefix(String methodName) {
      StringBuilder sb = new StringBuilder(methodName);
      if (methodName.startsWith("get")) {
          sb.delete(0, 3);
      } else if (methodName.startsWith("is")) {
          sb.delete(0, 2);
      }
      
      char c = sb.charAt(0);
      if (Character.isUpperCase(c)) {
          sb.setCharAt(0, Character.toLowerCase(c));
      }      
      return (sb.toString());
  }

  // fooBarZoo -> foo.bar.zoo
  private String convertMethodNameUsingUsingCamelCaseConvention(String methodName) {
      StringBuilder sb = new StringBuilder(methodName);
      for (int i = 0; i < sb.length(); i++) {
          char c = sb.charAt(i);
          if (Character.isUpperCase(c)) {
              // camel casing: replace fooBar -> foo.bar
              sb.setCharAt(i, Character.toLowerCase(c));
              sb.insert(i, ".");
          }
      }
      return sb.toString();            
  }

  // foo_bar -> foo.bar or foo__bar -> foo_bar
  // see metatype spec, chapter 105.9.2 in osgi r6 cmpn.
  private String convertMethodNameUsingMetaTypeConvention(String methodName) {
      StringBuilder sb = new StringBuilder(methodName);
      // replace "__" by "_" or "_" by ".": foo_bar -> foo.bar; foo__BAR_zoo -> foo_BAR.zoo
      for (int i = 0; i < sb.length(); i ++) {
          if (sb.charAt(i) == '_') {
              if (i < (sb.length() - 1) && sb.charAt(i+1) == '_') {
                  // replace foo__bar -> foo_bar
                  sb.replace(i, i+2, "_");
              } else {
                  // replace foo_bar -> foo.bar
                  sb.replace(i, i+1, ".");
              }
          } else if (sb.charAt(i) == '$') {
              if (i < (sb.length() - 1) && sb.charAt(i+1) == '$') {
                  // replace foo__bar -> foo_bar
                  sb.replace(i, i+2, "$");
              } else {
                  // remove single dollar character.
                  sb.delete(i, i+1);
              }
          }
      }
      return sb.toString();
  }

  /**
   * Check if the annotated applies on a void method, which returns an int.
   */
  private void checkCounterMethod(Annotation annotation) {
    if (!Utils.checkDescriptor(_descriptor, Utils.COUNTER_METHOD)) {
      throw new IllegalArgumentException("Annotation " + Utils.getName(annotation)
          + " must be applied on a void method which returns an int.");
    }
  }
}
