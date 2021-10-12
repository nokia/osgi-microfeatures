// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.alcatel.as.util.state;

/**
 * Exception which can be thrown when a state can't be entered or leaved.
 */
@SuppressWarnings({"serial", "unchecked"})
public class TransitionException extends Exception {
  private State _nextState;

  /**
   * Creates a new Transition exception.
   * @param nextState the next state to enter
   */
  public TransitionException(State nextState) {
    _nextState = nextState;
  }

  /**
   * Creates a new Transition exception.
   * @param msg a message description the exception
   * @param nextState the next state to enter
   */
  public TransitionException(String msg, State nextState) {
    this(msg, null, nextState);
  }

  /**
   * Creates a new Transition exception.
   * @param msg a message description the exception
   * @param t the root cause of this exception
   * @param nextState the next state to enter
   */
  public TransitionException(String msg, Throwable t, State nextState) {
    super(msg, t);
    _nextState = nextState;
  }

  /**
   * Returns the next state to enter
   * @return the next state to enter
   */
  State getNextState() {
    return _nextState;
  }
}
