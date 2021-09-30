
package javax.servlet.sip.annotation;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Inherited
public @interface SipApplicationKey {
}