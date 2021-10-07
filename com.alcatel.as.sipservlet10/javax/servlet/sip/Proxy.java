/**
 * 
 */
package javax.servlet.sip;

import java.util.List;

import javax.servlet.sip.ProxyBranch;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;

public interface Proxy {
    public SipServletRequest getOriginalRequest();

    public void proxyTo(URI uri);

    public void proxyTo(List list);

    public void cancel();

    public boolean getRecurse();

    public void setRecurse(boolean flag);

    public boolean getRecordRoute();

    public void setRecordRoute(boolean flag);

    public boolean getParallel();

    public void setParallel(boolean flag);

    public boolean getStateful();

    /**
     * @deprecated since SipServlet1.1
     */
    public void setStateful(boolean flag);

    public boolean getSupervised();

    public void setSupervised(boolean flag);
    
    /**
     * @deprecated
     */
    public int getSequentialSearchTimeout();
    /**
     * @deprecated
     */
    public void setSequentialSearchTimeout(int i);

    public SipURI getRecordRouteURI();

    // JSR289
    void setOutboundInterface(SipURI uri);

    ProxyBranch getProxyBranch(URI uri);

    List<ProxyBranch> getProxyBranches();

    void startProxy();

    SipURI getPathURI() throws IllegalStateException;

    void setAddToPath(boolean p);

    boolean getAddToPath();
    void setProxyTimeout(int timeout);
    int getProxyTimeout();

    List<ProxyBranch> createProxyBranches(List<? extends URI> targets)
            throws IllegalArgumentException;
}
