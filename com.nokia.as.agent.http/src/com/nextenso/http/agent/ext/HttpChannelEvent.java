package com.nextenso.http.agent.ext;

import com.nextenso.proxylet.ProxyletContext;
import com.nextenso.proxylet.event.ProxyletContextEvent;

/**
 * A ProxyletContextEvent encapsulates a ProxyletContext-wide event.
 * <p/>It may be overridden to fire specific events to selected listeners.
 */
@SuppressWarnings("serial")
public class HttpChannelEvent extends ProxyletContextEvent {
  public enum Type {
    CLIENT_SOCKET_OPEN, CLIENT_SOCKET_CLOSE, SERVER_SOCKET_CLOSE
  };
  
  private Type _type;
  private long _channelId;
  
  public HttpChannelEvent(Object source, ProxyletContext context, Type type, long channelId) {
    super(source, context);
    _type = type;
    _channelId = channelId;
  }
  
  public Type getType() {
    return _type;
  }
  
  public long getChannelId() {
    return _channelId;
  }
  
  public String toString() {
    return "HttpChannelEvent[type=" + _type + ", channelId=" + (_channelId & 0xFFFFFFFF) + "/"
        + (_channelId >> 32) + "]";
  }
}
