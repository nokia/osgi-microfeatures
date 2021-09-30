/*
 * Copyright (c) 2014 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.core.model.FeaturesWithTag;
import com.nsn.ood.cls.core.service.FeaturesService;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.error.ServiceExceptionFactory;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.errors.FeatureError;
import com.nsn.ood.cls.model.gen.features.Allocation;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.features.Feature.Type;
import com.nsn.ood.cls.model.gen.features.Features;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.ValidateETag;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.DescriptionBuilder;
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
@Produces({
		CLSMediaType.APPLICATION_FEATURE_JSON, CLSMediaType.APPLICATION_ERROR_JSON })
@Loggable(duration = true)
public class ClientsFeaturesResource {
	private static final Logger LOG = LoggerFactory.getLogger(ClientsFeaturesResource.class);
	private static final String LOG_RESERVE_CAPACITY = "Reserve capacity";
	private static final String LOG_RELEASE_CAPACITY = "Release capacity";
	private static final Pattern FEATURE_CODE_PATTERN = Pattern.compile("\\d{1,10}");
	private static final String FEATURE_CODE = "featureCode";
	private static final String CAPACITY = "capacity";
	
	private FeaturesService featuresService;
	private ServiceExceptionFactory serviceExceptionFactory;
	private BaseResource baseResource;
	private ResourceId resourceId;
	private Converter<UriInfo, Conditions> uriInfo2ConditionsConverter;
	private Converter<Feature, String> feature2StringConverter;

	private String clientId;

	public void start() {
		baseResource.init(LOG, "clients", resourceId.getResourceId(), "features");
		this.clientId = resourceId.getResourceId();
	}

	@GET
	@Operation(
			summary = "Synchronize complete client's license state",
			tags={"Features"},
			responses = {
					@ApiResponse(responseCode = "200", description = "The list of features", content = @Content(
							schema = @Schema(implementation = Features.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request"),
					@ApiResponse(responseCode = "404", description = "Client does not exist, resource not found")
			}
	)
	public Response getFeatures(@Context final UriInfo uriInfo) {
		final Conditions conditions = uriInfo2ConditionsConverter.convertTo(uriInfo);

		try {
			final MetaDataList<Feature> features = this.featuresService.getFeatures(this.clientId, conditions.clone());

			final List<Feature> correctedFeatures = correctResourceLinks(features.getList());
			return getFeaturesResponse(baseResource.getResourceFactory()//
					.metaData(baseResource.links(conditions, features.getMetaData().getFiltered()), features.getMetaData())//
					.features(correctedFeatures).build());
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	@GET
	@Path("{featureCode}")
	@Operation(
			summary = "Synchronize client's license state per feature",
			tags={"Features"},
			responses = {
					@ApiResponse(responseCode = "200", description = "The list of features", content = @Content(
							schema = @Schema(implementation = Features.class))),
					@ApiResponse(responseCode = "400", description = "Bad Request"),
					@ApiResponse(responseCode = "404", description = "Client does not exist, resource not found")
			}
	)
	public Response getFeature(@PathParam(FEATURE_CODE) final String featureCode) {
		verifyFeatureCode(featureCode);

		final Conditions conditions = ConditionsBuilder.createAndSkipMetaData().equalFilter(FEATURE_CODE, featureCode)
				.build();

		try {
			final MetaDataList<Feature> features = this.featuresService.getFeatures(this.clientId, conditions);
			verifyIfNotEmpty(features.getList(), featureCode);

			return getFeaturesResponse(createResource(features.getList(), featureCode));
		} catch (final ServiceException e) {
			return baseResource.exceptionResponse(e);
		}
	}

	private void verifyIfNotEmpty(final List<Feature> features, final String featureCode) throws ServiceException {
		if (CollectionUtils.isEmpty(features)) {
			throw this.serviceExceptionFactory.featureNotFound(new FeatureError().withFeatureCode(Long
					.valueOf(featureCode)));
		}
	}

	private Response getFeaturesResponse(final Resource resource) {
		return baseResource.getResponseFactory().ok(resource).build();
	}

	@POST
	@Consumes(CLSMediaType.APPLICATION_FEATURE_JSON)
	@ValidateETag
	@Operation(
			summary = "Initial request for capacity-based feature, on/off feature reservation",
			tags={"Features"},
			responses = {
					@ApiResponse(responseCode = "201", description = "Feature Capacity reserved", content = @Content(
							schema = @Schema(implementation = Features.class))),
					@ApiResponse(responseCode = "400", description = "Multiple Errors, error available on response \"developerMessage\"",
					content = @Content(schema = @Schema(implementation = Error.class)))
			}
	)
	public Response reserveCapacity(final Features features) {
		final List<Feature> featuresList = verifyReserveCapacityRequest(features);

		return reserveCapacityForFeatures(featuresList);
	}

	@POST
	@Path("{featureCode}")
	@Consumes(CLSMediaType.APPLICATION_FEATURE_JSON)
	@ValidateETag
	@Operation(
			summary = "Capacity reservation change (bulk update & update)",
			tags={"Features"},
			responses = {
					@ApiResponse(responseCode = "201", description = "Feature Capacity reserved", content = @Content(
							schema = @Schema(implementation = Features.class))),
					@ApiResponse(responseCode = "400", description = "Multiple Errors, error available on response \"developerMessage\"",
							content = @Content(schema = @Schema(implementation = Error.class)))
			}
	)
	public Response reserveCapacity(@PathParam(FEATURE_CODE) final String featureCode, final Features features) {
		verifyFeatureCode(featureCode);
		final List<Feature> featuresList = verifyReserveCapacityRequest(features);
		verifyReserveCapacityRequest(Long.valueOf(featureCode), featuresList);

		return reserveCapacityForFeatures(featuresList, featureCode);
	}

	private List<Feature> verifyReserveCapacityRequest(final Features features) {
		if (features == null || CollectionUtils.isEmpty(features.getFeatures())) {
			throw baseResource.getViolationFactory().pathException("features.oneOrMoreFeatures", "features");
		}
		final List<Feature> featuresList = features.getFeatures();
		for (int i = 0; i < featuresList.size(); i++) {
			verifyFeature(featuresList.get(i), i);
		}
		return featuresList;
	}

	private void verifyFeature(final Feature feature, final int index) {
		if (feature.getFeatureCode() == null) {
			throw baseResource.getViolationFactory().featureException("features.missingFeatureCode", index, FEATURE_CODE, null);
		}
		if (feature.getType() == null) {
			throw baseResource.getViolationFactory().featureException("features.missingType", index, "type", null);
		}
		if (feature.getType() == Type.CAPACITY && (feature.getCapacity() == null || feature.getCapacity() < 0L)) {
			throw baseResource.getViolationFactory().featureException("features.missingCapacity", index, CAPACITY,
					feature.getCapacity());
		}
		if (feature.getType() == Type.ON_OFF && feature.getCapacity() != null) {
			throw baseResource.getViolationFactory().featureException("features.unexpectedCapacity", index, CAPACITY,
					feature.getCapacity());
		}
	}

	private void verifyReserveCapacityRequest(final Long featureCode, final List<Feature> features) {
		for (int i = 0; i < features.size(); i++) {
			final Feature feature = features.get(i);
			if (!featureCode.equals(feature.getFeatureCode())) {
				throw baseResource.getViolationFactory().featureException("features.matchResource", i, FEATURE_CODE,
						feature.getFeatureCode());
			}
		}
	}

	private Response reserveCapacityForFeatures(final List<Feature> features, final String... path) {
		baseResource.logInit(LOG_RESERVE_CAPACITY, new DescriptionBuilder()//
				.append("clientId", this.clientId)//
				.append("features", features.stream().map(feature2StringConverter::convertTo).collect(Collectors.toList())).build());
		try {
			final FeaturesWithTag featuresWithTag = this.featuresService.reserveCapacity(this.clientId, features);
			baseResource.logSuccess(LOG_RESERVE_CAPACITY, featuresWithTag.getObject().stream().map(feature2StringConverter::convertTo).collect(Collectors.toList())
					.toString());

			return baseResource.getResponseFactory().created(createResource(featuresWithTag.getObject(), path))
					.tag(featuresWithTag.getClientTag()).build();
		} catch (final ServiceException e) {
			baseResource.logFailure(LOG_RESERVE_CAPACITY, e);
			return baseResource.exceptionResponse(e);
		}
	}

	private Resource createResource(final List<Feature> features, final String... path) {
		final List<Feature> correctedFeatures = correctResourceLinks(features);
		return baseResource.getResourceFactory().selfLink(baseResource.link(path)).features(correctedFeatures).build();
	}

	private List<Feature> correctResourceLinks(final List<Feature> features) {
		for (final Feature feature : features) {
			for (final Allocation allocation : feature.getAllocations()) {
				allocation.setPoolLicense(URI.create("/licenses/" + allocation.getPoolLicense().toString()));
			}
		}
		return features;
	}

	@DELETE
	@Operation(
			summary = "Release all capacity and on-off features - graceful shutdown)",
			tags={"Features"},
			responses = {
					@ApiResponse(responseCode = "204", description = "Feature Capacity released"),
					@ApiResponse(responseCode = "400", description = "Multiple Errors, error available on response \"developerMessage\"",
							content = @Content(schema = @Schema(implementation = Error.class)))
			}
	)
	public Response releaseCapacity(@Context final HttpHeaders headers) {
		return releaseCapacityForFeatures(Collections.<Long> emptyList(), isForceEnabled(headers));
	}

	@DELETE
	@Path("{featureCode}")
	@Operation(
			summary = "Release feature floating capacity or on/off reservation",
			tags={"Features"},
			responses = {
					@ApiResponse(responseCode = "204", description = "Feature Capacity or on/off released"),
					@ApiResponse(responseCode = "400", description = "Multiple Errors, error available on response \"developerMessage\"",
							content = @Content(schema = @Schema(implementation = Error.class)))
			}
	)
	public Response releaseCapacity(@Context final HttpHeaders headers,
			@PathParam(FEATURE_CODE) final String featureCode) {
		verifyFeatureCode(featureCode);

		return releaseCapacityForFeatures(Arrays.asList(Long.valueOf(featureCode)), isForceEnabled(headers));
	}

	private boolean isForceEnabled(final HttpHeaders headers) {
		return Boolean.valueOf(headers.getHeaderString("Force"));
	}

	private Response releaseCapacityForFeatures(final List<Long> featureCodes, final boolean force) {
		baseResource.logInit(LOG_RELEASE_CAPACITY, new DescriptionBuilder()//
				.append("clientId", this.clientId)//
				.append("featureCodes", featureCodes.isEmpty() ? "<all>" : featureCodes.toString())//
				.append("force", force).build());

		try {
			this.featuresService.releaseCapacity(this.clientId, featureCodes, force);
			baseResource.logSuccess(LOG_RELEASE_CAPACITY);

			return baseResource.getResponseFactory().noContent().build();
		} catch (final ServiceException e) {
			baseResource.logFailure(LOG_RELEASE_CAPACITY, e);
			return baseResource.exceptionResponse(e);
		}
	}

	private void verifyFeatureCode(final String featureCode) {
		if (!FEATURE_CODE_PATTERN.matcher(featureCode).matches()) {
			throw baseResource.getViolationFactory().valueException("features.featureCode", featureCode);
		}
	}
}
