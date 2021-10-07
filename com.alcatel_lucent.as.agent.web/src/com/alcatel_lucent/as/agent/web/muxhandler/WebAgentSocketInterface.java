package com.alcatel_lucent.as.agent.web.muxhandler;

import com.alcatel_lucent.as.agent.web.container.Container;


public interface WebAgentSocketInterface {
  
  public void received(byte[] data, int off, int len);
  
  public void closed(Container container);

}
