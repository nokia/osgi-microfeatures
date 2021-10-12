// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package $package;

import java.util.ArrayList; 
import java.util.List; 
import java.util.ServiceLoader; 
import java.util.concurrent.TimeUnit; 

import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass; 
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean; 
import org.springframework.context.annotation.Bean; 
import org.springframework.context.annotation.Configuration; 
import org.springframework.core.io.Resource; 
import org.springframework.core.io.support.PathMatchingResourcePatternResolver; 
import com.nokia.as.osgi.launcher.OsgiLauncher; 

@Configuration 
@ConditionalOnClass(OsgiLauncher.class) 
public class Starter {
    private String[] getBundles(String locationPattern) throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] resources = resolver.getResources(locationPattern);
        List<String> result = new ArrayList<>();
        for(Resource res : resources) {
            result.add(res.getURL().toString());
        }
        return result.stream().toArray(String[]::new);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public OsgiLauncher osgiLauncher() throws Exception {
        ServiceLoader <OsgiLauncher> servLoad = ServiceLoader.load(OsgiLauncher.class);
	    OsgiLauncher framework = servLoad.iterator().next();
	    framework.useExceptionHandler(Throwable::printStackTrace)
                 .withBundles(getBundles("classpath*:**/CASR-INF/bundles/*.jar")).start();
        return framework;
    }
}
