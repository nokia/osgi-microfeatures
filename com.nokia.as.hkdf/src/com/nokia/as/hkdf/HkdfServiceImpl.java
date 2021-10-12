// Copyright 2000-2021 Nokia
//
// Licensed under the Apache License 2.0
// SPDX-License-Identifier: Apache-2.0
//

package com.nokia.as.hkdf;

import com.nokia.as.service.hkdf.HkdfService;
import org.apache.felix.dm.annotation.api.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.util.Map;

@Component
public class HkdfServiceImpl implements HkdfService {

    public static final String MASTER_SECRET = "tcp.secure.keyexport.master_secret";
    public static final String ALGOS         = "tcp.secure.keyexport.algos";

    private final static Logger log = LoggerFactory.getLogger(HkdfService.class);

    @Override
    public byte[] expand(Map<String,Object> keyingMaterial, int length, Object ... labels) {

        final byte [] master_secret = (byte[]) keyingMaterial.get(MASTER_SECRET);
        String algos = (String) keyingMaterial.get(ALGOS);

        final String algos_choice;
            if (algos!=null && algos.endsWith("_SHA256")) {
                algos_choice = "SHA-256";
            } else if (algos != null && algos.endsWith("_SHA384")){
                algos_choice = "SHA-384";
            } else {
                throw new IllegalArgumentException("unknown algorithms "+algos);
            }

        final Mac mac = HKDF.build(algos_choice);

        int info_length = 0;
        for( Object label: labels ) {
            if (label instanceof byte[] ) {
                info_length += ((byte[])label).length;
            } else if ( label instanceof String) {
                info_length += ((String)label).getBytes().length;
            }
        }

        byte [] info = new byte[info_length];
        int pos = 0;
        for( Object label: labels ) {
            if (label instanceof byte[] ) {
                System.arraycopy( (byte[])label, 0, info, pos, ((byte[])label).length);
                pos += ((byte[])label).length;
            } else if ( label instanceof String) {
                System.arraycopy( ((String)label).getBytes(), 0, info, pos, ((String)label).getBytes().length);
                pos += ((String)label).getBytes().length;
            }
        }

        SecretKey key = null;
        try {
            key = HKDF.expand(mac,master_secret, info, length, "HKDF-generated");
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(e);
        }

        return key.getEncoded();
    }

    @Override
    public SecretKey expand(Mac mac, byte[] key, byte[] context, int length, String key_description) throws InvalidKeyException {
        return HKDF.expand(mac, key,context,length,key_description);
    }

    @Override
    public Mac build(String algorithm_name) {
        return HKDF.build(algorithm_name);
    }
}
