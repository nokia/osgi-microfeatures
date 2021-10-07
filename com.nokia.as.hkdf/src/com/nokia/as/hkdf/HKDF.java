package com.nokia.as.hkdf;

import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import java.util.Objects;

/**
 * RFC 5869
 */
public class HKDF {

    static public Mac build(String algorithm_name) {
        Objects.requireNonNull(algorithm_name, "HKDF is a meta algorithm, an underlying algorithm shall be specified.");
        String algorithm_name_;
        Mac hash_instance;
        algorithm_name_ = "Hmac" + algorithm_name.replace("-", "");
        try {
            hash_instance = Mac.getInstance(algorithm_name_);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("unknown algorithm",e);
        }
        return hash_instance;
    }

    /**
     * key derivation.
     *
     * @param key key input
     * @param context context input
     * @param length output length of key derivation
     * @param key_description the description used to create the output SecretKey.
     *
     * @return derivated key as a {@code SecretKey} object
     * @throws InvalidKeyException if the hash algorithm can not be initialized with given {@code key}.
     */
    static public SecretKey expand(Mac hash_instance, byte[] key, byte[] context, int length, String key_description) throws InvalidKeyException {
        final int hash_length= hash_instance.getMacLength();
        Objects.requireNonNull(key, "key must be provided.");
        if (length > 255*hash_length) {
            throw new IllegalArgumentException("exeeds algorithm capability.");
        }
        if (context==null) context = new byte[0];
        hash_instance.init(new SecretKeySpec(key,"HKDF-PRK"));

        // perform ceil by adding hash_length - 1
        int n = (hash_length - 1 + length) / hash_length;
        byte[] result = new byte[n * hash_length];
        int offset = 0;
        int length_of_T = 0;

        for (int round=1; round<=n; round++) {

            try {
                hash_instance.update(result, Math.max(0, offset - hash_length), length_of_T);
                hash_instance.update(context);
                hash_instance.update((byte)(round));
                hash_instance.doFinal(result, offset);

                length_of_T = hash_length;
                offset += hash_length;
            } catch (ShortBufferException e) {
                throw new RuntimeException(e);
            }
        }
        return new SecretKeySpec(result, 0, length, key_description);
    }
}

