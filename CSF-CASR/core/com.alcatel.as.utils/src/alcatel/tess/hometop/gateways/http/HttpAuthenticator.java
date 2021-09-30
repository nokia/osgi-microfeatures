package alcatel.tess.hometop.gateways.http;

import alcatel.tess.hometop.gateways.utils.Hashtable;
import alcatel.tess.hometop.gateways.utils.StringCaseHashtable;

/**
 * The Http Authenticator.
 */
public abstract class HttpAuthenticator {
  private final static StringCaseHashtable _authenticators = new StringCaseHashtable();
  
  static {
    _authenticators.putObject("Basic", HttpBasicAuthenticator.class);
    _authenticators.putObject("Digest", HttpDigestAuthenticator.class);
  }
  
  /**
   * Reads needed parameters from Authenticate header.
   * This method must be called before calling the authorize method.
   * 
   * @param authParams The Authenticate header params.
   * @return true if the credentials need to be computed again, false if the username/password is not correct.
   * @see RFC 2617
   */
  public abstract boolean handleAuthenticate(Hashtable authParams);
  
  /**
   * Builds the authorization string.
   * 
   * @param user The username.
   * @param password The password.
   * @param realm The realm
   * @param method The method.
   * @param uri The uri.
   * @return The authorization string.
   */
  public abstract String authorize(String user, String password, String realm, String method, String uri);
  
  /**
   * Builds the authorization string. It uses the realm found in the authParms when the last call to handleAuthenticate method.
   * 
   * @param user The username.
   * @param password The password.
   * @param method The method.
   * @param uri The uri.
   * @return The authorization string.
   */
  public abstract String authorize(String user, String password, String method, String uri);
  
  /**
   * Creates a new instance according to the scheme ("basic" or "digest").
   * 
   * @param scheme The scheme ("basic" or "digest").
   * @return A new instance.
   */
  public final static HttpAuthenticator createInstance(String scheme) {
    Class c = (Class) _authenticators.getObject(scheme);
    if (c == null) {
      throw new IllegalArgumentException("scheme not supported: " + scheme);
    }
    
    try {
      return (HttpAuthenticator) c.newInstance();
    } catch (Throwable t) {
      throw new RuntimeException("could not create authenticator for scheme " + scheme);
    }
  }
}
