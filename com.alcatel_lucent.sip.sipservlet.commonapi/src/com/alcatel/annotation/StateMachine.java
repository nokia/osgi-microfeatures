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
public @interface StateMachine{
    enum SCOPE { SESSION,SCOPE,OTHER};
    int initState() default 0;
    Class<?> stateclass() default StateMachine.class;
    Class<?> eventclass() default StateMachine.class;
    SCOPE scope() default SCOPE.SESSION;
}
