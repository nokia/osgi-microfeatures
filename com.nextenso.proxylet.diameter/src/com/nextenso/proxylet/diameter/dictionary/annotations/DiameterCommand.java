package com.nextenso.proxylet.diameter.dictionary.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.nextenso.proxylet.diameter.dictionary.DiameterCommandBuilder;

/**
 * Annotates a class to be used to generate a Diameter request or
 * answer.
 * <p/>
 * Either the name or the abbreviation and vendorId are required.
 * 
 *  @see DiameterCommandBuilder
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DiameterCommand {

	/**
	 * The name of the Diameter request or answer to refers to when checking
	 * the generated message
	 */
	String name();
	
	/**
	 * The abbreviation of the Diameter request or answer to refers to when checking
	 * the generated message 
	 */
	String abbreviation() default "";
	
	/**
	 * The vendor ID of the Diameter request or answer to refers to when checking
	 * the generated message 
	 */
	long vendorId() default -1;
}
