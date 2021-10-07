package $basePackageName$;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.lambda.DependencyManagerActivator;
import org.osgi.framework.BundleContext;

public class Activator extends DependencyManagerActivator {

    @Override
    public void init(BundleContext ctx, DependencyManager dm) throws Exception {    	
        component(comp -> comp.impl(Example.class));
    }

}
