package alcatel.tess.hometop.gateways.utils;

public interface State {
  void enter(StateMachine sm);
  
  void exit(StateMachine sm);
}
