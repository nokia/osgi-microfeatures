package alcatel.tess.hometop.gateways.test.state;

import alcatel.tess.hometop.gateways.utils.StateMachine;

/** In the READY state, we handle the "action" method */
public class ErrorState implements MyState {
  final static MyState instance = new ErrorState();
  
  // State methods ...
  public void enter(StateMachine m) {
  } // ALWAYS called when entering into this state
  
  public void exit(StateMachine m) {
  } // ALWAYS called when leaving this state
  
  // MyState methods ...
  public void action(MyStateMachine m, String msg) {
    m.log("action failed: " + msg);
  }
  
  public void suspend(MyStateMachine m) {
    m.log("can not suspend");
  }
  
  public void resume(MyStateMachine m) {
    m.log("can not resume");
  }
};
