//package com.gypsyengineer.tlsbunny.jsse;

package com.nokia.as.tlsexport;

import com.nokia.as.service.tlsexport.TlsExportService;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLSession;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;

import com.nokia.as.service.hkdf.HkdfService;

@Component
public class TlsExportServiceImpl implements TlsExportService {

    private final static String can_t_find_implementation= "System is not properly configured could not find implementation.";
    private final Logger log = LoggerFactory.getLogger(TlsExportService.class);

    @ServiceDependency
    static private HkdfService hkdf_service;

    private final boolean rfc7627_5_4_check_enable = Boolean.parseBoolean(
                System.getProperty("com.nokia.as.tlsexport.rfc7627_5.4", "true")
        );


    private final boolean force_null_context_flag = Boolean.parseBoolean(
                System.getProperty("com.nokia.as.tlsexport.force_null_context_flag", "false")
        );

    {
        if (log.isTraceEnabled()) {
            log.trace( "com.nokia.as.tlsexport.rfc7627_5.4:" +
                    System.getProperty("com.nokia.as.tlsexport.rfc7627_5.4", "true")
                    + " / " + rfc7627_5_4_check_enable
            );
            log.trace( "com.nokia.as.tlsexport.force_null_context_flag:" +
                    System.getProperty("com.nokia.as.tlsexport.force_null_context_flag", "false")
                    + " / " + force_null_context_flag
            );
        }
    }



    @Override
    public Map<String, Object> exportKey(SSLEngine engine, String asciiLabel, byte[] context_value, int length) {
        Objects.requireNonNull(engine, "Engine can't be null");
        Objects.requireNonNull(asciiLabel, "label can't be null");
        SSLEngineResult.HandshakeStatus status = engine.getHandshakeStatus();
        log.trace("entering exportKey(label: {}, length: {}, status: {})", asciiLabel, length, status);
        if (status != FINISHED && status != NOT_HANDSHAKING )
            throw new IllegalStateException("Handshake is not finished : "+status);
        SSLSession session = engine.getSession();

        return exportKey(session, asciiLabel, context_value, length);
    }


    public Map<String, Object> exportKey(SSLSession session, String asciiLabel, byte[] context_value, int length) {
        String version = session.getProtocol();
        String ciphersuite = session.getCipherSuite();

        byte[] exportedKey;
        switch (version) {
            case "TLSv1.3":
                byte[] context;
                String hash_choice;
                if (ciphersuite.endsWith("_SHA256")) {
                    hash_choice = "SHA-256";
                    context=sha256_null_digest;
                } else if (ciphersuite != null && ciphersuite.endsWith("_SHA384")){
                    hash_choice = "SHA-384";
                    context=sha384_null_digest;
                } else {
                    throw new IllegalArgumentException("unknown algorithms "+ciphersuite);
                }
                final Mac mac = hkdf_service.build(hash_choice);

                if (context_value == null || context_value.length == 0 )
                    context_value = context;
                else {
                    throw new IllegalStateException("non empty context not supported!");
                }
                exportedKey = tls13(session,mac,asciiLabel,context_value,length);
                break;
            case "TLSv1.2":
                exportedKey = tls12(session,asciiLabel,context_value,length);
                break;
            default:
                throw new IllegalStateException("unsupported TLS version: " + version);
        }
        Map<String, Object> map = new HashMap<>(3);
        map.put("tcp.secure.keyexport.master_secret", exportedKey);
        map.put("tcp.secure.keyexport.version", version);
        map.put("tcp.secure.keyexport.algos", ciphersuite);
        log.debug("exportKey(session: {}, label: {}, length: {}, version: {})={}",
                session, asciiLabel, length, version,( exportedKey==null? "null":new java.math.BigInteger(1,exportedKey).toString(16)));
        return map;
	/*
	if (session instanceof sun.security.ssl.SSLSessionImpl ) {
		Object secret=((sun.security.ssl.SSLSessionImpl)session).getMasterSecret();
		if (secret instanceof com.sun.crypto.provider.TLSMasterSecretKeyExportable)
			return ((com.sun.crypto.provider.TLSMasterSecretKeyExportable)secret).export(asciiLabel, context_value, length);
		else
			throw new IllegalStateException("Not TLSMasterSecretKeyExportable");
	}
	throw new IllegalStateException("Not sun.security.ssl.SSLSessionImpl");
	*/
    }

    private byte[] tls12(SSLSession session, String asciiLabel, byte[] context_value, int length) {
        try {
            Method gms = session.getClass().getDeclaredMethod("getMasterSecret");
            gms.setAccessible(true);
            Object secret = gms.invoke(session);
            Field rfc = secret.getClass().getDeclaredField("rfc7627_5_4");
            rfc.setAccessible(true);
            boolean rfc_flag = (Boolean)rfc.get(secret);
            if (!rfc_flag && rfc7627_5_4_check_enable)
                throw new IllegalStateException("c.f. RFC7627 paragraph 5.4 no extension negociated, this check can be disabled but is a security " +
                        "risk. ");

            if (force_null_context_flag)
               context_value = null;
            Method export = secret.getClass().getDeclaredMethod("export",String.class,byte[].class,int.class);
            export.setAccessible(true);
            byte[] exportedKey = (byte[]) export.invoke(secret, asciiLabel, context_value, length);
            return exportedKey;
        } catch (NoSuchFieldException nsfe) {
            throw new IllegalStateException(can_t_find_implementation,nsfe);
        } catch (NoSuchMethodException nsme) {
            throw new IllegalStateException(can_t_find_implementation,nsme);
        } catch (InvocationTargetException ite) {
            throw new IllegalStateException("exception raised",ite.getCause());
        } catch (IllegalAccessException iae) {
            throw new IllegalStateException("do not have necessary access rights",iae);
        }

    }

    private byte[] tls13(SSLSession session, Mac mac, String asciiLabel, byte[] context_value, int length) {
        try {
            Method gms = session.getClass().getDeclaredMethod("getExportKey");
            gms.setAccessible(true);
            SecretKey secretKey = ((SecretKey) gms.invoke(session));
            if (secretKey == null) {
                log.debug("secretKey is null for session: {}. label: {} length: {}.", session, asciiLabel, length);
            }
            byte[] secret = secretKey.getEncoded();
//	    return secret;
            return expand_tls13(secret,mac, length, context_value, asciiLabel);
	    /*
            Method export = secret.getClass().getDeclaredMethod("export",String.class,byte[].class,int.class);
            export.setAccessible(true);
            byte[] exportedKey = (byte[]) export.invoke(secret, asciiLabel, context_value, length);
            return exportedKey;
	    */
        } catch (NoSuchMethodException nsme) {
            throw new IllegalStateException(can_t_find_implementation,nsme);
        } catch (InvocationTargetException ite) {
            throw new IllegalStateException("exception raised",ite.getCause());
        } catch (IllegalAccessException iae) {
            throw new IllegalStateException("do not have necessary access rights",iae);
        }

    }

    static private byte[] getLabelsAsBytes(Object[] labels) {
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
        return info;
    }

    public static byte[] expand_tls13(byte [] master_secret, Mac mac, int length, byte[] context, Object ... labels) {
        byte[] label = getLabelsAsBytes(labels);
        byte[] label_rfc = create_rfc_info(label, context, mac.getMacLength());

        try {
            return hkdf_service.expand(mac,
                    hkdf_service.expand(mac,master_secret, label_rfc, mac.getMacLength(), "").getEncoded(),
                    create_rfc_info("exporter".getBytes(), context, length), length, ""
            ).getEncoded();
        } catch (InvalidKeyException ike) {
            throw new IllegalStateException("unexpected exception",ike);
        }
    }

    static private byte[] create_rfc_info(byte[] label, byte[] context, int length) {
        final int context_length = context ==null? 0 : context.length;
        byte[]     result = new byte[4 + label.length + context_length + 6];
        ByteBuffer m      = ByteBuffer.wrap(result);
        m.put((byte)((length >> 8) & 0xFF));
        m.put((byte)(length & 0xFF));
        if (label == null || label.length == 0) {
            m.put((byte)(0));
        } else {
            m.put((byte)((label.length+6) & 0xFF));
            m.put(tls13_bytes);
            m.put(label);
        }
        if (context == null || context_length == 0) {
            m.put((byte)(0));
        } else {
            m.put((byte)(context_length & 0xFF));
            m.put(context);
        }
        // manque hash de zero ...

        return result;
    }

    private static final byte[] tls13_bytes = "tls13 ".getBytes();

    private static final byte[] sha256_null_digest = new byte[] {
            (byte)0xE3, (byte)0xB0, (byte)0xC4, (byte)0x42, (byte)0x98, (byte)0xFC, (byte)0x1C, (byte)0x14,
            (byte)0x9A, (byte)0xFB, (byte)0xF4, (byte)0xC8, (byte)0x99, (byte)0x6F, (byte)0xB9, (byte)0x24,
            (byte)0x27, (byte)0xAE, (byte)0x41, (byte)0xE4, (byte)0x64, (byte)0x9B, (byte)0x93, (byte)0x4C,
            (byte)0xA4, (byte)0x95, (byte)0x99, (byte)0x1B, (byte)0x78, (byte)0x52, (byte)0xB8, (byte)0x55
    };

    private static final byte[] sha384_null_digest = new byte[] {
            (byte)0x38, (byte)0xB0, (byte)0x60, (byte)0xA7, (byte)0x51, (byte)0xAC, (byte)0x96, (byte)0x38,
            (byte)0x4C, (byte)0xD9, (byte)0x32, (byte)0x7E, (byte)0xB1, (byte)0xB1, (byte)0xE3, (byte)0x6A,
            (byte)0x21, (byte)0xFD, (byte)0xB7, (byte)0x11, (byte)0x14, (byte)0xBE, (byte)0x07, (byte)0x43,
            (byte)0x4C, (byte)0x0C, (byte)0xC7, (byte)0xBF, (byte)0x63, (byte)0xF6, (byte)0xE1, (byte)0xDA,
            (byte)0x27, (byte)0x4E, (byte)0xDE, (byte)0xBF, (byte)0xE7, (byte)0x6F, (byte)0x65, (byte)0xFB,
            (byte)0xD5, (byte)0x1A, (byte)0xD2, (byte)0xF1, (byte)0x48, (byte)0x98, (byte)0xB9, (byte)0x5B
    };

}

