package com.alcatel.as.session.distributed.mock;

import java.io.Serializable;
import java.util.Dictionary;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.Reference;

import com.alcatel.as.session.distributed.Session;
import com.alcatel.as.session.distributed.SessionException;
import com.alcatel.as.session.distributed.SessionManager;
import com.alcatel.as.session.distributed.SessionTask;
import com.alcatel.as.session.distributed.SessionTask.Callback;
import com.alcatel.as.session.distributed.SessionType;
import com.alcatel.as.session.distributed.Transaction;
import com.alcatel.as.session.distributed.TransactionListener;
import com.alcatel.as.session.distributed.event.GroupListener;
import com.alcatel.as.session.distributed.event.SessionActivationListener;
import com.alcatel.as.session.distributed.event.SessionEventFilter;
import com.alcatel.as.session.distributed.event.SessionListener;
import com.alcatel.as.session.distributed.smartkey.SmartKeyService;
import com.alcatel.as.util.config.ConfigConstants;
import com.alcatel.as.util.config.ConfigHelper;

@SuppressWarnings("deprecation")
@Component(service=SessionManager.class)
public class SessionManagerMock extends SessionManager {
  
  private final SessionType TYPE = new SessionTypeMock();
  private long instanceId;
  private SmartKeyService smartKeyService;
  
  @Reference(target="(service.pid=system)", policy=ReferencePolicy.DYNAMIC, unbind="unsetSystemConfig")
  protected void setSystemConfig(final Dictionary<String, String> config) {
    this.instanceId = ConfigHelper.getLong(config, ConfigConstants.INSTANCE_ID, 1L);
  }

  protected void unsetSystemConfig(final Dictionary<String, String> config) {
  }

  @Reference
  protected void setSmartKeyService(SmartKeyService service) {
    this.smartKeyService = service;
  }
  
  @Override
  public void execute(Transaction tx, TransactionListener listener) { }

  @Override
  public <T extends Serializable> T execute(Transaction cmd) throws SessionException { return null; }

  @Override
  public int addSessionTypeListener(SessionType type, SessionListener listener, SessionEventFilter filter) throws SessionException { return 0; }

  @Override
  public void removeSessionTypeListener(SessionType type, SessionListener listener) { }

  @Override
  public void removeSessionListener(int id) { }
  
  @Override
  public SessionType getSessionType(String type) { return TYPE; }

  @Override
  public int addActivationListener(SessionType type, SessionActivationListener listener) throws SessionException { return 0; }

  @Override
  public void removeActivationListener(int id) { }

  @Override
  public void addSessionListener(SessionType type, String sessionId, SessionListener listener, SessionEventFilter filter, AddSessionListenerCallback callback) { }

  @Override
  public int addSessionListener(SessionType type, String sessionId, SessionListener listener, SessionEventFilter filter) throws SessionException { return 0; }

  @Override
  public SessionType addSessionType(Dictionary<String, String> properties) throws IllegalArgumentException { return TYPE; }

  @Override
  public SessionType addSessionType(Dictionary<String, String> properties, SessionListener listener,
                                    SessionEventFilter filter) throws IllegalArgumentException { return TYPE; }

  @Override
  public SessionType addSessionType(Dictionary<String, String> properties, SessionListener listener,
                                    SessionEventFilter filter, GroupListener groupListener) throws IllegalArgumentException { return TYPE; }

  @Override
  public String getLocalGroupName() { return null; }

  @Override
  public String[] getIdsFromSmartKey(String value) {
    return smartKeyService.getIdsFromSmartKey(value);
  }
  
  @Override
  public Session createSession(int isolationLevel, SessionType type, String sessionId, int duration, boolean get) throws SessionException { return null; }

  @Override
  public void createSession(int isolationLevel, SessionType type, String sessionId, int duration, boolean get, SessionTask task, Callback callback) { }

  @Override
  public Session getSession(int isolationLevel, SessionType type, String sessionId) throws SessionException { return null; }

  @Override
  public void getSession(int isolationLevel, SessionType type, String sessionId, SessionTask task, Callback callback) { }

  @Override
  public List<Session> getSessions(SessionType type) throws SessionException { return null; }

  @Override
  public int getSessionsSize(SessionType type) throws SessionException { return 0; }

  @Override
  public int getDurationLeft(SessionType type, String sessionId) { return 0; }

  @Override
  public String version() { return "MOCK"; }

  class SessionTypeMock implements SessionType {

    @Override
    public String getType() { return "DUMMY"; }

    @Override
    public String getSessionKey(String userPart) { return createSmartKey(userPart); }

    @Override
    public String getSessionKey() { return createSmartKey(); }

    @Override
    public String createSmartKey(String userPart)  { return smartKeyService.createSmartKey(userPart, instanceId); }
    @Override
    public String createSmartKey() { return smartKeyService.createSmartKey(instanceId); }

    @Override
    public String getUserPart(String key) throws SessionException { return smartKeyService.getUserPart(key); }

    @Override
    public boolean isSmartKey(String key) { return smartKeyService.isSmartKey(key); }

    @Override
    public long[] getAgentIdsFromSmartKey(String key) throws IllegalArgumentException { return smartKeyService.getAgentIds(key); }

    @Override
    public SessionManager getSessionManager() { return null; }

    @Override
    public int getInstanceSessions() { return 0; }

    @Override
    public int getInstanceAliases() { return 0; }

    @Override
    public List<String> getSessionIds() { return null; }
    
  }
}
