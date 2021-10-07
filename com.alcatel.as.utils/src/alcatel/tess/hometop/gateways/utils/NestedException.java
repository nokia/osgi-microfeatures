package alcatel.tess.hometop.gateways.utils;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * This exception may be used in a gateway implementation.
 * It manage exception chaining (as in jdk 1.4)
 * 
 * <p>
 * Here are some examples of usage:
 * <blockquote>
 * <pre>
 *
 * String url = "http://foo.bar/com.html"
 * Date date = new Date ();
 *
 * try {
 *	Http http = new Http ();
 *	HttpResponse rsp = http.get (url);
 * }
 *
 * catch (IOException e) {
 *	throw new NestedException ("failed to get url: " + url", e);
 * }
 *
 * </pre>
 * </blockquote>
 *
 */
public class NestedException extends Exception {
  
  public NestedException() {
    super();
  }
  
  public NestedException(Throwable rootCause) {
    super(rootCause.getMessage());
    this.rootCause = rootCause;
  }
  
  public NestedException(String debugMessage) {
    super(debugMessage);
  }
  
  public NestedException(String debugMessage, Throwable rootCause) {
    super(debugMessage);
    this.rootCause = rootCause;
  }
  
  public Throwable getRootCause() {
    return (rootCause);
  }
  
  public void printStackTrace() {
    printStackTrace(System.err);
  }
  
  public void printStackTrace(PrintStream out) {
    synchronized (out) {
      super.printStackTrace(out);
      if (rootCause != null) {
        out.println();
        out.println("Caused by:");
        out.println();
        rootCause.printStackTrace(out);
      }
    }
  }
  
  public void printStackTrace(PrintWriter out) {
    synchronized (out) {
      super.printStackTrace(out);
      
      if (rootCause != null) {
        out.println();
        out.println("Caused by:");
        out.println();
        rootCause.printStackTrace(out);
      }
    }
  }
  
  public static void main(String args[]) {
    try {
      try {
        if (true) {
          throw new IOException("could not connect to web server");
        }
      } catch (IOException e) {
        throw new NestedException("could not retrieve wml file", e);
      }
    } catch (NestedException e) {
      e.printStackTrace();
    }
  }
  
  protected Throwable rootCause;
}
