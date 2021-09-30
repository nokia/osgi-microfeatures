package com.nokia.hello.dm;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;

import com.nokia.database.dm.IDatabase;

@Component
public class Example {

	@ServiceDependency
	volatile IDatabase database;
	
    @Start
    void start() {
        System.out.println("Example.start: "+database.get());
    }
    
    @Stop
    void stop() {
        System.out.println("Example.stop");
    }
}
