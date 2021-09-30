package javax.servlet.sip;

public interface SipSessionsUtil {
    SipApplicationSession getApplicationSessionById(java.lang.String applicationSessionId);
    SipApplicationSession getApplicationSessionKey(java.lang.String key,boolean create);
    SipSession getCorrespondingSipSession(SipSession session, java.lang.String headerName);
}
