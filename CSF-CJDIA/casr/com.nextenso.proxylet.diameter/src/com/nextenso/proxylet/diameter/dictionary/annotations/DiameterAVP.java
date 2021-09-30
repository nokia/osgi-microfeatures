package com.nextenso.proxylet.diameter.dictionary.annotations;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.nextenso.proxylet.diameter.dictionary.DiameterCommandBuilder;

/**
 * Annotate a field whose content will be used to generate a Diameter AVP
 * when processed by a {@link DiameterCommandBuilder DiameterCommandBuilder}
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.FIELD)
public @interface DiameterAVP {
	
	/**
	 * The name of the Diameter AVP definition to refers to.
	 */
	String name();
	
	/**
	 * If present, the AVP will be put in the grouped AVP referred to by this
	 * annotation property. This annotation property must refers to an AVP definition 
	 * of type Grouped.
	 * 
	 */
	String group() default "";
	
	/**
	 * If true, the AVP will not be put in the command if the field's value is null.
	 * If false, an empty AVP will be generated.
	 */
	boolean skipIfNull() default false;
}
