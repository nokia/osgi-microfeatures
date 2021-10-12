// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package alcatel.tess.hometop.gateways.http;

import alcatel.tess.hometop.gateways.utils.Hashtable;

/**
 * The HTTP authenticator for Digest scheme.
 */
public class HttpDigestAuthenticator extends HttpAuthenticator {
  
  private String _realm = null;
  
  public HttpDigestAuthenticator() {
  }
  
  /**
   * Return true if current password is correct and can be replayed, false if not.
   */
  public boolean handleAuthenticate(Hashtable authParams) {
    boolean STALE = (_nonce == null);
    
    // Store authenticate parameters.
    if ((_nonce = (String) authParams.get("nonce")) == null) {
      throw new IllegalArgumentException("no nonce parameter found in Authenticate header");
    }
    
    _opaque = (String) authParams.get("opaque");
    _algorithm = (String) authParams.get("algorithm");
    if (_algorithm != null
        && !("MD5".equalsIgnoreCase(_algorithm) || "MD5-sess".equalsIgnoreCase(_algorithm))) {
      throw new IllegalArgumentException("Invalid algorithm in HTTP Digest authencation: " + _algorithm);
    }
    _realm = (String) authParams.get("realm");
    
    String qop = (String) authParams.get("qop");
    if (qop != null) {
      String auth = HttpUtils.getParam(qop, "auth", ",", true);
      if (auth == null) {
        throw new RuntimeException("Unsupported qop in HTTP Digest authentication: " + qop);
      }
      _qop = auth;
    } else {
      _qop = null;
    }
    
    // If the authenticate header value contains "stale=FALSE": it means that the user/password is wrong.
    
    String stale = (String) authParams.get("stale");
    if ("TRUE".equalsIgnoreCase(stale)) {
      return true;
    }
    
    return (STALE);
  }
  
  public String authorize(String user, String password, String method, String uri) {
    return authorize(user, password, _realm, method, uri);
  }
  
  public String authorize(String user, String password, String realm, String method, String uri) {
    if (_nonce == null) {
      StringBuilder sb = new StringBuilder();
      sb.append("Digest ");
      sb.append("username=\"").append(user).append("\"");
      sb.append(", realm=\"").append(realm).append("\"");
      sb.append(", uri=\"").append(uri).append("\"");
      sb.append(", nonce=\"\"");
      sb.append(", response=\"\"");
      return sb.toString();
    }
    String response = DigestUtils.getDigestResponse(user, password, realm, method, uri, _algorithm, _qop,
                                                    _nonce, _nonce, _nc);
    return "Digest " + createDigestHeader(user, realm, uri, response);
  }
  
  private String createDigestHeader(String user, String realm, String uri, String response) {
    StringBuilder sb = new StringBuilder();
    sb.append("username=\"").append(user).append("\", ");
    sb.append("realm=\"").append(realm).append("\", ");
    sb.append("nonce=\"").append(_nonce).append("\", ");
    sb.append("uri=\"").append(uri).append("\", ");
    sb.append("response=\"").append(response).append("\"");
    if (_qop != null) {
      sb.append(", qop=").append(_qop).append(", ");
      sb.append("nc=").append(DigestUtils.formatNC(_nc)).append(", ");
      _nc++;
      String cnonce = _nonce;
      sb.append("cnonce=\"").append(cnonce).append("\"");
    }
    if (_algorithm != null) {
      sb.append(", algorithm=").append(_algorithm);
    }
    if (_opaque != null) {
      sb.append(", opaque=\"").append(_opaque).append("\"");
    }
    return sb.toString();
  }
  
  private String _nonce;
  private int _nc = 1;
  private String _qop;
  private String _algorithm;
  private String _opaque;
}
