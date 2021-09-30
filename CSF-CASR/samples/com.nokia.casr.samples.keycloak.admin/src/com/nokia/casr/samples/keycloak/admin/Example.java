package com.nokia.casr.samples.keycloak.admin;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.nokia.as.thirdparty.keycloak.admin.client.KeycloakAdminClient;

@Component
public class Example {

	@ServiceDependency(filter = "(realm=master)")
	private volatile KeycloakAdminClient kcadmin;

	@Start
	public void start() {
		try {
			// creating realm role
			String roleName = "newRole-" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
			System.out.println("creating realm role ");
			kcadmin.createRole(new RoleRepresentation(roleName, "description", false));

			// listing clients
			System.out.println("listing clients");
			kcadmin.findAllClients().stream().forEach(c -> System.out.println(c.getClientId()));

			// finding user by role
			System.out.println("finding user by role");
			kcadmin.getRoleUserMembers("admin").stream().forEach(u -> System.out.println(u.getUsername()));

			// create user
			UserRepresentation user = new UserRepresentation();
			user.setEnabled(true);
			String username = "newUser-" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new Date());
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

			System.out.println("creating user");
			Response createUserResponse = kcadmin.createUser(user);

			// find user id
			String userId = createUserResponse.getLocation().getPath().replaceAll(".*/" + "([^/]+)$", "$1");

			// set temporary password
			System.out.println("setting temporary password for " + userId);
			CredentialRepresentation temporaryCredential = new CredentialRepresentation();
			temporaryCredential.setTemporary(true);
			temporaryCredential.setType(CredentialRepresentation.PASSWORD);
			temporaryCredential.setValue("temporary");
			kcadmin.execute(() -> kcadmin.getRealm().users().get(userId).resetPassword(temporaryCredential));

			// assign role
			System.out.println("assigning role to user " + userId);
			kcadmin.assignRole("user", userId);

			// listing users
			System.out.println("listing users");
			kcadmin.listUsers().stream().forEach(u -> System.out.println(u.getUsername()));

			// create realm
			System.out.println("create realm");
			RealmRepresentation realm = new RealmRepresentation();
			realm.setRealm("testRealm");
			kcadmin.createRealm(realm);

			// create client
			System.out.println("create client");
			ClientRepresentation client = new ClientRepresentation();
			client.setId("newClient");
			kcadmin.createClient(client);

			// other methods : search user
			List<UserRepresentation> users = (List<UserRepresentation>) kcadmin
					.execute(() -> kcadmin.getRealm().users().search("nxuser"));
			users.stream().forEach(u -> System.out.println("User found = " + u.getUsername()));

			// other methods : get server info
			String javaHome = (String) kcadmin
					.execute(() -> kcadmin.serverInfo().getInfo().getSystemInfo().getJavaHome());
			System.out.println("info.javaHome = " + javaHome);

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
