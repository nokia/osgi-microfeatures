package alcatel.tess.hometop.gateways.test.state;

import static java.lang.System.out;

/**
 * This sample shows how to use the StateMachine tool.
 * Our simple MyState class has an "action(String name)" method. In READY state, the action
 * method just logs the "name" parameter. In SUSPENDED state, the method invocation is postponed
 * to the next time the READY state is entered.
 */
public class Main {
  public static void main(String args[]) throws Exception {
    MyStateMachine m = new MyStateMachine();
    MyState s = m.getState();
    
    out.println("CALLING action ...");
    s.action(m, "action"); // this action is performed right away because we are in the Ready state.
    
    out.println("CALLING suspend ...");
    s.suspend(m); // we transition to the Suspended State.
    
    out.println("CALLING action ...");
    s.action(m, "postponed action"); // this action is postponed until we go back to the Ready State.
    
    out.println("CALLING resume ...");
    s.resume(m); // will enter into the Ready state and invoke the postponed action.
  }
}
