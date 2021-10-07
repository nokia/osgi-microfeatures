package javax.servlet.sip.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.ElementType;

@Target({ElementType.TYPE})
@Retention(RUNTIME)
public @interface SipServlet {
    String name();
    String applicationName();
    int loadOnStartup() default -1;
}
