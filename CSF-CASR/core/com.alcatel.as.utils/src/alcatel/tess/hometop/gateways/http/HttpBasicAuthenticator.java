package alcatel.tess.hometop.gateways.http;

import alcatel.tess.hometop.gateways.utils.Coder;
import alcatel.tess.hometop.gateways.utils.Hashtable;

/**
 * The HTTP authenticator for Basic scheme.
 */
public class HttpBasicAuthenticator extends HttpAuthenticator {
  public HttpBasicAuthenticator() {
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.http.HttpAuthenticator#handleAuthenticate(alcatel.tess.hometop.gateways.utils.Hashtable)
   */
  public boolean handleAuthenticate(Hashtable authParams) {
    // Check if the authenticate header value is really Basic.
    String scheme = (String) authParams.get("scheme");
    if (scheme == null) {
      throw new IllegalArgumentException("No scheme found in Authenticate header");
    }
    if (!scheme.equalsIgnoreCase("Basic")) {
      throw new IllegalArgumentException("Authenticate scheme is not Digest");
    }
    return true;
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.http.HttpAuthenticator#authorize(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public String authorize(String user, String password, String method, String uri) {
    return authorize(user, password, null, method, uri);
  }
  
  /**
   * @see alcatel.tess.hometop.gateways.http.HttpAuthenticator#authorize(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
   */
  public String authorize(String user, String password, String realm, String method, String uri) {
    StringBuilder credentials = new StringBuilder();
    credentials.append(user);
    credentials.append(':');
    credentials.append(password);
    StringBuilder authorize = new StringBuilder();
    authorize.append("Basic ");
    authorize.append(Coder.uuencode(credentials.toString()));
    return authorize.toString();
  }
}
