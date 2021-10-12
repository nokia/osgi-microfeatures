// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

/**
 * 
 */
package com.alcatel_lucent.servlet.sip.services;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public interface Configurable {
    public void setConfiguration(Properties prop);
    public void setConfiguration(InputStream propertyfile)  throws FileNotFoundException, IOException ;
}
