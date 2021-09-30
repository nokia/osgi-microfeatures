/**
 * 
 */
package javax.servlet.sip;

import java.io.Serializable;
import java.util.List;

/**
 * @author christophe
 *
 */
public interface SipApplicationRouter {
    SipApplicationRouterInfo getNextApplication(SipServletRequest initialRequest,
            SipApplicationRoutingRegion region,
            SipApplicationRoutingDirective directive,
            Serializable stateInfo);
    void init(List<String> deployedApplicationNames);
    void destroy();
    void init();
    void applicationDeployed(List<String> newlyDeployedApplicationNames);
    void applicationUndeployed(List<String> undeployedApplicationNames);
}
