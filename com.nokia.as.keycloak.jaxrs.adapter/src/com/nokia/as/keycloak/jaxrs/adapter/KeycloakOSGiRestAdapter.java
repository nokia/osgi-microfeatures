package com.nokia.as.keycloak.jaxrs.adapter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.dm.annotation.api.AdapterService;
import org.apache.log4j.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.BasicAuthRequestAuthenticator;
import org.keycloak.adapters.BearerTokenRequestAuthenticator;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.RefreshableKeycloakSecurityContext;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.jaxrs.JaxrsBearerTokenFilterImpl;
import org.keycloak.jaxrs.JaxrsHttpFacade;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

/**
 * Keyclock adapter component. This component is created for each bundle having
 * a file-keycloak.json property
 */
@AdapterService(provides = KeycloakOSGiRestAdapter.class, adapteeService = Object.class, adapteeFilter = "(keycloak.json=*)", added = "setService")
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class KeycloakOSGiRestAdapter extends JaxrsBearerTokenFilterImpl {
	private static final String COM_NOKIA_KEYCLOAK_OFF = "com.nokia.keycloak.off";
	private final static Logger _log = Logger.getLogger("com.nokia.as.keycloak");
	private volatile Bundle _bundle;
	private WebXml _webXml; // defines keycloak managed paths globally
	private String _keycloakJson;

	@Inject
	Application app;

	@Context
	private HttpServletRequest servletRequest;

	public Map<String, Object> setService(Object service, Map<String, Object> properties) throws Exception {
		_bundle = FrameworkUtil.getBundle(service.getClass());
		setWebXml(service, properties);
		_keycloakJson = (String) properties.get("keycloak.json");
		if (_keycloakJson.isEmpty()) {
			if (_log.isInfoEnabled())
				_log.info(this + " disabled, keycloak.json file is not provided");
			return null;
		} else if (_log.isDebugEnabled())
			_log.debug("Created new KeycloakOSGiRestAdapter with properties = " + properties);

		setKeycloakConfigFile("instance/"); // starts the filter
		return deployOnProcessor((String) properties.get("http.port"));
	}

	void setWebXml(WebXml webXml) {
		_webXml = webXml;
	}

	void setWebXml(Object service, Map<String, Object> properties) throws Exception {
		try {
			// try reading conf from service properties first
			String webXmlContents = (String) properties.get("web.xml");
			if (webXmlContents == null || webXmlContents.isEmpty()) {
				// fallback to reading from bundle
				URL resource = _bundle.getResource("WEB-INF/web.xml");
				if (resource != null) {
					File tempWebXmlFile = File.createTempFile(resource.getFile(), ".tmp");
					InputStream initialStream = resource.openStream();
					byte[] buffer = new byte[initialStream.available()];
					initialStream.read(buffer);
					OutputStream outStream = new FileOutputStream(tempWebXmlFile);
					outStream.write(buffer);
					outStream.close();
					if (tempWebXmlFile != null) {
						if (_log.isDebugEnabled())
							_log.debug("Reading WEB-INF/web.xml for service [" + service + "] from bundle ["
									+ _bundle.getSymbolicName() + "]");
						_webXml = new WebXmlParser().parse(tempWebXmlFile);
						tempWebXmlFile.delete();
					}
				}
			} else {
				if (_log.isDebugEnabled())
					_log.debug("Reading instance/web.xml from runtime for service [" + service + "]");
				_webXml = new WebXmlParser().parse(webXmlContents);
			}
		} catch (SAXException | IOException | ParserConfigurationException e) {
			_log.error(e.getMessage());
			throw e;
		}
	}

	private HashMap<String, Object> deployOnProcessor(String httpPort) {
		HashMap<String, Object> serviceProperties = new HashMap<>();
		if (httpPort != null) {
			serviceProperties.put("http.port", httpPort);
		}
		return serviceProperties;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void filter(ContainerRequestContext request) throws IOException {
		ConcurrentHashMap<String, Boolean> routes = (ConcurrentHashMap<String, Boolean>) app.getProperties()
																							.get("sless.routes");
		boolean hasRoutes = routes != null;
		Boolean securedByRouteConstraints = false;
		if (hasRoutes) {
			Boolean routeFound = routes.get(request.getUriInfo().getPath());
			if (routeFound != null)
				securedByRouteConstraints = routeFound;
			if (_log.isDebugEnabled())
				_log.debug(this + " using route constraints, filter current path = " + securedByRouteConstraints);
		}
		
		if (request.getProperty(COM_NOKIA_KEYCLOAK_OFF) != null || (hasRoutes && !securedByRouteConstraints))
			return;

		SecurityContext securityContext = getRequestSecurityContext(request);
		JaxrsHttpFacade facade = new JaxrsHttpFacade(request, securityContext);
		boolean noToken = noToken(request, facade);
		if (noToken && !uriRequiresAuth(request) || handlePreauth(facade)) {
			return;
		}
		KeycloakDeployment resolvedDeployment = deploymentContext.resolveDeployment(facade);

		nodesRegistrationManagement.tryRegister(resolvedDeployment);
		bearerAuthentication(facade, request, resolvedDeployment);
	}
	
    protected void bearerAuthentication(JaxrsHttpFacade facade, ContainerRequestContext request, KeycloakDeployment resolvedDeployment) {
        BearerTokenRequestAuthenticator authenticator = new BearerTokenRequestAuthenticator(resolvedDeployment);
        AuthOutcome outcome = ((BearerTokenRequestAuthenticator)authenticator).authenticate(facade);
        
        if (outcome != AuthOutcome.AUTHENTICATED && resolvedDeployment.isEnableBasicAuth()) {
            authenticator = new BasicAuthRequestAuthenticator(resolvedDeployment);
            outcome = authenticator.authenticate(facade);
        }

        if (outcome == AuthOutcome.FAILED || outcome == AuthOutcome.NOT_ATTEMPTED) {
            AuthChallenge challenge = authenticator.getChallenge();
            _log.debug("Authentication outcome: " + outcome);
            boolean challengeSent = challenge.challenge(facade);
            if (!challengeSent) {
                // Use some default status code
                facade.getResponse().setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
            }

            // Send response now (if not already sent)
            if (!facade.isResponseFinished()) {
                facade.getResponse().end();
            }
            return;
        } else {
            if (verifySslFailed(facade, resolvedDeployment)) {
                return;
            }
        }

        propagateSecurityContext(facade, request, resolvedDeployment, authenticator);
        handleAuthActions(facade, resolvedDeployment);
    }
	
	protected boolean verifySslFailed(JaxrsHttpFacade facade, KeycloakDeployment deployment) {
		String remoteAddr;
		if (servletRequest == null) {
			remoteAddr = facade.getRequest().getRemoteAddr();
		} else {
			remoteAddr = servletRequest.getRemoteAddr();
		}
		if (_log.isDebugEnabled()) {
			_log.debug("verifySslFailed? - remoteAddr " + remoteAddr);
		}

		if ("255.255.255.255".equals(remoteAddr)) {
			remoteAddr = "127.0.0.1";
		}
		if (!facade.getRequest().isSecure() && deployment.getSslRequired().isRequired(remoteAddr)) {
			_log.warn("SSL is required to authenticate, but request is not secured");
			facade.getResponse().sendError(403, "SSL required!");
			return true;
		}
		return false;
	}

	public boolean noToken(ContainerRequestContext request, JaxrsHttpFacade facade) {
		List<String> authHeaders = facade.getRequest().getHeaders("Authorization");

		String tokenString = null;
		if (authHeaders == null)
			return true;

		for (String authHeader : authHeaders) {
			String[] split = authHeader.trim().split("\\s+");
			if (split == null || split.length != 2)
				continue;
			if (!split[0].equalsIgnoreCase("Bearer") && !split[0].equalsIgnoreCase("Basic"))
				continue;
			tokenString = split[1];
		}
		return tokenString == null;
	}

	boolean uriRequiresAuth(ContainerRequestContext request) {
		if (_webXml == null)
			return false; // path restrictions may be handled by jax-rs

		URI requestUri = request.getUriInfo().getRequestUri();
		String method = request.getMethod();
		final String cleanPath = addTrailingSlash(requestUri.getPath());
		boolean pathFound = false;
		for (SecurityConstraint constraint : _webXml.getSecurityConstraints()) {
			if (constraint.getAuthRoles() == null || constraint.getAuthRoles().size() == 0) {
				continue; // ignore security-constraint without auth-constraint
			}
			_log.debug("method = " + method);
			if (constraint.getWebResourceCollection().stream().anyMatch(collection -> {
				List<String> methods = collection.getMethods();
				boolean methodsEmpty = methods.isEmpty();
				boolean methodMatch = methods.stream().anyMatch(m -> m.equalsIgnoreCase(method));
				_log.debug("methodsEmpty = " + methodsEmpty);
				_log.debug("methodMatch = " + methodMatch);
				if (!(methodsEmpty || methodMatch)) {
					return false;
				}
				return isUriWebXmlSecured(cleanPath, method, collection);
			})) {
				pathFound = true;
				break;
			}
		}
		if (_log.isDebugEnabled()) {
			_log.debug("RequestURI.path: " + requestUri + "; is found in web.xml = " + pathFound);
		}
		return pathFound;
	}

	private boolean isUriWebXmlSecured(final String requestUriPath, final String method,
			WebResourceCollection collection) {
		String callPath = addTrailingSlash(requestUriPath);
		return collection.getPatterns().stream().map(p -> p.replace("*", "")).allMatch(isUriWebXmlSecured(callPath));
	}

	public static Predicate<String> isUriWebXmlSecured(String callPath) {
		return pattern -> callPath.startsWith(pattern)
				|| (pattern.startsWith("!") && !callPath.startsWith(pattern.substring(1, pattern.length())));
	}

	private String addTrailingSlash(final String path) {
		return path.endsWith("/") ? path : path + "/";
	}

	@Override
	protected void propagateSecurityContext(JaxrsHttpFacade facade, ContainerRequestContext request,
			KeycloakDeployment resolvedDeployment, BearerTokenRequestAuthenticator bearer) {
		RefreshableKeycloakSecurityContext skSession = new RefreshableKeycloakSecurityContext(resolvedDeployment, null,
				bearer.getTokenString(), bearer.getToken(), null, null, null);

		facade.setSecurityContext(skSession);
		String principalName = AdapterUtils.getPrincipalName(resolvedDeployment, bearer.getToken());
		final KeycloakPrincipal<RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<RefreshableKeycloakSecurityContext>(
				principalName, skSession);
		SecurityContext anonymousSecurityContext = getRequestSecurityContext(request);
		final boolean isSecure = anonymousSecurityContext.isSecure();
		final Set<String> roles = AdapterUtils.getRolesFromSecurityContext(skSession);
		SecurityContext ctx = new SecurityContext() {
			@Override
			public Principal getUserPrincipal() {
				return principal;
			}

			@Override
			public boolean isUserInRole(String role) {
				return role.equals("*") ? true : roles.contains(role);
			}

			@Override
			public boolean isSecure() {
				return isSecure;
			}

			@Override
			public String getAuthenticationScheme() {
				return "OAUTH_BEARER";
			}
		};
		request.setSecurityContext(ctx);

		if (!allowAccesByRole(request, roles) && uriRequiresAuth(request))
			facade.getResponse().sendError(403, "Error 403 !role : deployment descriptor doesn't match any realm role");
	}

	protected boolean allowAccesByRole(ContainerRequestContext request, final Set<String> rolesFromToken) {
		boolean allowAcces = true; // allow everyone by default
		if (_webXml == null || rolesFromToken.isEmpty())
			return allowAcces; // role restrictions may be handled by jax-rs

		final String cleanPath = addTrailingSlash(request.getUriInfo().getRequestUri().getPath());

		for (SecurityConstraint c : _webXml.getSecurityConstraints()) {
			// pattern match
			boolean patternFound = false;
			_log.debug("cleanPath = " + cleanPath);
			for (WebResourceCollection collection : c.getWebResourceCollection()) {
				_log.debug("collection.getPatterns() = " + collection.getPatterns());
				patternFound = collection	.getPatterns()
											.stream()
											.anyMatch(pattern -> cleanPath.startsWith(pattern.replace("*", "")));
				if (patternFound)
					break;
			}

			// method match
			String method = request.getMethod();
			boolean methodFound = false;
			for (WebResourceCollection collection : c.getWebResourceCollection()) {
				_log.debug("collection.getMethods() = " + collection.getMethods());
				methodFound = collection.getMethods().stream().anyMatch(m -> m.equalsIgnoreCase(method));
				if (methodFound)
					break;
			}

			// role match
			_log.debug("rolesFromToken = " + rolesFromToken);
			_log.debug("patternFound = " + patternFound);
			Set<String> authRoles = c.getAuthRoles();
			boolean roleFound;
			if (authRoles == null) {
				roleFound = methodFound;
			} else if (authRoles.contains("**")) {
				roleFound = true;
			} else {
				roleFound = authRoles.stream().anyMatch(r -> rolesFromToken.contains(r));
			}
			_log.debug("authRoles = " + authRoles);
			_log.debug("methodFound = " + methodFound);
			_log.debug("roleFound = " + roleFound);
			allowAcces = patternFound && roleFound;
			if (allowAcces)
				break;
		}
		return allowAcces;

	}

	@Override
	protected InputStream loadKeycloakConfigFile() {
		return new ByteArrayInputStream(_keycloakJson.getBytes(StandardCharsets.UTF_8));
	}
}
