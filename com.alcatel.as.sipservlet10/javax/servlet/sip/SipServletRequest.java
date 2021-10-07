package javax.servlet.sip;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipApplicationRoutingDirective;
import javax.servlet.sip.SipURI;

public interface SipServletRequest extends ServletRequest, SipServletMessage {

    public abstract URI getRequestURI();

    public abstract void setRequestURI(URI uri);

    public abstract void pushRoute(SipURI sipuri);

    public abstract int getMaxForwards();

    public abstract void setMaxForwards(int i);

    public abstract void send() throws IOException;

    public abstract boolean isInitial();

    public abstract ServletInputStream getInputStream() throws IOException;

    public abstract BufferedReader getReader() throws IOException;

    public abstract Proxy getProxy() throws TooManyHopsException;

    public abstract Proxy getProxy(boolean flag) throws TooManyHopsException;

    public abstract SipServletResponse createResponse(int i);

    public abstract SipServletResponse createResponse(int i, String s);

    public abstract SipServletRequest createCancel()
            throws IllegalStateException;

    /**
     * New interfaces of the JSR289
     * 
     */
        void setRoutingDirective(SipApplicationRoutingDirective directive,
                javax.servlet.sip.SipServletRequest origRequest)
                throws IllegalStateException;

        Address getPoppedRoute();
        Address getInitialPoppedRoute();
        SipApplicationRoutingRegion getRegion();
        SipURI getSubscriber();
        B2buaHelper getB2buaHelper() throws IllegalStateException;
}
