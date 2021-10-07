/**
 * 
 */
package com.alcatel.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.ElementType;

@Target({ElementType.FIELD})
@Retention(RUNTIME)
public @interface Property {
    String name();
    boolean dynamic() default false;
    boolean callback() default false;
    boolean readonly() default false;
}
