package javax.servlet.sip;

import java.util.List;

/**
 * @author christophe
 *
 */
public interface ProxyBranch {

    void setProxyBranchTimeout(int i);
    void cancel() throws IllegalStateException;
    int getProxyBranchTimeout();
    SipURI getRecordRouteURI();
    void setOutboundInterface(SipURI uri);
    SipServletResponse getResponse();
    SipServletRequest getRequest();
    boolean isStarted();
    List<ProxyBranch> getRecursedProxyBranches();
    Proxy getProxy();
}
