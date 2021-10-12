// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.core.db.license;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.nsn.ood.cls.core.condition.Conditions;
import com.nsn.ood.cls.core.db.ListConditionsQuery;
import com.nsn.ood.cls.core.db.creator.DBLicenseCreator;
import com.nsn.ood.cls.core.db.util.ConditionProcessingException;
import com.nsn.ood.cls.core.db.util.ConditionsMapper;
import com.nsn.ood.cls.model.gen.licenses.DBLicense;


/**
 * @author marynows
 *
 */
public class QueryDBLicenses extends ListConditionsQuery<DBLicense> {
    private final DBLicenseCreator creator;

    public QueryDBLicenses(final Conditions conditions, final ConditionsMapper mapper, final DBLicenseCreator creator)
            throws ConditionProcessingException {
        super("select * from cls.storedlicense", conditions, mapper, null);
        this.creator = creator;
    }

    @Override
    protected DBLicense handleRow(final ResultSet resultSet) throws SQLException {
        return this.creator.createDBLicense(resultSet);
    }
}