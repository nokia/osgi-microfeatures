package alcatel.tess.hometop.gateways.utils;

/**
  Exception thrown when attempting to access a non allocated timer.
*/
public class NxUnallocatedTimerException extends Exception {
  /**
    Constructor without message.
  */
  public NxUnallocatedTimerException() {
    super();
  }
  
  /**
    Constructor with user message.
  */
  public NxUnallocatedTimerException(String message) {
    super(message);
  }
}
