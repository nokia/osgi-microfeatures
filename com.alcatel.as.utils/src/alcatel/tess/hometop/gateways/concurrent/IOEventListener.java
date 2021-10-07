package alcatel.tess.hometop.gateways.concurrent;

import java.io.InputStream;

/**
 * Interface of input stream listeners.
 */
public interface IOEventListener {
  /**
   * Indicates if the listened stream is still active or not.
   *
   * @return true if the listened stream is alive, false if not.
   */
  public boolean done();
  
  /**
   * Return the listened input stream. The return stream must
   * correcly implements its available method (this is not the
   * case for all jdk input stream, like <code>ObjectInputStream</code>
   *
   * @return the listened input stream.
   */
  public InputStream input();
  
  /**
   * Process data available in the listened input stream.
   *
   * @param size the current size of data that is available in the
   *	listened input stream.
   */
  public void processIO(int size);
}
