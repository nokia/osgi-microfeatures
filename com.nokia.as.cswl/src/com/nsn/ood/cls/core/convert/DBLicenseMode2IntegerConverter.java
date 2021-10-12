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
@Property(name = "from", value = "dbLicenseMode")
@Property(name = "to", value = "integer")
public class DBLicenseMode2IntegerConverter implements Converter<DBLicense.LicenseMode, Integer> {
    private static final int ON_OFF = 1;
    private static final int CAPACITY = 2;

    @Override
    public Integer convertTo(final DBLicense.LicenseMode mode) {
        if (mode == DBLicense.LicenseMode.ON_OFF) {
            return ON_OFF;
        } else if (mode == DBLicense.LicenseMode.CAPACITY) {
            return CAPACITY;
        }
        throw new CLSIllegalArgumentException("Invalid DB license mode: \"" + mode + "\"");
    }

    @Override
    public DBLicense.LicenseMode convertFrom(final Integer mode) {
        if (mode == ON_OFF) {
            return DBLicense.LicenseMode.ON_OFF;
        } else if (mode == CAPACITY) {
            return DBLicense.LicenseMode.CAPACITY;
        }
        throw new CLSIllegalArgumentException("Invalid DB license mode: \"" + mode + "\"");
    }
}
