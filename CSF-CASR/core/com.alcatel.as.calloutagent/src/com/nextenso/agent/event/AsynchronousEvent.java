package com.nextenso.agent.event;

import alcatel.tess.hometop.gateways.tracer.Tracer;
import alcatel.tess.hometop.gateways.tracer.TracerManager;

public class AsynchronousEvent {
  
  private AsynchronousEventListener _listener = null;
  private Object _data = null;
  private int _type = -1;
  private Tracer _tracer;
  
  public AsynchronousEvent(AsynchronousEventListener listener, Object data, int type) {
    _listener = listener;
    _data = data;
    _type = type;
  }
  
  public AsynchronousEventListener getListener() {
    return _listener;
  }
  
  public Object getData() {
    return _data;
  }
  
  public int getType() {
    return _type;
  }
  
  public void execute() {
    _listener.asynchronousEvent(_data, _type);
  }
  
  protected void setTracer(Tracer newTracer) {
    _tracer = newTracer;
  }
}
