package alcatel.tess.hometop.gateways.test.state;

import alcatel.tess.hometop.gateways.utils.State;

/**
 * A Stateful class which behaviour depends on various states.
 * in the READY state: the action method just log the action name (given in argument).
 * in the SUSPENDED state: the action invocation is postponed to the next time we'll enter into the READY state.
 */
public interface MyState extends State {
  /**
   * in READY state, this method just log the action parameter.
   * in SUSPENDED state, this method invocation is POSTPONED to the next time we'll enter into
   * the READY state.
   * @return false if the current state could not handle this method invocation (the method will be
   * postponed when we transition to another state).
   */
  public void action(MyStateMachine tm, String action);
  
  /**
   * go into the SUSPENDED state.
   */
  public void suspend(MyStateMachine tm);
  
  /**
   * go into the READY state
   */
  public void resume(MyStateMachine tm);
};
