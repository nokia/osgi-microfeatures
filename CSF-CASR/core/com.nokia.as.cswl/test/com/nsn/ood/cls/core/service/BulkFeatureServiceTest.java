package com.nsn.ood.cls.core.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createMock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.nsn.ood.cls.core.service.error.ErrorException;
import com.nsn.ood.cls.core.service.error.UnknownErrorException;
import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.clients.Clients;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.rest.convert.ErrorException2ErrorConverter;
import com.nsn.ood.cls.util.convert.Converter;


public class BulkFeatureServiceTest {

	private BulkFeatureService bean;
	private Converter<ErrorException, Error> converterMock;

	@Before
	public void setUp() throws Exception {
		this.bean = new BulkFeatureService();

		this.converterMock = createMock(ErrorException2ErrorConverter.class);
	}

	@Test
	public void testFailDuplicatedClientsInReservations() throws Exception {
		final Clients clients = new Clients();
		final List<Client> cl = new ArrayList<>();
		clients.setClients(cl);

		final List<Feature> fe1 = new ArrayList<>();
		fe1.add(new Feature().withFeatureCode(222L).withCapacity(789L));

		final Client cl1 = new Client().withClientId("123").withFeatures(fe1);
		final Client cl2 = new Client().withClientId("123").withFeatures(fe1);
		cl.add(cl1);
		cl.add(cl2);

		try {
			this.bean.bulkFeatureReservations(clients);
			fail();
		} catch (final UnknownErrorException ex) {
			assertEquals("Duplicated client (123) - feature (222) conbination", ex.getMessage());
		}
	}

	@Test
	public void testFailDuplicatedFeaturesInClientInReservations() throws Exception {
		final Clients clients = new Clients();
		final List<Client> cl = new ArrayList<>();
		clients.setClients(cl);

		final List<Feature> fe1 = new ArrayList<>();
		fe1.add(new Feature().withFeatureCode(222L).withCapacity(789L));
		fe1.add(new Feature().withFeatureCode(222L).withCapacity(889L));

		final Client cl1 = new Client().withClientId("123").withFeatures(fe1);
		cl.add(cl1);

		try {
			this.bean.bulkFeatureReservations(clients);
			fail();
		} catch (final UnknownErrorException ex) {
			assertEquals("Duplicated client (123) - feature (222) conbination", ex.getMessage());
		}

	}

}
