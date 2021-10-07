/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.rest.query;

import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.UriInfo;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.condition.ConditionsBuilder;
import com.nsn.ood.cls.util.CollectionUtils;
import com.nsn.ood.cls.util.Strings;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSRuntimeException;
import com.nsn.ood.cls.util.log.Loggable;


/**
 * URI info <-> Conditions
 * 
 * @author marynows
 * 
 */
@Component
@Loggable
@Property(name = "from", value = "uriInfo")
@Property(name = "to", value = "conditions")
public class UriInfo2ConditionsConverter implements Converter<UriInfo, Conditions> {
	private static final Logger LOG = LoggerFactory.getLogger(UriInfo2ConditionsConverter.class);

	@ServiceDependency
	private FilterParser filterParser;
	
	@ServiceDependency
	private SortingParser sortingParser;
	
	@ServiceDependency
	private PaginationParser paginationParser;

	@Override
	public Conditions convertTo(final UriInfo uriInfo) {
		final ConditionsBuilder builder = ConditionsBuilder.create();

		for (final Entry<String, List<String>> e : uriInfo.getQueryParameters().entrySet()) {
			final String key = Strings.nullToEmpty(e.getKey()).trim();
			final List<String> values = e.getValue();
			LOG.trace("{}={}", key, values);

			if (!key.isEmpty() && CollectionUtils.isNotEmpty(values)) {
				parseSingleQuery(builder, key, values.get(0));
			}
		}

		return builder.build();
	}

	private void parseSingleQuery(final ConditionsBuilder builder, final String key, final String value) {
		if (QueryConst.SORTING_PARAMETER.equals(key)) {
			this.sortingParser.parse(builder, value);
		} else if (QueryConst.OFFSET_PARAMETER.equals(key)) {
			this.paginationParser.parseOffset(builder, value);
		} else if (QueryConst.LIMIT_PARAMETER.equals(key)) {
			this.paginationParser.parseLimit(builder, value);
		} else {
			this.filterParser.parse(builder, key, value);
		}
	}

	@Override
	public UriInfo convertFrom(final Conditions value) {
		throw new CLSRuntimeException("Unsupported operation");
	}
}
