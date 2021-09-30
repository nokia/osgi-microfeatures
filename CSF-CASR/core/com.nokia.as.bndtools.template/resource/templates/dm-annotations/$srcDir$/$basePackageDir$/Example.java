package $basePackageName$;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Start;

@Component
public class Example {

    @Start
    void start() {
        System.out.println("Example.start");
    }

}
