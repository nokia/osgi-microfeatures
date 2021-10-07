/**
 * 
 */
package com.alcatel.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.ElementType;

@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RUNTIME)
public @interface ActionStateMachine{
    String from();
    String to();
    String event();
}
