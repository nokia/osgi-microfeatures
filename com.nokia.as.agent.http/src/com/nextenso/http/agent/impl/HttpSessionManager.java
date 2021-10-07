package com.nextenso.http.agent.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alcatel.as.service.concurrent.PlatformExecutor;
import com.nextenso.http.agent.Utils;
import com.nextenso.proxylet.http.HttpCookie;
import com.nextenso.proxylet.http.HttpHeaders;
import com.nextenso.proxylet.http.HttpRequest;

public class HttpSessionManager implements HttpSessionFacade.SessionManager {
	
	private static volatile HttpSessionManager _instance = new HttpSessionManager();
	private final Map<String, CachedSession> _sessions = new HashMap<>();
	private volatile long _timeoutMS = 1200;
	private volatile Utils _utils;
	private final static Logger _log = Logger.getLogger("agent.http.session.manager");

	private class CachedSession {
		ScheduledFuture<?> timer;
		final HttpSessionFacade session;

		CachedSession(ScheduledFuture<?> timer, HttpSessionFacade session) {
			this.timer = timer;
			this.session = session;
		}
	}
	public static HttpSessionManager instance() {
		return _instance;
	}

	public void setTimeout(long timeoutSec) {
		_timeoutMS = timeoutSec * 1000;
	}
	
	public void setUtils(Utils utils) {
		_utils = utils;
	}
		
	public  HttpSessionFacade getSession(boolean create, HttpMessageFacade message) {
		if (message instanceof HttpRequestFacade) {
			return getRequestSession(create, (HttpRequestFacade) message);
		} else if (message instanceof HttpResponseFacade) {
			return getResponseSession(create, (HttpResponseFacade) message);
		} else {
			return null;
		}
	}

	public synchronized void destroySessions() {
		for (CachedSession cache : _sessions.values()) {
			cache.timer.cancel(false);
		}
		_sessions.clear();
	}
	
	public synchronized int getSessions() {
		return _sessions.size();
	}
	
	public synchronized void dumpSessions(StringBuilder sb) {
		for (String session : _sessions.keySet()) {
			sb.append(session).append(",");
		}
		sb.setLength(sb.length()-1);
	}
	
	private HttpSessionFacade getRequestSession(boolean create, HttpRequestFacade request) {
		HttpSessionFacade session = null;

		switch (_utils.getAgent().getSessionPolicy().getPolicy()) {
		case NONE:
			break;

		case COOKIE:
			String name = _utils.getAgent().getSessionPolicy().getName();
			String sessionId = getCookieSessionId(request.getHeaders());
			if (sessionId == null) {
				sessionId = (String) request.getProlog().getURL().getParameterValue(name);
			}
			if (sessionId != null) {
				session = getSession(create, sessionId, request);
			}
			break;
			
		case CLIENT_IP:
			session = getSessionFromClientIp(create, request);
			break;
			
		case HTTP_HEADER:
			session = getSessionFromHeader(create, request);
			break;
		}
		
		if (session == null && create) {
			// create a session without any session id (it will die when response is returned)
			session = createSession(request, null);
		}
		return session;
	}
	
	/**
	 * See if a session can be found from an incoming response, re-arm its timer and return it.
	 */
	private HttpSessionFacade getResponseSession(boolean create, HttpResponseFacade response) {
		HttpSessionFacade session = null;
		
		switch (_utils.getAgent().getSessionPolicy().getPolicy()) {
		case NONE:
			break;
		
		case COOKIE:
			session = ((HttpRequestFacade) response.getRequest()).getSession(false);
			String responseSessionId = getCookieSessionId(response.getHeaders());

			if (session != null) {
				if (responseSessionId != null) {
					if (session.getSessionId() == null) {
						session = registerSession(responseSessionId, session);	
					} else {
						if (! session.getSessionId().equals(responseSessionId)) {
							removeSession(session.getSessionId());
							session = getSession(create, responseSessionId, response.getRequest());
						}
					}
				}
			} else {
				if (responseSessionId != null) {
					session = getSession(create, responseSessionId, response.getRequest());
				} else {
					String requestSessionId = getCookieSessionId(response.getRequest().getHeaders());
					if (requestSessionId != null) {
						session = getSession(create, requestSessionId, response.getRequest());
					}
				}
			}
			break;
			
		case CLIENT_IP:
			session = getSessionFromClientIp(create, response.getRequest());
			break;
			
		case HTTP_HEADER:
			session = getSessionFromHeader(create, response.getRequest());
			break;
		}
		
		if (session == null && create) {
			// create a session without any session id (it will die when response is returned)
			session = createSession(response.getRequest(), null);
		}

		return session;
	}
		
	private HttpSessionFacade getSessionFromHeader(boolean create, HttpRequest request) {
		String sessionId = request.getHeaders().getHeader(_utils.getAgent().getSessionPolicy().getName());
		return sessionId != null ? getSession(create, sessionId, request) : null;
	}

	private HttpSessionFacade getSessionFromClientIp(boolean create, HttpRequest request) {
		String sessionId = request.getRemoteAddr();
		return sessionId != null ? getSession(create, sessionId, request) : null;
	}

	private HttpSessionFacade createSession(HttpRequest request, String sessionId) {
		HttpSessionFacade session = new HttpSessionFacade(this, request.getProlog().isSecure(), _utils);
		String headerName = _utils.getClidHeaderName();
		String remoteId = (headerName != null) ? request.getHeaders().getHeader(headerName) : null;
		session.setRemoteId(remoteId);
		session.setSessionId(sessionId);
	    _utils.getContainer().init(session, true);
		return session;
	}
	
	private String getCookieSessionId(HttpHeaders headers) {
		String cookieName = _utils.getAgent().getSessionPolicy().getName();
		HttpCookie cookie = headers.getCookie(cookieName);
		if (cookie != null) {
			return cookie.getValue();
		}
		return null;
	}

	private HttpSessionFacade getSession(boolean create, String sessionId, HttpRequest request) {
		PlatformExecutor tpool = _utils.getPlatformExecutors().getProcessingThreadPoolExecutor();
		CachedSession cached = null;
		synchronized (this) {
			cached = _sessions.get(sessionId);
			if (cached == null) {
				if (create) {
					HttpSessionFacade session = createSession(request, sessionId);
					cached = new CachedSession(_utils.getTimerService().schedule(tpool, () -> removeSession(sessionId),
							_timeoutMS, TimeUnit.MILLISECONDS), session);
					_sessions.put(sessionId, cached);
				}
			} else {
				cached.timer.cancel(false);
				cached.timer = _utils.getTimerService().schedule(tpool, () -> removeSession(sessionId), _timeoutMS, TimeUnit.MILLISECONDS);
			}
		}

		return cached == null ? null : cached.session;
	}
	
	private HttpSessionFacade registerSession(String sessionId, HttpSessionFacade session) {
		PlatformExecutor tpool = _utils.getPlatformExecutors().getProcessingThreadPoolExecutor();
		CachedSession cached = null;
		synchronized (this) {
			cached = _sessions.get(sessionId);
			if (cached == null) {
				session.setSessionId(sessionId);
				cached = new CachedSession(
						_utils.getTimerService().schedule(tpool, () -> removeSession(sessionId), _timeoutMS, TimeUnit.MILLISECONDS),
						session);
				_sessions.put(sessionId, cached);
			} else {
				cached.timer.cancel(false);
				cached.timer = _utils.getTimerService().schedule(tpool, () -> removeSession(sessionId), _timeoutMS, TimeUnit.MILLISECONDS);
			}
		}

		return cached.session;
	}

	private boolean removeSession(String sessionId) {
		_log.debug("session timeout: " + sessionId);
		CachedSession removed = null;
		synchronized (this) {
			removed = _sessions.remove(sessionId);
			if (removed != null) {
				removed.timer.cancel(false);
				_log.debug("session removed: " + sessionId);
			}
		}
		return removed != null;
	}

	// ------------------- HttpSessionFacade.SessionManager interface

	@Override
	public void invalidateSession(HttpSessionFacade session) {
		String sessionId = session.getSessionId();
		if (sessionId != null) {
			removeSession(sessionId);
		}
	}

	@Override
	public int getMaxInactiveInterval() {
	    if (_timeoutMS <= 0) {
	        return -1;
	      } else {
	        return (int) (_timeoutMS / 1000L);
	      }
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		_timeoutMS = interval * 1000;
	}

	@Override
	public void complete() {
		// TODO Auto-generated method stub
	}

	@Override
	public String newSession() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String changeSessionId() {
		// TODO Auto-generated method stub
		return null;
	}

}
