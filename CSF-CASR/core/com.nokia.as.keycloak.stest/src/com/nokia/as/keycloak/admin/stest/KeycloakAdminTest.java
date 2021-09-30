package com.nokia.as.keycloak.admin.stest;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.alcatel.as.service.log.LogService;
import com.alcatel.as.service.log.LogServiceFactory;
import com.nokia.as.thirdparty.keycloak.admin.client.KeycloakAdminClient;
import com.nokia.as.util.junit4osgi.OsgiJunitRunner;

@Component(provides = Object.class)
@Property(name = OsgiJunitRunner.JUNIT, value = "true")
@RunWith(OsgiJunitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KeycloakAdminTest {
	@ServiceDependency
	private LogServiceFactory logFactory;

	private LogService _log;

	@ServiceDependency(filter = "(realm=master)")
	private volatile KeycloakAdminClient kcadmin;

	@Start
	void start() {
		_log = logFactory.getLogger(KeycloakAdminTest.class);
	}

	@Test(timeout = 60000)
	public void test() throws Exception {
		test1_listClients();
		test2_createRole();
		test3_findUserByRole();
		test4_createUser();
		test5_listUsers();
		test6_createRealm();
		test7_createClient();
	}
	
	void test1_listClients() throws Exception {
		try {
			_log.warn("test1_listClients ...");
			List<ClientRepresentation> clients = kcadmin.findAllClients();
			_log.warn("clients=" + clients.size());
			for (ClientRepresentation client : clients) {
				_log.warn("client: " + client.getClientId());
			}
			
			assertThat(clients).extracting(ClientRepresentation::getClientId)
					.contains("broker", "master-realm", "admin-cli", "account", "security-admin-console");
			_log.warn("test1_listClients done");
		} catch (Exception e) {
			_log.warn("error", e);
			throw e;
		}
	}

	void test2_createRole() throws Exception{
		try {
			_log.warn("test2_createRole ...");
			String roleName = "user";
			kcadmin.createRole(new RoleRepresentation(roleName, "description", false));
			kcadmin.execute(() -> assertThat(kcadmin.getRealm().roles().list()).extracting(RoleRepresentation::getName)
					.contains(roleName));
		} catch (Exception e) {
			_log.warn("error", e);
			throw e;
		}

	}

	void test3_findUserByRole() throws Exception {
		try {
			_log.warn("test3_findUserByRole ...");
			assertThat(kcadmin.getRoleUserMembers("admin").stream()).hasSize(1)
					.extracting(UserRepresentation::getUsername).contains("admin");
		} catch (Exception e) {
			_log.warn("error", e);
			throw e;
		}
	}

	void test4_createUser() throws Exception {
		// create user
		_log.warn("test4_createUser ...");
		try {
			UserRepresentation user = new UserRepresentation();
			user.setEnabled(true);
			String username = "newuser";
			user.setUsername(username);
			user.setFirstName("First");
			user.setLastName("Last");
			user.setEmail(username + "@gmail.com");
			user.setRealmRoles(Arrays.asList("user"));

			// set password
			CredentialRepresentation credential = new CredentialRepresentation();
			credential.setType(CredentialRepresentation.PASSWORD);
			credential.setValue("test");
			user.setCredentials(Arrays.asList(credential));

			Response createUserResponse = kcadmin.createUser(user);

			// find user id
			if (createUserResponse == null) {
				_log.warn("createUserResponse is null");
			} else if (createUserResponse.getLocation() == null) {
				_log.warn("createUserResponse.getLocation() is null");
			} else if (createUserResponse.getLocation().getPath() == null) {
				_log.warn("createUserResponse.getLocation().getPath() is null");
			}

			_log.warn("createUserResponse=" + createUserResponse);
			_log.warn("status=" + createUserResponse.getStatus());
			_log.warn("headers=" + createUserResponse.getHeaders());
			String entity = createUserResponse.readEntity(String.class);
			_log.warn("entity=" + entity);

			String userId = createUserResponse.getLocation().getPath().replaceAll(".*/" + "([^/]+)$", "$1");

			// assign role
			kcadmin.assignRole("user", userId);

			System.out.println("Result: getRoleUserMembers=" + kcadmin.getRoleUserMembers("user"));

			assertThat(kcadmin.getRoleUserMembers("user").stream()).hasSize(1)
					.extracting(UserRepresentation::getUsername).contains(username);
		} catch (Exception e) {
			_log.warn("error", e);
			throw e;
		}
	}

	void test5_listUsers() throws Exception {
		try {
			_log.warn("test5_listUsers ...");
			assertThat(kcadmin.listUsers()).hasSize(2).extracting(UserRepresentation::getUsername).contains("admin",
					"newuser");
		} catch (Exception e) {
			_log.warn("error", e);
			throw e;
		}

	}

	void test6_createRealm() throws Exception {
		try {
			_log.warn("test6_createRealm ...");
			RealmRepresentation realm = new RealmRepresentation();
			String realmName = "testRealm";
			realm.setRealm(realmName);
			kcadmin.createRealm(realm);
			kcadmin.execute(() -> assertThat(kcadmin.getRealms().findAll()).extracting(RealmRepresentation::getRealm)
					.contains("master", realmName));
		} catch (Exception e) {
			_log.warn("error", e);
			throw e;
		}
	}

	void test7_createClient() throws Exception {
		try {
			_log.warn("test7_createClient ...");
			// create client
			ClientRepresentation client = new ClientRepresentation();
			String clientName = "newClient";
			client.setName(clientName);
			kcadmin.createClient(client);
			kcadmin.execute(() -> assertThat(kcadmin.getRealm().clients().findAll())
					.extracting(ClientRepresentation::getName).contains("master Realm", clientName));

		} catch (Exception e) {
			_log.warn("error", e);
			throw e;
		}

	}

}
