package alcatel.tess.hometop.gateways.test.state;

import alcatel.tess.hometop.gateways.utils.StateMachine;

/** In the READY state, we handle the "action" method */
public class SuspendedState implements MyState {
  final static MyState instance = new SuspendedState();
  
  // State methods ...
  public void enter(StateMachine m) {
  } // ALWAYS called when entering into this state
  
  public void exit(StateMachine m) {
  } // ALWAYS called when leaving this state
  
  // MyState methods ...
  public void action(MyStateMachine m, String msg) {
    m.postponeEvent();
  }
  
  public void suspend(MyStateMachine m) {
  }
  
  public void resume(MyStateMachine m) {
    m.changeState(ReadyState.instance);
  }
};
