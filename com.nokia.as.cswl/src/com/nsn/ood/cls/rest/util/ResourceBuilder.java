/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.util;

import java.util.Arrays;
import java.util.List;

import com.nsn.ood.cls.model.gen.clients.Client;
import com.nsn.ood.cls.model.gen.errors.Error;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.model.gen.hal.Link;
import com.nsn.ood.cls.model.gen.hal.Resource;
import com.nsn.ood.cls.model.gen.licenses.DBLicense;
import com.nsn.ood.cls.model.gen.licenses.License;
import com.nsn.ood.cls.model.gen.metadata.MetaData;
import com.nsn.ood.cls.model.internal.Activity;
import com.nsn.ood.cls.model.internal.ActivityDetail;
import com.nsn.ood.cls.model.internal.LicensedFeature;
import com.nsn.ood.cls.model.internal.Reservation;
import com.nsn.ood.cls.model.internal.Setting;
import com.nsn.ood.cls.model.internal.StoredLicense;
import com.nsn.ood.cls.model.util.HalResource;
import com.nsn.ood.cls.model.util.HalResourceBuilder;


/**
 * @author marynows
 * 
 */
public class ResourceBuilder {
	private final HalResourceBuilder<Resource> halBuilder;

	ResourceBuilder() {
		this.halBuilder = HalResource.builder(new Resource());
	}

	public Resource build() {
		return this.halBuilder.build();
	}

	public ResourceBuilder selfLink(final Link link) {
		return addLink("self", link);
	}

	public ResourceBuilder nextLink(final Link link) {
		return addLink("next", link);
	}

	public ResourceBuilder prevLink(final Link link) {
		return addLink("previous", link);
	}

	public ResourceBuilder links(final String name, final Link... links) {
		return links(name, Arrays.asList(links));
	}

	public ResourceBuilder links(final String name, final List<Link> links) {
		if (links.size() == 1) {
			addLink(name, links.get(0));
		} else if (links.size() > 1) {
			this.halBuilder.addLinksArray(name, links);
		}
		return this;
	}

	private ResourceBuilder addLink(final String name, final Link link) {
		if (link != null) {
			this.halBuilder.addLink(name, link);
		}
		return this;
	}

	public ResourceBuilder errors(final Error... errors) {
		return errors(Arrays.asList(errors));
	}

	public ResourceBuilder errors(final List<Error> errors) {
		this.halBuilder.addEmbeddedIfNotEmpty("errors", errors);
		return this;
	}

	public ResourceBuilder licenses(final List<License> licenses) {
		this.halBuilder.addEmbeddedIfNotNull("licenses", licenses);
		return this;
	}

	public ResourceBuilder dblicenses(final List<DBLicense> licenses) {
		this.halBuilder.addEmbeddedIfNotNull("dblicenses", licenses);
		return this;
	}

	public ResourceBuilder clients(final List<Client> clients) {
		this.halBuilder.addEmbeddedIfNotNull("clients", clients);
		return this;
	}

	public ResourceBuilder features(final List<Feature> features) {
		this.halBuilder.addEmbeddedIfNotNull("features", features);
		return this;
	}

	public ResourceBuilder activities(final List<Activity> activities) {
		this.halBuilder.addEmbeddedIfNotNull("activities", activities);
		return this;
	}

	public ResourceBuilder activityDetails(final List<ActivityDetail> activities) {
		this.halBuilder.addEmbeddedIfNotNull("activityDetails", activities);
		return this;
	}

	public ResourceBuilder reservations(final List<Reservation> reservations) {
		this.halBuilder.addEmbeddedIfNotNull("reservations", reservations);
		return this;
	}

	public ResourceBuilder licensedFeatures(final List<LicensedFeature> licensedFeatures) {
		this.halBuilder.addEmbeddedIfNotNull("licensedFeatures", licensedFeatures);
		return this;
	}

	public ResourceBuilder storedLicenses(final List<StoredLicense> storedLicenses) {
		this.halBuilder.addEmbeddedIfNotNull("storedLicenses", storedLicenses);
		return this;
	}

	public ResourceBuilder settings(final List<Setting> settings) {
		this.halBuilder.addEmbeddedIfNotNull("settings", settings);
		return this;
	}

	public ResourceBuilder metaData(final MetaData metaData) {
		this.halBuilder.addEmbeddedIfNotNull("metadata", metaData);
		return this;
	}

	public ResourceBuilder filterValues(final List<String> filterValues) {
		this.halBuilder.addEmbeddedIfNotNull("filterValues", filterValues);
		return this;
	}

	public ResourceBuilder embedded(final String name, final Object value) {
		this.halBuilder.addEmbeddedIfNotNull(name, value);
		return this;
	}
}
