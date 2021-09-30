/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.db.mapper;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;

import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.gen.features.Feature;
import com.nsn.ood.cls.util.convert.Converter;


/**
 * @author marynows
 * 
 */
@Component(provides = ConditionsMapper.class)
@Property(name = "name", value = "feature")
public class FeatureConditionsMapper extends ConditionsMapper {
	@ServiceDependency(filter = "(&(from=featureType)(to=integer))")
	private Converter<Feature.Type, Integer> featureType2IntegerConverter;

	@Start
	public void start() {
		map("clientId", "clientid", String.class);
		map("featureCode", "featurecode", Long.class);
		map("type", "mode", Integer.class, new TypeConverter());
	}

	private class TypeConverter implements ValueConverter<Integer> {
		@Override
		public Integer prepareConvert(final String value) {
			return FeatureConditionsMapper.this.featureType2IntegerConverter.convertTo(Feature.Type.fromValue(value));
		}

		@Override
		public String handleConvert(final Integer value) {
			return FeatureConditionsMapper.this.featureType2IntegerConverter.convertFrom(value).toString();
		}
	}
}
