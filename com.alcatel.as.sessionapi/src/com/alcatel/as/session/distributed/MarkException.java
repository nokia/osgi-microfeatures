package com.alcatel.as.session.distributed;

/**
 * @internal
 * During a transaction, 
 * this exception is thrown when the session mark does not match the expected transaction flag.
 * <p>See:
 * <ul>
 * <li>{@link SessionData#mark()}</li>
 * <li>{@link SessionData#unmark()}</li>
 * <li>{@link Transaction#TX_IF_MARKED}</li>
 * <li>{@link Transaction#TX_IF_NOT_MARKED}</li>
 * </ul>
 */
@SuppressWarnings("serial")
public class MarkException extends SessionException
{

  /**
   * A Constructor.
   * @param debugMessage an informative message
   */
  public MarkException(String debugMessage)
  {
    super(debugMessage);
  }

  /**
   * A Constructor.
   * @param rootCause the underlying exception
   */
  public MarkException(Throwable rootCause) {
    super(rootCause);
  }

  /**
   * A Constructor.
   * @param debugMessage an informative message
   * @param rootCause the underlying exception
   */
  public MarkException(String debugMessage, Throwable rootCause) {
    super(debugMessage, rootCause);
  }


}
