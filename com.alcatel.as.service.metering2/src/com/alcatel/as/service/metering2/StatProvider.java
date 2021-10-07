package com.alcatel.as.service.metering2;

/**
 * Any component may register this service in the OSGi registry in order to provide a given Monitorable whose meters have to be
 * mapped to Snmp and exposed to the webadmin counters. The service has to be annotated with the MBD stats annotations, and can possibly provide
 * a ConfigConstants.MODULE_NAME service property that will be used to display the components meters in the webadmin.
 * If the property is not provided, then the name of the Monitorable returned by the provider will be used.
 *  
 * <h3>Usage Example</h3>
 * 
 * <blockquote>
 * <pre>
 * &#64Stat(rootSnmpName = "alcatel.srd.a5350.MyClass", rootOid = { 637, 71, 6, 3050 })
 * &#64;Component(properties=ConfigConstants.MODULE_NAME + "=" + "MyIOH")
 * public class MyStats implements StatProvider
 * {
 *     private Monitorable mon;
 *     
 *     &#64;Reference(target="(monitorable.name=asr.service.myIOH)")
 *     void bindMonitorable(Monitorable mon) { 
 *         this.mon = mon;
 *     }
 *     
 *     // Map the "msg.received" meter being part of the Monitorable exposed by this provider.
 *     &#64;Counter(snmpName="MySnmpShortName", oid=101, desc="MyCounter Description")
 *     public final static String msgReceived = "msg.received";
 *     
 *     public Monitorable getMonitorable() {
 *         return mon;
 *     }
 * }
 * </pre>
 * </blockquote>
 */
public interface StatProvider {
    /**
     * Returns a monitorable that has to be mapped to Snmp.
     */
    Monitorable getMonitorable();
}
