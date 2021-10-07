package com.nokia.as.thirdparty.keycloak.admin.client;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Stop;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RealmsResource;
import org.keycloak.admin.client.resource.ServerInfoResource;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.apache.log4j.Logger;

@SuppressWarnings("unchecked")
@Component(provides = KeycloakAdminClient.class, factoryPid = "keycloak.admin.client", propagate = true)
public class KeycloakAdminClient {
	private Keycloak keycloak;
	private String authUrl;
	private String realm;
	private String clientId;
        private final static Logger _log = Logger.getLogger(KeycloakAdminClient.class);

	void updated(KeycoakAdminClientConfiguration properties) {
		authUrl = properties.authUrl();
		realm = properties.realm();
		clientId = properties.clientId();
		execute(() -> {
			keycloak = Keycloak.getInstance(authUrl, realm, properties.username(), properties.password(), clientId,
					properties.getClientSecret());
		});
	}

	public String getClientId() {
		return clientId;
	}

	@Stop
	public void close() {
		execute(() -> {
			if (keycloak != null)
				keycloak.close();
		});
	}

	public void execute(Runnable r) {
		ClassLoader currentThread = Thread.currentThread().getContextClassLoader();
		try {
		    Thread.currentThread().setContextClassLoader(ResteasyClient.class.getClassLoader());
		    r.run();
		}
		finally {
			Thread.currentThread().setContextClassLoader(currentThread);
		}
	}

	public Object execute(Callable<Object> c) throws Exception {
		ClassLoader currentThread = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(ResteasyClient.class.getClassLoader());
			return c.call();
		} finally {
			Thread.currentThread().setContextClassLoader(currentThread);
		}
	}

	public Keycloak getKeycloak() {
		return keycloak;
	}

	public RealmResource getRealm() {
		return keycloak.realm(realm);
	}

	public RealmsResource getRealms() {
		return keycloak.realms();
	}

	public boolean isClosed() {
		return keycloak.isClosed();
	}

	public <T> T proxy(Class<T> proxyClass, URI absoluteURI) {
		return keycloak.proxy(proxyClass, absoluteURI);
	}

	public ServerInfoResource serverInfo() {
		return keycloak.serverInfo();
	}

	public TokenManager tokenManager() {
		return keycloak.tokenManager();
	}

	public void createRole(RoleRepresentation roleRepresentation) {
		execute(() -> keycloak.realm(realm).roles().create(roleRepresentation));
	}

	public List<ClientRepresentation> findAllClients() throws Exception {
		return (List<ClientRepresentation>) execute(() -> keycloak.realm(realm).clients().findAll());
	}

	public List<ClientRepresentation> findAllClients(boolean viewableOnly) throws Exception {
		return (List<ClientRepresentation>) execute(() -> keycloak.realm(realm).clients().findAll(viewableOnly));
	}

	public List<ClientRepresentation> findByClientId(String clientId) throws Exception {
		return (List<ClientRepresentation>) execute(() -> keycloak.realm(realm).clients().findByClientId(clientId));
	}

	public Set<UserRepresentation> getRoleUserMembers(String roleName) throws Exception {
		return (Set<UserRepresentation>) execute(
				() -> keycloak.realm(realm).roles().get(roleName).getRoleUserMembers());
	}

	public Response createUser(UserRepresentation userRepresentation) throws Exception {
		return (Response) execute(() -> keycloak.realm(realm).users().create(userRepresentation));
	}

	public List<UserRepresentation> listUsers() throws Exception {
		return (List<UserRepresentation>) execute(() -> keycloak.realm(realm).users().list());
	}

	public void assignRole(String roleName, String userId) {
		execute(() -> {
			RealmResource realmResource = keycloak.realm(realm);
			realmResource.users().get(userId).roles().realmLevel()
					.add(Arrays.asList(realmResource.roles().get(roleName).toRepresentation()));
		});
	}

	public void createRealm(RealmRepresentation realmRepresentation) {
		execute(() -> keycloak.realms().create(realmRepresentation));
	}

	public void createClient(ClientRepresentation clientRepresentation) throws Exception {
		execute(() -> keycloak.realm(realm).clients().create(clientRepresentation));
	}
}
