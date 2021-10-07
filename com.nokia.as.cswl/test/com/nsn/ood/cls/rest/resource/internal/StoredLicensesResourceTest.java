/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.resource.internal;

import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorException;
import static com.nsn.ood.cls.core.test.ErrorExceptionTestUtil.errorExceptionsList;
import static com.nsn.ood.cls.core.test.LicenseFileTestUtil.licenseFile;
import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.storedLicense;
import static com.nsn.ood.cls.model.internal.test.StoredLicenseTestUtil.storedLicensesList;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.error;
import static com.nsn.ood.cls.model.test.ErrorTestUtil.errorsList;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.license;
import static com.nsn.ood.cls.model.test.LicenseTestUtil.licensesList;
import static com.nsn.ood.cls.model.test.LinkTestUtil.assertLink;
import static com.nsn.ood.cls.model.test.MetaDataTestUtil.metaData;
import static com.nsn.ood.cls.util.test.AnnotationTestUtil.assertAnnotation;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.expectLastCall;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static org.powermock.api.easymock.PowerMock.verifyAll;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.service.LicensesService;
import com.nsn.ood.cls.core.service.error.ErrorCode;
import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.ErrorExceptionFactory;
import com.nsn.ood.cls.core.service.error.ServiceException;
import com.nsn.ood.cls.core.service.internal.ActivityService;
import com.nsn.ood.cls.model.CLSMediaType;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.licenses.Licenses;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.model.metadata.MetaDataList;
import com.nsn.ood.cls.rest.BasicSecure;
import com.nsn.ood.cls.rest.convert.ErrorException2StringConverter;
import com.nsn.ood.cls.rest.resource.AbstractResourceTest;
import com.nsn.ood.cls.rest.resource.BaseResource;
import com.nsn.ood.cls.rest.util.HttpUtils;
import com.nsn.ood.cls.rest.util.MultipartOutputBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilder;
import com.nsn.ood.cls.rest.util.ResourceBuilderFactory.Links;
import com.nsn.ood.cls.rest.util.ResponseBuilder;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
public class StoredLicensesResourceTest extends AbstractResourceTest {
	private StoredLicensesResource resource;
	private LicensesService licensesServiceMock;
	private ActivityService activityServiceMock;
	private HttpUtils httpUtilsMock;
	private ErrorExceptionFactory errorExceptionFactoryMock;
	private MultipartOutputBuilder multipartOutputBuilderMock;
	private BaseResource base;

	@Before
	public void setUp() throws Exception {
		this.licensesServiceMock = createMock(LicensesService.class);
		this.activityServiceMock = createMock(ActivityService.class);
		this.httpUtilsMock = createMock(HttpUtils.class);
		this.errorExceptionFactoryMock = createMock(ErrorExceptionFactory.class);
		this.multipartOutputBuilderMock = createMock(MultipartOutputBuilder.class);

		this.base = createMock(BaseResource.class);
		init(base);
		
		this.resource = new StoredLicensesResource();
		setInternalState(this.resource, this.licensesServiceMock, this.activityServiceMock, this.httpUtilsMock,
				this.errorExceptionFactoryMock, this.multipartOutputBuilderMock, this.base);
		setInternalState(this.resource, "uriInfo2ConditionsConverter", uriInfo2ConditionsConverter);
		setInternalState(this.resource, "errorException2ErrorConverter", errorException2ErrorConverter);
		this.base.init(EasyMock.anyObject(), EasyMock.eq("licenses"));
	}

	@Test
	public void testGetStoredLicenses() throws Exception {
		final Resource resourceMock = createMock(Resource.class);
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final Capture<Links> capturedLinks = new Capture<>();
		final List<StoredLicense> storedLicenses = storedLicensesList(storedLicense("1"), storedLicense("2"));
		final MetaData metaData = metaData(20L, 10L);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.licensesServiceMock.getStoredLicenses(conditions)).andReturn(
				new MetaDataList<>(storedLicenses, metaData));
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.metaData(capture(capturedLinks), eq(metaData))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.storedLicenses(storedLicenses)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.links(conditions, metaData.getFiltered())).andReturn(new Links(new Link().withHref(("/licenses?query")), null, null));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getStoredLicenses(uriInfoMock));
		verifyAll();

		assertLinks(capturedLinks.getValue(), "/licenses?query");
	}

	@Test
	public void testGetStoredLicensesAndExpectError() throws Exception {
		final UriInfo uriInfoMock = createMock(UriInfo.class);
		final ServiceException exceptionMock = createMock(ServiceException.class);

		final Conditions conditions = mockUriConditions(uriInfoMock, this.uriInfo2ConditionsConverter);
		expect(this.licensesServiceMock.getStoredLicenses(conditions)).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getStoredLicenses(uriInfoMock));
		verifyAll();
	}

	@Test
	public void testGetFilters() throws Exception {
		final Capture<Link> capturedLink = new Capture<>();
		final List<String> filterValues = Arrays.asList("filter1", "filter2");

		expect(this.licensesServiceMock.getStoredLicenseFilterValues("fff")).andReturn(filterValues);
		final Resource resourceMock = createMock(Resource.class);
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.filterValues(filterValues)).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.ok(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("filters", "fff")).andReturn(new Link().withHref("/licenses/filters/fff"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();

		assertLink(capturedLink.getValue(), "/licenses/filters/fff");
	}

	@Test
	public void testGetFiltersAndExpectError() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);

		expect(this.licensesServiceMock.getStoredLicenseFilterValues("fff")).andThrow(exceptionMock);
		final Response responseMock = createMock(Response.class);
		expect(this.base.exceptionResponse(exceptionMock)).andReturn(responseMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.getFilters("fff"));
		verifyAll();
	}

	@Test
	public void testUpload() throws Exception {
		final MultiPart multipartInput = createMock(MultiPart.class);
		final Capture<Link> capturedLink = new Capture<>();
		final List<License> licenses = licensesList(license("1"));
		final List<ErrorException> errorExceptions = errorExceptionsList(
				errorException(ErrorCode.CLJL_LICENSE_INSTALL_FAIL));
		final List<BodyPart> inputParts = Arrays.asList(createMock(BodyPart.class), createMock(BodyPart.class),
				createMock(BodyPart.class));//, createMock(BodyPart.class));

		this.base.logInit(EasyMock.eq("Install license"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Install license"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Install license"));
		this.base.logSuccess(EasyMock.eq("Add activity"));
		
		expect(multipartInput.getBodyParts()).andReturn(inputParts);
		// part 1 - non XML
		expect(inputParts.get(0).getMediaType()).andReturn(MediaType.TEXT_PLAIN_TYPE);
		// part 2 - ok
		expect(inputParts.get(1).getMediaType()).andReturn(MediaType.APPLICATION_XML_TYPE);
		expect(this.httpUtilsMock.extractFileName(inputParts.get(1))).andReturn("filename1");
		expect(inputParts.get(1).getEntityAs(String.class)).andReturn("content1");
		expect(this.licensesServiceMock.install(licenseFile("filename1", "content1"))).andReturn(license("1"));
		// part 3 - IOException
//		expect(inputParts.get(2).getMediaType()).andReturn(MediaType.APPLICATION_XML_TYPE);
//		expect(this.httpUtilsMock.extractFileName(inputParts.get(2))).andReturn("filename2");
//		final IOException ioException = new IOException("message2");
//		expect(inputParts.get(2).getEntityAs(String.class)).andReturn("content2");;
//		expect(this.errorExceptionFactoryMock.error(ErrorCode.LICENSE_VERIFICATION_FAIL, ioException)).andReturn(errorExceptions.get(0));
		// part 4 - ServiceException
		expect(inputParts.get(2).getMediaType()).andReturn(MediaType.APPLICATION_XML_TYPE);
		expect(this.httpUtilsMock.extractFileName(inputParts.get(2))).andReturn("filename3");
		expect(inputParts.get(2).getEntityAs(String.class)).andReturn("content3");
		final ServiceException exceptionMock = createMock(ServiceException.class);
		this.base.logFailure("Install license", exceptionMock);
		expect(this.licensesServiceMock.install(licenseFile("filename3", "content3"))).andThrow(exceptionMock);
		
		expect(exceptionMock.getExceptions()).andReturn(Arrays.asList(errorExceptions.get(0)));
		// activity
		this.activityServiceMock.addLicenseInstallActivity(licenses, errorExceptions);
		// response
		final Resource resourceMock = mockCreateResource(capturedLink, licenses, errorExceptions);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.created(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("upload")).andReturn(new Link().withHref("/licenses/upload"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.upload(multipartInput));
		verifyAll();

		assertLink(capturedLink.getValue(), "/licenses/upload");
	}

	@Test
	public void testUploadNoPartsAndExpectErrorDuringActivityUpdate() throws Exception {
		final MultiPart multipartInput = createMock(MultiPart.class);
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final Capture<Link> capturedLink = new Capture<>();
		final List<License> licenses = licensesList();
		final List<ErrorException> errorExceptions = errorExceptionsList();
		
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));

		expect(multipartInput.getBodyParts()).andReturn(Collections.<BodyPart> emptyList());
		this.activityServiceMock.addLicenseInstallActivity(licenses, errorExceptions);
		expectLastCall().andThrow(exceptionMock);
		final Resource resourceMock = mockCreateResource(capturedLink, licenses, errorExceptions);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.created(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		
		this.base.logFailure("Add activity", exceptionMock);
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		expect(this.base.link("upload")).andReturn(new Link().withHref("/licenses/upload"));

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.upload(multipartInput));
		verifyAll();

		assertLink(capturedLink.getValue(), "/licenses/upload");
	}

	@Test
	public void testCancel() throws Exception {
		final Capture<Link> capturedLink = new Capture<>();
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final List<ErrorException> errorExceptions = errorExceptionsList(errorException(ErrorCode.CLJL_LICENSE_CANCEL_FAIL));
		
		this.base.logInit(EasyMock.eq("Cancel license"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Cancel license"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Cancel license"));
		this.base.logSuccess(EasyMock.eq("Add activity"));

		expect(this.licensesServiceMock.cancel(license("1"))).andReturn(license("1"));
		expect(this.licensesServiceMock.cancel(license("2"))).andThrow(exceptionMock);
		expect(exceptionMock.getExceptions()).andReturn(errorExceptions);
		Converter<ErrorException, String> converter = createMock(ErrorException2StringConverter.class);
		errorExceptionsList().forEach(e -> expect(converter.convertTo(e)).andReturn("error"));
		// activity
		this.activityServiceMock.addLicenseCancelActivity(licensesList(license("1")), errorExceptions);
		// response
		final Resource resourceMock = mockCreateResource(capturedLink, licensesList(license("1")), errorExceptions);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.accepted(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(this.base.link("cancel")).andReturn(new Link().withHref("/licenses/cancel"));
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		
		this.base.logFailure("Cancel license", exceptionMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock,
				this.resource.cancel(new Licenses().withLicenses(licensesList(license("1"), license("2")))));
		verifyAll();

		assertLink(capturedLink.getValue(), "/licenses/cancel");
	}

	@Test
	public void testCancelNoLicensesAndExpectErrorDuringActivityUpdate() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final Capture<Link> capturedLink = new Capture<>();
		final List<License> licenses = licensesList();
		final List<ErrorException> errorExceptions = errorExceptionsList();
		
		this.base.logInit(EasyMock.eq("Add activity"), EasyMock.anyObject(String.class));

		this.activityServiceMock.addLicenseCancelActivity(licenses, errorExceptions);
		expectLastCall().andThrow(exceptionMock);
		Converter<ErrorException, String> converter = createMock(ErrorException2StringConverter.class);
		errorExceptionsList().forEach(e -> expect(converter.convertTo(e)).andReturn("error"));
		final Resource resourceMock = mockCreateResource(capturedLink, licenses, errorExceptions);
		final ResponseBuilder responseBuilderMock = createMock(ResponseBuilder.class);
		final Response responseMock = createMock(Response.class);
		expect(this.responseBuilderFactoryMock.accepted(resourceMock)).andReturn(responseBuilderMock);
		expect(responseBuilderMock.build()).andReturn(responseMock);
		expect(this.base.link("cancel")).andReturn(new Link().withHref("/licenses/cancel"));
		expect(base.getResourceFactory()).andReturn(resourceBuilderFactoryMock);
		expect(this.base.getResponseFactory()).andReturn(responseBuilderFactoryMock);
		this.base.logFailure("Add activity", exceptionMock);

		replayAll();
		this.resource.start();
		assertEquals(responseMock, this.resource.cancel(new Licenses()));
		verifyAll();

		assertLink(capturedLink.getValue(), "/licenses/cancel");
	}

	private Resource mockCreateResource(final Capture<Link> capturedLink, final List<License> licenses,
			final List<ErrorException> errorExceptions) {
		final Resource resourceMock = createMock(Resource.class);
		final ResourceBuilder resourceBuilderMock = createMock(ResourceBuilder.class);
		expect(this.resourceBuilderFactoryMock.selfLink(capture(capturedLink))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.licenses(licenses)).andReturn(resourceBuilderMock);
		errorExceptions.forEach(e -> expect(errorException2ErrorConverter.convertTo(e)).andReturn(error(1L)));
		expect(resourceBuilderMock.errors(EasyMock.anyObject(List.class))).andReturn(resourceBuilderMock);
		expect(resourceBuilderMock.build()).andReturn(resourceMock);
		return resourceMock;
	}

	@Test
	public void testExport() throws Exception {
		final ServiceException exceptionMock = createMock(ServiceException.class);
		final MultiPart multipartOutput = new MultiPart();
		
		this.base.logInit(EasyMock.eq("Export license"), EasyMock.anyObject(String.class));
		this.base.logInit(EasyMock.eq("Export license"), EasyMock.anyObject(String.class));
		this.base.logSuccess(EasyMock.eq("Export license"));

		expect(this.licensesServiceMock.export(license("1"))).andReturn(licenseFile("filename1", "content1"));
		expect(this.multipartOutputBuilderMock.addFilePart("content1", MediaType.TEXT_XML_TYPE, "filename1"))
				.andReturn(this.multipartOutputBuilderMock);

		expect(this.licensesServiceMock.export(license("2"))).andThrow(exceptionMock);
		
		List<ErrorException> errors = errorExceptionsList(errorException());
		
		expect(exceptionMock.getExceptions()).andReturn(errors);
		errors.forEach(e -> expect(errorException2ErrorConverter.convertTo(e)).andReturn(error(2L)));
		expect(
				this.multipartOutputBuilderMock.addJsonPart(errorsList(error(2L)),
						CLSMediaType.APPLICATION_ERROR_JSON_TYPE)).andReturn(this.multipartOutputBuilderMock);

		expect(this.multipartOutputBuilderMock.build()).andReturn(multipartOutput);
		
		this.base.logFailure("Export license", exceptionMock);

		replayAll();
		this.resource.start();
		final Response response = this.resource.export(new Licenses().withLicenses(licensesList(license("1"),
				license("2"))));
		verifyAll();

		assertEquals(Status.OK, response.getStatusInfo());
		assertEquals(multipartOutput, response.getEntity());
	}

	@Test
	public void testAnnotations() throws Exception {
		assertAnnotation(StoredLicensesResource.class.getMethod("upload", MultiPart.class),
				BasicSecure.class);
		assertAnnotation(StoredLicensesResource.class.getMethod("cancel", Licenses.class), BasicSecure.class);
		PowerMock.resetAll();
	}
}
