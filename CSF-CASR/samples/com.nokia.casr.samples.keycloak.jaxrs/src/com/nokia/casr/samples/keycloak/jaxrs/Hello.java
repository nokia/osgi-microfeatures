package com.nokia.casr.samples.keycloak.jaxrs;

import java.security.Principal;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.IDToken;
import org.osgi.service.component.annotations.Component;

@Component(service = Object.class, configurationPid = "keycloak") 
@Path("/")
public class Hello {

	@Context
	private SecurityContext securityContext;

	@Context
	UriInfo requestUriInfo;

	@GET
	@Path("/public")
	public Response publicMessage() {
		return Response.status(200).entity("Public Message").build();
	}

	@GET
	@Path("/user")
	@RolesAllowed("*")
	public Response userMessage() { // method secured by web.xml
		Principal userPrincipal = securityContext.getUserPrincipal();
		if (userPrincipal instanceof KeycloakPrincipal) {
			KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) userPrincipal;
			IDToken token = kp.getKeycloakSecurityContext().getToken();
			System.out.println("\u001B[31m" + "tokenID = " + token.getId() + "\u001B[0m");
		}
		return Response.status(200).entity("User Message:" + userPrincipal).build();
	}

	@GET
	@Path("/admin")
	@RolesAllowed("admin") // method secured by jax-rs annotation using keycloak server roles
	public Response adminMessage() {
		return Response.status(200).entity("Admin Message").build();
	}

}