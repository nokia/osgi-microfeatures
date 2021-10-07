package com.alcatel.as.util.state;

/**
 * All concrete states must extend this class and provide the concrete state context as template
 * parameter.
 */
@SuppressWarnings("unchecked")
public abstract class State<SM extends StateMachine, EVENT extends Enum> {
  /**
   * Postpone an event to another state. This constant may be returned by the handleEvent method
   * for postponing the handling of a given event, until we enter into another state. Indeed,
   * some states don't actually want to handle themselves a given event, and need to delay the
   * actual handling of the event until we enter into another state. The next time we enter into
   * another state, then, the next state will be invoked in it's enter method, and in it's
   * handleEvent method.
   */
  public final static State POSTPONE_EVENT = null;

  /**
   * Enter into this state.
   * 
   * @param sm the state machine associated with this state.
   * @throws TransitionException if this state can't be actually entered because an unexpected
   *           exception occured.
   */
  public void enter(SM sm) throws TransitionException {
  }

  /**
   * Exit from this state.
   * 
   * @param sm the state machine associated with this state.
   * @throws TransitionException if this state can't be properly deactivated because an
   *           unexpected exception occured.
   */
  public void exit(SM sm) throws TransitionException {
  }

  /**
   * Handle an event from that state.
   * 
   * @param sm the state machine associated with this state.
   * @param event the event to handle.
   * @param args optional arguments associated to this event.
   * @return the next state to be entered. if the current state must remain the current active
   *         state, then this method must return "this". If the event can't be processed, this
   *         method may return the {@link #POSTPONE_EVENT} constant, and the event will be
   *         postponed until we enter into another state.
   */
  public abstract State handleEvent(SM sm, EVENT event, Object... args);
}
