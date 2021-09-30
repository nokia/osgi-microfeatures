package alcatel.tess.hometop.gateways.http;

import static java.lang.System.out;
import alcatel.tess.hometop.gateways.utils.Hashtable;

public class Test {
  
  public static void main(String args[]) throws Exception {
    String authenticate = "Digest realm=\"alcatel.fr\", nonce=\"c363beefaf1828f7d372adc8f1ca7cea\", opaque=\"c6fc5105b3470a09b0bfc5e020a6ad0f\", algorithm=MD5, qop=\"auth\"";
    
    Hashtable authParams = HttpUtils.extractAuthParams(authenticate);
    HttpAuthenticator authenticator = HttpAuthenticator.createInstance((String) authParams.get("scheme"));
    out.println("parse: " + authenticator.handleAuthenticate(authParams));
    out.println("parse: " + authenticator.handleAuthenticate(authParams));
    
    String user = "gerard_private@jaguar.net";
    String password = "toto";
    String realm = "alcatel.fr";
    String method = "MESSAGE";
    String uri = "sip:LongNotifyUAS@139.54.130.93:5060";
    out.println(authenticator.authorize(user, password, realm, method, uri));
  }
}
