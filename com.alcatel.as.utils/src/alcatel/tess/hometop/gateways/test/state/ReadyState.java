// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.test.state;

import alcatel.tess.hometop.gateways.utils.StateMachine;

/** In the READY state, we handle the "action" method */
public class ReadyState implements MyState {
  final static MyState instance = new ReadyState();
  
  // State methods ...
  public void enter(StateMachine m) {
  } // ALWAYS called when entering into this state.
  
  public void exit(StateMachine m) {
  } // ALWAYS called when leaving this state.
  
  // MyState methods ...
  public void action(MyStateMachine m, String msg) {
    m.log(msg);
  }
  
  public void suspend(MyStateMachine m) {
    m.changeState(SuspendedState.instance);
  }
  
  public void resume(MyStateMachine m) {
  }
};
