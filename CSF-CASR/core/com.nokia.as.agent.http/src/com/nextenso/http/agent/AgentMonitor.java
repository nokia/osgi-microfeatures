package com.nextenso.http.agent;

import java.util.Hashtable;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.alcatel.as.service.metering2.ValueSupplier;
import com.alcatel.as.service.reporter.api.CommandScopes;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel_lucent.as.management.annotation.command.Command;
import com.alcatel_lucent.as.management.annotation.command.Commands;
import com.alcatel_lucent.as.management.annotation.stat.Counter;
import com.alcatel_lucent.as.management.annotation.stat.Gauge;
import com.alcatel_lucent.as.management.annotation.stat.Stat;
import com.nextenso.mux.MuxHandler;

/**
 * This component is used to report counters and handle commands.
 */
@Stat(rootSnmpName = "alcatel.srd.a5350.HttpAgent", rootOid = { 637, 71, 6, 1010 })
@Commands(rootSnmpName = "alcatel.srd.a5350.HttpAgent", rootOid = { 637, 71, 6, 1010 })
@Component
public class AgentMonitor {
  private CopyOnWriteArrayList<Agent> _agents = new CopyOnWriteArrayList<Agent>();
  private static final int APP_HTTP_AGENT = 276;
  private static final String MODULE_HTTP_AGENT = "HttpAgent";
  
  @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policy = ReferencePolicy.DYNAMIC, target="(protocol=Http)")
  public void addAgent(MuxHandler mh) {
	Agent a = (Agent) mh;
    if (_agents.isEmpty()) {
      a.getAgentMeters().addValueSuppliedMeters(new ValueSupplier() {       
        @Override
        public long getValue() {
          return getActiveClients();
        }
      }, 
      new ValueSupplier() {      
        @Override
        public long getValue() {
          return getActiveChannels();
        }
      });
    }
    _agents.add((Agent) a);
  }

  public void removeAgent(MuxHandler mh) {
  }

  @SuppressWarnings("serial")
  @Activate
  public void start(BundleContext ctx) {
    ctx.registerService(Object.class.getName(), this, new Hashtable<String, Object>() {
      {
        put(ConfigConstants.MODULE_ID, APP_HTTP_AGENT);
        put(ConfigConstants.MODULE_NAME, MODULE_HTTP_AGENT);
        put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COUNTER_SCOPE);
      }
    });
    ctx.registerService(Object.class.getName(), this, new Hashtable<String, Object>() {
      {
        put(ConfigConstants.MODULE_ID, APP_HTTP_AGENT);
        put(ConfigConstants.MODULE_NAME, MODULE_HTTP_AGENT);
        put(CommandScopes.COMMAND_SCOPE, CommandScopes.APP_COMMAND_SCOPE);
      }
    });
  }

  //order of methods is important!
  @Counter(index = 0, snmpName = "NumProcessedReq", oid = 100, desc = "Processed requests")
  public int getProcessedRequests() {
    return (int) Utils.getMonitorable().getProcessedRequests();
  }
  
  @Gauge(index = 1, snmpName = "NumClients", oid = 101, desc = "Active clients")
  public int getActiveClients() {
    int size = 0;
    for (Agent agent : _agents) {
      size += agent.getActiveClients();
    }
    return size;
  }
  
  @Gauge(index = 2, snmpName = "NumSockets", oid = 102, desc = "Active sockets")
  public int getActiveChannels() {
    int size = 0;
    for (Agent agent : _agents) {
      size += agent.getActiveChannels();
    }
    return size;
  }
  
  @Counter(index = 3, snmpName = "NumAbortedReq", oid = 103, desc = "Aborted requests")
  public int getAbortedRequests() {
    return (int) Utils.getMonitorable().getAbortedRequests();
  }
  
  @Gauge(index = 4, snmpName = "NumPendingReq", oid = 104, desc = "Pending requests")
  public int getPendingRequests() {
    return (int) Utils.getMonitorable().getPendingRequests();
  } 
  
  @Command(code = 0, desc = "Kill all Sessions")
  public void killAllUsers() {
    for (Agent agent : _agents) {
      agent.killAllUsers(false);
    }
  }
  
  @Command(code = 1, desc = "List all Sessions")
  public void listAllUsers() {
    for (Agent agent : _agents) {
      agent.listAllUsers();
    }
  }
}
