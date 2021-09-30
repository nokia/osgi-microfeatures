/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Inject;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.model.ClientTag;
import com.nsn.ood.cls.core.model.ClientWithTag;
import com.nsn.ood.cls.core.service.BulkFeatureService;
import com.nsn.ood.cls.core.service.ClientsService;
import com.nsn.ood.cls.core.service.FeaturesService;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.clients.Clients;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.rest.ValidateETag;
import com.nsn.ood.cls.rest.util.MultipartOutputBuilder;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.log.Loggable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;


/**
 * @author marynows
 *
 */
@Component(provides = ClientsResource.class)
@Path(CLSApplication.API + CLSApplication.VERSION + "/clients")
@Produces({
		CLSMediaType.APPLICATION_CLIENT_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
@Loggable(duration = true)
public class ClientsResource {
	private static final Logger LOG = LoggerFactory.getLogger(ClientsResource.class);
	private static final String LOG_RESERVE_CLIENT_ID = "Reserve new client ID";
	private static final String LOG_KEEP_RESERVATION_ALIVE = "Keep reservation alive";
	private static final String CLIENT_ID = "clientId";
	private static final String TARGET_TYPE = "targetType";
	private static final int CLIENT_ID_MAX_LENGTH = 50;
	private static final int TARGET_TYPE_MAX_LENGTH = 20;
	
	@Inject
	private DependencyManager dm;

	@Context
	private ResourceContext resourceContext;
	@ServiceDependency
	private ClientsService clientsService;
	@ServiceDependency
	private BulkFeatureService bulkFeatureService;
	@ServiceDependency
	private MultipartOutputBuilder multipartOutputBuilder;
	
	@ServiceDependency
	private BaseResource baseResource;
	@ServiceDependency(filter = "(&(from=client)(to=string))")
	private Converter<Client, String> client2StringConverter;

	@Start
	public void start() {
		baseResource.init(LOG, "clients");
	}

	// TODO: @GET and @GET({clientId})

	@POST
	@Consumes(CLSMediaType.APPLICATION_CLIENT_JSON)
	@Produces({
			"multipart/mixed", CLSMediaType.APPLICATION_CLIENT_JSON

	})
	@Operation(
			summary = "Reserve Client ID (optional)",
			description = "Client MAY ask CLS to generate and return unique client identifier (if not based on HW CNumber). Given identifier MUST be preserved client-side\n" +
					"and provided in every further license requests. If optional targetType attribute is specified - newly created client will only receive capacity from\n" +
					"licenses with the same target type. If targetType attribute is not specified newly created client will receive capacity from all licenses available in\n" +
					"CLS.",
			tags={"Clients"},
			responses = {
					@ApiResponse(responseCode = "201", description = "Reservation done", content = @Content(
							schema = @Schema(implementation = Clients.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request")
			}
	)
	public Response reserveClientIdOrBulkOperation(final Clients clients, @Context final HttpHeaders headers) {
		if (isBulkReservation(headers)) {
			return bulkFeaturesReservation(clients);
		}
		return reserveClientId(clients);
	}

	private Response bulkFeaturesReservation(final Clients clients) {
		try {
			final Pair<List<Client>, List<Error>> reservationsAndErrors = this.bulkFeatureService
					.bulkFeatureReservations(clients);

			if (isEverythingOk(reservationsAndErrors)) {
				return createNormalResponses(reservationsAndErrors.getLeft());
			} else if (isEverythingWrong(reservationsAndErrors)) {
				return createErrorResponse(reservationsAndErrors.getRight());
			} else {
				return crateMixedResponse(reservationsAndErrors);
			}

		} catch (final UnknownErrorException e) {
			LOG.error("Error occured during bulk reservation", e);
			return Response.serverError().build();
		}
	}

	private Response crateMixedResponse(final Pair<List<Client>, List<Error>> reservationsAndErrors) {
		final Resource resourceOK = baseResource.getResourceFactory().selfLink(baseResource.link()).clients(reservationsAndErrors.getLeft())
				.build();

		final Resource resourceError = baseResource.getResourceFactory().selfLink(baseResource.link()).errors(reservationsAndErrors.getRight())
				.build();

		this.multipartOutputBuilder.addJsonPartAsText(resourceOK, CLSMediaType.APPLICATION_CLIENT_JSON_TYPE);
		this.multipartOutputBuilder.addJsonPartAsText(resourceError, CLSMediaType.APPLICATION_ERROR_JSON_TYPE);

		return Response.ok(this.multipartOutputBuilder.build()).build();
	}

	private Response createErrorResponse(final List<Error> errors) {
		final Resource resourceError = baseResource.getResourceFactory().selfLink(baseResource.link()).errors(errors).build();
		return baseResource.getResponseFactory().error(Status.BAD_REQUEST, resourceError).type(CLSMediaType.APPLICATION_ERROR_JSON)
				.build();
	}

	private Response createNormalResponses(final Collection<Client> clients) {
		final Resource resource = baseResource.getResourceFactory().selfLink(baseResource.link()).clients(new ArrayList<>(clients)).build();
		return baseResource.getResponseFactory().created(resource).type(CLSMediaType.APPLICATION_CLIENT_JSON).build();
	}

	private boolean isEverythingWrong(final Pair<List<Client>, List<Error>> reservationsAndErrors) {
		return reservationsAndErrors.getLeft().size() == 0;
	}

	private boolean isEverythingOk(final Pair<List<Client>, List<Error>> reservationsAndErrors) {
		return reservationsAndErrors.getRight().size() == 0;
	}

	private boolean isBulkReservation(final HttpHeaders headers) {
		for (final MediaType mediaType : headers.getAcceptableMediaTypes()) {
			if (mediaType.toString().equals("multipart/mixed")) {
				return true;
			}
		}
		return false;
	}

	private Response reserveClientId(final Clients clients) {
		final Client client = verifyReserveClientIdRequest(clients);
		baseResource.logInit(LOG_RESERVE_CLIENT_ID, client2StringConverter.convertTo(client));

		try {
			final ClientWithTag clientWithTag = this.clientsService.reserveClientId(client);
			baseResource.logSuccess(LOG_RESERVE_CLIENT_ID, client2StringConverter.convertTo(clientWithTag.getObject()));

			final Resource resource = baseResource.getResourceFactory().selfLink(baseResource.link())
					.clients(Arrays.asList(clientWithTag.getObject())).build();
			return baseResource.getResponseFactory().created(resource).tag(clientWithTag.getClientTag()).build();
		} catch (final ServiceException e) {
			baseResource.logFailure(LOG_RESERVE_CLIENT_ID, e);
			return baseResource.exceptionResponse(e);

		}
	}

	private Client verifyReserveClientIdRequest(final Clients clients) {
		final Client client = verifyClients(clients);
		if (client.getClientId() != null) {
			throw baseResource.getViolationFactory().clientException("clients.unexpectedClientId", CLIENT_ID, client.getClientId());
		}
		if ((client.getTargetType() != null) && (client.getTargetType().length() > TARGET_TYPE_MAX_LENGTH)) {
			throw baseResource.getViolationFactory().clientException("clients.targetType", TARGET_TYPE, client.getTargetType());
		}
		verifyKeepAliveTime(client);
		return client;
	}

	@PUT
	@Path("{clientId}")
	@Consumes(CLSMediaType.APPLICATION_CLIENT_JSON)
	@ValidateETag
	@Operation(
			summary = "Configure floating reservations alive time",
			tags={"Clients"},
			responses = {
					@ApiResponse(responseCode = "204", description = "Reservation done", content = @Content(
							schema = @Schema(implementation = Clients.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request, Client does not exist...")
			}
	)
	public Response keepReservationAlive(@PathParam(CLIENT_ID) final String clientId, final Clients clients) {
		verifyClientId(clientId);
		final Client client = verifyKeepReservationAliveRequest(clientId, clients);
		baseResource.logInit(LOG_KEEP_RESERVATION_ALIVE, client2StringConverter.convertTo(client));

		try {
			final ClientTag clientTag = this.clientsService.keepReservationAlive(client);
			baseResource.logSuccess(LOG_KEEP_RESERVATION_ALIVE);

			return baseResource.getResponseFactory().noContent().tag(clientTag).build();
		} catch (final ServiceException e) {
			baseResource.logFailure(LOG_KEEP_RESERVATION_ALIVE, e);
			return baseResource.exceptionResponse(e);
		}
	}

	private Client verifyKeepReservationAliveRequest(final String clientId, final Clients clients) {
		final Client client = verifyClients(clients);
		if (!clientId.equals(client.getClientId())) {
			throw baseResource.getViolationFactory().clientException("clients.matchResource", CLIENT_ID, client.getClientId());
		}
		if (client.getTargetType() != null) {
			throw baseResource.getViolationFactory().clientException("clients.unexpectedTargetType", TARGET_TYPE,
					client.getTargetType());
		}
		verifyKeepAliveTime(client);
		return client;
	}

	private Client verifyClients(final Clients clients) {
		if (isNotExactlyOneClient(clients)) {
			throw baseResource.getViolationFactory().pathException("clients.oneClient", "clients");
		}
		return clients.getClients().get(0);
	}

	private boolean isNotExactlyOneClient(final Clients clients) {
		return (clients == null) || CollectionUtils.isEmpty(clients.getClients()) || (clients.getClients().size() > 1);
	}

	private void verifyKeepAliveTime(final Client client) {
		if ((client.getKeepAliveTime() != null) && (client.getKeepAliveTime() <= 0L)) {
			throw baseResource.getViolationFactory().clientException("clients.positiveKeepAliveTime", "keepAliveTime",
					client.getKeepAliveTime());
		}
	}

	@Path("{clientId}/features")
	public ClientsFeaturesResource getFeaturesResource(@PathParam(CLIENT_ID) final String clientId) {
		verifyClientId(clientId);
		ResourceId resourceId = new ResourceId();
		resourceId.setResourceId(clientId);
		
		Dictionary<String, Object> rIdProps = new Hashtable<String, Object>();
		rIdProps.put("resource", "clientsFeatures");
		dm.add(dm.createComponent()
		  .setInterface(ResourceId.class, rIdProps)
		  .setImplementation(resourceId));
		
		CountDownLatch cd = new CountDownLatch(1);
		org.apache.felix.dm.Component comp =
			dm.createComponent()
			  .setInterface(ClientsFeaturesResource.class, null)
			  .setImplementation(ClientsFeaturesResource.class)
			  .add(dm.createServiceDependency().setService(FeaturesService.class).setRequired(true))
			  .add(dm.createServiceDependency().setService(ServiceExceptionFactory.class).setRequired(true))
			  .add(dm.createServiceDependency().setService(BaseResource.class).setRequired(true))
			  .add(dm.createServiceDependency().setService(ResourceId.class, "(resource=clientsFeatures)").setRequired(true))
			  .add(dm.createServiceDependency().setService(Converter.class, "(&(from=uriInfo)(to=conditions))").setRequired(true))
			  .add(dm.createServiceDependency().setService(Converter.class, "(&(from=feature)(to=string))").setRequired(true))
			  .add((c, s) -> {
				  if(s.equals(ComponentState.STARTED)) cd.countDown();
			  });
		dm.add(comp);
		try {
			cd.await();
		} catch (InterruptedException e) { return null; }
		ClientsFeaturesResource instance = comp.getInstance();
		return this.resourceContext.initResource(instance);
	}

	private void verifyClientId(final String clientId) {
		if (clientId.length() > CLIENT_ID_MAX_LENGTH) {
			throw baseResource.getViolationFactory().valueException("clients.clientId", clientId);
		}
	}
}
