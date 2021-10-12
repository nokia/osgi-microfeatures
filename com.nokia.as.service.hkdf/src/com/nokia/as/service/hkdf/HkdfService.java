// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.service.hkdf;

import org.osgi.annotation.versioning.ProviderType;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.util.Map;

@ProviderType
public interface HkdfService {

    /**
     * Takes the keying material from TLS export (RFC5705) and
     * performs RFC 5869 expansion from it.
     * @param keyingMaterial contains the material from TLS export service
     * @param length length of expansion
     * @param labels can be String or byte[] objects used to concatenate into a single byte[] label
     * @return the expanded key as a byte[]
     */
    byte[] expand(Map<String,Object> keyingMaterial, int length, Object ... labels);

    SecretKey expand(Mac mac, byte[] key, byte[] context, int length, String key_description) throws InvalidKeyException;

    Mac build(String algorithm_name);
}
