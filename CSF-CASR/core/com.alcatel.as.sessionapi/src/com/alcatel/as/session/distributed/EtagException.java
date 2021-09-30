package com.alcatel.as.session.distributed;

/**
 * During a transaction, 
 * this exception is thrown when the session etag does not match the expected etag.
 *
 */
@SuppressWarnings("serial")
public class EtagException extends SessionException
{

  /**
   * A Constructor.
   * @param debugMessage an informative message
   */
  public EtagException(String debugMessage)
  {
    super(debugMessage);
  }

  /**
   * A Constructor.
   * @param rootCause the underlying exception
   */
  public EtagException(Throwable rootCause) {
    super(rootCause);
  }

  /**
   * A Constructor.
   * @param debugMessage an informative message
   * @param rootCause the underlying exception
   */
  public EtagException(String debugMessage, Throwable rootCause) {
    super(debugMessage, rootCause);
  }


}
