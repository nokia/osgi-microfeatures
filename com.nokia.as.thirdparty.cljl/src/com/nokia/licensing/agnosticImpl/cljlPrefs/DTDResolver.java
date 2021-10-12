// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.licensing.agnosticImpl.cljlPrefs;

import java.io.StringReader;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Class for resolving DTD file for plugin configuration xml files.
 */
public class DTDResolver implements EntityResolver {

    // The required DTD URI for exported preferences
    private static final String PREFS_DTD_URI = "http://java.sun.com/dtd/preferences.dtd";

    // The actual DTD corresponding to the URI
    private static final String PREFS_DTD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //
            + "<!-- DTD for preferences -->" //
            + "<!ELEMENT preferences (root) >" //
            + "<!ATTLIST preferences" //
            + " EXTERNAL_XML_VERSION CDATA \"0.0\"  >" //
            + "<!ELEMENT root (map, node*) >" //
            + "<!ATTLIST root" //
            + "          type (system|user) #REQUIRED >" //
            + "<!ELEMENT node (map, node*) >" //
            + "<!ATTLIST node" //
            + "          name CDATA #REQUIRED >" //
            + "<!ELEMENT map (entry*) >" //
            + "<!ATTLIST map" //
            + "  MAP_XML_VERSION CDATA \"0.0\"  >" //
            + "<!ELEMENT entry EMPTY >" //
            + "<!ATTLIST entry" //
            + "          key CDATA #REQUIRED" //
            + "          value CDATA #REQUIRED >";

    @Override
    public InputSource resolveEntity(final String pid, final String sid) throws SAXException {
        if (sid.equals(PREFS_DTD_URI)) {
            InputSource is;
            is = new InputSource(new StringReader(PREFS_DTD));
            is.setSystemId(PREFS_DTD_URI);
            return is;
        }
        throw new SAXException("Invalid system identifier: " + sid);
    }
}
