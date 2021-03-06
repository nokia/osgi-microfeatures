// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package javax.servlet.sip.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.ElementType;

@Target({ElementType.PACKAGE})
@Retention(RUNTIME)
public @interface SipApplication {
      String name() default "";
      String displayName() default "";
      String smallIcon() default "";
      String largeIcon() default "";
      String description() default "";
      boolean distributable() default false;
      int proxyTimeout();
      int sessionTimeout();
      String mainServlet() default "";
}
