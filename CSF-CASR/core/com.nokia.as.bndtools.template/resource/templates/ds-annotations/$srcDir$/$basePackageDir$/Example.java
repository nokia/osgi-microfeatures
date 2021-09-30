package $basePackageName$;

import org.osgi.service.component.annotations.*;

@Component
public class Example {

    @Activate
    void start() {
        System.out.println("Example.start");
    }

}
