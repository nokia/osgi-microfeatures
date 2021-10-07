package alcatel.tess.hometop.gateways.test.state;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import alcatel.tess.hometop.gateways.utils.StateMachine;

class MyStateMachine extends StateMachine<MyState> {
  final static Logger logger = Logger.getLogger("MyStateMachine");
  
  static {
    logger.setLevel(Level.DEBUG);
  }
  
  public MyStateMachine() {
    super(ErrorState.instance, logger);
    super.changeState(ReadyState.instance);
  }
  
  public String toString() {
    return "MyStateMachine";
  }
  
  /************************ Stateless methods called by our states. **************************/
  
  void log(String s) {
    System.out.println(s);
  }
}
