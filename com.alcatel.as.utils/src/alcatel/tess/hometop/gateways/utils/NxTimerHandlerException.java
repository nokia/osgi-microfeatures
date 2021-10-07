package alcatel.tess.hometop.gateways.utils;

/**
  Exception thrown when attempting to start a timer without
  providing a handler
*/
public class NxTimerHandlerException extends Exception {
  /**
    Constructor without message.
  */
  public NxTimerHandlerException() {
    super();
  }
  
  /**
    Constructor with user message.

    @param message Exception message
  */
  public NxTimerHandlerException(String message) {
    super(message);
  }
}
