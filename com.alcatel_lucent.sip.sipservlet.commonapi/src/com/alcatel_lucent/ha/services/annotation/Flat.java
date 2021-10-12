// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package com.alcatel_lucent.ha.services.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;

import com.alcatel_lucent.ha.services.Flattable;

/**
 * 
 * An annotation of a field that can be flatten ,i.e set into a map with the key 
 * 
 */

@Target( { ElementType.FIELD})
@Retention(RUNTIME)
public @interface Flat {
    /**
     * a short name than uses as key for the field in a map
     */
    String key();
    /**
     * an indicator if the value of the field has to be stringified before putting in a map
     */
    boolean stringable() default false;
    /**
     * 
     * the reference type of the field when a Flattable field has to be recreated from a map
     * this is a reference of a class extending a Flattable that can be substitute by a concrete class later
     */
    Class<? extends Flattable> type() default Flattable.class;
    /**
     * 
     * the reference type of the collection on a multi-valued field when a Collection of Flattable field has be recreated from a map
     */
    @SuppressWarnings("rawtypes")
	Class<? extends Collection> col() default ArrayList.class;
    /**
     * 
     * the name of the field in which a Flattable field has to aggregated (relationship 1-n, or 1-1)
     */
    String aggField() default "";
}
