package com.nokia.as.autoconfig.command;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;
import org.osgi.service.cm.ConfigurationAdmin;

import com.nokia.as.autoconfig.Configuration;
import com.nokia.as.autoconfig.Utils;

@Component(provides = Object.class)
@Property(name = CommandProcessor.COMMAND_SCOPE, value = "asr.autoconfig")
@Property(name = CommandProcessor.COMMAND_FUNCTION, value = {"pids", "config"})
public class AutoConfiguratorProcessor {
    
    @ServiceDependency(required = true)
    ConfigurationAdmin configAdmin;
    
    @Descriptor("List the pids registered in the configuration admin")
    public void pids() {
        Configuration config = Utils.getConfigFromConfigAdmin(configAdmin, Throwable::printStackTrace);    
        System.out.println("Registered pids:");
        config.config.keySet().forEach(s -> System.out.println("    " + s));
        System.out.println("Registered factory pids:");
        config.factoryConfig.keySet().forEach(s -> System.out.println("    " + s));
    }
    
    @Descriptor("List the existing properties for a pid")
    public void config(@Descriptor("The pid to query")
                       @Parameter(names = { "-pid", "-p" }, absentValue = "")
                       String pid) {
        
        Configuration config = Utils.getConfigFromConfigAdmin(configAdmin, Throwable::printStackTrace);
        System.out.println("Registered values:");
        
        config.config.entrySet().stream()
              .filter(e -> pid.isEmpty() || e.getKey().contains(pid))
              .forEach(e -> {
                  System.out.println("    pid = " + e.getKey());
                  System.out.println("    properties = ");
                  e.getValue().entrySet()
                              .forEach(entry -> {
                                          System.out.print("        ");
                                          System.out.println(entry.getKey() + " = " + entry.getValue());
                  });
        });
        
        config.factoryConfig.entrySet().stream()
              .filter(c -> pid.isEmpty() || c.getKey().equals(pid))
              .forEach(e -> {
                  System.out.println("    factoryPid = " + e.getKey());
                  System.out.println("    properties = [");
                  e.getValue().forEach(m -> {
                      m.entrySet()
                       .forEach(entry -> {
                                  System.out.print("        ");
                                  System.out.println(entry.getKey() + " = " + entry.getValue());
                       });
                      System.out.println("        -----");
                  });
                  System.out.println("    ]");
        });
    }
}
