// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nsn.ood.cls.core.convert;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.Property;

import com.nsn.ood.cls.model.gen.licenses.DBLicense;
import com.nsn.ood.cls.util.convert.Converter;
import com.nsn.ood.cls.util.exception.CLSIllegalArgumentException;

@Component
@Property(name = "from", value = "dbLicenseType")
@Property(name = "to", value = "integer")
public class DBLicenseType2IntegerConverter implements Converter<DBLicense.LicenseType, Integer> {
    private static final int POOL = 2;
    private static final int FLOATING_POOL = 4;

    @Override
    public Integer convertTo(final DBLicense.LicenseType type) {
        if (type == DBLicense.LicenseType.POOL) {
            return POOL;
        } else if (type == DBLicense.LicenseType.FLOATING_POOL) {
            return FLOATING_POOL;
        }
        throw new CLSIllegalArgumentException("Invalid license type: \"" + type + "\"");
    }

    @Override
    public DBLicense.LicenseType convertFrom(final Integer type) {
        if (type == POOL) {
            return DBLicense.LicenseType.POOL;
        } else if (type == FLOATING_POOL) {
            return DBLicense.LicenseType.FLOATING_POOL;
        }
        throw new CLSIllegalArgumentException("Invalid DB license type: \"" + type + "\"");
    }
}
