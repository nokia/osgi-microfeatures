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
/**
 * 
 * An annotation of a class that can be bound to a Flattable object in a dynamic way (typically registering into a map field of another flattable object)
 * 
 */
@Target( {ElementType.TYPE })
@Retention(RUNTIME)
public @interface Flattable {
    Class<? extends com.alcatel_lucent.ha.services.Flattable> type() default com.alcatel_lucent.ha.services.Flattable.class;

}
