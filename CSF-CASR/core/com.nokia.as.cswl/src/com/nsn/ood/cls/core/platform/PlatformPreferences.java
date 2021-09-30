/*
 * Copyright (c) 2015 Nokia Solutions and Networks. All rights reserved.
 */
package com.nsn.ood.cls.core.platform;

import java.sql.SQLException;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nsn.ood.cls.core.db.LogMessage;
import com.nsn.ood.cls.core.db.Query;
import com.nsn.ood.cls.core.db.StatementExecutor;
import com.nsn.ood.cls.core.db.platform.QueryTargetId;
import com.nsn.ood.cls.util.log.Loggable;
import com.nsn.ood.cls.util.log.Loggable.Level;


/**
 * @author marynows
 * 
 */
@Component(provides = PlatformPreferences.class)
@Loggable
public class PlatformPreferences {
	private static final Logger LOG = LoggerFactory.getLogger(PlatformPreferences.class);

	@ServiceDependency(filter = "(name=query)")
	private StatementExecutor<Query> queryExecutor;

	private String targetId = null;

	public String getTargetId() {
		if (this.targetId == null) {
			this.targetId = retrieveTargetId();
		}
		return this.targetId;
	}

	@Loggable(Level.WARNING)
	public String reloadTargetId() {
		final String id = retrieveTargetId();
		if (id != null) {
			this.targetId = id;
		}
		return this.targetId;
	}

	private String retrieveTargetId() {
		final QueryTargetId query = createTargetIdQuery();
		try {
			this.queryExecutor.execute(query);
		} catch (final SQLException e) {
			LOG.error(LogMessage.QUERY_FAIL, e);
			return null;
		}
		return query.getValue();
	}

	protected QueryTargetId createTargetIdQuery() {
		return new QueryTargetId();
	}
}
